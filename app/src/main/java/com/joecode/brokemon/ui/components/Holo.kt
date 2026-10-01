package com.joecode.brokemon.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import com.joecode.brokemon.data.model.Rarity

/** Phone tilt, each axis -1..1, relative to how the phone was held when the screen opened. */
@Stable
class TiltState {
    var x by mutableFloatStateOf(0f) // left/right (roll)
        internal set
    var y by mutableFloatStateOf(0f) // forward/back (pitch)
        internal set
}

/**
 * Listens to the rotation-vector sensor while this composable is on screen.
 * Off when the user has disabled animations in Android settings.
 */
@Composable
fun rememberTilt(): TiltState {
    val context = LocalContext.current
    val state = remember { TiltState() }
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    DisposableEffect(animationsOff) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (animationsOff || manager == null || sensor == null) return@DisposableEffect onDispose { }

        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        var base: FloatArray? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val b = base ?: orientation.copyOf().also { base = it }
                val roll = ((orientation[2] - b[2]) / MAX_TILT_RAD).coerceIn(-1f, 1f)
                val pitch = ((orientation[1] - b[1]) / MAX_TILT_RAD).coerceIn(-1f, 1f)
                // Low-pass so the shine glides instead of jittering.
                state.x += (roll - state.x) * 0.2f
                state.y += (pitch - state.y) * 0.2f
                // Slowly re-center, so however you hold the phone becomes "flat".
                b[1] += (orientation[1] - b[1]) * 0.01f
                b[2] += (orientation[2] - b[2]) * 0.01f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
    return state
}

private const val MAX_TILT_RAD = 0.45f

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
