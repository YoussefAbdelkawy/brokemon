package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeMatchupsTest {
    @Test
    fun `chart is balanced`() {
        BroType.entries.forEach {
            assertEquals("$it beats", 2, TypeMatchups.beats(it).size)
            assertEquals("$it beaten by", 2, TypeMatchups.beatenBy(it).size)
        }
    }

    @Test
    fun `effectiveness multiplies across dual types`() {
        assertEquals(4f, TypeMatchups.effectiveness(BroType.HYPE, listOf(BroType.CHILL, BroType.BRAIN)), 0f)
        assertEquals(0.5f, TypeMatchups.effectiveness(BroType.CHILL, listOf(BroType.HYPE)), 0f)
        assertEquals(1f, TypeMatchups.effectiveness(BroType.HYPE, listOf(BroType.GYM)), 0f)
    }

    @Test
    fun `squad report finds counters and threats`() {
        val squad = listOf(testBro(1, type1 = BroType.HYPE))
        val vsChill = TypeMatchups.squadReport(squad).first { it.opponent == BroType.CHILL }
        assertEquals(TypeMatchups.Verdict.STRONG, vsChill.verdict)
        val vsChaos = TypeMatchups.squadReport(squad).first { it.opponent == BroType.CHAOS }
        assertEquals(TypeMatchups.Verdict.WEAK, vsChaos.verdict)
    }
}
