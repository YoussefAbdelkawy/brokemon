package com.joecode.brokemon.data.model

enum class Rarity(val label: String, val code: String) {
    COMMON("Common", "C"),
    RARE("Rare", "R"),
    LEGENDARY("Legendary", "L");

    companion object {
        fun fromCode(code: String?): Rarity = entries.firstOrNull { it.code == code } ?: COMMON
    }
}
