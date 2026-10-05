package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.min

/**
 * Stage thresholds are provisional (0 / 20 / 40) — tune these once there's
 * real data. The stage is always computed from the score, never stored.
 */
enum class EvolutionStage(val title: String, val threshold: Int) {
    ROOKIE("Rookie", 0),
    HOMIE("Homie", 20),
    DAY_ONE("Day One", 40);

    val next: EvolutionStage? get() = entries.getOrNull(ordinal + 1)
}

data class EvolutionInfo(
    val score: Int,
    val stage: EvolutionStage,
    /** 0f..1f progress toward the next stage; 1f when maxed out. */
    val progress: Float,
    val pointsToNext: Int?,
)

object Evolution {
    const val MEMORY_POINTS = 4
    const val CHECK_IN_POINTS = 3
    const val FACT_POINTS = 2
    const val MONTH_POINTS = 1
    const val MONTHS_CAP = 24

    /** Only the first 10 facts count, so adding junk facts can't power-level a bro. */
    const val FACTS_CAP = 10

    fun monthsKnown(catchDate: Long, now: Long = System.currentTimeMillis()): Int {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(catchDate).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return ChronoUnit.MONTHS.between(start, end).toInt().coerceAtLeast(0)
    }

    fun score(
        memories: Int,
        checkIns: Int,
        facts: Int,
        monthsKnown: Int,
    ): Int = memories * MEMORY_POINTS +
        checkIns * CHECK_IN_POINTS +
        min(facts, FACTS_CAP) * FACT_POINTS +
        min(monthsKnown, MONTHS_CAP) * MONTH_POINTS

    fun score(bro: Bro, now: Long = System.currentTimeMillis()): Int = score(
        memories = bro.memories.size,
        checkIns = bro.checkInCount,
        facts = bro.facts.size,
        monthsKnown = monthsKnown(bro.catchDate, now),
    )

    fun stageFor(score: Int): EvolutionStage =
        EvolutionStage.entries.last { score >= it.threshold }

    fun info(bro: Bro, now: Long = System.currentTimeMillis()): EvolutionInfo = info(score(bro, now))

    fun info(score: Int): EvolutionInfo {
        val stage = stageFor(score)
        val next = stage.next ?: return EvolutionInfo(score, stage, 1f, null)
        val span = next.threshold - stage.threshold
        val progress = (score - stage.threshold).toFloat() / span
        return EvolutionInfo(score, stage, progress.coerceIn(0f, 1f), next.threshold - score)
    }
}
