package com.joecode.brokemon.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.joecode.brokemon.R

/** Classic Brokemon: a red pixel handheld with Press Start 2P and nearest-neighbor sprites. */
object AppStyle : StyleSpec {
    override val flat = false
    override val palette = StylePalette(
        background = Color(0xFF121216),
        surface = Color(0xFF1B1B21),
        surfaceHigh = Color(0xFF25252D),
        outline = Color(0xFF3A3A46),
        primary = Color(0xFFD7263D),
        primaryDark = Color(0xFF8C1427),
        primaryLight = Color(0xFFFF5A6E),
        blue = Color(0xFF3FA7FF),
        green = Color(0xFF4CE07A),
        yellow = Color(0xFFFFD23F),
        red = Color(0xFFFF4D4D),
        screen = Color(0xFF0E171A),
        screenBorder = Color(0xFF2C3A3F),
        screenText = Color(0xFF8FF0DC),
        text = Color(0xFFECECF1),
        textMuted = Color(0xFF9A9AA8),
        gold = Color(0xFFFFD54A),
        rareBlue = Color(0xFF7FB8FF),
        epic = Color(0xFFC77DFF),
        legendary = Color(0xFFFFB13B),
    )
    override val display = FontFamily(Font(R.font.press_start_2p))
    override val displayFontRes = R.font.press_start_2p
    override val sizes = TextSizes(title = 18f, header = 13f, label = 10f, tiny = 8f, lineScale = 1.5f)
}
