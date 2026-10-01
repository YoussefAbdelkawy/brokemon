package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.BroType.*

/** Lighthearted matchup chart. Every type beats exactly two and loses to exactly two. */
object TypeMatchups {

    private val chart: Map<BroType, Map<BroType, String>> = mapOf(
        ROAD_RAGER to mapOf(BAD_DRIVER to "honks them off the road", FOODIE to "won't stop for the drive-thru"),
        BAD_DRIVER to mapOf(YAPPER to "takes a turn so hard the yapping stops", GAMER to "makes real driving scarier than any game"),
        YAPPER to mapOf(GHOST to "sends 40 texts until they answer", NERD to "out-talks every fun fact"),
        GHOST to mapOf(GYM_RAT to "never shows up to spot them", SPORTS_FAN to "left the watch-party chat on read"),
        GYM_RAT to mapOf(FOODIE to "meal-preps them into silence", PARTY_ANIMAL to "is in bed by 9 for a 5am lift"),
        FOODIE to mapOf(GAMER to "lures them off the couch with snacks", CHILL_GUY to "drags them to a two-hour brunch line"),
        GAMER to mapOf(NERD to "has a speedrun for that", CHAOS_AGENT to "has seen worse chaos in ranked"),
        NERD to mapOf(SPORTS_FAN to "has the advanced stats", MAIN_CHARACTER to "fact-checks the origin story"),
        SPORTS_FAN to mapOf(PARTY_ANIMAL to "turned the party into a watch party", ALWAYS_LATE to "would never miss kickoff"),
        PARTY_ANIMAL to mapOf(CHILL_GUY to "turned the chill hang into a rager", CRYPTO_BRO to "spent the gains on bottle service"),
        CHILL_GUY to mapOf(CHAOS_AGENT to "is simply unbothered", OUTDOORSY to "says the couch is also nature"),
        CHAOS_AGENT to mapOf(MAIN_CHARACTER to "hijacked the plot", WINGMAN to "ruined the setup on purpose"),
        MAIN_CHARACTER to mapOf(ALWAYS_LATE to "made the big entrance first", ROAD_RAGER to "made the traffic part of the montage"),
        ALWAYS_LATE to mapOf(CRYPTO_BRO to "missed the whole pitch", BAD_DRIVER to "was never in the car on time anyway"),
        CRYPTO_BRO to mapOf(OUTDOORSY to "bought the mountain as an NFT", YAPPER to "out-yaps them about the blockchain"),
        OUTDOORSY to mapOf(WINGMAN to "is too busy hiking to need a wingman", GHOST to "found them hiding on the trail"),
        WINGMAN to mapOf(ROAD_RAGER to "calms them down at the red light", GYM_RAT to "gets them a number at the gym"),
    )

    fun beats(attacker: BroType): Set<BroType> = chart[attacker]?.keys.orEmpty()

    fun beatenBy(defender: BroType): Set<BroType> =
        chart.filterValues { defender in it }.keys

    fun reason(attacker: BroType, defender: BroType): String? = chart[attacker]?.get(defender)

    /** 2x per type advantage, 0.5x per disadvantage, multiplied across a dual-typed defender. */
    fun effectiveness(attacker: BroType, defender: List<BroType>): Float =
        defender.fold(1f) { acc, d ->
            when {
                d in beats(attacker) -> acc * 2f
                attacker in beats(d) -> acc * 0.5f
                else -> acc
            }
        }

    data class TypeReport(
        val opponent: BroType,
        /** Members who hit this type super effectively. */
        val counters: List<Bro>,
        /** Members this type hits super effectively. */
        val threatened: List<Bro>,
    ) {
        val verdict: Verdict
            get() = when {
                counters.size > threatened.size -> Verdict.STRONG
                counters.size < threatened.size -> Verdict.WEAK
                else -> Verdict.EVEN
            }
    }

    enum class Verdict(val label: String) { STRONG("Squad wins"), EVEN("Even fight"), WEAK("Watch out") }

    fun squadReport(members: List<Bro>): List<TypeReport> = BroType.entries.map { opponent ->
        TypeReport(
            opponent = opponent,
            counters = members.filter { m -> m.types.any { opponent in beats(it) } },
            threatened = members.filter { m -> m.types.any { it in beats(opponent) } },
        )
    }

    data class SquadClash(val homeScore: Float, val awayScore: Float, val highlight: String?)

    /** Sum of every member-vs-member effectiveness, one way and the other. */
    fun clash(home: List<Bro>, away: List<Bro>): SquadClash {
        var homeScore = 0f
        var awayScore = 0f
        var best: Triple<Bro, Bro, Float>? = null
        for (h in home) for (a in away) {
            val hv = h.types.maxOfOrNull { effectiveness(it, a.types) } ?: 1f
            val av = a.types.maxOfOrNull { effectiveness(it, h.types) } ?: 1f
            homeScore += hv
            awayScore += av
            if (best == null || hv > best.third) best = Triple(h, a, hv)
        }
        val highlight = best?.takeIf { it.third > 1f }?.let { (h, a, _) ->
            val attackType = h.types.first { t -> a.types.any { it in beats(t) } }
            val defType = a.types.first { it in beats(attackType) }
            "${h.name} ${reason(attackType, defType)} vs ${a.name}"
        }
        return SquadClash(homeScore, awayScore, highlight)
    }
}
