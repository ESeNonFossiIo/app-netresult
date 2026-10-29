package org.comichao.netresult

import org.junit.Assert.*
import org.junit.Test

class WatchSessionTest {
    @Test fun matchRoundTrips() {
        val s = WatchSession(WatchSession.MATCH, "Marco", listOf("6-4", "3-6", "7-6"), 1000L, 9000L,
            listOf(2000L to 120, 3000L to 131), 87.5)
        assertEquals(s, WatchSession.fromJson(s.toJson()))
    }

    @Test fun warmupHasNoScoreAndNoVitalsIsFine() {
        val s = WatchSession(WatchSession.WARMUP, "", emptyList(), 1L, 2L)
        val back = WatchSession.fromJson(s.toJson())
        assertEquals(s, back)
        assertTrue(back.sets.isEmpty()); assertNull(back.kcal)
    }
}
