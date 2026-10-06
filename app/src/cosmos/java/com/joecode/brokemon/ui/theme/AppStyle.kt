package com.joecode.brokemon.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.joecode.brokemon.R

/**
 * Brokemon Cosmos: flat vector illustration in a deep-space palette, with
 * soft glows, round shapes and a rounded display font (Fredoka, SIL OFL 1.1).
 */
object AppStyle : StyleSpec {
    override val flat = true
    override val palette = StylePalette(
        background = Color(0xFF130E30),
        surface = Color(0xFF1E1748),
        surfaceHigh = Color(0xFF2A2160),
        outline = Color(0xFF41378A),
        primary = Color(0xFFFF6B4A),
        primaryDark = Color(0xFF6B2FA3),
        primaryLight = Color(0xFFFF8FB1),
        blue = Color(0xFF4CC3FF),
        green = Color(0xFF3EE6B0),
        yellow = Color(0xFFFFD447),
        red = Color(0xFFFF4F6D),
        screen = Color(0xFF0E0A27),
        screenBorder = Color(0xFF382C7A),
        screenText = Color(0xFF8FF5E4),
        text = Color(0xFFF7F3FF),
        textMuted = Color(0xFFADA6DA),
        gold = Color(0xFFFFC94A),
        rareBlue = Color(0xFF7FD3FF),
        epic = Color(0xFFD08CFF),
        legendary = Color(0xFFFFA94D),
    )
    override val display = FontFamily(
        Font(R.font.fredoka_medium, FontWeight.Medium),
        Font(R.font.fredoka_semibold, FontWeight.Normal),
        Font(R.font.fredoka_bold, FontWeight.Bold),
    )
    override val displayFontRes = R.font.fredoka_bold
    override val sizes = TextSizes(title = 24f, header = 17f, label = 14f, tiny = 12f, lineScale = 1.3f)
}
