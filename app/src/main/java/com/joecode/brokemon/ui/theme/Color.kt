package com.joecode.brokemon.ui.theme

import androidx.compose.ui.graphics.Color
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity

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

    /** The inner "screen" panels — a dim teal-black like an old LCD. */
    val Screen = Color(0xFF0E171A)
    val ScreenBorder = Color(0xFF2C3A3F)
    val ScreenText = Color(0xFF8FF0DC)

    val Text = Color(0xFFECECF1)
    val TextMuted = Color(0xFF9A9AA8)

    val Gold = Color(0xFFFFD54A)
    val RareBlue = Color(0xFF7FB8FF)
    val Legendary = Color(0xFFFFB13B)
}

val BroType.color: Color get() = Color(argb)

val Rarity.color: Color
    get() = when (this) {
        Rarity.COMMON -> DexColors.TextMuted
        Rarity.RARE -> DexColors.RareBlue
        Rarity.LEGENDARY -> DexColors.Legendary
    }
