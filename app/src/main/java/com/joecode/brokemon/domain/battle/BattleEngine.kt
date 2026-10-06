package com.joecode.brokemon.domain.battle

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.TypeMatchups
import kotlin.math.max
import kotlin.random.Random

data class BattleStats(val hp: Int, val attack: Int, val defense: Int, val speed: Int)

/**
 * Battle numbers. Level = 5 + evolution score (capped at 50), so long-time
 * friends hit harder. Fair Mode puts everyone at [FAIR_LEVEL].
 *
 * Base stats come from the funny stats:
 * HP = Loyal + Aura, Attack = Rizz + Chaos, Defense = Loyal + (100 − Flake), Speed = Yap + Chaos.
 */
object BattleMath {
    const val MIN_LEVEL = 5
    const val MAX_LEVEL = 50
    const val FAIR_LEVEL = 30

    fun level(score: Int): Int = (MIN_LEVEL + score).coerceIn(MIN_LEVEL, MAX_LEVEL)
    fun scoreForLevel(level: Int): Int = level - MIN_LEVEL

    fun stats(base: BroStats, level: Int): BattleStats {
        fun scaled(b: Int) = b * 2 * level / 100 + 5
        val hpBase = (base.loyalty + base.aura) / 2
        return BattleStats(
            hp = hpBase * 2 * level / 100 + level + 20,
            attack = scaled((base.rizz + base.chaos) / 2),
            defense = scaled((base.loyalty + (100 - base.flake)) / 2),
            speed = scaled((base.yap + base.chaos) / 2),
        )
    }
}

/**
 * What goes over the air for a fighter. No memories, facts, photos or dates:
 * just the name (or alias), look, types, level, base stats and 4 moves.
 */
data class FighterSnapshot(
    val name: String,
    val look: List<Int>,
    val types: List<String>,
    val level: Int,
    val base: List<Int>,
    val moves: List<MoveSpec>,
    val shiny: Boolean = false,
    /** Local bro id, only meaningful on the phone that owns the bro (MVP, rewards). */
    val localId: Long = 0,
)

object Snapshots {
    const val MAX_TEAM = 3

    fun of(bro: Bro): FighterSnapshot {
        val score = Evolution.score(bro)
        return FighterSnapshot(
            name = bro.name.take(24),
            look = bro.resolvedLook.toList(),
            types = bro.types.map { it.name },
            level = BattleMath.level(score),
            base = bro.stats.toArray(),
            moves = MoveCatalog.equipped(bro, score).map(MoveCatalog::specOf),
            shiny = bro.isShiny,
            localId = bro.id,
        )
    }

    /** Checks a received team against the rules. Returns null if anything is off. */
    fun validate(team: List<FighterSnapshot>): List<FighterSnapshot>? {
        if (team.isEmpty() || team.size > MAX_TEAM) return null
        return team.map { s ->
            val types = s.types.mapNotNull { BroType.from(it) }.distinct()
            if (types.isEmpty() || types.size > 2) return null
            if (s.level !in BattleMath.MIN_LEVEL..BattleMath.MAX_LEVEL) return null
            if (s.base.size != 6 || s.base.any { it !in BroStats.MIN..BroStats.MAX }) return null
            if (s.moves.isEmpty() || s.moves.size > 4) return null
            if (s.moves.any { MoveCatalog.resolve(it, s.level, types) == null }) return null
            if (s.look.size > 20) return null
            s.copy(name = s.name.trim().take(24).ifBlank { "Bro" }, types = types.map { it.name })
        }
    }
}

sealed interface BattleAction {
    data class Move(val slot: Int) : BattleAction
    data class Switch(val index: Int) : BattleAction
    data object Forfeit : BattleAction

    companion object {
        /** Tiny wire format: "M2", "S1", "F". */
        fun encode(a: BattleAction): String = when (a) {
            is Move -> "M${a.slot}"
            is Switch -> "S${a.index}"
            Forfeit -> "F"
        }

        fun decode(s: String): BattleAction? = when {
            s == "F" -> Forfeit
            s.startsWith("M") -> s.drop(1).toIntOrNull()?.let { Move(it) }
            s.startsWith("S") -> s.drop(1).toIntOrNull()?.let { Switch(it) }
            else -> null
        }
    }
}

data class Fighter(
    val snap: FighterSnapshot,
    val types: List<BroType>,
    val moves: List<BattleMove>,
    val stats: BattleStats,
    val hp: Int,
    val energy: List<Int>,
    val statuses: Map<StatusEffect, Int> = emptyMap(),
    val lastMoveSlot: Int? = null,
    val damageDealt: Int = 0,
    val knockouts: Int = 0,
) {
    val fainted: Boolean get() = hp <= 0
    val hpFraction: Float get() = hp.toFloat() / stats.hp
}

