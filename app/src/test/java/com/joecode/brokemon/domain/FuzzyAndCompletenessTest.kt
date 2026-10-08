package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyAndCompletenessTest {
    @Test
    fun `exact and partial names match`() {
        assertTrue(FuzzySearch.matches("omar", "Omar"))
        assertTrue(FuzzySearch.matches("OM", "Omar"))
        assertTrue(FuzzySearch.matches("", "Anyone"))
        assertTrue(FuzzySearch.matches("ahmed ali", "Ahmed Mohamed Ali"))
    }

    @Test
    fun `typos still find the bro`() {
        assertTrue(FuzzySearch.matches("marcuss", "Marcus"))
        assertTrue(FuzzySearch.matches("layal", "Layla"))   // swapped letters
        assertTrue(FuzzySearch.matches("omr", "Omar"))      // missing letter
        assertTrue(FuzzySearch.matches("kareem", "Karim"))
        assertTrue(FuzzySearch.matches("joseh", "Joseph"))
    }

    @Test
    fun `different names don't match`() {
        assertFalse(FuzzySearch.matches("sam", "Omar"))
        assertFalse(FuzzySearch.matches("zz", "Omar"))
        assertFalse(FuzzySearch.matches("marcus", "Layla"))
    }

    @Test
    fun `accents and punctuation are ignored`() {
        assertTrue(FuzzySearch.matches("jose", "José"))
        assertTrue(FuzzySearch.matches("omar b", "Omar B."))
    }

    @Test
    fun `distance counts a swap as one typo`() {
        assertEquals(1, FuzzySearch.distance("layal", "layla"))
        assertEquals(2, FuzzySearch.distance("kitten", "sittin"))
    }

    @Test
    fun `a fresh quick catch is 20 percent and asks for moves first`() {
        val c = Completeness.of(testBro())
        assertEquals(20, c.percent)
        assertEquals(Missing.MOVES, c.missing.first())
        assertEquals("Card 20% complete: add moves?", c.nudge)
    }

    @Test
    fun `filling things in raises the percent up to 100`() {
        val bro = testBro().copy(moves = listOf("Spot Me"), facts = listOf(Fact(category = "Food", value = "Pizza")))
        assertEquals(52, Completeness.of(bro).percent)
        val full = bro.copy(
            memories = listOf(com.joecode.brokemon.data.model.Memory(fileUri = "x", mediaType = com.joecode.brokemon.data.model.MediaType.PHOTO)),
            flavorText = "Funny line.",
            habitat = "Gym",
        )
        val c = Completeness.of(full)
        assertEquals(100, c.percent)
        assertTrue(c.complete)
        assertEquals(null, c.nudge)
    }
}
