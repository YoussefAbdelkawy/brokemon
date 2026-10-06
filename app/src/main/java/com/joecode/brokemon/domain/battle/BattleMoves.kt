package com.joecode.brokemon.domain.battle

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.CustomMoveSpec
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionMoves

/** Every status effect in one place. Small on purpose. */
enum class StatusEffect(val label: String, val description: String, val turns: Int, val selfBuff: Boolean) {
    CRINGED("Cringed", "30% chance to skip a turn.", 3, selfBuff = false),
    LEFT_ON_READ("Left on Read", "Can't use the same move twice in a row.", 3, selfBuff = false),
    HYPED("Hyped", "Attack up for 2 turns.", 2, selfBuff = true),
    SLEEPY("Sleepy", "Speed down.", 3, selfBuff = false),
    MAIN_CHARACTER("Main Character", "Next move always hits.", 99, selfBuff = true);

    companion object {
        const val CRINGE_SKIP_PERCENT = 30
        const val HYPED_ATTACK = 1.5f
        const val SLEEPY_SPEED = 0.5f
    }
}

/**
 * A battle move. [type] null = a plain "bro" move with no type matchup.
 * [energy] is how many times it can be used in one battle.
 */
data class BattleMove(
    val key: String,
    val name: String,
    val type: BroType?,
    val power: Int,
    val accuracy: Int,
    val energy: Int,
    val status: StatusEffect? = null,
    val statusChance: Int = 0,
    val flavor: String = "",
    /** Evolution score needed before a bro can learn it. */
    val unlockScore: Int = 0,
) {
    val isBuff: Boolean get() = power == 0 && status?.selfBuff == true
}

/** Fixed power presets for user-made signature moves, so nobody makes a 999-power move. */
enum class MoveTemplate(val label: String, val power: Int, val accuracy: Int, val energy: Int, val status: StatusEffect? = null, val statusChance: Int = 0, val blurb: String) {
    STRIKE("Strike", 70, 95, 10, blurb = "Reliable hit."),
    HEAVY("Heavy", 95, 75, 5, blurb = "Big hit, might miss."),
    TRICK("Trick", 45, 100, 15, StatusEffect.CRINGED, 40, "Weak hit, often leaves them Cringed."),
    HYPE("Hype", 0, 100, 10, StatusEffect.HYPED, 100, "Hypes yourself up: attack up for 2 turns."),
    SNOOZE("Snooze", 35, 100, 15, StatusEffect.SLEEPY, 50, "Light hit that can make them Sleepy."),
    SPOTLIGHT("Spotlight", 0, 100, 6, StatusEffect.MAIN_CHARACTER, 100, "Your next move can't miss."),
}

/** One move as it travels in a team snapshot: catalog/evolution moves by key, signature moves with their template. */
data class MoveSpec(val key: String, val name: String = "", val type: String? = null, val template: String? = null)

object MoveCatalog {
    /** Moves anyone can learn as the friendship grows. */
    val catalog: List<BattleMove> = listOf(
        BattleMove("cat:awkward_wave", "Awkward Wave", null, 40, 100, 30, flavor = "A wave nobody returned.", unlockScore = 0),
        BattleMove("cat:dap_up", "Dap Up", null, 50, 95, 25, flavor = "Firm. Too firm.", unlockScore = 0),
        BattleMove("cat:side_eye", "Side Eye", null, 35, 100, 20, StatusEffect.CRINGED, 30, "Says nothing. Says everything.", 8),
        BattleMove("cat:group_chat_ping", "Group Chat Ping", null, 55, 95, 20, flavor = "@everyone at 3am.", unlockScore = 8),
        BattleMove("cat:left_on_read", "Left You on Read", null, 45, 90, 15, StatusEffect.LEFT_ON_READ, 100, "Seen 2h ago.", 14),
        BattleMove("cat:hype_speech", "Hype Speech", null, 0, 100, 10, StatusEffect.HYPED, 100, "LET'S GOOO.", 14),
        BattleMove("cat:six_seven", "6,7!", null, 70, 90, 12, StatusEffect.CRINGED, 30, "Nobody knows why. Everybody says it.", 20),
        BattleMove("cat:yawn", "Contagious Yawn", null, 30, 100, 12, StatusEffect.SLEEPY, 80, "Now everyone's tired.", 20),
        BattleMove("cat:voice_note_barrage", "Voice Note Barrage", null, 80, 85, 10, flavor = "Seven of them. Each 4 minutes.", unlockScore = 26),
        BattleMove("cat:deflected_cringe", "Deflected with Cringe", null, 65, 95, 10, StatusEffect.CRINGED, 50, "Uno reverse, but awkward.", 30),
        BattleMove("cat:main_character_moment", "Main Character Moment", null, 0, 100, 6, StatusEffect.MAIN_CHARACTER, 100, "Slow-mo. Wind in hair.", 36),
        BattleMove("cat:ultimate_roast", "Ultimate Roast", null, 100, 80, 5, flavor = "Ends friendships (temporarily).", unlockScore = 40),
    )

    /** Used when every equipped move is out of energy. Can't run out. */
    val panicText = BattleMove("panic", "Panic Text", null, 30, 100, Int.MAX_VALUE, flavor = "\"u up?\"")

