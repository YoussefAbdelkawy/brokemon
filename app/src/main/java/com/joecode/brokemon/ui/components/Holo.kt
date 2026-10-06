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
 * Hold-the-card handling: press and drag on the card and it leans
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

/** Leans the card toward the finger in 3D, like holding a real card. */
fun Modifier.tilt3d(tilt: TiltState?, maxDegrees: Float = 7f): Modifier =
    if (tilt == null) this else graphicsLayer {
        rotationY = tilt.x * maxDegrees
        rotationX = -tilt.y * maxDegrees
        cameraDistance = 14f * density
    }