data class Side(val name: String, val fighters: List<Fighter>, val active: Int = 0) {
    val current: Fighter get() = fighters[active]
    val alive: Boolean get() = fighters.any { !it.fainted }
}

/** Things that happened in a turn, in order. The UI turns these into text and animations. */
sealed interface BattleEvent {
    data class Used(val side: Int, val fighter: String, val move: String, val moveType: BroType?) : BattleEvent
    data class Missed(val side: Int, val fighter: String) : BattleEvent
    data class Hit(val side: Int, val target: String, val damage: Int, val effectiveness: Float, val crit: Boolean, val hpLeft: Int, val maxHp: Int) : BattleEvent
    data class StatusApplied(val side: Int, val fighter: String, val status: StatusEffect) : BattleEvent
    data class StatusBlocked(val side: Int, val fighter: String, val status: StatusEffect) : BattleEvent
    data class StatusEnded(val side: Int, val fighter: String, val status: StatusEffect) : BattleEvent
    data class Fainted(val side: Int, val fighter: String) : BattleEvent
    data class SwitchedIn(val side: Int, val fighter: String) : BattleEvent
    data class Forfeited(val side: Int) : BattleEvent
    data class Won(val side: Int) : BattleEvent
    data object Draw : BattleEvent
}

data class BattleState(
    val sides: List<Side>,
    val seed: Long,
    val fairMode: Boolean,
    val turn: Int = 0,
    val events: List<BattleEvent> = emptyList(),
    /** 0 or 1 when someone has won, -1 for a draw, null while the battle is on. */
    val winner: Int? = null,
) {
    val over: Boolean get() = winner != null
}

/**
 * The shared battle engine. Pure Kotlin and deterministic: the same start
 * state, seed and actions give the same result on both phones, so phones
 * only ever exchange actions, never results.
 */
object BattleEngine {
    const val TURN_LIMIT = 80

    fun start(team0: List<FighterSnapshot>, team1: List<FighterSnapshot>, name0: String, name1: String, seed: Long, fairMode: Boolean): BattleState {
        fun fighter(s: FighterSnapshot): Fighter {
            val level = if (fairMode) BattleMath.FAIR_LEVEL else s.level.coerceIn(BattleMath.MIN_LEVEL, BattleMath.MAX_LEVEL)
            val types = s.types.mapNotNull { BroType.from(it) }.ifEmpty { listOf(BroType.CHILL_GUY) }
            // Moves are checked against the bro's real level (unlocks), even in Fair Mode.
            val moves = s.moves.mapNotNull { MoveCatalog.resolve(it, s.level, types) }.ifEmpty { listOf(MoveCatalog.catalog.first()) }
            val stats = BattleMath.stats(BroStats.fromArray(s.base), level)
            return Fighter(s.copy(level = level), types, moves, stats, stats.hp, moves.map { it.energy })
        }
        return BattleState(
            sides = listOf(Side(name0, team0.map(::fighter)), Side(name1, team1.map(::fighter))),
            seed = seed,
            fairMode = fairMode,
        )
    }

    fun legalActions(state: BattleState, side: Int): List<BattleAction> {
        if (state.over) return emptyList()
        val s = state.sides[side]
        val f = s.current
        val moves = f.moves.indices.filter { canUse(f, it) }.map { BattleAction.Move(it) }
            .ifEmpty { listOf(BattleAction.Move(PANIC_SLOT)) }
        val switches = s.fighters.indices.filter { it != s.active && !s.fighters[it].fainted }.map { BattleAction.Switch(it) }
        return moves + switches + BattleAction.Forfeit
    }

    fun isLegal(state: BattleState, side: Int, action: BattleAction): Boolean = action in legalActions(state, side)

    private const val PANIC_SLOT = -1

    private fun canUse(f: Fighter, slot: Int): Boolean =
        f.energy[slot] > 0 && !(StatusEffect.LEFT_ON_READ in f.statuses && f.lastMoveSlot == slot)

