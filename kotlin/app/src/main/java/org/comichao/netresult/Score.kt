package org.comichao.netresult

/** Trailing marker on a set token flagging a tie-break set (e.g. "10-8*"). */
const val TIEBREAK_MARK = "*"

data class SetScore(val player: Int, val opponent: Int, val tiebreak: Boolean = false)

enum class Result { Win, Loss, Draw }

/** Parse one set token such as "6-4" or "10-8*". Throws [IllegalArgumentException] if malformed. */
fun parseSet(token: String): SetScore {
    var tok = token.trim()
    val tb = tok.endsWith(TIEBREAK_MARK)
    if (tb) tok = tok.dropLast(TIEBREAK_MARK.length)
    val parts = tok.split("-")
    require(parts.size == 2) { "Malformed set score: '$token'. Expected 'p-o' e.g. '6-4'." }
    val p = parts[0].trim().toIntOrNull()
    val o = parts[1].trim().toIntOrNull()
    require(p != null && o != null) { "Malformed set score: '$token'. Expected 'p-o' e.g. '6-4'." }
    return SetScore(p, o, tb)
}

fun formatSet(player: Int, opponent: Int, tiebreak: Boolean = false): String =
    "$player-$opponent" + if (tiebreak) TIEBREAK_MARK else ""

fun computeResult(sets: List<String>): Result {
    var p = 0
    var o = 0
    for (s in sets) {
        val set = parseSet(s)
        if (set.player > set.opponent) p++ else if (set.opponent > set.player) o++
    }
    return when {
        p > o -> Result.Win
        o > p -> Result.Loss
        else -> Result.Draw
    }
}

fun setsToScore(sets: List<String>): String = sets.joinToString(",")

fun scoreToSets(score: String?): List<String> =
    if (score.isNullOrBlank()) emptyList() else score.split(",").map { it.trim() }.filter { it.isNotEmpty() }

data class Stats(
    val total: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val setsWon: Int = 0,
    val setsLost: Int = 0,
) {
    val winPct: Int get() = if (total == 0) 0 else Math.round(wins * 100f / total)
    val setWinPct: Int
        get() = if (setsWon + setsLost == 0) 0 else Math.round(setsWon * 100f / (setsWon + setsLost))
}

/** Compute stats from stored (result, score) pairs. */
fun computeStats(games: List<org.comichao.netresult.data.Game>): Stats {
    var won = 0
    var lost = 0
    for (g in games) for (s in scoreToSets(g.score)) {
        val set = runCatching { parseSet(s) }.getOrNull() ?: continue
        if (set.player > set.opponent) won++ else if (set.opponent > set.player) lost++
    }
    val wins = games.count { it.result == Result.Win.name }
    val draws = games.count { it.result == Result.Draw.name }
    return Stats(games.size, wins, draws, games.size - wins - draws, won, lost)
}
