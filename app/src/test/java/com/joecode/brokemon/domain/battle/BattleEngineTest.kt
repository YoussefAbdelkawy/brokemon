package com.joecode.brokemon.domain.battle

import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BattleEngineTest {

    private fun team(vararg types: BroType) = types.mapIndexed { i, t ->
        Snapshots.of(testBro(i + 1L, "Bro$i", t).copy(stats = BroStats(60, 60, 60, 60, 60, 40), moves = listOf("Spot Me")))
    }

    private fun start(seed: Long = 42, fair: Boolean = false) =
        BattleEngine.start(team(BroType.GYM_RAT, BroType.NERD, BroType.GAMER), team(BroType.FOODIE, BroType.YAPPER, BroType.GHOST), "A", "B", seed, fair)

    @Test
    fun `same seed and actions give the same result on both phones`() {
        val actions = List(30) { BattleAction.Move(it % 2) to BattleAction.Move((it + 1) % 2) }
        val a = BattleEngine.replay(start(), actions).last()
        val b = BattleEngine.replay(start(), actions).last()
        assertEquals(a, b)
    }

    @Test
    fun `different seeds can play out differently`() {
        val actions = List(10) { BattleAction.Move(0) to BattleAction.Move(0) }
        val results = (1L..20L).map { BattleEngine.replay(start(it), actions).last().sides.map { s -> s.fighters.map { f -> f.hp } } }.toSet()
        assertTrue(results.size > 1)
    }

    @Test
    fun `battles always end`() {
        repeat(25) { seed ->
            var s = start(seed.toLong())
            val r = Random(seed)
            while (!s.over) s = BattleEngine.resolve(s, BattleAi.choose(s, 0, r), BattleAi.choose(s, 1, r))
            assertNotNull(s.winner)
        }
    }

    @Test
    fun `forfeit ends the battle for the other side`() {
        val s = BattleEngine.resolve(start(), BattleAction.Forfeit, BattleAction.Move(0))
        assertEquals(1, s.winner)
    }

    @Test
    fun `illegal actions count as forfeits`() {
        val s = BattleEngine.resolve(start(), BattleAction.Move(9), BattleAction.Move(0))
        assertEquals(1, s.winner)
        val sw = BattleEngine.resolve(start(), BattleAction.Move(0), BattleAction.Switch(0)) // switching to the active bro
        assertEquals(0, sw.winner)
    }

    @Test
    fun `energy runs out`() {
        var s = start()
        val slot = s.sides[0].current.moves.indexOfFirst { it.power > 0 }
        val energy = s.sides[0].current.energy[slot]
        // Opponent just keeps switching between two bros so nobody faints.
        repeat(energy) { i ->
            if (s.over) return
            s = BattleEngine.resolve(s, BattleAction.Move(slot), BattleAction.Switch(if (s.sides[1].active == 0) 1 else 0))
        }
        if (!s.over) assertFalse(BattleAction.Move(slot) in BattleEngine.legalActions(s, 0))
    }

    @Test
    fun `fair mode levels everyone`() {
        val s = start(fair = true)
        assertTrue(s.sides.all { side -> side.fighters.all { it.snap.level == BattleMath.FAIR_LEVEL } })
    }

    @Test
    fun `stats grow with level`() {
        val base = BroStats(50, 50, 50, 50, 50, 50)
        val low = BattleMath.stats(base, 5)
        val high = BattleMath.stats(base, 50)
        assertTrue(high.hp > low.hp && high.attack > low.attack && high.defense > low.defense && high.speed > low.speed)
    }

    @Test
    fun `snapshot validation rejects tampered teams`() {
        val good = team(BroType.GYM_RAT)
        assertNotNull(Snapshots.validate(good))
        assertNull(Snapshots.validate(good.map { it.copy(base = listOf(999, 1, 1, 1, 1, 1)) }))
        assertNull(Snapshots.validate(good.map { it.copy(level = 99) }))
        assertNull(Snapshots.validate(good.map { it.copy(moves = it.moves + MoveSpec("cat:ultimate_roast")) }.map { it.copy(level = 5) }))
        assertNull(Snapshots.validate(good.map { it.copy(moves = listOf(MoveSpec("sig:Nuke", "Nuke", "GYM_RAT", "OVER_9000"))) }))
        assertNull(Snapshots.validate(team(BroType.GYM_RAT, BroType.NERD, BroType.GAMER, BroType.GHOST)))
    }

    @Test
    fun `new bros know only basic moves`() {
        val bro = testBro()
        val moves = MoveCatalog.available(bro, score = 0)
        assertTrue(moves.none { it.unlockScore > 0 })
        assertTrue(MoveCatalog.available(bro, score = 40).any { it.name == "Ultimate Roast" })
        assertTrue(MoveCatalog.equipped(bro, 0).size in 1..4)
    }

    @Test
    fun `custom moves use fixed templates`() {
        MoveTemplate.entries.forEach { assertTrue(it.power <= 100) }
        val sig = MoveCatalog.resolve(MoveSpec("sig:Big Yap", "Big Yap", "YAPPER", "HEAVY"), 10, listOf(BroType.YAPPER))!!
        assertEquals(MoveTemplate.HEAVY.power, sig.power)
    }

    @Test
    fun `action wire format round trips`() {
        listOf(BattleAction.Move(3), BattleAction.Switch(2), BattleAction.Forfeit).forEach {
            assertEquals(it, BattleAction.decode(BattleAction.encode(it)))
        }
        val acts = listOf(BattleAction.Move(1) to BattleAction.Switch(2))
        assertEquals(acts, BattleCodec.actionsFromJson(BattleCodec.actionsToJson(acts)))
    }
}
