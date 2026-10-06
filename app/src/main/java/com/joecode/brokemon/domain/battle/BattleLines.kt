package com.joecode.brokemon.domain.battle

import kotlin.random.Random

/**
 * Turns battle events into text. Big pools of original reaction lines so
 * battles don't read the same twice.
 */
object BattleLines {
    private val bigHit = listOf(
        "It hit DIFFERENT.", "That one's going in the group chat.", "Absolutely cooked.", "No way that landed.",
        "Emotional damage.", "Someone check on them.", "That's a core memory now.", "Certified banger of a hit.",
    )
    private val weakHit = listOf(
        "It barely tickled.", "They didn't even look up from their phone.", "Mid. Very mid.", "That was a soft one.",
        "They felt that... a little.", "Low effort. Low damage.",
    )
    private val normalHit = listOf(
        "Solid.", "Clean hit.", "Respect.", "That'll leave a mark.", "Okay, okay, we see you.", "Not bad at all.",
    )
    private val crits = listOf("CRITICAL VIBE!", "Right in the ego!", "A crit! Unreal timing.", "Perfectly timed. Brutal.")
    private val misses = listOf(
        "...and missed. Awkward.", "Swing and a miss.", "It went straight to voicemail.", "Nobody saw that. Good.",
        "Totally whiffed it.", "Left on delivered.",
    )
    private val faints = listOf(
        "%s went AFK.", "%s logged off for the day.", "%s needs a minute. Or a nap.", "%s has left the chat.",
        "%s is touching grass now.", "%s's battery died.",
    )
    private val wins = listOf(
        "%s takes the W!", "%s wins. Group chat is going wild.", "GG. %s runs it.", "%s is built different.",
    )
    private val switches = listOf("Come on out, %s!", "%s, you're up!", "Tag in, %s!", "%s steps in.")

    fun pick(pool: List<String>, random: Random) = pool[random.nextInt(pool.size)]

    /** Text for an event, from the point of view of [mySide] ("you" vs "them"). */
    fun describe(event: BattleEvent, sideNames: List<String>, random: Random): List<String> = when (event) {
        is BattleEvent.Used -> listOf("${event.fighter} used ${event.move}!")
        is BattleEvent.Missed -> listOf(pick(misses, random))
        is BattleEvent.Hit -> buildList {
            if (event.crit) add(pick(crits, random))
            add(
                when {
                    event.effectiveness > 1f -> pick(bigHit, random)
                    event.effectiveness < 1f -> pick(weakHit, random)
                    else -> pick(normalHit, random)
                },
            )
        }
        is BattleEvent.StatusApplied -> listOf(
            when (event.status) {
                StatusEffect.CRINGED -> "${event.fighter} is Cringed!"
                StatusEffect.LEFT_ON_READ -> "${event.fighter} got Left on Read! No repeat moves."
                StatusEffect.HYPED -> "${event.fighter} is HYPED! Attack up!"
                StatusEffect.SLEEPY -> "${event.fighter} is getting Sleepy..."
                StatusEffect.MAIN_CHARACTER -> "${event.fighter} is the Main Character now. Next move can't miss."
            },
        )
        is BattleEvent.StatusBlocked -> listOf("${event.fighter} is too Cringed to move!")
        is BattleEvent.StatusEnded -> listOf("${event.fighter} is no longer ${event.status.label}.")
        is BattleEvent.Fainted -> listOf(pick(faints, random).format(event.fighter))
        is BattleEvent.SwitchedIn -> listOf(pick(switches, random).format(event.fighter))
        is BattleEvent.Forfeited -> listOf("${sideNames[event.side]} forfeited.")
        is BattleEvent.Won -> listOf(pick(wins, random).format(sideNames[event.side]))
        BattleEvent.Draw -> listOf("It's a draw. Everyone's tired.")
    }

    val emotes = listOf("😂", "💀", "GG", "bro…", "🔥", "😤")
}
