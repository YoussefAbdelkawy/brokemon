package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.MediaType
import com.joecode.brokemon.data.model.Memory
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WrappedTest {
    private fun millis(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `summarizes a year`() {
        val a = testBro(1, "A", BroType.GAMER, catchDate = millis(2025, 3, 1))
        val b = testBro(2, "B", BroType.GAMER, BroType.FOODIE, catchDate = millis(2025, 1, 5)).copy(rarity = Rarity.LEGENDARY)
        val c = testBro(3, "C", BroType.GYM_RAT, catchDate = millis(2024, 6, 1)).copy(
            memories = listOf(
                Memory(fileUri = "x", mediaType = MediaType.PHOTO, date = millis(2025, 7, 1)),
                Memory(fileUri = "y", mediaType = MediaType.PHOTO, date = millis(2025, 8, 1)),
            ),
        )
        val s = Wrapped.summarize(listOf(a, b, c), 2025)
        assertEquals(2, s.caughtCount)
        assertEquals(BroType.GAMER to 2, s.topTypes.first())
        assertEquals("B", s.firstBro?.name)
        assertEquals(listOf("B"), s.rarest.map { it.name })
        assertEquals("C" to 2, s.mostMemories?.let { it.first.name to it.second })
        assertEquals(2, s.memoriesMade)
    }

    @Test
    fun `wrapped gives you a title`() {
        val empty = Wrapped.summarize(emptyList(), 2025)
        assertEquals("The Quiet Year", Wrapped.title(empty))
        val shiny = testBro(1, catchDate = millis(2025, 2, 2)).copy(isShiny = true)
        assertEquals("Shiny Hunter", Wrapped.title(Wrapped.summarize(listOf(shiny), 2025)))
        val plain = testBro(1, catchDate = millis(2025, 2, 2))
        assertEquals("Day One Energy", Wrapped.title(Wrapped.summarize(listOf(plain), 2025)))
    }

    @Test
    fun `wrapped only shows around new year`() {
        assertEquals(2025, Wrapped.seasonYear(LocalDate.of(2025, 12, 20)))
        assertEquals(2025, Wrapped.seasonYear(LocalDate.of(2025, 12, 31)))
        assertEquals(2025, Wrapped.seasonYear(LocalDate.of(2026, 1, 1)))
        assertEquals(2025, Wrapped.seasonYear(LocalDate.of(2026, 1, 15)))
        assertNull(Wrapped.seasonYear(LocalDate.of(2026, 1, 16)))
        assertNull(Wrapped.seasonYear(LocalDate.of(2026, 10, 1)))
        assertNull(Wrapped.seasonYear(LocalDate.of(2025, 12, 19)))
    }
}
