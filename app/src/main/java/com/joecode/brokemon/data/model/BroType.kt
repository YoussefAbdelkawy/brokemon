package com.joecode.brokemon.data.model

/**
 * Bro archetypes. Stored in Room as the enum name (String), so new types can
 * be added without a migration. Every type needs an entry in TypeMatchups and
 * EvolutionMoves (unit tests check this).
 */
enum class BroType(val label: String, val argb: Long, val blurb: String) {
    ROAD_RAGER("Road Rager", 0xFFE63946, "Honks first, asks questions never"),
    BAD_DRIVER("Bad Driver", 0xFFFF7A1A, "Parallel parking is a group activity"),
    YAPPER("Yapper", 0xFFFFC23D, "Voice notes: seven minutes minimum"),
    GHOST("Ghost", 0xFF9AA5B8, "Left you on read since 2019"),
    GYM_RAT("Gym Rat", 0xFFFF4D6D, "Never skips leg day, never lets you forget"),
    FOODIE("Foodie", 0xFFF4A261, "Knows the spot. Always knows the spot"),
    GAMER("Gamer", 0xFF8B5CF6, "One more match, then bed (lie)"),
    NERD("Nerd", 0xFF3A86FF, "Has a fun fact for literally everything"),
    SPORTS_FAN("Sports Fan", 0xFF9BE15D, "Will explain the offside rule unprompted"),
    PARTY_ANIMAL("Party Animal", 0xFFFF4FA3, "Last to leave, first to plan the next one"),
    CHILL_GUY("Chill Guy", 0xFF2EC4B6, "Unbothered. Moisturized. In their lane"),
    CHAOS_AGENT("Chaos Agent", 0xFFD62AD0, "Every plan becomes a story"),
    MAIN_CHARACTER("Main Character", 0xFFB892FF, "Lives life like the camera's always rolling"),
    ALWAYS_LATE("Always Late", 0xFFC9A27E, "\"On my way\" (hasn't left the house)"),
    CRYPTO_BRO("Crypto Bro", 0xFFFFD54A, "Still holding. Still explaining it"),
    OUTDOORSY("Outdoorsy", 0xFF4CAF50, "Would rather be on a trail right now"),
    WINGMAN("Wingman", 0xFF5E9BFF, "Hypes you up to total strangers");

    companion object {
        /** Types from the first build, mapped so older cards still resolve. */
        private val legacy = mapOf(
            "HYPE" to MAIN_CHARACTER,
            "CHILL" to CHILL_GUY,
            "GYM" to GYM_RAT,
            "BRAIN" to NERD,
            "PARTY" to PARTY_ANIMAL,
            "ARTSY" to MAIN_CHARACTER,
            "OUTDOORS" to OUTDOORSY,
            "CHAOS" to CHAOS_AGENT,
            "SPORTS" to SPORTS_FAN,
            "MYSTIC" to GHOST,
        )

        fun from(name: String?): BroType? =
            entries.firstOrNull { it.name == name } ?: legacy[name]
    }
}
