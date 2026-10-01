package com.joecode.brokemon.data.model

import kotlin.random.Random

data class StatInfo(val label: String, val blurb: String)

data class BroStats(
    val rizz: Int = 50,
    val aura: Int = 50,
    val yap: Int = 50,
    val loyalty: Int = 50,
    val chaos: Int = 50,
    val flake: Int = 50,
) {
    fun asList(): List<Pair<StatInfo, Int>> = INFO.zip(toArray())

    fun toArray(): List<Int> = listOf(rizz, aura, yap, loyalty, chaos, flake)

    val total: Int get() = toArray().sum()

    companion object {
        const val MIN = 1
        const val MAX = 100

        val INFO = listOf(
            StatInfo("RIZZ", "Charm. Can talk to anyone"),
            StatInfo("AURA", "Main-character energy"),
            StatInfo("YAP", "How long they can talk for"),
            StatInfo("LOYAL", "Ride or die level"),
            StatInfo("CHAOS", "How often plans go sideways"),
            StatInfo("FLAKE", "Chance they cancel last minute"),
        )

        fun random(random: Random = Random.Default) =
            fromArray(List(6) { random.nextInt(20, 96) })

        fun fromArray(values: List<Int>): BroStats {
            fun at(i: Int) = values.getOrElse(i) { 50 }.coerceIn(MIN, MAX)
            return BroStats(at(0), at(1), at(2), at(3), at(4), at(5))
        }
    }
}
