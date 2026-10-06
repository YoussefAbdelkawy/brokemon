package com.joecode.brokemon.domain.battle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TournamentsTest {
    @Test
    fun `room codes skip look-alike characters`() {
        repeat(200) {
            val code = RoomCodes.generate(Random(it))
            assertTrue(RoomCodes.isValid(code))
            assertTrue(code.none { c -> c in "0O1IL" })
        }
        assertEquals("ABC234", RoomCodes.normalize(" abc-234 "))
    }

    @Test
    fun `bracket with byes plays out to one champion`() {
        val players = listOf("A", "B", "C", "D", "E")
        var m = Brackets.create(TournamentFormat.BRACKET, players)
        // Byes already advanced: three in round 0.
        assertEquals(3, m.count { it.round == 0 && it.winner != null })
        while (Brackets.champion(TournamentFormat.BRACKET, players, m) == null) {
            val next = m.first { it.ready }
            m = Brackets.record(m, next.round, next.index, next.a!!)
        }
        assertEquals(3, Brackets.rounds(m))
        assertTrue(Brackets.champion(TournamentFormat.BRACKET, players, m) in players)
    }

    @Test
    fun `round robin needs every match`() {
        val players = listOf("A", "B", "C", "D")
        var m = Brackets.create(TournamentFormat.ROUND_ROBIN, players)
        assertEquals(6, m.size)
        assertNull(Brackets.champion(TournamentFormat.ROUND_ROBIN, players, m))
        m.forEach { match -> m = Brackets.record(m, match.round, match.index, if ("A" in listOf(match.a, match.b)) "A" else match.a!!) }
        assertEquals("A", Brackets.champion(TournamentFormat.ROUND_ROBIN, players, m))
        assertEquals(3, Brackets.standings(players, m).first().wins)
    }
}
