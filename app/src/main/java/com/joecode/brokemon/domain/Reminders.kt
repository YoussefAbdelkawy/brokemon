package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Birthdays {
    private val storage = DateTimeFormatter.ofPattern("MM-dd")

    fun toStorage(monthDay: MonthDay): String = monthDay.format(storage)

    fun parse(value: String?): MonthDay? = value?.let { runCatching { MonthDay.parse("--$it") }.getOrNull() }

    fun of(bro: Bro): MonthDay? = bro.facts.firstNotNullOfOrNull { parse(it.monthDay) }

    /** 0 = today. Feb 29 birthdays are celebrated on Feb 28 in non-leap years. */
    fun daysUntil(monthDay: MonthDay, today: LocalDate = LocalDate.now()): Int {
        var next = monthDay.atYear(today.year)
        if (next.isBefore(today)) next = monthDay.atYear(today.year + 1)
        return ChronoUnit.DAYS.between(today, next).toInt()
    }

    fun isToday(bro: Bro, today: LocalDate = LocalDate.now()): Boolean =
        of(bro)?.let { daysUntil(it, today) == 0 } == true
}

/** Decides when reminders are due. Pure logic so it can be unit tested. */
object ReminderSchedule {
    const val BIRTHDAY_HOUR = 9
    val NUDGE_DAY: DayOfWeek = DayOfWeek.SUNDAY
    const val NUDGE_HOUR = 17

    fun birthdaysDue(now: LocalDateTime, lastFiredDay: Long?): Boolean =
        now.hour >= BIRTHDAY_HOUR && lastFiredDay != now.toLocalDate().toEpochDay()

    fun nudgeDue(now: LocalDateTime, lastFiredDay: Long?): Boolean =
        now.dayOfWeek == NUDGE_DAY && now.hour >= NUDGE_HOUR && lastFiredDay != now.toLocalDate().toEpochDay()
}
