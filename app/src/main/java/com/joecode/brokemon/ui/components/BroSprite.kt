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
    /** Idle animation (breathing and blinking). Only turn this on for sprites that are the star of the screen. */
    animated: Boolean = false,
    frameSeed: Long = 0L,
) {
    val frame = rememberIdleFrame(frameSeed + look.hashCode(), enabled = animated)
    val bitmap = remember(look, stage, shiny, tint, frame) { SpriteCache.portrait(look, stage, shiny, tint?.toArgb(), frame) }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = AvatarBitmaps.filterQuality,
    )
}

@Composable
fun BroSprite(bro: Bro, stage: Int, modifier: Modifier = Modifier, tint: Color? = null, animated: Boolean = false) =
    BroSprite(bro.resolvedLook, stage, bro.isShiny, modifier, tint, animated, bro.id)

/** Avatar bitmaps: 32x32 portraits and 32x52 full bodies of pixel art, scaled with nearest-neighbor. */
object AvatarBitmaps {
    val filterQuality: FilterQuality = FilterQuality.None

    fun portrait(look: BroLook, stage: Int, shiny: Boolean, tint: Int? = null, frame: Int = 0): Bitmap {
        val pixels = HumanSprite.render(look, stage, shiny, frame)
        if (tint != null) for (i in pixels.indices) if (pixels[i] != HumanSprite.CLEAR) pixels[i] = tint
        return Bitmap.createBitmap(pixels, HumanSprite.SIZE, HumanSprite.SIZE, Bitmap.Config.ARGB_8888)
    }

    fun fullBody(look: BroLook, stage: Int, shiny: Boolean, sitting: Boolean, tint: Int? = null): Bitmap {
        val pixels = HumanSprite.renderFullBody(look, stage, shiny, sitting)
        if (tint != null) for (i in pixels.indices) if (pixels[i] != HumanSprite.CLEAR) pixels[i] = tint
        return Bitmap.createBitmap(pixels, HumanSprite.BODY_W, HumanSprite.BODY_H, Bitmap.Config.ARGB_8888)
    }
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
        AvatarBitmaps.fullBody(look, stage, shiny, sitting, tint?.toArgb()).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = AvatarBitmaps.filterQuality,
    )
}
