package com.joecode.brokemon.ui.theme

import androidx.compose.ui.graphics.Color
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity

/** The app palette. Values come from the current style (see AppStyle in each flavor). */
object DexColors {
    private val p = AppStyle.palette
    val Background = p.background
    val Surface = p.surface
    val SurfaceHigh = p.surfaceHigh
    val Outline = p.outline

    val DexRed = p.primary
    val DexRedDark = p.primaryDark
    val DexRedLight = p.primaryLight

    val LedBlue = p.blue
    val LedGreen = p.green
    val LedYellow = p.yellow
    val LedRed = p.red

    /** The inner "screen" panels. */
    val Screen = p.screen
    val ScreenBorder = p.screenBorder
    val ScreenText = p.screenText

    val Text = p.text
    val TextMuted = p.textMuted

    val Gold = p.gold
    val RareBlue = p.rareBlue
    val Epic = p.epic
    val Legendary = p.legendary
}

val BroType.color: Color get() = Color(argb)

val Rarity.color: Color
    get() = when (this) {
        Rarity.COMMON -> DexColors.TextMuted
        Rarity.RARE -> DexColors.RareBlue
        Rarity.EPIC -> DexColors.Epic
        Rarity.LEGENDARY -> DexColors.Legendary
    }
