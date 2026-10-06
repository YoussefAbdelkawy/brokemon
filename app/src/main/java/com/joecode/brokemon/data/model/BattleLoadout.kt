package com.joecode.brokemon.data.model

/**
 * A bro's battle setup, stored as JSON on the bro (nullable = never set up).
 *
 * - [equipped]: up to 4 move keys ("cat:<id>" for catalog moves, "sig:<name>" for signature moves).
 * - [customs]: signature moves the user made (name + type + a fixed power template).
 * - [winBonus] / [lastBonusDay]: small daily evolution boost from winning, capped.
 * - [championships]: tournament wins with this bro (shows a champion frame).
 */
data class BattleLoadout(
    val equipped: List<String> = emptyList(),
    val customs: List<CustomMoveSpec> = emptyList(),
    val winBonus: Int = 0,
    val lastBonusDay: Long = 0,
    val championships: Int = 0,
)

data class CustomMoveSpec(val name: String, val type: String, val template: String)

/** Gson leaves missing fields null (it skips Kotlin defaults); patch them up. */
@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
fun BattleLoadout.sanitized(): BattleLoadout = copy(
    equipped = (equipped ?: emptyList()).filterNotNull().take(4),
    customs = (customs ?: emptyList()).filterNotNull().filter { it.name != null && it.type != null && it.template != null },
)
