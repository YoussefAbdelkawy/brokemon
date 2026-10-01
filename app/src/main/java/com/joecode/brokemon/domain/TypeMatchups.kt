package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.BroType.ARTSY
import com.joecode.brokemon.data.model.BroType.BRAIN
import com.joecode.brokemon.data.model.BroType.CHAOS
import com.joecode.brokemon.data.model.BroType.CHILL
import com.joecode.brokemon.data.model.BroType.FOODIE
import com.joecode.brokemon.data.model.BroType.GAMER
import com.joecode.brokemon.data.model.BroType.GYM
import com.joecode.brokemon.data.model.BroType.HYPE
import com.joecode.brokemon.data.model.BroType.MYSTIC
import com.joecode.brokemon.data.model.BroType.OUTDOORS
import com.joecode.brokemon.data.model.BroType.PARTY
import com.joecode.brokemon.data.model.BroType.SPORTS

/** Lighthearted matchup chart. Every type beats exactly two and loses to exactly two. */
object TypeMatchups {

    private val chart: Map<BroType, Map<BroType, String>> = mapOf(
        HYPE to mapOf(CHILL to "drags them off the couch", BRAIN to "out-talks the fun facts"),
        CHILL to mapOf(CHAOS to "is simply unbothered", GYM to "says rest day is a lifestyle"),
        GAMER to mapOf(BRAIN to "has a speedrun for that", MYSTIC to "already saw the patch notes"),
        GYM to mapOf(FOODIE to "meal-preps them into silence", GAMER to "spots them a reality check"),
        FOODIE to mapOf(PARTY to "controls the snack table", OUTDOORS to "brought the better trail mix"),
        BRAIN to mapOf(SPORTS to "has the advanced stats", CHAOS to "saw the plan falling apart"),
        PARTY to mapOf(CHILL to "turned the chill hang into a rager", ARTSY to "made the vibe the art"),
        ARTSY to mapOf(MYSTIC to "painted the prophecy first", HYPE to "made a mood board of the hype"),
        OUTDOORS to mapOf(GAMER to "touched grass on their behalf", GYM to "hiked further, no mirror"),
        CHAOS to mapOf(PARTY to "is the after-party", HYPE to "escalated beyond hype"),
        SPORTS to mapOf(OUTDOORS to "turned the hike into a race", FOODIE to "wins the hot-dog contest"),
        MYSTIC to mapOf(SPORTS to "called the final score", ARTSY to "read their aura"),
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
