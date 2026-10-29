package org.comichao.netresult

import org.json.JSONArray
import org.json.JSONObject

/** Data Layer message paths for mirroring a running match between phone and watch. */
const val LIVE_PATH = "/netresult/live"
const val LIVE_REQUEST_PATH = "/netresult/live/request"
const val LIVE_END_PATH = "/netresult/live/end"

/**
 * A match in progress, as both devices see it. [rev] is a timestamp-like counter: the side with the higher one
 * wins, so a stale or echoed message never overwrites a newer score. [watch] is true while the watch is
 * recording the workout (so the phone does not also write a vitals-less session to Health Connect).
 */
data class LiveState(
    val rev: Long,
    val opponent: String,
    val startedAt: Long,
    val match: LiveMatch = LiveMatch(),
    val history: List<LiveMatch> = emptyList(),
    val watch: Boolean = false,
    val health: Boolean = false,
) {
    fun point(me: Boolean) = copy(rev = nextRev(), match = match.point(me), history = history + match)
    fun undo() = if (history.isEmpty()) this else copy(rev = nextRev(), match = history.last(), history = history.dropLast(1))
    fun withWatch() = copy(rev = nextRev(), watch = true)
    private fun nextRev() = maxOf(rev + 1, System.currentTimeMillis())

    fun toJson(): String = JSONObject()
        .put("rev", rev).put("opp", opponent).put("start", startedAt).put("watch", watch).put("health", health)
        .put("match", match.toJson()).put("history", JSONArray(history.map { it.toJson() }))
        .toString()

    companion object {
        fun fromJson(text: String): LiveState = JSONObject(text).let { o ->
            LiveState(o.getLong("rev"), o.getString("opp"), o.getLong("start"), matchFromJson(o.getJSONObject("match")),
                o.getJSONArray("history").let { a -> (0 until a.length()).map { matchFromJson(a.getJSONObject(it)) } },
                o.optBoolean("watch"), o.optBoolean("health"))
        }
    }
}

private fun LiveMatch.toJson() = JSONObject()
    .put("sets", sets.joinToString(",") { "${it.player}-${it.opponent}" })
    .put("gm", gamesMe).put("go", gamesOpp).put("pm", pointsMe).put("po", pointsOpp)

private fun matchFromJson(o: JSONObject) = LiveMatch(
    sets = o.getString("sets").split(",").filter { it.isNotBlank() }.map { t -> t.split("-").let { SetScore(it[0].toInt(), it[1].toInt()) } },
    gamesMe = o.getInt("gm"), gamesOpp = o.getInt("go"), pointsMe = o.getInt("pm"), pointsOpp = o.getInt("po"),
)
