package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object CheckOnBro {
    /**
     * Picks a bro to check on, weighted toward whoever you haven't checked on
     * in the longest time. Never returns [lastRecommendedId] unless it's the
     * only bro in the dex.
     */
    fun recommend(
        bros: List<Bro>,
        lastRecommendedId: Long?,
        now: Long = System.currentTimeMillis(),
        random: Random = Random.Default,
    ): Bro? {
        if (bros.isEmpty()) return null
        val candidates = bros.filter { it.id != lastRecommendedId }.ifEmpty { return bros.first() }
        val weights = candidates.map { bro ->
            val since = now - (bro.lastCheckIn ?: bro.catchDate)
            TimeUnit.MILLISECONDS.toDays(since).coerceAtLeast(0) + 1.0
        }
        var roll = random.nextDouble() * weights.sum()
        candidates.forEachIndexed { i, bro ->
            roll -= weights[i]
            if (roll <= 0) return bro
        }
        return candidates.last()
    }

    fun daysSinceContact(bro: Bro, now: Long = System.currentTimeMillis()): Long =
        TimeUnit.MILLISECONDS.toDays(now - (bro.lastCheckIn ?: bro.catchDate)).coerceAtLeast(0)
}

object CheckInPrompts {
    fun forBro(bro: Bro): List<String> {
        val fromFacts = bro.facts.shuffled().take(2).map { "Bring up ${it.value} (${it.category.lowercase()})." }
        val fromType = when (bro.primaryType) {
            BroType.ROAD_RAGER -> "Ask what the traffic did to them this week."
            BroType.BAD_DRIVER -> "Offer to drive next time. For everyone's sake."
            BroType.YAPPER -> "Call them. You've got an hour, right?"
            BroType.GHOST -> "Send a meme. Low pressure, they might even reply."
            BroType.GYM_RAT -> "Ask what they're training for lately."
            BroType.FOODIE -> "Ask for their latest food spot rec."
            BroType.GAMER -> "Ask if they're down for a match tonight."
            BroType.NERD -> "Send an article and ask for their hot take."
            BroType.SPORTS_FAN -> "Talk trash about this week's game."
            BroType.PARTY_ANIMAL -> "Start planning the next hangout."
            BroType.CHILL_GUY -> "Low-key invite: coffee, walk, nothing fancy."
            BroType.CHAOS_AGENT -> "Ask for the latest unhinged story."
            BroType.MAIN_CHARACTER -> "Ask what the latest episode of their life is."
            BroType.ALWAYS_LATE -> "Make a plan. Tell them it starts 30 minutes early."
            BroType.CRYPTO_BRO -> "Ask how the portfolio's doing. Brace yourself."
            BroType.OUTDOORSY -> "Pitch a hike or a day outside."
            BroType.WINGMAN -> "Ask how their love life's going for once."
        }
        return fromFacts + fromType + "Or just: \"yo, been a minute. how you doing?\""
    }
}
