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
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.domain.HumanSprite

/**
 * Crisp pixel portrait. The bitmap is 32x32 and scaled up with
 * FilterQuality.None (nearest neighbor), so pixels never blur.
 */
@Composable
fun BroSprite(
    look: BroLook,
    stage: Int,
    shiny: Boolean,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val bitmap = remember(look, stage, shiny, tint) { spriteBitmap(look, stage, shiny, tint) }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

@Composable
fun BroSprite(bro: Bro, stage: Int, modifier: Modifier = Modifier, tint: Color? = null) =
    BroSprite(bro.resolvedLook, stage, bro.isShiny, modifier, tint)

private fun spriteBitmap(look: BroLook, stage: Int, shiny: Boolean, tint: Color?): ImageBitmap {
    val pixels = HumanSprite.render(look, stage, shiny)
    if (tint != null) {
        // Silhouettes / evolution flashes: every drawn pixel becomes the tint.
        val t = tint.toArgb()
        for (i in pixels.indices) if (pixels[i] != HumanSprite.CLEAR) pixels[i] = t
    }
    val size = HumanSprite.SIZE
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Full standing (or sitting) figure, 32x52, nearest-neighbor scaled like the portraits. */
@Composable
fun BroFullBody(
    look: BroLook,
    stage: Int,
    shiny: Boolean,
    modifier: Modifier = Modifier,
    sitting: Boolean = false,
    tint: Color? = null,
) {
    val bitmap = remember(look, stage, shiny, sitting, tint) {
        val pixels = HumanSprite.renderFullBody(look, stage, shiny, sitting)
        if (tint != null) {
            val t = tint.toArgb()
            for (i in pixels.indices) if (pixels[i] != HumanSprite.CLEAR) pixels[i] = t
        }
        Bitmap.createBitmap(pixels, HumanSprite.BODY_W, HumanSprite.BODY_H, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}
