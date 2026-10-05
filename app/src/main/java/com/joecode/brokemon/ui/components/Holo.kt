package com.joecode.brokemon.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import com.joecode.brokemon.data.model.Rarity

/** Card tilt, each axis -1..1 (where the finger is on the card). */
@Stable
class TiltState {
    var x by mutableFloatStateOf(0f) // left/right (roll)
        internal set
    var y by mutableFloatStateOf(0f) // forward/back (pitch)
        internal set
    internal var springBack: Job? = null
}

@Composable
fun rememberTiltState(): TiltState = remember { TiltState() }

/**
 * Pokémon TCG Pocket-style handling: press and drag on the card and it leans
 * toward your finger in 3D, with the foil following. Let go and it springs back.
 */
fun Modifier.dragToTilt(state: TiltState, scope: CoroutineScope): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.springBack?.cancel()
        fun follow(position: Offset) {
            state.x = (position.x / size.width * 2f - 1f).coerceIn(-1f, 1f)
            state.y = (position.y / size.height * 2f - 1f).coerceIn(-1f, 1f)
        }
        follow(down.position)
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            follow(change.position)
            if (change.positionChanged()) change.consume()
        }
        val fromX = state.x
        val fromY = state.y
        state.springBack = scope.launch {
            animate(0f, 1f, animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow)) { t, _ ->
                state.x = fromX * (1f - t)
                state.y = fromY * (1f - t)
            }
        }
    }
}

/** Rotates the card a few degrees with the phone, like holding a real card. */
fun Modifier.tilt3d(tilt: TiltState?, maxDegrees: Float = 7f): Modifier =
    if (tilt == null) this else graphicsLayer {
        rotationY = tilt.x * maxDegrees
        rotationX = -tilt.y * maxDegrees
        cameraDistance = 14f * density
    }

/** How strong the holo foil is: Common has none; shiny cards get a bit even if Common. */
fun holoStrength(rarity: Rarity, shiny: Boolean): Float = when {
    rarity == Rarity.LEGENDARY -> 0.5f
    rarity == Rarity.RARE -> 0.32f
    shiny -> 0.28f
    else -> 0f
}

private val foil = listOf(
    Color(0xFFFF6EC7), Color(0xFFFFE66D), Color(0xFF7CFFCB), Color(0xFF74B9FF), Color(0xFFB892FF), Color(0xFFFF6EC7),
)

/**
 * Holographic foil: rainbow bands plus a white glare that slide across the
 * card as the phone tilts. Values are read in the draw phase, so tilting
 * never recomposes.
 */
fun Modifier.holoSheen(tilt: TiltState?, strength: Float): Modifier =
    if (tilt == null || strength <= 0f) this else drawWithContent {
        drawContent()
        // Flat phone: foil band and glare rest near the top-left corner, off the face.
        // Tilting slides them across the whole card.
        val fx = (0.2f + tilt.x * 0.8f).coerceIn(-0.3f, 1.3f)
        val fy = (0.15f + tilt.y * 0.8f).coerceIn(-0.3f, 1.3f)
        val cx = size.width * fx
        val cy = size.height * fy
        drawRect(
            brush = Brush.linearGradient(
                colors = foil.map { it.copy(alpha = strength * 0.28f) },
                start = Offset(cx - size.width * 0.25f, cy - size.height * 0.25f),
                end = Offset(cx + size.width * 0.25f, cy + size.height * 0.25f),
                tileMode = TileMode.Mirror,
            ),
            blendMode = BlendMode.Overlay,
        )
        drawRect(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = strength * 0.45f), Color.Transparent),
                center = Offset(cx, cy),
                radius = size.minDimension * 0.4f,
            ),
            blendMode = BlendMode.Screen,
        )
    }
