package com.joecode.brokemon.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShinyHuntTest {
    @Test
    fun `one roll per bro per day, never for shinies`() {
        assertTrue(ShinyHunt.canRoll(alreadyShiny = false, lastRollDay = null, today = 100))
        assertFalse(ShinyHunt.canRoll(alreadyShiny = false, lastRollDay = 100, today = 100))
        assertTrue(ShinyHunt.canRoll(alreadyShiny = false, lastRollDay = 99, today = 100))
        assertFalse(ShinyHunt.canRoll(alreadyShiny = true, lastRollDay = null, today = 100))
    }

    @Test
    fun `odds are roughly one in twenty`() {
        val r = Random(1)
        val hits = (1..20_000).count { ShinyHunt.roll(r) }
        assertTrue(hits in 800..1200)
    }
}
