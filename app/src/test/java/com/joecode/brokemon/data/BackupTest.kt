package com.joecode.brokemon.data

import com.google.gson.Gson
import com.joecode.brokemon.data.backup.BackupFile
import com.joecode.brokemon.data.backup.BackupManager
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.data.model.MediaType
import com.joecode.brokemon.data.model.Memory
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.data.model.Squad
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    @Test
    fun `only flat memory files are accepted from a zip`() {
        assertTrue(BackupManager.isSafeMemoryEntry("memories/0b3c9a1e-1111-2222-3333-444455556666.jpg"))
        assertTrue(BackupManager.isSafeMemoryEntry("memories/abc.mp4"))
        assertTrue(BackupManager.isSafeMemoryEntry("memories/voice-0b3c9a1e-1111-2222-3333-444455556666.m4a"))
        assertFalse(BackupManager.isSafeMemoryEntry("memories/../../databases/brokemon.db"))
        assertFalse(BackupManager.isSafeMemoryEntry("../evil.jpg"))
        assertFalse(BackupManager.isSafeMemoryEntry("memories/sub/dir.jpg"))
        assertFalse(BackupManager.isSafeMemoryEntry("memories/x.sh"))
        assertFalse(BackupManager.isSafeMemoryEntry("/data/data/x.jpg"))
    }

    @Test
    fun `backup json round trips every field`() {
        val bro = testBro(7, "Marcus").copy(
            stats = BroStats(1, 2, 3, 4, 5, 6),
            moves = listOf("Horn Solo"),
            rarity = Rarity.LEGENDARY,
            isShiny = true,
            look = BroLook(skin = 3, hair = 9, glasses = 3),
            memories = listOf(Memory(fileUri = "memories/a.jpg", mediaType = MediaType.VIDEO, caption = "hi", date = 5)),
            facts = listOf(Fact(category = "Birthday", value = "May 4", monthDay = "05-04")),
            realMeetDate = 99,
            checkInCount = 3,
            isTradeable = false,
            voiceLine = "memories/voice-a.m4a",
            eventFrame = "EID|2026",
        )
        val file = BackupFile(exportedAt = 1, bros = listOf(bro), squads = listOf(Squad(id = 2, name = "Crew", memberIds = listOf(7), createdAt = 3)))
        val gson = Gson()
        val back = gson.fromJson(gson.toJson(file), BackupFile::class.java)
        assertEquals(file, back)
    }
}