    private val byKey = catalog.associateBy { it.key }

    private fun evoKey(type: BroType, index: Int) = "evo:${type.name}:$index"

    /** The two moves each type learns on evolving, as battle moves. */
    fun evolutionMove(type: BroType, index: Int): BattleMove? {
        val name = EvolutionMoves.all(type).getOrNull(index) ?: return null
        return BattleMove(
            key = evoKey(type, index),
            name = name,
            type = type,
            power = if (index == 0) 80 else 95,
            accuracy = if (index == 0) 90 else 85,
            energy = if (index == 0) 10 else 6,
            flavor = "${type.label} signature.",
            unlockScore = EvolutionStageScores.forIndex(index),
        )
    }

    /** A signature move (from the bro's own move list) built from a fixed template. */
    fun signature(name: String, type: BroType, template: MoveTemplate) = BattleMove(
        key = "sig:${name.take(24)}",
        name = name.take(24),
        type = if (template.power == 0) null else type,
        power = template.power,
        accuracy = template.accuracy,
        energy = template.energy,
        status = template.status,
        statusChance = template.statusChance,
        flavor = "${template.label} signature move.",
    )

    /** Signature moves the user didn't customize get a stable template from their name. */
    fun defaultTemplate(name: String): MoveTemplate {
        val damaging = listOf(MoveTemplate.STRIKE, MoveTemplate.STRIKE, MoveTemplate.HEAVY, MoveTemplate.TRICK, MoveTemplate.SNOOZE)
        return damaging[name.lowercase().hashCode().mod(damaging.size)]
    }

    /** Everything this bro can equip right now. */
    fun available(bro: Bro, score: Int = Evolution.score(bro)): List<BattleMove> {
        val evo = bro.types.flatMap { t -> (0..1).mapNotNull { evolutionMove(t, it) } }.filter { it.unlockScore <= score }
        val evoNames = evo.map { it.name }.toSet()
        val customs = bro.resolvedBattle.customs.associateBy { it.name }
        val signatures = (bro.moves + bro.resolvedBattle.customs.map { it.name }).distinct()
            .filterNot { it in evoNames }
            .map { name -> signatureFor(name, bro.primaryType, customs[name]) }
        return signatures + evo + catalog.filter { it.unlockScore <= score }
    }

    private fun signatureFor(name: String, primary: BroType, custom: CustomMoveSpec?): BattleMove {
        val template = custom?.let { c -> MoveTemplate.entries.firstOrNull { it.name == c.template } } ?: defaultTemplate(name)
        val type = custom?.let { BroType.from(it.type) } ?: primary
        return signature(name, type, template)
    }

    /** The 4 moves the bro brings: the saved loadout, or a sensible default. */
    fun equipped(bro: Bro, score: Int = Evolution.score(bro)): List<BattleMove> {
        val all = available(bro, score)
        val saved = bro.resolvedBattle.equipped.mapNotNull { key -> all.firstOrNull { it.key == key } }.distinctBy { it.key }
        if (saved.isNotEmpty()) return saved.take(4)
        val damaging = all.filter { it.power > 0 }.sortedByDescending { it.power * it.accuracy }
        val support = all.filter { it.power == 0 || it.status != null }.take(1)
        return (damaging.take(3) + support + damaging.drop(3)).distinctBy { it.key }.take(4)
    }

    fun specOf(move: BattleMove): MoveSpec = when {
        move.key.startsWith("sig:") -> MoveSpec(
            key = move.key,
            name = move.name,
            type = move.type?.name,
            template = MoveTemplate.entries.firstOrNull {
                it.power == move.power && it.accuracy == move.accuracy && it.energy == move.energy && it.status == move.status
            }?.name ?: MoveTemplate.STRIKE.name,
        )
        else -> MoveSpec(move.key)
    }

    /**
     * Turns a received move spec back into a move, enforcing the rules: catalog
     * and evolution moves must exist and be unlocked at that level; signature
     * moves must use a fixed template. Returns null for anything invalid.
     */
    fun resolve(spec: MoveSpec, level: Int, types: List<BroType>): BattleMove? {
        val score = BattleMath.scoreForLevel(level)
        return when {
            spec.key.startsWith("cat:") -> byKey[spec.key]?.takeIf { it.unlockScore <= score }
            spec.key.startsWith("evo:") -> {
                val parts = spec.key.split(":")
                val type = BroType.from(parts.getOrNull(1)) ?: return null
                val index = parts.getOrNull(2)?.toIntOrNull() ?: return null
                if (type !in types) return null
                evolutionMove(type, index)?.takeIf { it.unlockScore <= score }
            }
            spec.key.startsWith("sig:") -> {
                val template = MoveTemplate.entries.firstOrNull { it.name == spec.template } ?: return null
                val name = spec.name.trim().take(24).ifBlank { return null }
                val type = spec.type?.let { BroType.from(it) } ?: types.firstOrNull() ?: return null
                signature(name, type, template)
            }
            else -> null
        }
    }
}

/** Evolution moves unlock with the evolution stages (Homie at 20, Day One at 40). */
internal object EvolutionStageScores {
    fun forIndex(index: Int): Int = if (index == 0) 20 else 40
}
