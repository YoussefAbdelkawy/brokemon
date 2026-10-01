package com.joecode.brokemon.share

import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.data.model.LookPart
import com.joecode.brokemon.data.model.MediaType
import com.joecode.brokemon.data.model.Memory
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.testBro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodecTest {
    @Test
    fun `round trip keeps card data`() {
        val bro = testBro(5, "Marcus", BroType.GYM_RAT, BroType.FOODIE).copy(
            stats = BroStats(10, 20, 30, 40, 50, 60),
            moves = listOf("Spot Me", "Snack Run"),
            look = BroLook(skin = 3, hair = 5, hairColor = 9, glasses = 3, hat = 1, outfit = 2, outfitColor = 4),
            eventFrame = "RAMADAN|2027",
            voiceLine = "file:///data/secret-voice.m4a",
            rarity = Rarity.RARE,
            isShiny = true,
        )
        val decoded = QrCodec.decode(QrCodec.encode(bro), now = 42)!!
        assertEquals("Marcus", decoded.name)
        assertEquals("GYM_RAT", decoded.type1)
        assertEquals("FOODIE", decoded.type2)
        assertEquals(bro.stats, decoded.stats)
        assertEquals(bro.moves, decoded.moves)
        assertEquals(Rarity.RARE, decoded.rarity)
        assertTrue(decoded.isShiny)
        assertEquals(bro.avatarSeed, decoded.avatarSeed)
        assertEquals(bro.resolvedLook, decoded.look)
        // The limited frame travels with the card; the voice line never does.
        assertEquals("RAMADAN|2027", decoded.eventFrame)
        assertNull(decoded.voiceLine)
        assertEquals(42L, decoded.catchDate)
        assertTrue(decoded.isTraded)
        assertEquals(0L, decoded.id)
    }

    @Test
    fun `never carries memories facts or dates`() {
        val bro = testBro(catchDate = 123456789).copy(
            memories = listOf(Memory(fileUri = "file:///secret.jpg", mediaType = MediaType.PHOTO, caption = "secret")),
            facts = listOf(Fact(category = "Birthday", value = "May 4")),
            catchLocation = "My house",
        )
        val text = QrCodec.encode(bro)
        assertFalse(text.contains("secret"))
        assertFalse(text.contains("May 4"))
        assertFalse(text.contains("My house"))
        assertFalse(text.contains("123456789"))
        assertTrue(text.length < 300)
    }

    @Test
    fun `rejects junk`() {
        assertNull(QrCodec.decode("https://example.com"))
        assertNull(QrCodec.decode("BRKM1:not json"))
        assertNull(QrCodec.decode("BRKM1:{}"))
        assertNull(QrCodec.decode("""BRKM1:{"v":2,"n":"X","t":["NOPE"],"s":[1,2,3,4,5,6]}"""))
        assertNull(QrCodec.decode("""BRKM1:{"v":9,"n":"X","t":["GYM_RAT"],"s":[1,2,3,4,5,6]}"""))
        assertNull(QrCodec.decode("""BRKM1:{"v":2,"n":" ","t":["GYM_RAT"],"s":[1,2,3,4,5,6]}"""))
    }

    @Test
    fun `clamps hostile values`() {
        val long = "A".repeat(500)
        val bro = QrCodec.decode(
            """BRKM1:{"v":2,"n":"$long","t":["GYM_RAT","GYM_RAT","CHILL_GUY","YAPPER"],"s":[999,-5,3,4,5,6],"m":["a","b","c","d","e","f","g","h"],"l":[99,-1,0,0,0,0,0,0,0]}""",
        )!!
        assertEquals(QrCodec.MAX_NAME, bro.name.length)
        assertEquals("GYM_RAT", bro.type1)
        assertEquals("CHILL_GUY", bro.type2)
        assertEquals(100, bro.stats.rizz)
        assertEquals(1, bro.stats.aura)
        assertEquals(QrCodec.MAX_MOVES, bro.moves.size)
        // Out-of-range look indices wrap into valid options instead of crashing.
        assertTrue(bro.look!!.skin in 0 until LookPart.SKIN.count)
        assertTrue(bro.look!!.hair in 0 until LookPart.HAIR.count)
        // Junk event frames are dropped rather than trusted.
        assertNull(QrCodec.decode("""BRKM1:{"v":2,"n":"X","t":["GYM_RAT"],"s":[1,2,3,4,5,6],"e":"HACKED|1"}""")!!.eventFrame)
    }

    @Test
    fun `first-version cards still import`() {
        val bro = QrCodec.decode("""BRKM1:{"v":1,"n":"Old","t":["GYM"],"s":[1,2,3,4,5,6],"a":7}""")!!
        // Old type names are upgraded to the new ones on import.
        assertEquals("GYM_RAT", bro.type1)
        assertNull(bro.look)
    }
}
