package org.comichao.netresult

import org.json.JSONArray
import org.json.JSONObject

/** Message path the watch sends finished sessions to. */
const val SESSION_PATH = "/netresult/session"

/**
 * A finished watch session. A [MATCH] has set scores; a [WARMUP] only has vitals ([sets] is empty).
 * [heartRate] is (epoch millis, bpm) samples, [kcal] the active energy, both null/empty when unavailable.
 */
data class WatchSession(
    val kind: String,
    val opponent: String,
    val sets: List<String>,
    val startMillis: Long,
    val endMillis: Long,
    val heartRate: List<Pair<Long, Int>> = emptyList(),
    val kcal: Double? = null,
) {
    fun toJson(): String = JSONObject()
        .put("kind", kind).put("opponent", opponent).put("sets", JSONArray(sets))
        .put("start", startMillis).put("end", endMillis)
        .put("hr", JSONArray(heartRate.map { JSONArray(listOf(it.first, it.second)) }))
        .apply { kcal?.let { put("kcal", it) } }
        .toString()

    companion object {
        const val MATCH = "match"
        const val WARMUP = "warmup"

        fun fromJson(text: String): WatchSession = JSONObject(text).let { o ->
            WatchSession(
                kind = o.getString("kind"), opponent = o.optString("opponent"),
                sets = o.getJSONArray("sets").let { a -> (0 until a.length()).map(a::getString) },
                startMillis = o.getLong("start"), endMillis = o.getLong("end"),
                heartRate = o.getJSONArray("hr").let { a -> (0 until a.length()).map { a.getJSONArray(it).let { p -> p.getLong(0) to p.getInt(1) } } },
                kcal = if (o.has("kcal")) o.getDouble("kcal") else null,
            )
        }
    }
}
