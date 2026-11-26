package org.comichao.netresult

import org.comichao.netresult.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Doubles, the details of a match, and how both travel through export and import. */
class DoublesAndDetailsTest {
    private val singles = Game(1, "2026-11-20", "Anna", "Win", "6-4,6-3", "good serve")
    private val doubles = GameInfo(
        date = "2026-11-22", opponent = "Anna", opponent2 = "Bob", doubles = true, partner = "Marco", notes = "fun",
        surface = "clay", setting = "outdoor", startTime = "18:30", duration = 95, weather = "sunny", temp = 22, venue = "Central courts, court 3",
    ).toGame(listOf("6-4", "3-6", "10-8*"), id = 2)

    @Test fun anOldGameIsASinglesMatchWithNoDetails() {
        assertFalse(singles.isDoubles)
        assertEquals("Anna", singles.opponentLabel)
        assertNull(singles.surface)
    }

    @Test fun doublesNamesBothOpponents() {
        assertTrue(doubles.isDoubles)
        assertEquals("Anna / Bob", doubles.opponentLabel)
        assertEquals("Marco", doubles.partner)
        assertEquals("Win", doubles.result)
    }

    @Test fun theSamePairIsOneTeamInEitherOrder() {
        val swapped = doubles.copy(opponent = "Bob", opponent2 = "Anna")
        assertEquals(doubles.teamKey, swapped.teamKey)
        assertEquals("Anna", singles.teamKey)
    }

    @Test fun filteringByOpponentFindsBothOpponentsOfADoublesMatch() {
        assertTrue(doubles.playedAgainst("Anna"))
        assertTrue(doubles.playedAgainst("Bob"))
        assertFalse(doubles.playedAgainst("Marco"))   // the partner is not an opponent
        assertFalse(singles.playedAgainst("Bob"))
    }

    @Test fun singlesDropsDoublesNamesAndUnknownDetailValues() {
        val g = GameInfo("2026-11-22", "Anna", doubles = false, partner = "Marco", opponent2 = "Bob", surface = "ice", weather = "snow").toGame(listOf("6-0"))
        assertNull(g.mode); assertNull(g.partner); assertNull(g.opponent2)
        assertNull(g.surface); assertNull(g.weather)
    }

    @Test fun csvRoundTripKeepsEverything() {
        val back = parseGamesCsv(gamesToCsv(listOf(singles, doubles)))
        assertEquals(2, back.size)
        val g = back[1].toGame()!!
        assertEquals(doubles.copy(id = 0), g)
        assertEquals("Central courts, court 3", g.venue)   // a comma in a value survives
    }

    @Test fun jsonRoundTripKeepsEverything() {
        val back = parseGamesJson(gamesToJson(listOf(doubles)))
        assertEquals(doubles.copy(id = 0), back.single().toGame()!!)
    }

    @Test fun aFileFromAnOlderVersionStillImports() {
        val old = "id,date,opponent,result,score,notes,created_at\n1,2026-11-20,Anna,Win,\"6-4,6-3\",good serve,\n"
        val g = parseGamesCsv(old).single().toGame()!!
        assertEquals("Anna", g.opponent)
        assertFalse(g.isDoubles)
        assertNull(g.surface); assertNull(g.duration)
    }

    @Test fun importingAgainDoesNotDuplicateEvenIfDetailsWereAddedLater() {
        val withDetails = singles.copy(surface = "hard", weather = "cloudy")
        assertEquals(singles.importKey(), withDetails.importKey())
        assertTrue(doubles.importKey() != doubles.copy(partner = "Luca").importKey())   // a different partner is a different match
    }

    @Test fun badImportedValuesAreDropped() {
        val g = ImportRow("2026-11-22", "Anna", "6-4,6-3", "", "Win", mode = "doubles", opponent2 = "Bob",
            surface = "Clay", startTime = "7pm", duration = "-5", weather = "Rain", temp = "x").toGame()!!
        assertEquals("clay", g.surface)        // case is forgiven
        assertEquals("rain", g.weather)
        assertNull(g.startTime); assertNull(g.duration); assertNull(g.temp)
        assertNull(ImportRow("", "Anna", "6-4", "", "Win").toGame())
    }

    @Test fun liveStateCarriesDoublesAndStaysReadableByOlderMessages() {
        val live = LiveState(rev = 1, opponent = "Anna", startedAt = 5, doubles = true, partner = "Marco", opponent2 = "Bob")
        val back = LiveState.fromJson(live.toJson())
        assertEquals("Anna / Bob", back.label)
        assertEquals("Marco", back.partner)
        val old = org.json.JSONObject(live.toJson()).apply { remove("doubles"); remove("partner"); remove("opp2") }.toString()
        assertEquals("Anna", LiveState.fromJson(old).label)
    }
}
