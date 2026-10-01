package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay

class RemindersTest {
    @Test
    fun `birthday countdown wraps around the year`() {
        val today = LocalDate.of(2026, 10, 1)
        assertEquals(0, Birthdays.daysUntil(MonthDay.of(10, 1), today))
        assertEquals(3, Birthdays.daysUntil(MonthDay.of(10, 4), today))
        assertEquals(364, Birthdays.daysUntil(MonthDay.of(9, 30), today))
    }

    @Test
    fun `leap day birthdays land on feb 28 in normal years`() {
        assertEquals(0, Birthdays.daysUntil(MonthDay.of(2, 29), LocalDate.of(2027, 2, 28)))
        assertEquals(0, Birthdays.daysUntil(MonthDay.of(2, 29), LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun `birthday is read from the dated fact`() {
        val bro = testBro().copy(
            facts = listOf(
                Fact(category = "Birthday", value = "whenever"),
                Fact(category = "Birthday", value = "May 4", monthDay = Birthdays.toStorage(MonthDay.of(5, 4))),
            ),
        )
        assertEquals(MonthDay.of(5, 4), Birthdays.of(bro))
        assertTrue(Birthdays.isToday(bro, LocalDate.of(2026, 5, 4)))
        assertFalse(Birthdays.isToday(bro, LocalDate.of(2026, 5, 5)))
        assertNull(Birthdays.parse("garbage"))
    }

    @Test
    fun `birthday reminders fire once a day after 9am`() {
        val morning = LocalDateTime.of(2026, 5, 4, 8, 0)
        val later = LocalDateTime.of(2026, 5, 4, 9, 30)
        assertFalse(ReminderSchedule.birthdaysDue(morning, null))
        assertTrue(ReminderSchedule.birthdaysDue(later, null))
        assertFalse(ReminderSchedule.birthdaysDue(later, later.toLocalDate().toEpochDay()))
    }

    @Test
    fun `weekly nudge is sunday evening only, once`() {
        val sundayEvening = LocalDateTime.of(2026, 10, 4, 18, 0) // a Sunday
        val sundayMorning = LocalDateTime.of(2026, 10, 4, 10, 0)
        val mondayEvening = LocalDateTime.of(2026, 10, 5, 18, 0)
        assertTrue(ReminderSchedule.nudgeDue(sundayEvening, null))
        assertFalse(ReminderSchedule.nudgeDue(sundayMorning, null))
        assertFalse(ReminderSchedule.nudgeDue(mondayEvening, null))
        assertFalse(ReminderSchedule.nudgeDue(sundayEvening, sundayEvening.toLocalDate().toEpochDay()))
    }
}
