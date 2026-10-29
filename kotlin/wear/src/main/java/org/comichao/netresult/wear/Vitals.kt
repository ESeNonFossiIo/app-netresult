package org.comichao.netresult.wear

import android.content.Context
import android.util.Log
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import kotlinx.coroutines.guava.await
import kotlin.math.roundToInt

/** Runs a Health Services tennis exercise and reports heart rate and calories while it lasts. */
class Vitals(context: Context, private val onHeartRate: (Int) -> Unit, private val onCalories: (Double) -> Unit) {
    private val client = HealthServices.getClient(context).exerciseClient

    private val callback = object : ExerciseUpdateCallback {
        override fun onRegistered() {}
        override fun onRegistrationFailed(throwable: Throwable) {}
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let { onHeartRate(it.value.roundToInt()) }
            update.latestMetrics.getData(DataType.CALORIES_TOTAL)?.let { onCalories(it.total) }
        }
        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
    }

    /** Returns false if the sensors or the permission are not available; scoring still works without them. */
    suspend fun start(): Boolean = runCatching {
        client.setUpdateCallback(callback)
        client.startExerciseAsync(
            ExerciseConfig.builder(ExerciseType.TENNIS)
                .setDataTypes(setOf(DataType.HEART_RATE_BPM, DataType.CALORIES_TOTAL))
                .setIsAutoPauseAndResumeEnabled(false)
                .setIsGpsEnabled(false)
                .build()
        ).await()
    }.onFailure { Log.w("Vitals", "Could not start the exercise: $it") }.isSuccess

    suspend fun stop() {
        runCatching { client.endExerciseAsync().await() }
        runCatching { client.clearUpdateCallbackAsync(callback).await() }
    }
}
