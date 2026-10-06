package com.joecode.brokemon.ui

import com.joecode.brokemon.ui.wild.ShakeDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class ShakeDetectorTest {
    private var now = 1_000L
    private var shakes = 0
    private val detector = ShakeDetector(clock = { now }) { shakes++ }

    private fun jolt(atMs: Long, g: Float = 3f) {
        now = atMs
        detector.onReading(g * 9.81f, 0f, 0f)
    }

    @Test
    fun `normal handling does not trigger`() {
        now = 1_000
        repeat(20) { detector.onReading(3f, 9.8f, 2f); now += 50 }
        assertEquals(0, shakes)
    }

    @Test
    fun `two jolts close together trigger once`() {
        jolt(1_000)
        jolt(1_300)
        assertEquals(1, shakes)
    }

    @Test
    fun `a single long jolt is not a shake`() {
        jolt(1_000)
        jolt(1_030) // same jolt, next sensor sample
        assertEquals(0, shakes)
    }

    @Test
    fun `cooldown stops repeat triggers`() {
        jolt(1_000); jolt(1_200)
        jolt(1_500); jolt(1_700)
        assertEquals(1, shakes)
        jolt(5_000); jolt(5_200)
        assertEquals(2, shakes)
    }

    @Test
    fun `jolts too far apart don't count`() {
        jolt(1_000)
        jolt(2_000)
        assertEquals(0, shakes)
        jolt(2_300)
        assertEquals(1, shakes)
    }
}
