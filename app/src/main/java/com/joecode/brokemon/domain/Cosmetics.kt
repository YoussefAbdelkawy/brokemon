package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.LookPart
import com.joecode.brokemon.data.model.RoomPart
import kotlin.random.Random

enum class CosmeticKind(val label: String, val weight: Int) {
    STICKER("Sticker", 55),
    FRAME("Card frame", 12),
    BACKDROP("Backdrop", 13),
    ACCESSORY("Accessory", 10),
    DECOR("Room decor", 10),
}

/**
 * Everything the Daily Pack can give. All of it is cosmetic: none of it touches stats,
 * moves, evolution or battles. Ids are the enum names, stored in [UserPrefs.ownedCosmetics].
 */
enum class Cosmetic(
    val kind: CosmeticKind,
    val label: String,
    /** For ACCESSORY / BACKDROP: which look part and index it unlocks. */
    val lookPart: LookPart? = null,
    val lookIndex: Int = -1,
    /** For FRAME: the value stored in Bro.cardFrame. */
    val frame: String? = null,
    /** For DECOR: the Trainer Room part and option it unlocks. */
    val roomPart: RoomPart? = null,
    val roomIndex: Int = -1,
    val secret: Boolean = false,
) {
    // Stickers (art in [StickerArt])
    ST_STAR(CosmeticKind.STICKER, "Gold star"),
    ST_HEART(CosmeticKind.STICKER, "Heart"),
    ST_BOLT(CosmeticKind.STICKER, "Lightning"),
    ST_PIZZA(CosmeticKind.STICKER, "Pizza slice"),
    ST_PAD(CosmeticKind.STICKER, "Game pad"),
    ST_BALL(CosmeticKind.STICKER, "Football"),
    ST_NOTE(CosmeticKind.STICKER, "Music note"),
    ST_CROWN(CosmeticKind.STICKER, "Crown"),
    ST_GHOST(CosmeticKind.STICKER, "Ghost"),
    ST_FLAME(CosmeticKind.STICKER, "Flame"),
    ST_COFFEE(CosmeticKind.STICKER, "Coffee"),
    ST_SUN(CosmeticKind.STICKER, "Sun"),
    ST_MOON(CosmeticKind.STICKER, "Moon"),
    ST_CAR(CosmeticKind.STICKER, "Little car"),
    ST_DUMBBELL(CosmeticKind.STICKER, "Dumbbell"),

    // Card frames
    FR_NEON(CosmeticKind.FRAME, "Neon frame", frame = "NEON"),
    FR_WOOD(CosmeticKind.FRAME, "Wood frame", frame = "WOOD"),
    FR_CANDY(CosmeticKind.FRAME, "Candy frame", frame = "CANDY"),
    FR_CIRCUIT(CosmeticKind.FRAME, "Circuit frame", frame = "CIRCUIT"),
    FR_SECRET(CosmeticKind.FRAME, "Secret frame", frame = "SECRET", secret = true),

    // Backdrops and accessories for characters
    BD_SUNSET(CosmeticKind.BACKDROP, "Sunset backdrop", LookPart.BACKGROUND, 6),
    BD_STARRY(CosmeticKind.BACKDROP, "Starry night backdrop", LookPart.BACKGROUND, 7),
    BD_ARCADE(CosmeticKind.BACKDROP, "Arcade backdrop", LookPart.BACKGROUND, 8),
    BD_SAKURA(CosmeticKind.BACKDROP, "Sakura backdrop", LookPart.BACKGROUND, 9),
    AC_PARTY_HAT(CosmeticKind.ACCESSORY, "Party hat", LookPart.HAT, 11),
    AC_TOP_HAT(CosmeticKind.ACCESSORY, "Top hat", LookPart.HAT, 12),

    // Trainer Room decor
    DC_NEON_SIGN(CosmeticKind.DECOR, "Neon sign", roomPart = RoomPart.WALL_RIGHT, roomIndex = 5),
    DC_LANTERN(CosmeticKind.DECOR, "Lantern", roomPart = RoomPart.WALL_LEFT, roomIndex = 8),
    DC_FAIRY(CosmeticKind.DECOR, "Fairy lights", roomPart = RoomPart.WALL_LEFT, roomIndex = 7),
    DC_GUITAR(CosmeticKind.DECOR, "Guitar", roomPart = RoomPart.FLOOR_LEFT, roomIndex = 6),
    DC_SPEAKER(CosmeticKind.DECOR, "Speaker", roomPart = RoomPart.FLOOR_RIGHT, roomIndex = 8),
    DC_KILIM(CosmeticKind.DECOR, "Kilim rug", roomPart = RoomPart.RUG, roomIndex = 3);

    val id: String get() = name
    val isSticker: Boolean get() = kind == CosmeticKind.STICKER

    companion object {
        fun from(id: String?): Cosmetic? = entries.firstOrNull { it.name == id }

        fun stickers(): List<Cosmetic> = entries.filter { it.isSticker }

        /** The character option locked behind an item (a backdrop or hat), if any. */
        fun forLook(part: LookPart, index: Int): Cosmetic? =
            entries.firstOrNull { it.lookPart == part && it.lookIndex == index }

        fun forRoom(part: RoomPart, index: Int): Cosmetic? =
            entries.firstOrNull { it.roomPart == part && it.roomIndex == index }

        fun forFrame(frame: String?): Cosmetic? = entries.firstOrNull { it.frame != null && it.frame == frame }

        /** Items the Daily Pack can roll (the secret frame is never in a pack). */
        val packable: List<Cosmetic> get() = entries.filterNot { it.secret }

        /**
         * One item per pack. Picks a kind by weight among kinds you still have something new in,
         * then an item you don't own yet. Returns null once the whole collection is complete.
         */
        fun roll(owned: Set<String>, random: Random = Random.Default): Cosmetic? {
            val missing = packable.filter { it.id !in owned }
            if (missing.isEmpty()) return null
            val kinds = missing.groupBy { it.kind }
            val total = kinds.keys.sumOf { it.weight }
            var pick = random.nextInt(total)
            val kind = kinds.keys.first { k -> pick -= k.weight; pick < 0 }
            return kinds.getValue(kind).random(random)
        }
    }
}

