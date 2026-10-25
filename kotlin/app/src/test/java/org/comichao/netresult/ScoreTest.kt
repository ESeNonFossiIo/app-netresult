package org.comichao.netresult

import org.junit.Assert.*
import org.junit.Test

class ScoreTest {
    @Test fun parseSetPlainAndTiebreak() {
        assertEquals(SetScore(6, 4, false), parseSet("6-4"))
        assertEquals(SetScore(10, 8, true), parseSet("10-8*"))
        assertEquals(SetScore(7, 6, false), parseSet(" 7-6 "))
    }

    @Test fun parseSetMalformed() {
        for (bad in listOf("", "6", "a-b", "6-4-2", "-")) assertThrows(IllegalArgumentException::class.java) { parseSet(bad) }
    }

    @Test fun formatSetRoundTrip() {
        assertEquals("6-4", formatSet(6, 4))
        assertEquals("10-8*", formatSet(10, 8, true))
        assertEquals(SetScore(10, 8, true), parseSet(formatSet(10, 8, true)))
    }

    @Test fun resultFromSets() {
        assertEquals(Result.Win, computeResult(listOf("6-4", "3-6", "7-5")))
        assertEquals(Result.Loss, computeResult(listOf("4-6", "6-3", "5-7")))
        assertEquals(Result.Draw, computeResult(listOf("6-4", "4-6")))
        assertEquals(Result.Win, computeResult(listOf("6-4", "3-6", "10-8*")))
        assertThrows(IllegalArgumentException::class.java) { computeResult(listOf("x")) }
    }

    @Test fun scoreSerialisation() {
        assertEquals("6-4,3-6,10-8*", setsToScore(listOf("6-4", "3-6", "10-8*")))
        assertEquals(listOf("6-4", "3-6"), scoreToSets("6-4, 3-6"))
        assertTrue(scoreToSets("").isEmpty())
        assertTrue(scoreToSets(null).isEmpty())
    }

    @Test fun statsCountTiebreakSets() {
        val g = { r: String, s: String -> org.comichao.netresult.data.Game(date = "2026-01-01", opponent = "A", result = r, score = s) }
        val st = computeStats(listOf(g("Win", "6-4,3-6,10-8*"), g("Loss", "4-6,6-7"), g("Draw", "")))
        assertEquals(Stats(total = 3, wins = 1, draws = 1, losses = 1, setsWon = 2, setsLost = 3), st)
        assertEquals(33, st.winPct)
    }

    @Test fun csvParsing() {
        val rows = parseCsvForTest("a,b\n\"x,1\",\"he said \"\"hi\"\"\"\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("x,1", "he said \"hi\"")), rows)
    }
}

private fun parseCsvForTest(s: String) = org.comichao.netresult.data.parseCsv(s)
