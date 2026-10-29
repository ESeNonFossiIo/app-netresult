package org.comichao.netresult

/** Sets needed to win a live match (best of three). */
const val SETS_TO_WIN = 2

/**
 * Immutable state of a match being scored point by point. Standard tennis rules: deuce with advantage,
 * a tie-break to 7 (win by two) at 6-6 in every set, best of three sets.
 */
data class LiveMatch(
    val sets: List<SetScore> = emptyList(),
    val gamesMe: Int = 0,
    val gamesOpp: Int = 0,
    val pointsMe: Int = 0,
    val pointsOpp: Int = 0,
) {
    val inTiebreak get() = gamesMe == 6 && gamesOpp == 6
    val setsMe get() = sets.count { it.player > it.opponent }
    val setsOpp get() = sets.count { it.opponent > it.player }
    val over get() = setsMe == SETS_TO_WIN || setsOpp == SETS_TO_WIN
    val started get() = sets.isNotEmpty() || gamesMe + gamesOpp + pointsMe + pointsOpp > 0

    /** Score the next point for the player ([me] = true) or the opponent. */
    fun point(me: Boolean): LiveMatch {
        if (over) return this
        val m = pointsMe + if (me) 1 else 0
        val o = pointsOpp + if (me) 0 else 1
        val target = if (inTiebreak) 7 else 4
        val gameWon = maxOf(m, o) >= target && kotlin.math.abs(m - o) >= 2
        if (!gameWon) return copy(pointsMe = m, pointsOpp = o)

        val gm = gamesMe + if (me) 1 else 0
        val go = gamesOpp + if (me) 0 else 1
        val setWon = (maxOf(gm, go) >= 6 && kotlin.math.abs(gm - go) >= 2) || maxOf(gm, go) == 7
        return if (setWon) copy(sets = sets + SetScore(gm, go), gamesMe = 0, gamesOpp = 0, pointsMe = 0, pointsOpp = 0)
        else copy(gamesMe = gm, gamesOpp = go, pointsMe = 0, pointsOpp = 0)
    }

    /** Text for one side's current game score: 0/15/30/40/Ad, or the plain count in a tie-break. */
    fun pointLabel(me: Boolean): String {
        val mine = if (me) pointsMe else pointsOpp
        val theirs = if (me) pointsOpp else pointsMe
        if (inTiebreak) return mine.toString()
        return when {
            mine >= 3 && theirs >= 3 -> if (mine == theirs) "40" else if (mine > theirs) "Ad" else "40"
            else -> listOf("0", "15", "30", "40").getOrElse(mine) { "40" }
        }
    }

    /** Set tokens to store: finished sets plus the set in progress, if any game was played in it. */
    fun tokens(): List<String> =
        (sets + listOfNotNull(SetScore(gamesMe, gamesOpp).takeIf { gamesMe + gamesOpp > 0 }))
            .map { formatSet(it.player, it.opponent) }
}
