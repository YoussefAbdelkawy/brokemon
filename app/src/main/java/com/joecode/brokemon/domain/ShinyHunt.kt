package com.joecode.brokemon.domain

import kotlin.random.Random

/**
 * Shinies are earned, not picked: every logged memory has a small chance to
 * turn the bro shiny. Only one roll per bro per day, so spamming empty
 * memories can't farm it.
 */
object ShinyHunt {
    /** 1 in [ODDS] per roll. Tune here. */
    const val ODDS = 20

    fun canRoll(alreadyShiny: Boolean, lastRollDay: Long?, today: Long): Boolean =
        !alreadyShiny && lastRollDay != today

    fun roll(random: Random = Random.Default): Boolean = random.nextInt(ODDS) == 0
}
