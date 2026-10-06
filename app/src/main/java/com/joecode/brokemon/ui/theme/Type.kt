package com.joecode.brokemon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/** Display font for headers, stat labels and dex numbers (Press Start 2P or Fredoka, per style). */
val PixelFont: FontFamily = AppStyle.display

/** Pixel styles. Pixel fonts read best at small, whole sizes with extra line height. */
object PixelText {
    private val s = AppStyle.sizes
    val Title = TextStyle(fontFamily = PixelFont, fontSize = s.title.sp, lineHeight = (s.title * s.lineScale).sp)
    val Header = TextStyle(fontFamily = PixelFont, fontSize = s.header.sp, lineHeight = (s.header * s.lineScale).sp)
    val Label = TextStyle(fontFamily = PixelFont, fontSize = s.label.sp, lineHeight = (s.label * s.lineScale).sp)
    val Tiny = TextStyle(fontFamily = PixelFont, fontSize = s.tiny.sp, lineHeight = (s.tiny * s.lineScale).sp)
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
