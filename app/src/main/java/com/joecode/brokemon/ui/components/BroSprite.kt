package com.joecode.brokemon.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.ColorUtils
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.domain.SpriteGenerator
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.color

/**
 * Crisp pixel avatar. The bitmap is 16x16 and scaled up with
 * FilterQuality.None (nearest neighbor), so pixels never blur.
 */
@Composable
fun BroSprite(
    seed: Long,
    stage: Int,
    type1: BroType,
    type2: BroType?,
    shiny: Boolean,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val bitmap = remember(seed, stage, type1, type2, shiny, tint) {
        spriteBitmap(seed, stage, type1, type2, shiny, tint)
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

private fun spriteBitmap(
    seed: Long,
    stage: Int,
    type1: BroType,
    type2: BroType?,
    shiny: Boolean,
    tint: Color?,
): ImageBitmap {
    val grid = SpriteGenerator.generate(seed, stage)
    var body = type1.color.toArgb()
    var accent = (type2?.color ?: type1.color).toArgb()
    if (type2 == null) accent = ColorUtils.blendARGB(accent, android.graphics.Color.WHITE, 0.35f)
    if (shiny) {
        body = shiftHue(body, 150f)
        accent = DexColors.Gold.toArgb()
    }
    val palette = IntArray(8)
    palette[SpriteGenerator.EMPTY] = android.graphics.Color.TRANSPARENT
    palette[SpriteGenerator.OUTLINE] = 0xFF0B0B10.toInt()
    palette[SpriteGenerator.BODY] = body
    palette[SpriteGenerator.SHADE] = ColorUtils.blendARGB(body, android.graphics.Color.BLACK, 0.3f)
    palette[SpriteGenerator.ACCENT] = accent
    palette[SpriteGenerator.EYE] = 0xFF0B0B10.toInt()
    palette[SpriteGenerator.EYE_SHINE] = android.graphics.Color.WHITE
    palette[SpriteGenerator.CROWN] = DexColors.Gold.toArgb()

    if (tint != null) {
        // Used for silhouettes / evolution flashes: every filled pixel becomes the tint.
        val t = tint.toArgb()
        for (i in 1 until palette.size) palette[i] = t
    }

    val size = SpriteGenerator.SIZE
    val pixels = IntArray(size * size) { i -> palette[grid[i / size][i % size]] }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private fun shiftHue(argb: Int, degrees: Float): Int {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    hsv[0] = (hsv[0] + degrees) % 360f
    return android.graphics.Color.HSVToColor(hsv)
}
