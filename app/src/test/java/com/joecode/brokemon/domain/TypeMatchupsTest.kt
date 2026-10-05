package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TypeMatchupsTest {
    @Test
    fun `chart is balanced and every win has a reason`() {
        BroType.entries.forEach { type ->
            assertEquals("$type beats", 2, TypeMatchups.beats(type).size)
            assertEquals("$type beaten by", 2, TypeMatchups.beatenBy(type).size)
            TypeMatchups.beats(type).forEach { assertNotNull(TypeMatchups.reason(type, it)) }
        }
    }

    @Test
    fun `every type learns two evolution moves`() {
        BroType.entries.forEach { assertEquals("$it", 2, EvolutionMoves.all(it).size) }
        // Every evolution style is used by at least one type.
        assertEquals(EvolutionStyle.entries.toSet(), BroType.entries.map { EvolutionStyle.forType(it) }.toSet())
        assertEquals(listOf("Horn Solo"), EvolutionMoves.unlocked(BroType.ROAD_RAGER, 0, 1))
        assertEquals(listOf("Horn Solo", "Brake Check"), EvolutionMoves.unlocked(BroType.ROAD_RAGER, 0, 2))
        assertEquals(listOf("Brake Check"), EvolutionMoves.unlocked(BroType.ROAD_RAGER, 1, 2))
    }

    @Test
    fun `effectiveness multiplies across dual types`() {
        val rager = BroType.ROAD_RAGER
        assertEquals(4f, TypeMatchups.effectiveness(rager, listOf(BroType.BAD_DRIVER, BroType.FOODIE)), 0f)
        assertEquals(0.5f, TypeMatchups.effectiveness(rager, listOf(BroType.WINGMAN)), 0f)
        assertEquals(1f, TypeMatchups.effectiveness(rager, listOf(BroType.NERD)), 0f)
    }

    @Test
    fun `squad report finds counters and threats`() {
        val squad = listOf(testBro(1, type1 = BroType.ROAD_RAGER))
        val report = TypeMatchups.squadReport(squad)
        assertEquals(TypeMatchups.Verdict.STRONG, report.first { it.opponent == BroType.BAD_DRIVER }.verdict)
        assertEquals(TypeMatchups.Verdict.WEAK, report.first { it.opponent == BroType.WINGMAN }.verdict)
    }

    @Test
    fun `legacy type names still resolve`() {
        assertEquals(BroType.GYM_RAT, BroType.from("GYM"))
        assertEquals(BroType.NERD, BroType.from("BRAIN"))
        assertEquals(null, BroType.from("NOPE"))
    }
}
