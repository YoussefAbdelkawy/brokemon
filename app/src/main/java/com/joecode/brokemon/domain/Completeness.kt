package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro

/** Things a card can still be missing. A Quick Catch only needs a name; the rest fills in later. */
enum class Missing(val label: String, val nudge: String, val action: String) {
    MOVES("moves", "add moves", "Add moves"),
    FACT("a fact", "add a fact", "Add a fact"),
    MEMORY("a memory", "log a memory", "Add memory"),
    DEX_ENTRY("a dex entry", "write a dex entry", "Write entry"),
    HABITAT("a habitat", "set a habitat", "Set habitat"),
}

data class CardCompleteness(val percent: Int, val missing: List<Missing>) {
    val complete: Boolean get() = missing.isEmpty()

    /** "Card 40% complete: add moves?" */
    val nudge: String? get() = missing.firstOrNull()?.let { "Card $percent% complete: ${it.nudge}?" }
}

object Completeness {
    /** Name, type and look are always there, so a fresh Quick Catch starts at 20%. */
    private const val BASE = 20
    private const val PER_ITEM = 16

    fun of(bro: Bro): CardCompleteness {
        val missing = buildList {
            if (bro.moves.isEmpty()) add(Missing.MOVES)
            if (bro.facts.isEmpty()) add(Missing.FACT)
            if (bro.memories.isEmpty()) add(Missing.MEMORY)
            if (bro.flavorText.isBlank()) add(Missing.DEX_ENTRY)
            if (bro.habitat.isNullOrBlank()) add(Missing.HABITAT)
        }
        val done = Missing.entries.size - missing.size
        return CardCompleteness((BASE + done * PER_ITEM).coerceAtMost(100), missing)
    }
}
