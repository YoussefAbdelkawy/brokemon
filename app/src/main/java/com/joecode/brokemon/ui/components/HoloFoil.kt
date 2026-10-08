package com.joecode.brokemon.ui.components

import android.content.Context
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.joecode.brokemon.data.model.Rarity
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Real-foil holo for cards. Three layers, all driven by where the light is:
 *
 * 1. A rainbow band that sweeps along the card's diagonal as you tilt.
 * 2. A specular glare that moves the *opposite* way (like a reflection) and
 *    only shows up when the card is actually tilted.
 * 3. A foil texture (fine diagonal lines + sparkle grain) the rainbow shows
 *    through, so it never looks like a flat wash.
 *
 * Android 13+ draws it with an AGSL shader; older phones get the same layers
 * built from gradients and a tiled texture bitmap.
 */
enum class HoloLevel(val band: Float, val glare: Float, val sparkle: Float) {
    NONE(0f, 0f, 0f),
    /** Rare: subtle band only. */
    SUBTLE(0.09f, 0f, 0f),
    /** Epic: band plus glare. */
    BAND_GLARE(0.14f, 0.22f, 0f),
    /** Legendary: everything, plus sparkles. */
    FULL(0.19f, 0.3f, 1f),
}

data class HoloStyle(val level: HoloLevel, val shiny: Boolean) {
    val visible: Boolean get() = level != HoloLevel.NONE

    companion object {
        val OFF = HoloStyle(HoloLevel.NONE, false)

        fun of(rarity: Rarity, shiny: Boolean): HoloStyle {
            val level = when (rarity) {
                Rarity.COMMON -> HoloLevel.NONE
                Rarity.RARE -> HoloLevel.SUBTLE
                Rarity.EPIC -> HoloLevel.BAND_GLARE
                Rarity.LEGENDARY -> HoloLevel.FULL
            }
            // Shiny always gets foil (in gold/silver), even on a Common.
            return HoloStyle(if (shiny && level == HoloLevel.NONE) HoloLevel.SUBTLE else level, shiny)
        }
    }
}

/**
 * The phone's tilt from the gyro (game rotation vector), relative to how it
 * was held when the card opened. Low-pass filtered and clamped to ±25°.
 */
@Stable
class DeviceTilt {
    var x by mutableFloatStateOf(0f)
        internal set
    var y by mutableFloatStateOf(0f)
        internal set
}

private const val MAX_DEGREES = 25f
private const val SMOOTHING = 0.12f

/** Listens to the gyro only while this screen is resumed; unregisters on pause/dispose. */
@Composable
fun rememberDeviceTilt(): DeviceTilt {
    val context = LocalContext.current
    val tilt = remember { DeviceTilt() }
    LifecycleResumeEffect(tilt) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) return@LifecycleResumeEffect onPauseOrDispose { }
        val listener = TiltListener(tilt)
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose {
            manager.unregisterListener(listener)
            tilt.x = 0f
            tilt.y = 0f
        }
    }
    return tilt
}

