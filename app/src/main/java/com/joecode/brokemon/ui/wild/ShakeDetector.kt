package com.joecode.brokemon.ui.wild

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.sqrt

/**
 * Calls [onShake] when the phone is shaken: two hard jolts (over ~2.4 g)
 * within 700 ms, at most once every 2.5 s. Listens only while the screen is
 * resumed, so it never runs in the background.
 */
@Composable
fun ShakeEffect(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)
    LifecycleResumeEffect(enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || manager == null || sensor == null) return@LifecycleResumeEffect onPauseOrDispose { }
        val detector = ShakeDetector { currentOnShake() }
        manager.registerListener(detector, sensor, SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose { manager.unregisterListener(detector) }
    }
}

class ShakeDetector(
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val onShake: () -> Unit,
) : SensorEventListener {
    private var firstJolt = 0L
    private var lastShake = Long.MIN_VALUE / 2

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        onReading(x, y, z)
    }

    /** Split out so the logic is unit-testable without a real sensor. */
    fun onReading(x: Float, y: Float, z: Float) {
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (g < THRESHOLD_G) return
        val now = clock()
        if (now - lastShake < COOLDOWN_MS) return
        if (firstJolt != 0L && now - firstJolt in MIN_GAP_MS..WINDOW_MS) {
            firstJolt = 0L
            lastShake = now
            onShake()
        } else if (firstJolt == 0L || now - firstJolt > WINDOW_MS) {
            firstJolt = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        const val THRESHOLD_G = 2.4f
        const val WINDOW_MS = 700L
        const val MIN_GAP_MS = 80L
        const val COOLDOWN_MS = 2500L
    }
}
