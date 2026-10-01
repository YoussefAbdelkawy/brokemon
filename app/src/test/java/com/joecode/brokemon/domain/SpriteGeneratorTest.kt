package com.joecode.brokemon.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpriteGeneratorTest {
    @Test
    fun `same seed gives the same sprite`() {
        val a = SpriteGenerator.generate(1234, 1)
        val b = SpriteGenerator.generate(1234, 1)
        a.indices.forEach { assertArrayEquals(a[it], b[it]) }
    }

    @Test
    fun `sprites are mirrored and have eyes`() {
        for (stage in 0..2) {
            val grid = SpriteGenerator.generate(99, stage)
            assertEquals(SpriteGenerator.SIZE, grid.size)
            for (row in grid) for (x in row.indices) assertEquals(row[x], row[row.size - 1 - x])
            assertTrue(grid.any { row -> row.any { it == SpriteGenerator.EYE } })
        }
    }

    @Test
    fun `later stages are bigger`() {
        fun filled(stage: Int) = SpriteGenerator.generate(5, stage).sumOf { r -> r.count { it != SpriteGenerator.EMPTY } }
        assertTrue(filled(1) > filled(0))
        assertTrue(filled(2) > filled(1))
    }
}