private class TiltListener(private val tilt: DeviceTilt) : SensorEventListener {
    private val rotation = FloatArray(9)
    private val angles = FloatArray(3)
    private var basePitch = Float.NaN
    private var baseRoll = Float.NaN

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        SensorManager.getOrientation(rotation, angles)
        val pitch = Math.toDegrees(angles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(angles[2].toDouble()).toFloat()
        if (basePitch.isNaN()) { basePitch = pitch; baseRoll = roll }
        val tx = ((roll - baseRoll) / MAX_DEGREES).coerceIn(-1f, 1f)
        val ty = ((pitch - basePitch) / MAX_DEGREES).coerceIn(-1f, 1f)
        tilt.x += (tx - tilt.x) * SMOOTHING
        tilt.y += (ty - tilt.y) * SMOOTHING
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** Where the light is: the finger tilt plus the phone tilt, clamped to -1..1. Read in draw only. */
class HoloLight(private val finger: TiltState?, private val device: DeviceTilt?) {
    val x: Float get() = ((finger?.x ?: 0f) + (device?.x ?: 0f)).coerceIn(-1f, 1f)
    val y: Float get() = ((finger?.y ?: 0f) + (device?.y ?: 0f)).coerceIn(-1f, 1f)
    val strength: Float get() = (hypot(x, y) / 1.2f).coerceIn(0f, 1f)
}

/** Foil over a whole area (the card art). Content is drawn first, foil on top. */
fun Modifier.holoFoil(light: HoloLight?, style: HoloStyle): Modifier =
    if (light == null || !style.visible) this else drawWithCache {
        val painter = FoilPainter(size, style)
        onDrawWithContent {
            drawContent()
            painter.draw(this, light)
        }
    }

/** Foil masked to the card's border ring only, so text inside stays clean. */
fun Modifier.holoFoilBorder(light: HoloLight?, style: HoloStyle, shape: Shape, width: Dp): Modifier =
    if (light == null || !style.visible) this else drawWithCache {
        val painter = FoilPainter(size, style)
        // The border ring: the card outline minus the same outline inset by the border width.
        val w = width.toPx()
        val outer = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val inner = Path().apply {
            addOutline(shape.createOutline(Size(size.width - 2 * w, size.height - 2 * w), layoutDirection, this@drawWithCache))
            translate(Offset(w, w))
        }
        val ring = Path.combine(PathOperation.Difference, outer, inner)
        onDrawWithContent {
            drawContent()
            clipPath(ring) { painter.draw(this, light, boost = 2.2f) }
        }
    }

private class FoilPainter(private val size: Size, private val style: HoloStyle) {
    private val agsl: Any? = if (Build.VERSION.SDK_INT >= 33) runCatching { AgslFoil() }.getOrNull() else null

    fun draw(scope: DrawScope, light: HoloLight, boost: Float = 1f) {
        val lx = light.x
        val ly = light.y
        val band = (style.level.band * boost).coerceAtMost(0.7f)
        // Glare grows with tilt and is almost gone when the card is flat.
        val glare = style.level.glare * light.strength
        if (Build.VERSION.SDK_INT >= 33 && agsl is AgslFoil) {
            agsl.draw(scope, size, lx, ly, band, glare, style.level.sparkle, style.shiny)
        } else {
            drawFallback(scope, lx, ly, band, glare)
        }
    }

    private fun drawFallback(scope: DrawScope, lx: Float, ly: Float, band: Float, glare: Float) = with(scope) {
        val shift = lx * 0.6f + ly * 0.4f
        val colors = (if (style.shiny) SHINY_STOPS else RAINBOW_STOPS).map { it.copy(alpha = band) }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { blendMode = BlendMode.Screen })
            // 1) Rainbow band: colors slide along the diagonal with the tilt.
            val span = size.width * 0.9f
            val start = Offset(-shift * span, -shift * span)
            drawRect(Brush.linearGradient(colors, start, start + Offset(span, span), TileMode.Mirror))
            // 3) Texture: keep the rainbow only where the foil lines and grains are.
            drawRect(ShaderBrush(ImageShader(texture, TileMode.Repeated, TileMode.Repeated)), blendMode = BlendMode.DstIn)
            canvas.restore()
        }
        // 2) Glare, opposite to the tilt.
        if (glare > 0.01f) {
            val center = Offset(size.width * (0.5f - lx * 0.45f), size.height * (0.5f - ly * 0.45f))
            drawRect(
                Brush.radialGradient(listOf(Color.White.copy(alpha = glare), Color.Transparent), center, size.minDimension * 0.55f),
                blendMode = BlendMode.Screen,
            )
        }
    }

