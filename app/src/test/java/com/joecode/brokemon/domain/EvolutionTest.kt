package com.joecode.brokemon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvolutionTest {
    @Test
    fun `score follows the locked formula`() {
        // (3 x 4) + (2 x 3) + (5 x 2) + 7 = 35
        assertEquals(35, Evolution.score(memories = 3, checkIns = 2, facts = 5, monthsKnown = 7))
    }

    @Test
    fun `months known is capped at 24`() {
        assertEquals(24, Evolution.score(memories = 0, checkIns = 0, facts = 0, monthsKnown = 60))
    }

    @Test
    fun `facts stop counting after the cap`() {
        assertEquals(Evolution.FACTS_CAP * Evolution.FACT_POINTS, Evolution.score(memories = 0, checkIns = 0, facts = 50, monthsKnown = 0))
    }

    @Test
    fun `stage thresholds`() {
        assertEquals(EvolutionStage.ROOKIE, Evolution.stageFor(0))
        assertEquals(EvolutionStage.ROOKIE, Evolution.stageFor(19))
        assertEquals(EvolutionStage.HOMIE, Evolution.stageFor(20))
        assertEquals(EvolutionStage.DAY_ONE, Evolution.stageFor(40))
        assertEquals(EvolutionStage.DAY_ONE, Evolution.stageFor(500))
    }

    @Test
    fun `progress toward next stage`() {
        val info = Evolution.info(30)
        assertEquals(EvolutionStage.HOMIE, info.stage)
        assertEquals(0.5f, info.progress, 0.001f)
        assertEquals(10, info.pointsToNext)
    }

    @Test
    fun `max stage has no next`() {
        val info = Evolution.info(99)
        assertEquals(1f, info.progress, 0f)
        assertNull(info.pointsToNext)
    }

    @Test
    fun `months known from catch date`() {
        val day = 24L * 60 * 60 * 1000
        assertEquals(0, Evolution.monthsKnown(catchDate = 0, now = 10 * day))
        assertEquals(3, Evolution.monthsKnown(catchDate = 0, now = 100 * day))
        assertEquals(0, Evolution.monthsKnown(catchDate = 100 * day, now = 0))
    }
}
