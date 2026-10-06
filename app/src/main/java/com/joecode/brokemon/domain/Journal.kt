package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.TrainerFrame
import kotlin.math.sqrt

enum class RewardKind(val label: String) { BADGE("Badge"), FRAME("Card frame"), PART("Sprite part") }

/** Things the Trainer's Journal hands out. Stored by name, so only append. */
enum class Reward(val label: String, val kind: RewardKind, val description: String) {
    ROOKIE_BADGE("Rookie Badge", RewardKind.BADGE, "Every legend starts somewhere."),
    BRONZE_FRAME("Bronze Frame", RewardKind.FRAME, "A bronze trim for your Trainer Card."),
    SCHOLAR_BADGE("Scholar Badge", RewardKind.BADGE, "You actually listen when they talk."),
    TRAINER_CAP("Trainer Cap", RewardKind.PART, "New hat for you and your bros' characters."),
    SILVER_FRAME("Silver Frame", RewardKind.FRAME, "Shiny silver trim for your Trainer Card."),
    GOLD_FRAME("Gold Holo Frame", RewardKind.FRAME, "The flex. A gold holo Trainer Card."),
    MASTER_BADGE("Bro Master Badge", RewardKind.BADGE, "Finished the whole Journal."),
    ;

    val frame: TrainerFrame?
        get() = when (this) {
            BRONZE_FRAME -> TrainerFrame.BRONZE
            SILVER_FRAME -> TrainerFrame.SILVER
            GOLD_FRAME -> TrainerFrame.GOLD
            else -> null
        }
}

/** Starter quests, in order. Each one teaches a feature and pays out a reward. */
enum class Quest(val title: String, val hint: String, val reward: Reward) {
    MAKE_TRAINER_CARD("Make your Trainer Card", "Catch yourself first: build your own card.", Reward.ROOKIE_BADGE),
    CATCH_FIRST_BRO("Catch your first Bro", "Tap CATCH and add a friend.", Reward.BRONZE_FRAME),
    ADD_FACT("Add a Fact", "Open a card and add something you know about them.", Reward.SCHOLAR_BADGE),
    LOG_MEMORY("Log a Memory", "Add a photo, video or voice note to a card.", Reward.TRAINER_CAP),
    CHECK_IN("Check in", "Talked to a bro today? Hit Check in on their card.", Reward.SILVER_FRAME),
    TRADE_QR("Trade a QR", "Show a card's QR code to a friend, or scan theirs.", Reward.GOLD_FRAME),
}

data class QuestStatus(val quest: Quest, val done: Boolean, val claimed: Boolean) {
    val claimable: Boolean get() = done && !claimed
}

object Journal {

    fun status(hasTrainer: Boolean, bros: List<Bro>, tradedQr: Boolean, claimed: Set<String>): List<QuestStatus> =
        Quest.entries.map { quest ->
            val done = when (quest) {
                Quest.MAKE_TRAINER_CARD -> hasTrainer
                Quest.CATCH_FIRST_BRO -> bros.isNotEmpty()
                Quest.ADD_FACT -> bros.any { it.facts.isNotEmpty() }
                Quest.LOG_MEMORY -> bros.any { it.memories.isNotEmpty() }
                Quest.CHECK_IN -> bros.any { it.checkInCount > 0 }
                Quest.TRADE_QR -> tradedQr || bros.any { it.isTraded }
            }
            // A claimed quest stays claimed even if, say, the bro was released later.
            QuestStatus(quest, done || quest.name in claimed, quest.name in claimed)
        }

    /** Rewards the user owns: one per claimed quest, plus Bro Master for finishing. */
    fun unlocked(claimed: Set<String>): Set<Reward> {
        val fromQuests = Quest.entries.filter { it.name in claimed }.map { it.reward }.toSet()
        return if (fromQuests.size == Quest.entries.size) fromQuests + Reward.MASTER_BADGE else fromQuests
    }

    fun unlockedFrames(claimed: Set<String>): List<TrainerFrame> =
        listOf(TrainerFrame.BASIC) + unlocked(claimed).mapNotNull { it.frame }.sortedBy { it.ordinal }
}

data class LevelInfo(val level: Int, val xp: Int, val levelStartXp: Int, val nextLevelXp: Int?) {
    val progress: Float
        get() = nextLevelXp?.let { ((xp - levelStartXp).toFloat() / (it - levelStartXp)).coerceIn(0f, 1f) } ?: 1f
    val toNext: Int? get() = nextLevelXp?.let { it - xp }
}

/**
 * Trainer level, earned by actually being a good friend. Level N needs
 * 50 * (N - 1)^2 XP total, so early levels come fast and later ones take a while.
 */
object TrainerLevel {
    const val TRAINER_CARD_XP = 50
    const val CATCH_XP = 100
    const val CHECK_IN_XP = 25
    const val MEMORY_XP = 40
    const val FACT_XP = 10
    const val QUEST_XP = 100
    const val MAX_LEVEL = 99

    fun xp(bros: List<Bro>, hasTrainer: Boolean, claimedQuests: Int): Int =
        (if (hasTrainer) TRAINER_CARD_XP else 0) +
            bros.size * CATCH_XP +
            bros.sumOf { it.checkInCount } * CHECK_IN_XP +
            bros.sumOf { it.memories.size } * MEMORY_XP +
            bros.sumOf { it.facts.size } * FACT_XP +
            claimedQuests * QUEST_XP

    fun xpFor(level: Int): Int = 50 * (level - 1) * (level - 1)

    fun info(xp: Int): LevelInfo {
        val level = (sqrt(xp / 50.0).toInt() + 1).coerceIn(1, MAX_LEVEL)
        return LevelInfo(
            level = level,
            xp = xp,
            levelStartXp = xpFor(level),
            nextLevelXp = if (level >= MAX_LEVEL) null else xpFor(level + 1),
        )
    }
}