    companion object {
        val RAINBOW_STOPS = listOf(
            Color(0xFF5CF2FF), Color(0xFF9B6BFF), Color(0xFFFF5FCB), Color(0xFFFFD66B), Color(0xFF7CFF9E), Color(0xFF5CF2FF),
        )
        val SHINY_STOPS = listOf(
            Color(0xFFFFE08A), Color(0xFFF4F6FA), Color(0xFFC9A23A), Color(0xFFDDE3EC), Color(0xFFFFE08A),
        )

        /** 12x12 foil grain: diagonal hairlines at varying strength plus a couple of bright sparkle pixels. */
        val texture: ImageBitmap by lazy {
            val n = 12
            val px = IntArray(n * n) { i ->
                val x = i % n
                val y = i / n
                val line = (x + y) % 4
                val alpha = when {
                    (x == 3 && y == 8) || (x == 9 && y == 2) -> 255
                    line == 0 -> 200
                    line == 1 -> 140
                    else -> 100
                }
                alpha shl 24 or 0xFFFFFF
            }
            android.graphics.Bitmap.createBitmap(px, n, n, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }
}

@RequiresApi(33)
private class AgslFoil {
    private val shader = RuntimeShader(SOURCE)
    private val brush = ShaderBrush(shader)

    fun draw(scope: DrawScope, size: Size, lx: Float, ly: Float, band: Float, glare: Float, sparkle: Float, shiny: Boolean) {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("light", lx, ly)
        shader.setFloatUniform("band", band)
        shader.setFloatUniform("glare", glare)
        shader.setFloatUniform("sparkle", sparkle)
        shader.setFloatUniform("shiny", if (shiny) 1f else 0f)
        scope.drawRect(brush, blendMode = BlendMode.Screen)
    }

    companion object {
        private val SOURCE = """
            uniform float2 size;
            uniform float2 light;
            uniform float band;
            uniform float glare;
            uniform float sparkle;
            uniform float shiny;

            half3 hue(float h) {
                half3 c = abs(fract(h + half3(0.0, 0.6667, 0.3333)) * 6.0 - 3.0) - 1.0;
                return clamp(c, 0.0, 1.0);
            }
            float hash(float2 p) {
                return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
            }

            half4 main(float2 coord) {
                float2 uv = coord / size;
                float d = (uv.x + uv.y) * 0.5;
                float shift = light.x * 0.6 + light.y * 0.4;

                // 1) Rainbow that sweeps along the diagonal.
                float h = d * 1.4 + shift * 0.8;
                half3 rainbow = hue(h);
                half3 gold = half3(1.0, 0.86, 0.48);
                half3 silver = half3(0.92, 0.94, 0.98);
                half3 metal = mix(gold, silver, 0.5 + 0.5 * sin(h * 9.0));
                half3 color = mix(rainbow, metal, shiny);

                // A brighter stripe of foil that travels with the tilt.
                float center = 0.5 - shift * 0.7;
                float stripe = exp(-pow((d - center) * 3.2, 2.0));

                // 3) Texture: fine diagonal hairlines + sparkle grain.
                float lines = 0.5 + 0.5 * sin((uv.x - uv.y) * size.x * 0.45);
                float2 cell = floor(coord / 5.0);
                float n = hash(cell);
                float twinkle = 0.5 + 0.5 * sin(n * 50.0 + shift * 14.0);
                float grain = step(0.985, n) * twinkle * sparkle * 0.6;
                float tex = 0.6 + 0.4 * lines;

                float a = band * (0.45 + 0.9 * stripe) * tex;
                half3 col = color * a + half3(grain * 0.9);

                // 2) Glare, moving opposite the tilt.
                float2 g = float2(0.5 - light.x * 0.45, 0.5 - light.y * 0.45);
                float gl = glare * smoothstep(0.55, 0.0, distance(uv, g));
                col += half3(gl);

                float alpha = clamp(max(max(a, gl), grain), 0.0, 1.0);
                return half4(clamp(col, 0.0, 1.0), alpha);
            }
        """.trimIndent()
    }
}


/** Settings > Holo shine. When false, cards show no tilt foil. */
val LocalHoloEnabled = androidx.compose.runtime.staticCompositionLocalOf { true }
