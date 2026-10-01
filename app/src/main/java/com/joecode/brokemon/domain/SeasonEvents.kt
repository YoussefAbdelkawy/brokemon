package com.joecode.brokemon.domain

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** Pixel icons drawn on event frames. See [PixelIcons]. */
enum class EventIcon { LANTERN, CRESCENT, FIREWORK, SUN, PENCIL }

/**
 * Limited-time events. They run purely off the phone's date (no server), and
 * any bro caught during one keeps that event's frame forever.
 */
enum class SeasonEvent(
    val label: String,
    val tagline: String,
    val primary: Long,
    val accent: Long,
    val icon: EventIcon,
) {
    RAMADAN("Ramadan", "Bros caught this month get the lantern frame", 0xFF7B4FD6, 0xFFFFC94A, EventIcon.LANTERN),
    EID("Eid", "Eid Mubarak! Catches get the crescent frame", 0xFF2EC4B6, 0xFFFFD54A, EventIcon.CRESCENT),
    NEW_YEAR("New Year", "Ring in the year: catches get the fireworks frame", 0xFFFFD54A, 0xFFD7263D, EventIcon.FIREWORK),
    SUMMER("Summer", "Summer catches get the sunset frame", 0xFFFF9F1C, 0xFF2EC4B6, EventIcon.SUN),
    EXAMS("Exam Season", "Study buddies caught now get the exam frame", 0xFF3A86FF, 0xFFE8E8EE, EventIcon.PENCIL),
}

data class EventStamp(val event: SeasonEvent, val year: Int) {
    val label: String get() = "${event.label.uppercase()} $year"
}

object SeasonEvents {
    /**
     * The event running on [date], if any. Religious holidays win over the
     * calendar ones when they overlap.
     */
    fun active(date: LocalDate): SeasonEvent? {
        hijri(date)?.let { (month, day) ->
            if (month == 9) return SeasonEvent.RAMADAN // Ramadan
            if (month == 10 && day <= 3) return SeasonEvent.EID // Eid al-Fitr
            if (month == 12 && day in 9..13) return SeasonEvent.EID // Eid al-Adha
        }
        val md = date.monthValue * 100 + date.dayOfMonth
        return when {
            md >= 1220 || md <= 115 -> SeasonEvent.NEW_YEAR
            md in 116..131 || md in 520..620 -> SeasonEvent.EXAMS
            md in 621..831 -> SeasonEvent.SUMMER
            else -> null
        }
    }

    /** What gets stored on a bro caught today, e.g. "NEW_YEAR|2027". */
    fun stamp(event: SeasonEvent, date: LocalDate): String {
        // A New Year caught in late December counts toward the year that's arriving.
        val year = if (event == SeasonEvent.NEW_YEAR && date.monthValue == 12) date.year + 1 else date.year
        return "${event.name}|$year"
    }

    fun parse(stamp: String?): EventStamp? {
        val parts = stamp?.split("|") ?: return null
        if (parts.size != 2) return null
        val event = SeasonEvent.entries.firstOrNull { it.name == parts[0] } ?: return null
        val year = parts[1].toIntOrNull()?.takeIf { it in 2000..2200 } ?: return null
        return EventStamp(event, year)
    }

    /** Islamic (Umm al-Qura) month/day, or null if the date is out of the supported range. */
    private fun hijri(date: LocalDate): Pair<Int, Int>? = runCatching {
        val h = HijrahDate.from(date)
        h.get(ChronoField.MONTH_OF_YEAR) to h.get(ChronoField.DAY_OF_MONTH)
    }.getOrNull()
}

/** Tiny original pixel icons for event frames ('.' = clear). Shared by Compose and the story renderer. */
object PixelIcons {
    // Palette keys: P = event primary, A = event accent, W = white, D = dark outline.
    fun rows(icon: EventIcon): List<String> = when (icon) {
        EventIcon.LANTERN -> listOf(
            "...D...",
            "..DAD..",
            ".DAAAD.",
            "DPAWAPD",
            "DPAWAPD",
            "DPAAAPD",
            "DPAWAPD",
            ".DAAAD.",
            "..DAD..",
            "...D...",
        )
        EventIcon.CRESCENT -> listOf(
            "..AAA..",
            ".AA....",
            "AA.....",
            "AA...W.",
            "AA.....",
            ".AA....",
            "..AAA..",
        )
        EventIcon.FIREWORK -> listOf(
            "A..P..A",
            ".A.P.A.",
            "..WWW..",
            "PPWAWPP",
            "..WWW..",
            ".A.P.A.",
            "A..P..A",
        )
        EventIcon.SUN -> listOf(
            "A..A..A",
            ".APPPA.",
            ".PPWPP.",
            "APWPPPA",
            ".PPPPP.",
            ".APPPA.",
            "A..A..A",
        )
        EventIcon.PENCIL -> listOf(
            "....DD",
            "...DAD",
            "..DPD.",
            ".DPD..",
            "DWD...",
            "DD....",
        )
    }
}
