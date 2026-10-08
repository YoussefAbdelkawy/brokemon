package com.joecode.brokemon.data.model

import kotlin.random.Random

/**
 * How a bro's pixel character looks. Every field is an index into one of the
 * option lists below, which keeps it tiny in Room and in QR codes.
 */
data class BroLook(
    val skin: Int = 0,
    val hair: Int = 1,
    val hairColor: Int = 0,
    val expression: Int = 0,
    val facialHair: Int = 0,
    val glasses: Int = 0,
    val hat: Int = 0,
    val outfit: Int = 0,
    val outfitColor: Int = 0,
    val extra: Int = 0,
    /** Accessory slots (one item each): something held, something worn on the back, a backdrop. */
    val hand: Int = 0,
    val back: Int = 0,
    val background: Int = 0,
) {
    fun toList(): List<Int> =
        listOf(skin, hair, hairColor, expression, facialHair, glasses, hat, outfit, outfitColor, extra, hand, back, background)

    operator fun get(part: LookPart): Int = toList()[part.ordinal]

    fun with(part: LookPart, index: Int): BroLook {
        val values = toList().toMutableList()
        values[part.ordinal] = index.mod(part.count)
        return fromList(values)
    }

    companion object {
        fun fromList(values: List<Int>): BroLook {
            fun at(part: LookPart) = values.getOrElse(part.ordinal) { 0 }.mod(part.count)
            return BroLook(
                skin = at(LookPart.SKIN),
                hair = at(LookPart.HAIR),
                hairColor = at(LookPart.HAIR_COLOR),
                expression = at(LookPart.EXPRESSION),
                facialHair = at(LookPart.FACIAL_HAIR),
                glasses = at(LookPart.GLASSES),
                hat = at(LookPart.HAT),
                outfit = at(LookPart.OUTFIT),
                outfitColor = at(LookPart.OUTFIT_COLOR),
                extra = at(LookPart.EXTRA),
                hand = at(LookPart.HAND),
                back = at(LookPart.BACK),
                background = at(LookPart.BACKGROUND),
            )
        }

        /**
         * Natural-looking random character; used for new catches and older cards.
         * The first rolls use the original option counts so older cards keep their look.
         */
        fun random(seed: Long): BroLook {
            val r = Random(seed)
            val base = BroLook(
                skin = r.nextInt(6),
                hair = r.nextInt(10),
                // Mostly natural hair colors; the fun ones are opt-in.
                hairColor = r.nextInt(LookOptions.NATURAL_HAIR_COLORS),
                expression = r.nextInt(4),
                facialHair = if (r.nextFloat() < 0.5f) 0 else r.nextInt(5),
                glasses = if (r.nextFloat() < 0.65f) 0 else r.nextInt(4),
                hat = if (r.nextFloat() < 0.65f) 0 else r.nextInt(4),
                outfit = r.nextInt(4),
                outfitColor = r.nextInt(LookOptions.outfitColors.size),
            )
            // Newer options, rolled afterwards so the rolls above never shift.
            return base.copy(
                hair = if (r.nextFloat() < 0.35f) 10 + r.nextInt(6) else base.hair,
                extra = if (r.nextFloat() < 0.6f) 0 else r.nextInt(1, LookPart.EXTRA.count),
            )
        }
    }
}

enum class LookPart(val label: String, val count: Int) {
    SKIN("Skin", LookOptions.skinTones.size),
    HAIR("Hair", LookOptions.hairStyles.size),
    HAIR_COLOR("Hair color", LookOptions.hairColors.size),
    EXPRESSION("Face", LookOptions.expressions.size),
    FACIAL_HAIR("Beard", LookOptions.facialHair.size),
    GLASSES("Glasses", LookOptions.glasses.size),
    HAT("Hat", LookOptions.hats.size),
    OUTFIT("Fit", LookOptions.outfits.size),
    OUTFIT_COLOR("Fit color", LookOptions.outfitColors.size),
    EXTRA("Extras", LookOptions.extras.size),
    HAND("Hand", LookOptions.handItems.size),
    BACK("Back", LookOptions.backItems.size),
    BACKGROUND("Backdrop", LookOptions.backgrounds.size);

    /** Color parts show swatches; the rest show a label under the preview. */
    val isColor: Boolean get() = this == SKIN || this == HAIR_COLOR || this == OUTFIT_COLOR

    fun optionLabel(index: Int): String? = when (this) {
        HAIR -> LookOptions.hairStyles[index]
        EXPRESSION -> LookOptions.expressions[index]
        FACIAL_HAIR -> LookOptions.facialHair[index]
        GLASSES -> LookOptions.glasses[index]
        HAT -> LookOptions.hats[index]
        OUTFIT -> LookOptions.outfits[index]
        EXTRA -> LookOptions.extras[index]
        HAND -> LookOptions.handItems[index]
        BACK -> LookOptions.backItems[index]
        BACKGROUND -> LookOptions.backgrounds[index]
        else -> null
    }
}

/** Option tables. The order matters: indices are what gets stored. Only append. */
object LookOptions {
    val skinTones = listOf(
        0xFFFFE0BD, 0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFF5C3A21,
        0xFFFFF0E4, 0xFF3D2418,
    )

    val hairColors = listOf(
        0xFF1E1A1A, 0xFF3B2416, 0xFF6A4423, 0xFFB5651D, 0xFFE5C16A, 0xFFD4652F, 0xFF9E9E9E,
        // Fun colors
        0xFFEDEDED, 0xFF3A86FF, 0xFFFF4FA3, 0xFF4CAF50, 0xFF8B5CF6,
    )
    const val NATURAL_HAIR_COLORS = 7

    val hairStyles = listOf(
        "Buzz", "Short", "Spiky", "Curly", "Long", "Mohawk", "Bald", "Bun", "Side part", "Mullet",
        "Fade", "Afro", "Braids", "Locs", "Ponytail", "Waves", "Anime spikes",
    )
    val expressions = listOf("Smile", "Grin", "Chill", "Smirk", "Shocked", "Sleepy")
    val facialHair = listOf("None", "Stubble", "Mustache", "Beard", "Goatee")
    val glasses = listOf("None", "Round", "Square", "Shades", "Aviators", "Thick", "Hero mask")
    val hats = listOf("None", "Cap", "Beanie", "Headband", "Bucket", "Backwards", "Hijab", "Trainer cap", "Headset", "Headphones", "Ninja band", "Party hat", "Top hat")

    /** Hats you unlock through the Trainer's Journal instead of having from the start. */
    const val TRAINER_CAP = 7
    /** Football kits use made-up colors and a blank crest, never a real club's. */
    val outfits = listOf("Tee", "Hoodie", "Jersey", "Suit", "Galabeya", "Leather", "Football kit", "Captain kit")
    val handItems = listOf(
        "None", "Manga", "Comic", "Football", "Controller", "Coffee", "Shawarma", "Gym bag", "Keeper gloves",
    )
    val backItems = listOf("None", "Hero cape", "Cosplay cape", "Backpack", "Scarf")
    val backgrounds = listOf("None", "POW!", "Gaming chair", "Pitch", "Speed lines", "Trophy glow", "Sunset", "Starry night", "Arcade", "Sakura")
    val extras = listOf("None", "Earring", "Freckles", "Blush", "Nose ring", "Scar", "Mole")

    val outfitColors = listOf(
        0xFFD7263D, 0xFF3A86FF, 0xFF2EC4B6, 0xFF4CAF50, 0xFFFFC23D, 0xFFFF7A1A,
        0xFF8B5CF6, 0xFFFF4FA3, 0xFF2E2E3A, 0xFFE8E8EE,
    )
}
