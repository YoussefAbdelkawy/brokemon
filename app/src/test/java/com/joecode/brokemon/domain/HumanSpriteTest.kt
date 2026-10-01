package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.LookPart
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HumanSpriteTest {
    @Test
    fun `same look gives the same sprite`() {
        val look = BroLook.random(1234)
        assertArrayEquals(HumanSprite.render(look, 1, false), HumanSprite.render(look, 1, false))
        assertEquals(look, BroLook.random(1234))
    }

    @Test
    fun `every option renders without crashing`() {
        LookPart.entries.forEach { part ->
            repeat(part.count) { i ->
                val px = HumanSprite.render(BroLook().with(part, i), 2, i % 2 == 0)
                assertEquals(HumanSprite.SIZE * HumanSprite.SIZE, px.size)
            }
        }
    }

    @Test
    fun `changing a part changes the picture`() {
        val base = BroLook()
        LookPart.entries.forEach { part ->
            val other = base.with(part, base[part] + 1)
            assertFalse("$part", HumanSprite.render(base, 0, false).contentEquals(HumanSprite.render(other, 0, false)))
        }
    }

    @Test
    fun `evolving adds bling`() {
        val look = BroLook()
        fun filled(stage: Int) = HumanSprite.render(look, stage, false).count { it != HumanSprite.CLEAR }
        val rookie = HumanSprite.render(look, 0, false)
        val homie = HumanSprite.render(look, 1, false)
        assertFalse(rookie.contentEquals(homie))
        assertTrue(filled(2) > filled(1))
    }

    @Test
    fun `look survives a list round trip and clamps bad input`() {
        val look = BroLook.random(99)
        assertEquals(look, BroLook.fromList(look.toList()))
        val wrapped = BroLook.fromList(listOf(-1, 1000, 0))
        assertTrue(wrapped.skin in 0 until LookPart.SKIN.count)
        assertTrue(wrapped.hair in 0 until LookPart.HAIR.count)
    }
}