    fun resolve(state: BattleState, action0: BattleAction, action1: BattleAction): BattleState {
        if (state.over) return state
        val rng = Random(state.seed xor (state.turn.toLong() * -7046029254386353131L))
        val events = mutableListOf<BattleEvent>()
        val sides = state.sides.toMutableList()
        // Anything illegal (only possible with a tampered client) counts as forfeiting.
        val actions = listOf(action0, action1).mapIndexed { i, a -> if (isLegal(state, i, a)) a else BattleAction.Forfeit }

        fun fighter(side: Int) = sides[side].current
        fun update(side: Int, f: Fighter) {
            val s = sides[side]
            sides[side] = s.copy(fighters = s.fighters.toMutableList().also { it[s.active] = f })
        }
        fun finish(winner: Int?): BattleState {
            if (winner != null) events += if (winner >= 0) BattleEvent.Won(winner) else BattleEvent.Draw
            return state.copy(sides = sides, turn = state.turn + 1, events = events, winner = winner)
        }

        // Forfeits end it immediately.
        val forfeits = actions.indices.filter { actions[it] == BattleAction.Forfeit }
        if (forfeits.isNotEmpty()) {
            forfeits.forEach { events += BattleEvent.Forfeited(it) }
            return finish(if (forfeits.size == 2) -1 else 1 - forfeits.first())
        }

        // Switches happen first.
        actions.forEachIndexed { i, a ->
            if (a is BattleAction.Switch) {
                sides[i] = sides[i].copy(active = a.index)
                events += BattleEvent.SwitchedIn(i, sides[i].current.snap.name)
            }
        }

        // Then moves, fastest first.
        val movers = actions.indices.filter { actions[it] is BattleAction.Move }
        fun speed(i: Int): Float = fighter(i).stats.speed * if (StatusEffect.SLEEPY in fighter(i).statuses) StatusEffect.SLEEPY_SPEED else 1f
        val order = when (movers.size) {
            2 -> if (speed(0) == speed(1)) (if (rng.nextBoolean()) listOf(0, 1) else listOf(1, 0)) else movers.sortedByDescending { speed(it) }
            else -> movers
        }
        for (i in order) {
            val target = 1 - i
            val attacker = fighter(i)
            if (attacker.fainted || fighter(target).fainted) continue
            val slot = (actions[i] as BattleAction.Move).slot
            useMove(i, slot, attacker, fighter(target), rng, events, ::update)
        }

        // Status timers tick down at the end of the turn.
        for (i in 0..1) {
            val f = fighter(i)
            if (f.fainted || f.statuses.isEmpty()) continue
            val ticked = f.statuses.mapValues { (status, left) -> if (status == StatusEffect.MAIN_CHARACTER) left else left - 1 }
            ticked.filterValues { it <= 0 }.keys.forEach { events += BattleEvent.StatusEnded(i, f.snap.name, it) }
            update(i, f.copy(statuses = ticked.filterValues { it > 0 }))
        }

        // Fainted bros are replaced by the next one standing.
        for (i in 0..1) {
            val s = sides[i]
            if (!s.current.fainted) continue
            val next = s.fighters.indexOfFirst { !it.fainted }
            if (next >= 0) {
                sides[i] = s.copy(active = next)
                events += BattleEvent.SwitchedIn(i, sides[i].current.snap.name)
            }
        }

        val alive = sides.map { it.alive }
        return when {
            !alive[0] && !alive[1] -> finish(-1)
            !alive[0] -> finish(1)
            !alive[1] -> finish(0)
            state.turn + 1 >= TURN_LIMIT -> {
                // Time's up: more HP left (as a share of max) wins.
                val left = sides.map { s -> s.fighters.sumOf { it.hp }.toFloat() / s.fighters.sumOf { it.stats.hp } }
                finish(if (left[0] == left[1]) -1 else if (left[0] > left[1]) 0 else 1)
            }
            else -> finish(null)
        }
    }

