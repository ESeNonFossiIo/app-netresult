package org.comichao.netresult

import org.junit.Assert.*
import org.junit.Test

class LiveMatchTest {
    private fun LiveMatch.points(me: Boolean, n: Int): LiveMatch = (1..n).fold(this) { m, _ -> m.point(me) }
    private fun LiveMatch.game(me: Boolean) = points(me, 4)

    @Test fun pointLabels() {
        var m = LiveMatch()
        assertEquals("0", m.pointLabel(true))
        m = m.point(true); assertEquals("15", m.pointLabel(true))
        m = m.point(true); assertEquals("30", m.pointLabel(true))
        m = m.point(true); assertEquals("40", m.pointLabel(true))
    }

    @Test fun deuceAndAdvantage() {
        var m = LiveMatch().points(true, 3).points(false, 3)
        assertEquals("40", m.pointLabel(true)); assertEquals("40", m.pointLabel(false))
        m = m.point(true)
        assertEquals("Ad", m.pointLabel(true)); assertEquals("40", m.pointLabel(false))
        m = m.point(false)
        assertEquals("40", m.pointLabel(true)); assertEquals("40", m.pointLabel(false))
        m = m.point(false).point(false)
        assertEquals(1, m.gamesOpp); assertEquals(0, m.pointsMe + m.pointsOpp)
    }

    @Test fun setWonSixLove() {
        val m = (1..6).fold(LiveMatch()) { m, _ -> m.game(true) }
        assertEquals(listOf(SetScore(6, 0)), m.sets)
        assertEquals(0, m.gamesMe)
    }

    @Test fun playsOnAtSixFive() {
        var m = LiveMatch()
        repeat(5) { m = m.game(true).game(false) }   // 5-5
        m = m.game(true)                              // 6-5
        assertTrue(m.sets.isEmpty())
        m = m.game(true)                              // 7-5
        assertEquals(listOf(SetScore(7, 5)), m.sets)
    }

    @Test fun tiebreakCountsNumericallyAndEndsSevenSix() {
        var m = LiveMatch()
        repeat(5) { m = m.game(true).game(false) }
        m = m.game(true).game(false)                  // 6-6
        assertTrue(m.inTiebreak)
        m = m.points(true, 3)
        assertEquals("3", m.pointLabel(true))
        m = m.points(false, 6).points(true, 3)        // 6-6 in the tie-break
        assertTrue(m.sets.isEmpty())
        m = m.points(true, 2)                         // 8-6
        assertEquals(listOf(SetScore(7, 6)), m.sets)
    }

    @Test fun matchEndsAtTwoSetsAndIgnoresFurtherPoints() {
        var m = LiveMatch()
        repeat(12) { m = m.game(true) }
        assertTrue(m.over); assertEquals(2, m.setsMe)
        assertEquals(m, m.point(false))
        assertEquals(listOf("6-0", "6-0"), m.tokens())
    }

    @Test fun tokensIncludeSetInProgress() {
        var m = LiveMatch()
        repeat(6) { m = m.game(true) }
        m = m.game(false).game(false)
        assertEquals(listOf("6-0", "0-2"), m.tokens())
        assertEquals(Result.Draw, computeResult(m.tokens()))
    }
}
