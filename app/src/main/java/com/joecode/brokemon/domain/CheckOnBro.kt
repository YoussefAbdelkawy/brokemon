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
            BroType.HYPE -> "Send them something that'll get them hyped."
            BroType.CHILL -> "Low-key invite: coffee, walk, nothing fancy."
            BroType.GAMER -> "Ask if they're down for a match tonight."
            BroType.GYM -> "Ask what they're training for lately."
            BroType.FOODIE -> "Ask for their latest food spot rec."
            BroType.BRAIN -> "Send an article and ask for their hot take."
            BroType.PARTY -> "Start planning the next hangout."
            BroType.ARTSY -> "Ask what they're working on right now."
            BroType.OUTDOORS -> "Pitch a hike or a day outside."
            BroType.CHAOS -> "Ask for the latest unhinged story."
            BroType.SPORTS -> "Talk trash about this week's game."
            BroType.MYSTIC -> "Ask for this month's vibe forecast."
        }
        return fromFacts + fromType + "Or just: \"yo, been a minute. how you doing?\""
    }
}
