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
) {
    fun toList(): List<Int> = listOf(skin, hair, hairColor, expression, facialHair, glasses, hat, outfit, outfitColor)

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
            )
        }

        /** Natural-looking random character; used for new catches and older cards. */
        fun random(seed: Long): BroLook {
            val r = Random(seed)
            fun pick(part: LookPart) = r.nextInt(part.count)
            return BroLook(
                skin = pick(LookPart.SKIN),
                hair = pick(LookPart.HAIR),
                // Mostly natural hair colors; the fun ones are opt-in.
                hairColor = r.nextInt(LookOptions.NATURAL_HAIR_COLORS),
                expression = pick(LookPart.EXPRESSION),
                facialHair = if (r.nextFloat() < 0.5f) 0 else pick(LookPart.FACIAL_HAIR),
                glasses = if (r.nextFloat() < 0.65f) 0 else pick(LookPart.GLASSES),
                hat = if (r.nextFloat() < 0.65f) 0 else pick(LookPart.HAT),
                outfit = pick(LookPart.OUTFIT),
                outfitColor = pick(LookPart.OUTFIT_COLOR),
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
    OUTFIT_COLOR("Fit color", LookOptions.outfitColors.size);

    /** Color parts show swatches; the rest show a label under the preview. */
    val isColor: Boolean get() = this == SKIN || this == HAIR_COLOR || this == OUTFIT_COLOR

    fun optionLabel(index: Int): String? = when (this) {
        HAIR -> LookOptions.hairStyles[index]
        EXPRESSION -> LookOptions.expressions[index]
        FACIAL_HAIR -> LookOptions.facialHair[index]
        GLASSES -> LookOptions.glasses[index]
        HAT -> LookOptions.hats[index]
        OUTFIT -> LookOptions.outfits[index]
        else -> null
    }
}

/** Option tables. The order matters: indices are what gets stored. Only append. */
object LookOptions {
    val skinTones = listOf(0xFFFFE0BD, 0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFF5C3A21)

    val hairColors = listOf(
        0xFF1E1A1A, 0xFF3B2416, 0xFF6A4423, 0xFFB5651D, 0xFFE5C16A, 0xFFD4652F, 0xFF9E9E9E,
        // Fun colors
        0xFFEDEDED, 0xFF3A86FF, 0xFFFF4FA3, 0xFF4CAF50, 0xFF8B5CF6,
    )
    const val NATURAL_HAIR_COLORS = 7

    val hairStyles = listOf("Buzz", "Short", "Spiky", "Curly", "Long", "Mohawk", "Bald", "Bun", "Side part", "Mullet")
    val expressions = listOf("Smile", "Grin", "Chill", "Smirk")
    val facialHair = listOf("None", "Stubble", "Mustache", "Beard", "Goatee")
    val glasses = listOf("None", "Round", "Square", "Shades")
    val hats = listOf("None", "Cap", "Beanie", "Headband")
    val outfits = listOf("Tee", "Hoodie", "Jersey", "Suit")

    val outfitColors = listOf(
        0xFFD7263D, 0xFF3A86FF, 0xFF2EC4B6, 0xFF4CAF50, 0xFFFFC23D, 0xFFFF7A1A,
        0xFF8B5CF6, 0xFFFF4FA3, 0xFF2E2E3A, 0xFFE8E8EE,
    )
}