    private fun useMove(
        side: Int,
        slot: Int,
        attackerIn: Fighter,
        defenderIn: Fighter,
        rng: Random,
        events: MutableList<BattleEvent>,
        update: (Int, Fighter) -> Unit,
    ) {
        var attacker = attackerIn
        var defender = defenderIn
        val target = 1 - side

        if (StatusEffect.CRINGED in attacker.statuses && rng.nextInt(100) < StatusEffect.CRINGE_SKIP_PERCENT) {
            events += BattleEvent.StatusBlocked(side, attacker.snap.name, StatusEffect.CRINGED)
            return
        }
        val move = if (slot == PANIC_SLOT) MoveCatalog.panicText else attacker.moves[slot]
        if (slot != PANIC_SLOT) {
            attacker = attacker.copy(energy = attacker.energy.toMutableList().also { it[slot] = it[slot] - 1 }, lastMoveSlot = slot)
        }
        events += BattleEvent.Used(side, attacker.snap.name, move.name, move.type)

        if (move.isBuff) {
            val status = move.status!!
            attacker = attacker.copy(statuses = attacker.statuses + (status to status.turns))
            events += BattleEvent.StatusApplied(side, attacker.snap.name, status)
            update(side, attacker)
            return
        }

        val sure = StatusEffect.MAIN_CHARACTER in attacker.statuses
        if (sure) attacker = attacker.copy(statuses = attacker.statuses - StatusEffect.MAIN_CHARACTER)
        if (!sure && rng.nextInt(100) >= move.accuracy) {
            events += BattleEvent.Missed(side, attacker.snap.name)
            update(side, attacker)
            return
        }

        if (move.power > 0) {
            val level = attacker.snap.level
            val attack = attacker.stats.attack * if (StatusEffect.HYPED in attacker.statuses) StatusEffect.HYPED_ATTACK else 1f
            val base = ((2f * level / 5f + 2f) * move.power * attack / max(1, defender.stats.defense)) / 50f + 2f
            val sameType = if (move.type != null && move.type in attacker.types) 1.2f else 1f
            val effectiveness = move.type?.let { TypeMatchups.effectiveness(it, defender.types) } ?: 1f
            val crit = rng.nextInt(16) == 0
            val roll = (85 + rng.nextInt(16)) / 100f
            val damage = max(1, (base * sameType * effectiveness * (if (crit) 1.5f else 1f) * roll).toInt()).coerceAtMost(defender.hp)
            defender = defender.copy(hp = defender.hp - damage)
            attacker = attacker.copy(damageDealt = attacker.damageDealt + damage)
            events += BattleEvent.Hit(target, defender.snap.name, damage, effectiveness, crit, defender.hp, defender.stats.hp)
        }

        val status = move.status
        if (status != null && !status.selfBuff && !defender.fainted && status !in defender.statuses && rng.nextInt(100) < move.statusChance) {
            defender = defender.copy(statuses = defender.statuses + (status to status.turns))
            events += BattleEvent.StatusApplied(target, defender.snap.name, status)
        }
        if (defender.fainted) {
            attacker = attacker.copy(knockouts = attacker.knockouts + 1)
            events += BattleEvent.Fainted(target, defender.snap.name)
        }
        update(side, attacker)
        update(target, defender)
    }

    /** Best performer on a side: most damage, then knockouts. */
    fun mvp(state: BattleState, side: Int): Fighter =
        state.sides[side].fighters.maxWith(compareBy<Fighter> { it.damageDealt }.thenBy { it.knockouts })

    /** Replays a whole battle from its start state and the recorded actions. */
    fun replay(start: BattleState, actions: List<Pair<BattleAction, BattleAction>>): List<BattleState> =
        actions.runningFold(start) { s, (a, b) -> resolve(s, a, b) }
}

/**
 * Practice opponent. Usually picks the move with the best expected damage
 * (type matchup, same-type bonus, accuracy), sometimes plays a status move,
 * sometimes just does something random so it's beatable.
 */
object BattleAi {
    fun choose(state: BattleState, side: Int, random: Random = Random.Default): BattleAction {
        val legal = state.legalActions(side)
        val me = state.sides[side].current
        val foe = state.sides[1 - side].current
        if (random.nextInt(100) < 15) return legal.filter { it != BattleAction.Forfeit }.random(random)

        // Switch out of a terrible matchup now and then.
        val switches = legal.filterIsInstance<BattleAction.Switch>()
        if (switches.isNotEmpty() && me.hpFraction < 0.3f && random.nextInt(100) < 35) {
            return switches.maxBy { sw -> bestValue(state.sides[side].fighters[sw.index], foe) }
        }
        val moves = legal.filterIsInstance<BattleAction.Move>()
        return moves.maxByOrNull { value(me, foe, it.slot) + random.nextFloat() * 8f } ?: legal.first()
    }

    private fun bestValue(f: Fighter, foe: Fighter) = f.moves.indices.maxOfOrNull { value(f, foe, it) } ?: 0f

    private fun value(me: Fighter, foe: Fighter, slot: Int): Float {
        if (slot < 0) return 10f
        val move = me.moves[slot]
        if (move.isBuff) return if (move.status in me.statuses) 0f else 42f
        val eff = move.type?.let { TypeMatchups.effectiveness(it, foe.types) } ?: 1f
        val stab = if (move.type != null && move.type in me.types) 1.2f else 1f
        var v = move.power * eff * stab * move.accuracy / 100f
        if (move.status != null && move.status !in foe.statuses) v += move.statusChance * 0.25f
        return v
    }
}

private fun BattleState.legalActions(side: Int) = BattleEngine.legalActions(this, side)
