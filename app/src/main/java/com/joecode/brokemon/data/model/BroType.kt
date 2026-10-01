package com.joecode.brokemon.data.model

/**
 * Original bro types. Stored in Room as the enum name (String) so new types
 * can be added without a migration.
 */
enum class BroType(val label: String, val argb: Long, val blurb: String) {
    HYPE("Hype", 0xFFFF7A1A, "Brings the energy to every room"),
    CHILL("Chill", 0xFF2EC4B6, "Unbothered. Moisturized. In their lane"),
    GAMER("Gamer", 0xFF8B5CF6, "One more match, then bed (lie)"),
    GYM("Gym", 0xFFE63946, "Never skips leg day, never lets you forget"),
    FOODIE("Foodie", 0xFFFFC23D, "Knows the spot. Always knows the spot"),
    BRAIN("Brain", 0xFF3A86FF, "Has a fun fact for literally everything"),
    PARTY("Party", 0xFFFF4FA3, "Last to leave, first to plan the next one"),
    ARTSY("Artsy", 0xFFB892FF, "Sees the world in color palettes"),
    OUTDOORS("Outdoors", 0xFF4CAF50, "Would rather be on a trail right now"),
    CHAOS("Chaos", 0xFFD62AD0, "Every plan becomes a story"),
    SPORTS("Sports", 0xFF9BE15D, "Has opinions about your fantasy team"),
    MYSTIC("Mystic", 0xFF5E60CE, "Already knew you were going to text");

    companion object {
        fun from(name: String?): BroType? = entries.firstOrNull { it.name == name }
    }
}
