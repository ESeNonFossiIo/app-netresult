package org.comichao.netresult

import org.comichao.netresult.ui.allPlayers
import org.comichao.netresult.ui.distinctNames
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayersTest {
    @Test fun opponentsAndPartnersAreOneSortedListWithoutRepeats() {
        assertEquals(listOf("Anna", "bob", "Carla"), allPlayers(listOf("bob", "Anna"), listOf("Carla", "anna", "Bob ")))
    }

    @Test fun blankNamesAreLeftOut() {
        assertEquals(listOf("Anna"), allPlayers(listOf("", "Anna"), listOf(" ")))
    }

    @Test fun venuesAreListedOnceIgnoringCaseAndSpaces() {
        assertEquals(listOf("Club Roma", "park"), distinctNames(listOf("park", "Club Roma", "club roma ", "")))
    }

    @Test fun noPlayersIsEmpty() {
        assertEquals(emptyList<String>(), allPlayers(emptyList(), emptyList()))
    }
}
