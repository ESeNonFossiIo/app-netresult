package org.comichao.netresult.health

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.units.Energy
import java.time.Instant
import java.time.ZoneId

/**
 * Writes sessions to Health Connect as tennis exercise (with heart rate and energy when the watch sent them);
 * Google Fit and other fitness apps read them from there (the Google Fit APIs are being retired in favour of
 * Health Connect). Needs Android 8+.
 */
@RequiresApi(26)
object HealthSync {
    val permissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
    )

    fun supported() = Build.VERSION.SDK_INT >= 26

    fun available(context: Context) =
        supported() && HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun granted(context: Context) =
        available(context) && HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(permissions)

    suspend fun writeSession(
        context: Context, title: String, startMillis: Long, endMillis: Long,
        heartRate: List<Pair<Long, Int>> = emptyList(), kcal: Double? = null,
    ) {
        val start = Instant.ofEpochMilli(startMillis)
        val end = Instant.ofEpochMilli(maxOf(endMillis, startMillis + 1000))
        val zone = ZoneId.systemDefault().rules
        val records = mutableListOf<Record>(ExerciseSessionRecord(
            startTime = start, startZoneOffset = zone.getOffset(start), endTime = end, endZoneOffset = zone.getOffset(end),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_TENNIS, title = title,
        ))
        val samples = heartRate.filter { it.first in startMillis..endMillis && it.second > 0 }
            .map { HeartRateRecord.Sample(Instant.ofEpochMilli(it.first), it.second.toLong()) }
        if (samples.isNotEmpty()) records += HeartRateRecord(
            startTime = start, startZoneOffset = zone.getOffset(start), endTime = end, endZoneOffset = zone.getOffset(end), samples = samples)
        if (kcal != null && kcal > 0) records += ActiveCaloriesBurnedRecord(
            startTime = start, startZoneOffset = zone.getOffset(start), endTime = end, endZoneOffset = zone.getOffset(end),
            energy = Energy.kilocalories(kcal))
        HealthConnectClient.getOrCreate(context).insertRecords(records)
    }
}
