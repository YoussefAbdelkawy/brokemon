package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.data.model.MediaType
import com.joecode.brokemon.data.model.Memory
import com.joecode.brokemon.data.model.TrainerFrame
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalTest {

    @Test
    fun `fresh install has nothing done`() {
        val quests = Journal.status(hasTrainer = false, bros = emptyList(), tradedQr = false, claimed = emptySet())
        assertEquals(Quest.entries.size, quests.size)
        assertTrue(quests.none { it.done })
    }

    @Test
    fun `quests complete from real activity`() {
        val bro = testBro().copy(
            facts = listOf(Fact(category = "Food", value = "Shawarma")),
            memories = listOf(Memory(fileUri = "file:///x.jpg", mediaType = MediaType.PHOTO, date = 0)),
            checkInCount = 1,
        )
        val done = Journal.status(true, listOf(bro), tradedQr = false, claimed = emptySet())
            .filter { it.done }.map { it.quest }
        assertEquals(Quest.entries - Quest.TRADE_QR, done)
        assertTrue(Journal.status(true, listOf(bro.copy(isTraded = true)), false, emptySet()).all { it.done })
    }

    @Test
    fun `claimed quests stay done and pay out rewards`() {
        // Bro released after claiming: the quest stays claimed.
        val status = Journal.status(false, emptyList(), false, setOf(Quest.CATCH_FIRST_BRO.name))
        val catch = status.first { it.quest == Quest.CATCH_FIRST_BRO }
        assertTrue(catch.done && catch.claimed && !catch.claimable)
        assertEquals(setOf(Reward.BRONZE_FRAME), Journal.unlocked(setOf(Quest.CATCH_FIRST_BRO.name)))
        assertEquals(listOf(TrainerFrame.BASIC, TrainerFrame.BRONZE), Journal.unlockedFrames(setOf(Quest.CATCH_FIRST_BRO.name)))
    }

    @Test
    fun `finishing the journal adds the master badge`() {
        val all = Quest.entries.map { it.name }.toSet()
        assertTrue(Reward.MASTER_BADGE in Journal.unlocked(all))
        assertFalse(Reward.MASTER_BADGE in Journal.unlocked(all - Quest.TRADE_QR.name))
    }

    @Test
    fun `every quest has its own reward`() {
        assertEquals(Quest.entries.size, Quest.entries.map { it.reward }.toSet().size)
    }

    @Test
    fun `level curve`() {
        assertEquals(1, TrainerLevel.info(0).level)
        assertEquals(1, TrainerLevel.info(49).level)
        assertEquals(2, TrainerLevel.info(50).level)
        assertEquals(3, TrainerLevel.info(200).level)
        val info = TrainerLevel.info(125)
        assertEquals(50, info.levelStartXp)
        assertEquals(200, info.nextLevelXp)
        assertEquals(0.5f, info.progress, 0.001f)
        assertEquals(75, info.toNext)
        val max = TrainerLevel.info(Int.MAX_VALUE / 2)
        assertEquals(TrainerLevel.MAX_LEVEL, max.level)
        assertNull(max.nextLevelXp)
        assertEquals(1f, max.progress)
    }

    @Test
    fun `xp counts catches, check-ins, memories, facts and quests`() {
        val bro = testBro().copy(checkInCount = 2, facts = listOf(Fact(category = "A", value = "B")))
        val xp = TrainerLevel.xp(listOf(bro, testBro(2)), hasTrainer = true, claimedQuests = 1)
        assertEquals(
            TrainerLevel.TRAINER_CARD_XP + 2 * TrainerLevel.CATCH_XP + 2 * TrainerLevel.CHECK_IN_XP +
                TrainerLevel.FACT_XP + TrainerLevel.QUEST_XP,
            xp,
        )
    }
}
