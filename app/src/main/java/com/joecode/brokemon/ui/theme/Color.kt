package com.joecode.brokemon.ui.theme

import androidx.compose.ui.graphics.Color
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity

/**
 * The app palette. Everything in the app (UI, sprites, backgrounds) draws from
 * this small set so it all looks like one game. See [PixelPalette] for the
 * full list of ~32 art colors.
 */
object DexColors {
    val Background = Color(0xFF121216)
    val Surface = Color(0xFF1B1B21)
    val SurfaceHigh = Color(0xFF25252D)
    val Outline = Color(0xFF3A3A46)

    val DexRed = Color(0xFFD7263D)
    val DexRedDark = Color(0xFF8C1427)
    val DexRedLight = Color(0xFFFF5A6E)

    val LedBlue = Color(0xFF3FA7FF)
    val LedGreen = Color(0xFF4CE07A)
    val LedYellow = Color(0xFFFFD23F)
    val LedRed = Color(0xFFFF4D4D)

    /** The inner "screen" panels: a dim teal-black like an old LCD. */
    val Screen = Color(0xFF0E171A)
    val ScreenBorder = Color(0xFF2C3A3F)
    val ScreenText = Color(0xFF8FF0DC)

    val Text = Color(0xFFECECF1)
    val TextMuted = Color(0xFF9A9AA8)

    val Gold = Color(0xFFFFD54A)
    val RareBlue = Color(0xFF7FB8FF)
    val Epic = Color(0xFFC77DFF)
    val Legendary = Color(0xFFFFB13B)

    /** Cream paper used for dialogue boxes and speech bubbles. */
    val Paper = Color(0xFFF7F4E8)
    val Ink = Color(0xFF16161C)
}

/** The ~32 colors all art is drawn from (UI, sprites, backgrounds, icons). */
object PixelPalette {
    val colors: List<Color> = listOf(
        0xFF0B0B10, 0xFF121216, 0xFF1B1B21, 0xFF25252D, 0xFF3A3A46, 0xFF6E6890, 0xFF9A9AA8, 0xFFECECF1,
        0xFFD7263D, 0xFF8C1427, 0xFFFF5A6E, 0xFFFF7A1A, 0xFFFFC23D, 0xFFFFD23F, 0xFFFFF3B0, 0xFFB5651D,
        0xFF4CE07A, 0xFF2E8B45, 0xFF2EC4B6, 0xFF8FF0DC, 0xFF3FA7FF, 0xFF3A86FF, 0xFF1F5FA8, 0xFF8B5CF6,
        0xFFC77DFF, 0xFFFF4FA3, 0xFFF7F4E8, 0xFF16161C, 0xFF0E171A, 0xFF2C3A3F, 0xFFE8C9A0, 0xFF6B4A2E,
    ).map { Color(it) }
}

val BroType.color: Color get() = Color(argb)

val Rarity.color: Color
    get() = when (this) {
        Rarity.COMMON -> DexColors.TextMuted
        Rarity.RARE -> DexColors.RareBlue
        Rarity.EPIC -> DexColors.Epic
        Rarity.LEGENDARY -> DexColors.Legendary
    }