/** 8x8 pixel stickers. '.' is clear; other letters index [StickerArt.palette]. */
object StickerArt {
    val palette: Map<Char, Long> = mapOf(
        'y' to 0xFFFFD54A, 'o' to 0xFFFF7A1A, 'r' to 0xFFD7263D, 'w' to 0xFFF5F5F7, 'g' to 0xFF9A9AA8,
        'k' to 0xFF2A2A33, 'n' to 0xFF8B5A2B, 'b' to 0xFF3A86FF, 'c' to 0xFF5CE1E6,
    )

    private val art: Map<Cosmetic, List<String>> = mapOf(
        Cosmetic.ST_STAR to listOf("...yy...", "...yy...", "yyyyyyyy", ".yyyyyy.", "..yyyy..", ".yyyyyy.", ".yy..yy.", ".y....y."),
        Cosmetic.ST_HEART to listOf(".rr..rr.", "rrrrrrrr", "rrwrrrrr", "rrrrrrrr", ".rrrrrr.", "..rrrr..", "...rr...", "........"),
        Cosmetic.ST_BOLT to listOf("....yy..", "...yy...", "..yy....", ".yyyyyy.", "...yy...", "..yy....", ".yy.....", ".y......"),
        Cosmetic.ST_PIZZA to listOf("oooooooo", "oyryyryo", ".oyyyyo.", ".oyryyo.", "..oyyo..", "..oyro..", "...oo...", "...o...."),
        Cosmetic.ST_PAD to listOf("........", ".kkkkkk.", "kkgkkkrk", "kgggkrkr", "kkgkkkrk", "kkkkkkkk", "kk....kk", "........"),
        Cosmetic.ST_BALL to listOf("..kkkk..", ".kwwwwk.", "kwwkkwwk", "kwkkkkwk", "kwkkkkwk", "kwwkkwwk", ".kwwwwk.", "..kkkk.."),
        Cosmetic.ST_NOTE to listOf("...kkkk.", "...kk.k.", "...kk.k.", "...kk...", "...kk...", ".kkkk...", "kkkkk...", ".kkk...."),
        Cosmetic.ST_CROWN to listOf("y..y..y.", "yy.yy.yy", "yyyyyyyy", "yyryyryy", "yyyyyyyy", "oooooooo", "........", "........"),
        Cosmetic.ST_GHOST to listOf("..wwww..", ".wwwwww.", "wwkwwkww", "wwkwwkww", "wwwwwwww", "wwwwwwww", "wwwwwwww", "w.ww.ww."),
        Cosmetic.ST_FLAME to listOf("...o....", "...oo...", "..ooor..", ".ooyyor.", ".oyyyyor", ".oyyyyor", "..oyyor.", "...rrr.."),
        Cosmetic.ST_COFFEE to listOf("..w..w..", "...w..w.", "nnnnnnn.", "nwwwwwnn", "nwwwwwn.", "nwwwwwnn", ".nnnnn..", "........"),
        Cosmetic.ST_SUN to listOf("y..yy..y", ".y.yy.y.", "..yyyy..", "yyyyyyyy", "yyyyyyyy", "..yyyy..", ".y.yy.y.", "y..yy..y"),
        Cosmetic.ST_MOON to listOf("..yyyy..", ".yyy....", "yyy.....", "yyy.....", "yyy.....", "yyyy....", ".yyyyyy.", "..yyyy.."),
        Cosmetic.ST_CAR to listOf("........", "..rrrr..", ".rwwrrw.", "rrrrrrrr", "rrrrrrrr", "rkkrrkkr", ".kk..kk.", "........"),
        Cosmetic.ST_DUMBBELL to listOf("........", "........", "kk....kk", "kkkkkkkk", "kkkkkkkk", "kk....kk", "........", "........"),
    )

    fun rows(sticker: Cosmetic): List<String> = art[sticker] ?: List(8) { "........" }
}
