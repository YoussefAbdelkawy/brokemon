package com.joecode.brokemon.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Everything that differs between the two app styles. Each product flavor
 * (classic = pixel handheld, cosmos = flat space illustration) provides an
 * `AppStyle` object with these values; the rest of the app reads them.
 */
data class StylePalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val blue: Color,
    val green: Color,
    val yellow: Color,
    val red: Color,
    val screen: Color,
    val screenBorder: Color,
    val screenText: Color,
    val text: Color,
    val textMuted: Color,
    val gold: Color,
    val rareBlue: Color,
    val epic: Color,
    val legendary: Color,
)

data class TextSizes(val title: Float, val header: Float, val label: Float, val tiny: Float, val lineScale: Float)

interface StyleSpec {
    /** True for the flat vector style (rounded shapes, smooth avatars), false for pixel art. */
    val flat: Boolean
    val palette: StylePalette
    val display: FontFamily
    /** Font resource for drawing share images with android.graphics. */
    val displayFontRes: Int
    val sizes: TextSizes
}

/** The app's corner style: chamfered pixel corners, or soft round ones in the flat style. */
fun DexShape(size: Dp): CornerBasedShape = if (AppStyle.flat) RoundedCornerShape(size * 2f) else CutCornerShape(size)

fun DexShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): CornerBasedShape =
    if (AppStyle.flat) {
        RoundedCornerShape(topStart * 2f, topEnd * 2f, bottomEnd * 2f, bottomStart * 2f)
    } else {
        CutCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    }
