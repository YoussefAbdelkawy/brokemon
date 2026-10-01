package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import java.time.Instant
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
    fun yearOf(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).year

    fun availableYears(bros: List<Bro>): List<Int> {
        val years = bros.map { yearOf(it.catchDate) } +
            bros.flatMap { b -> b.memories.map { yearOf(it.date) } }
        return years.distinct().sortedDescending()
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
