package com.joecode.brokemon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.joecode.brokemon.R

/** Press Start 2P (SIL OFL 1.1) — headers, stat labels, dex numbers. */
val PixelFont = FontFamily(Font(R.font.press_start_2p))

/** Pixel styles. Pixel fonts read best at small, whole sizes with extra line height. */
object PixelText {
    val Title = TextStyle(fontFamily = PixelFont, fontSize = 18.sp, lineHeight = 26.sp)
    val Header = TextStyle(fontFamily = PixelFont, fontSize = 13.sp, lineHeight = 20.sp)
    val Label = TextStyle(fontFamily = PixelFont, fontSize = 10.sp, lineHeight = 16.sp)
    val Tiny = TextStyle(fontFamily = PixelFont, fontSize = 8.sp, lineHeight = 12.sp)
}

val BrokemonTypography = Typography().let { base ->
    base.copy(
        titleLarge = PixelText.Title,
        titleMedium = PixelText.Header,
        titleSmall = PixelText.Label,
        labelLarge = PixelText.Label,
        labelMedium = PixelText.Tiny,
        labelSmall = PixelText.Tiny,
        // Body stays a readable sans-serif.
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    )
}
