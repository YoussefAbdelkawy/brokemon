package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WrappedSummary(
    val year: Int,
    val caughtCount: Int,
    val topTypes: List<Pair<BroType, Int>>,
    val rarest: List<Bro>,
    val mostMemories: Pair<Bro, Int>?,
    val firstBro: Bro?,
    val memoriesMade: Int,
    val shinyCount: Int,
)

object Wrapped {
    /** A playful identity label for the recap ("you're a Shiny Hunter"). */
    fun title(s: WrappedSummary): String = when {
        s.caughtCount == 0 && s.memoriesMade == 0 -> "The Quiet Year"
        s.shinyCount > 0 -> "Shiny Hunter"
        s.rarest.any { it.rarity == Rarity.LEGENDARY } -> "Legend Finder"
        (s.mostMemories?.second ?: 0) >= 10 -> "Memory Keeper"
        s.caughtCount >= 10 -> "The Collector"
        s.memoriesMade >= s.caughtCount * 3 -> "Ride or Die"
        else -> "Day One Energy"
    }

    fun yearOf(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).year

    /**
     * Wrapped is a New Year thing: it shows up from Dec 20 through Jan 15 and
     * recaps the year that's ending. Returns null outside that window.
     */
    fun seasonYear(today: LocalDate = LocalDate.now()): Int? = when {
        today.monthValue == 12 && today.dayOfMonth >= 20 -> today.year
        today.monthValue == 1 && today.dayOfMonth <= 15 -> today.year - 1
        else -> null
    }

    fun summarize(bros: List<Bro>, year: Int): WrappedSummary {
        val caught = bros.filter { yearOf(it.catchDate) == year }.sortedBy { it.catchDate }
        val topTypes = caught
            .flatMap { it.types }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }
        val rarest = caught
            .filter { it.rarity != Rarity.COMMON || it.isShiny }
            .sortedWith(compareByDescending<Bro> { it.rarity.ordinal }.thenByDescending { it.isShiny })
            .take(3)
        val memoriesPerBro = bros.associateWith { b -> b.memories.count { yearOf(it.date) == year } }
        val mostMemories = memoriesPerBro.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.toPair()
        return WrappedSummary(
            year = year,
            caughtCount = caught.size,
            topTypes = topTypes,
            rarest = rarest,
            mostMemories = mostMemories,
            firstBro = caught.firstOrNull(),
            memoriesMade = memoriesPerBro.values.sum(),
            shinyCount = caught.count { it.isShiny },
        )
    }
}
