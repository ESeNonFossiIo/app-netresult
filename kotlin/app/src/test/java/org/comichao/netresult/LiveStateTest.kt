package org.comichao.netresult

import org.junit.Assert.*
import org.junit.Test

class LiveStateTest {
    private fun start() = LiveState(rev = 1000, opponent = "Marco", startedAt = 1000, watch = true)

    @Test fun roundTripsWithSetsPointsAndHistory() {
        var s = start()
        repeat(30) { s = s.point(it % 3 != 0) }          // enough points to finish a few games and a set
        val back = LiveState.fromJson(s.toJson())
        assertEquals(s, back)
        assertTrue(back.history.isNotEmpty())
    }

    @Test fun undoRestoresThePreviousState() {
        val s = start().point(true).point(false)
        val u = s.undo()
        assertEquals(1, u.history.size)
        assertEquals("15", u.match.pointLabel(true)); assertEquals("0", u.match.pointLabel(false))
    }

    @Test fun revAlwaysIncreasesSoNewerWins() {
        val a = start().point(true)
        val b = a.point(true)
        assertTrue(b.rev > a.rev && a.rev > 1000)
    }

    @Test fun undoWithNothingToUndoChangesNothing() = assertEquals(start(), start().undo())
}
