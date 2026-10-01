package com.joecode.brokemon.data.model

import kotlin.random.Random

data class BroStats(
    val hype: Int = 50,
    val loyalty: Int = 50,
    val humor: Int = 50,
    val brains: Int = 50,
    val chaos: Int = 50,
    val clutch: Int = 50,
) {
    fun asList(): List<Pair<String, Int>> = listOf(
        "HYPE" to hype,
        "LOYAL" to loyalty,
        "HUMOR" to humor,
        "BRAIN" to brains,
        "CHAOS" to chaos,
        "CLUTCH" to clutch,
    )

    fun toArray(): List<Int> = listOf(hype, loyalty, humor, brains, chaos, clutch)

    val total: Int get() = hype + loyalty + humor + brains + chaos + clutch

    companion object {
        const val MIN = 1
        const val MAX = 100

        fun random(random: Random = Random.Default) = BroStats(
            hype = random.nextInt(25, 96),
            loyalty = random.nextInt(25, 96),
            humor = random.nextInt(25, 96),
            brains = random.nextInt(25, 96),
            chaos = random.nextInt(25, 96),
            clutch = random.nextInt(25, 96),
        )

        fun fromArray(values: List<Int>): BroStats {
            fun at(i: Int) = values.getOrElse(i) { 50 }.coerceIn(MIN, MAX)
            return BroStats(at(0), at(1), at(2), at(3), at(4), at(5))
        }
    }
}
