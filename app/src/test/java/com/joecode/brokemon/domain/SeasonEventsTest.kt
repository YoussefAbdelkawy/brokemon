package com.joecode.brokemon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SeasonEventsTest {
    @Test
    fun `ramadan and eid follow the hijri calendar`() {
        // Ramadan 1447 AH runs roughly Feb 18 – Mar 19, 2026 (Umm al-Qura).
        assertEquals(SeasonEvent.RAMADAN, SeasonEvents.active(LocalDate.of(2026, 3, 1)))
        assertEquals(SeasonEvent.EID, SeasonEvents.active(LocalDate.of(2026, 3, 20)))
        // Eid al-Adha 1447 ≈ May 27, 2026; it beats exam season.
        assertEquals(SeasonEvent.EID, SeasonEvents.active(LocalDate.of(2026, 5, 27)))
    }

    @Test
    fun `calendar events`() {
        assertEquals(SeasonEvent.NEW_YEAR, SeasonEvents.active(LocalDate.of(2026, 12, 25)))
        assertEquals(SeasonEvent.NEW_YEAR, SeasonEvents.active(LocalDate.of(2027, 1, 10)))
        assertEquals(SeasonEvent.SUMMER, SeasonEvents.active(LocalDate.of(2026, 7, 15)))
        assertEquals(SeasonEvent.EXAMS, SeasonEvents.active(LocalDate.of(2026, 6, 10)))
        assertNull(SeasonEvents.active(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun `stamps round trip and new year counts toward the new year`() {
        assertEquals("NEW_YEAR|2027", SeasonEvents.stamp(SeasonEvent.NEW_YEAR, LocalDate.of(2026, 12, 31)))
        assertEquals("NEW_YEAR|2027", SeasonEvents.stamp(SeasonEvent.NEW_YEAR, LocalDate.of(2027, 1, 2)))
        val stamp = SeasonEvents.parse("RAMADAN|2027")!!
        assertEquals(SeasonEvent.RAMADAN, stamp.event)
        assertEquals("RAMADAN 2027", stamp.label)
        assertNull(SeasonEvents.parse("NOPE|2027"))
        assertNull(SeasonEvents.parse("RAMADAN"))
        assertNull(SeasonEvents.parse(null))
    }

    @Test
    fun `pixel icons are rectangular`() {
        EventIcon.entries.forEach { icon ->
            val rows = PixelIcons.rows(icon)
            rows.forEach { assertEquals("$icon", rows[0].length, it.length) }
        }
    }
}
