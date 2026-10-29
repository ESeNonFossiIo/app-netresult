package org.comichao.netresult

/** Trailing marker on a set token flagging a tie-break set (e.g. "10-8*"). */
const val TIEBREAK_MARK = "*"

data class SetScore(val player: Int, val opponent: Int, val tiebreak: Boolean = false)

fun formatSet(player: Int, opponent: Int, tiebreak: Boolean = false): String =
    "$player-$opponent" + if (tiebreak) TIEBREAK_MARK else ""
