package com.joecode.brokemon.domain

import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class CheckOnBroTest {
    private val bros = listOf(testBro(1), testBro(2), testBro(3))

    @Test
    fun `never recommends the same bro twice in a row`() {
        val random = Random(1)
        var last: Long? = null
        repeat(500) {
            val pick = CheckOnBro.recommend(bros, last, now = 1_000_000_000L, random = random)!!
            assertNotEquals(last, pick.id)
            last = pick.id
        }
    }

    @Test
    fun `single bro is still returned`() {
        assertEquals(1L, CheckOnBro.recommend(listOf(testBro(1)), lastRecommendedId = 1)?.id)
    }

    @Test
    fun `empty dex gives nothing`() {
        assertNull(CheckOnBro.recommend(emptyList(), null))
    }

    @Test
    fun `favors whoever was checked on longest ago`() {
        val day = 24L * 60 * 60 * 1000
        val now = 400 * day
        val stale = testBro(1, lastCheckIn = 0)
        val fresh = testBro(2, lastCheckIn = now)
        val random = Random(7)
        val picks = List(1000) { CheckOnBro.recommend(listOf(stale, fresh), null, now, random)!!.id }
        assert(picks.count { it == 1L } > 900)
    }
}
