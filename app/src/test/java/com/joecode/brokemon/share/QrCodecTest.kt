package com.joecode.brokemon.share

import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Fact
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
        val bro = testBro(5, "Marcus", BroType.GYM, BroType.FOODIE).copy(
            stats = BroStats(10, 20, 30, 40, 50, 60),
            moves = listOf("Spot Me", "Snack Run"),
            rarity = Rarity.RARE,
            isShiny = true,
        )
        val decoded = QrCodec.decode(QrCodec.encode(bro), now = 42)!!
        assertEquals("Marcus", decoded.name)
        assertEquals("GYM", decoded.type1)
        assertEquals("FOODIE", decoded.type2)
        assertEquals(bro.stats, decoded.stats)
        assertEquals(bro.moves, decoded.moves)
        assertEquals(Rarity.RARE, decoded.rarity)
        assertTrue(decoded.isShiny)
        assertEquals(bro.avatarSeed, decoded.avatarSeed)
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
        assertNull(QrCodec.decode("""BRKM1:{"v":1,"n":"X","t":["NOPE"],"s":[1,2,3,4,5,6]}"""))
        assertNull(QrCodec.decode("""BRKM1:{"v":1,"n":" ","t":["GYM"],"s":[1,2,3,4,5,6]}"""))
    }

    @Test
    fun `clamps hostile values`() {
        val long = "A".repeat(500)
        val bro = QrCodec.decode(
            """BRKM1:{"v":1,"n":"$long","t":["GYM","GYM","CHILL","HYPE"],"s":[999,-5,3,4,5,6],"m":["a","b","c","d","e","f"]}""",
        )!!
        assertEquals(QrCodec.MAX_NAME, bro.name.length)
        assertEquals("GYM", bro.type1)
        assertEquals("CHILL", bro.type2)
        assertEquals(100, bro.stats.hype)
        assertEquals(1, bro.stats.loyalty)
        assertEquals(QrCodec.MAX_MOVES, bro.moves.size)
    }
}
