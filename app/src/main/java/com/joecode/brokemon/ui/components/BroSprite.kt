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
    val bitmap = remember(look, stage, shiny, tint) { AvatarBitmaps.portrait(look, stage, shiny, tint?.toArgb()).asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = AvatarBitmaps.filterQuality,
    )
}

@Composable
fun BroSprite(bro: Bro, stage: Int, modifier: Modifier = Modifier, tint: Color? = null) =
    BroSprite(bro.resolvedLook, stage, bro.isShiny, modifier, tint)

/**
 * Avatar bitmaps in the current style: 32x32 / 32x52 pixel art (classic) or
 * smooth flat vector art (cosmos). Used by cards, share images and the widget.
 */
object AvatarBitmaps {
    val smooth: Boolean get() = com.joecode.brokemon.ui.theme.AppStyle.flat
    val filterQuality: FilterQuality get() = if (smooth) FilterQuality.Medium else FilterQuality.None

    fun portrait(look: BroLook, stage: Int, shiny: Boolean, tint: Int? = null): Bitmap {
        if (smooth) return FlatAvatar.portrait(look, stage, shiny, 256, tint)
        val pixels = HumanSprite.render(look, stage, shiny)
        if (tint != null) for (i in pixels.indices) if (pixels[i] != HumanSprite.CLEAR) pixels[i] = tint
        return Bitmap.createBitmap(pixels, HumanSprite.SIZE, HumanSprite.SIZE, Bitmap.Config.ARGB_8888)
    }

    fun fullBody(look: BroLook, stage: Int, shiny: Boolean, sitting: Boolean, tint: Int? = null): Bitmap {
        if (smooth) return FlatAvatar.fullBody(look, stage, shiny, sitting, 200, tint)
        val pixels = HumanSprite.renderFullBody(look, stage, shiny, sitting)
        if (tint != null) for (i in pixels.indices) if (pixels[i] != HumanSprite.CLEAR) pixels[i] = tint
        return Bitmap.createBitmap(pixels, HumanSprite.BODY_W, HumanSprite.BODY_H, Bitmap.Config.ARGB_8888)
    }
}

/** Full standing (or sitting) figure in the current style. */
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
