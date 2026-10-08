package com.joecode.brokemon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CosmeticsAndNotesTest {

    @Test
    fun packNeverRepeatsAnOwnedItemAndNeverRollsTheSecretFrame() {
        val owned = mutableSetOf<String>()
        val random = Random(7)
        repeat(Cosmetic.packable.size) {
            val item = Cosmetic.roll(owned, random)
            assertNotNull(item)
            assertFalse("rolled a duplicate: $item", item!!.id in owned)
            assertFalse(item.secret)
            owned += item.id
        }
        // Collection complete: nothing left to give.
        assertNull(Cosmetic.roll(owned, random))
    }

    @Test
    fun everyStickerHasEightByEightArt() {
        Cosmetic.stickers().forEach { sticker ->
            val rows = StickerArt.rows(sticker)
            assertEquals("${sticker.id} height", 8, rows.size)
            rows.forEach { row ->
                assertEquals("${sticker.id} width", 8, row.length)
                row.filter { it != '.' }.forEach { assertTrue("${sticker.id}: unknown color $it", it in StickerArt.palette) }
            }
        }
    }

    @Test
    fun lockedLookOptionsPointAtRealIndexes() {
        Cosmetic.entries.filter { it.lookPart != null }.forEach {
            assertTrue("${it.id} index out of range", it.lookIndex in 0 until it.lookPart!!.count)
        }
        Cosmetic.entries.filter { it.roomPart != null }.forEach {
            assertTrue("${it.id} index out of range", it.roomIndex in 0 until it.roomPart!!.count)
        }
    }

    @Test
    fun whatsNewShowsOnceForUpgradersAndNeverOnFreshInstalls() {
        val current = ReleaseNotes.all.maxOf { it.versionCode }
        // Fresh install: onboarding not done yet.
        assertFalse(ReleaseNotes.shouldShow(onboardingDone = false, lastSeen = null, current = current))
        // Upgrader from before this feature existed (no stored version).
        assertTrue(ReleaseNotes.shouldShow(onboardingDone = true, lastSeen = null, current = current))
        // Already saw it.
        assertFalse(ReleaseNotes.shouldShow(onboardingDone = true, lastSeen = current, current = current))
        // A version without notes stays quiet.
        assertFalse(ReleaseNotes.shouldShow(onboardingDone = true, lastSeen = 1, current = 999))
    }

    @Test
    fun groupPhotoFitsEveryPoseOnTheCanvas() {
        for (pose in PhotoPose.entries) for (n in 1..GroupPhoto.MAX_BROS) {
            val spots = GroupPhoto.layout(n, pose)
            assertEquals(n, spots.size)
            spots.forEach { (x, feet) ->
                assertTrue("$pose/$n x=$x", x >= 0 && x + HumanSprite.BODY_W <= GroupPhoto.W)
                assertTrue("$pose/$n feet=$feet", feet - HumanSprite.BODY_H >= 0 && feet < GroupPhoto.H)
            }
        }
    }
}
