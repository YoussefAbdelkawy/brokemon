package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.LookPart
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
    HERO_CAPE("Hero Cape", RewardKind.PART, "Back accessory. Earned when a memory turns a bro shiny."),
    POW_BACKDROP("POW! Backdrop", RewardKind.PART, "Comic backdrop. Earned by finishing the whole Journal."),
    CHAMPION_GLOW("Trophy Glow", RewardKind.PART, "Champion backdrop. Earned by winning a tournament."),
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

    /**
     * Rewards the user owns: one per claimed quest, Bro Master + the POW! backdrop
     * for finishing, the Hero Cape for an earned shiny, Trophy Glow for a tournament win.
     */
    fun unlocked(claimed: Set<String>, shinyEarned: Boolean = false, champion: Boolean = false): Set<Reward> = buildSet {
        val fromQuests = Quest.entries.filter { it.name in claimed }.map { it.reward }
        addAll(fromQuests)
        if (fromQuests.size == Quest.entries.size) { add(Reward.MASTER_BADGE); add(Reward.POW_BACKDROP) }
        if (shinyEarned) add(Reward.HERO_CAPE)
        if (champion) add(Reward.CHAMPION_GLOW)
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
    const val BATTLE_WIN_XP = 15
    const val DAILY_BATTLE_XP = 60
    const val MAX_LEVEL = 99

    fun xp(bros: List<Bro>, hasTrainer: Boolean, claimedQuests: Int, battleWins: Int = 0, dailyQuests: Int = 0): Int =
        battleWins * BATTLE_WIN_XP + dailyQuests * DAILY_BATTLE_XP +
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

/** Avatar options that start locked and the reward that unlocks each. */
object AvatarLocks {
    fun requiredReward(part: LookPart, index: Int): Reward? {
        return when {
            part == LookPart.HAT && index == com.joecode.brokemon.data.model.LookOptions.TRAINER_CAP -> Reward.TRAINER_CAP
            part == LookPart.BACK && index == 1 -> Reward.HERO_CAPE
            part == LookPart.BACKGROUND && index == 1 -> Reward.POW_BACKDROP
            part == LookPart.BACKGROUND && index == 5 -> Reward.CHAMPION_GLOW
            else -> null
        }
    }

    /** Daily Pack items that unlock a character option (backdrops and hats). */
    fun requiredCosmetic(part: LookPart, index: Int): Cosmetic? = Cosmetic.forLook(part, index)

    /** A sample look showing off a PART reward, for icons. */
    fun showcase(reward: Reward): com.joecode.brokemon.data.model.BroLook {
        val base = com.joecode.brokemon.data.model.BroLook(skin = 2, hair = 1, outfit = 1, outfitColor = 0)
        return when (reward) {
            Reward.HERO_CAPE -> base.copy(back = 1)
            Reward.POW_BACKDROP -> base.copy(background = 1)
            Reward.CHAMPION_GLOW -> base.copy(background = 5)
            else -> base.copy(hat = com.joecode.brokemon.data.model.LookOptions.TRAINER_CAP)
        }
    }
}
