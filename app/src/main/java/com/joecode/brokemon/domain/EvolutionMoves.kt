package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroType

/** Each type learns one bonus move when it reaches Homie and another at Day One. */
object EvolutionMoves {
    private val byType: Map<BroType, List<String>> = mapOf(
        BroType.ROAD_RAGER to listOf("Horn Solo", "Brake Check"),
        BroType.BAD_DRIVER to listOf("Curb Check", "Wrong-Way Merge"),
        BroType.YAPPER to listOf("7-Min Voice Note", "Story Time"),
        BroType.GHOST to listOf("Left On Read", "Vanish Mid-Plan"),
        BroType.GYM_RAT to listOf("PR Flex", "Protein Splash"),
        BroType.FOODIE to listOf("Secret Menu", "Food Coma"),
        BroType.GAMER to listOf("Rage Quit", "One More Game"),
        BroType.NERD to listOf("Well Actually", "Wiki Spiral"),
        BroType.SPORTS_FAN to listOf("Hot Take", "Jersey Swap"),
        BroType.PARTY_ANIMAL to listOf("Last Call", "Afterparty Summon"),
        BroType.CHILL_GUY to listOf("Unbothered Aura", "Nap Anywhere"),
        BroType.CHAOS_AGENT to listOf("Plot Twist", "Bad Idea Speedrun"),
        BroType.MAIN_CHARACTER to listOf("Slow-Mo Entrance", "Montage Mode"),
        BroType.ALWAYS_LATE to listOf("On My Way (Lie)", "Fashionably Late"),
        BroType.CRYPTO_BRO to listOf("To The Moon", "Buy The Dip"),
        BroType.OUTDOORSY to listOf("Touch Grass", "Sunrise Summit"),
        BroType.WINGMAN to listOf("Hype Intro", "Perfect Assist"),
    )

    fun all(type: BroType): List<String> = byType[type].orEmpty()

    /** Moves learned going from [fromStage] up to [toStage] (stage 1 → first move, stage 2 → second). */
    fun unlocked(type: BroType, fromStage: Int, toStage: Int): List<String> =
        ((fromStage + 1)..toStage).mapNotNull { stage -> all(type).getOrNull(stage - 1) }
}

/** Each type evolves with its own animation, so evolutions don't all look the same. */
enum class EvolutionStyle(val evolvingText: String) {
    POWER_UP("is powering up..."),
    LIGHTNING("is charging up..."),
    GLITCH("is updating..."),
    SPOTLIGHT("is stepping into the spotlight...");

    companion object {
        fun forType(type: BroType): EvolutionStyle = when (type) {
            BroType.GYM_RAT, BroType.SPORTS_FAN, BroType.ROAD_RAGER, BroType.OUTDOORSY -> POWER_UP
            BroType.CHAOS_AGENT, BroType.BAD_DRIVER, BroType.ALWAYS_LATE, BroType.CRYPTO_BRO -> LIGHTNING
            BroType.GAMER, BroType.NERD, BroType.GHOST, BroType.YAPPER -> GLITCH
            BroType.MAIN_CHARACTER, BroType.PARTY_ANIMAL, BroType.WINGMAN, BroType.FOODIE, BroType.CHILL_GUY -> SPOTLIGHT
        }
    }
}
