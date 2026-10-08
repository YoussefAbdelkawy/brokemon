package com.joecode.brokemon.ui.components

import android.os.SystemClock
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.domain.HumanSprite
import com.joecode.brokemon.ui.feedback.LocalReduceMotion
import com.joecode.brokemon.ui.theme.DexColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// "Alive art": idle sprite frames, type particles and illustrated rarity frames.
// Everything here runs only for what is on screen, off a shared slow clock, and
// turns into a still picture when Reduce motion is on.

/** Small LRU of rendered sprites so scrolling never re-renders the same pixel art. */
object SpriteCache {
    private data class Key(val look: BroLook, val stage: Int, val shiny: Boolean, val tint: Int?, val frame: Int)

    private val cache = LruCache<Key, ImageBitmap>(300)

    fun portrait(look: BroLook, stage: Int, shiny: Boolean, tint: Int?, frame: Int = 0): ImageBitmap {
        val key = Key(look, stage, shiny, tint, frame)
        return cache.get(key) ?: AvatarBitmaps.portrait(look, stage, shiny, tint, frame).asImageBitmap().also { cache.put(key, it) }
    }
}

/** A slow shared clock (milliseconds). Frozen at 0 when Reduce motion is on. */
@Composable
fun rememberClock(fps: Int = 12): State<Long> {
    val reduce = LocalReduceMotion.current
    val time = remember { mutableLongStateOf(0L) }
    if (!reduce) {
        LaunchedEffect(fps) {
            val step = 1000L / fps
            while (true) {
                time.longValue = SystemClock.uptimeMillis()
                delay(step)
            }
        }
    }
    return time
}

/**
 * The idle animation: a one-pixel breathing bob and an occasional blink, offset per bro
 * so a whole grid never moves in lockstep. Returns the [HumanSprite] frame flags.
 */
@Composable
fun rememberIdleFrame(seed: Long, enabled: Boolean = true): Int {
    val reduce = LocalReduceMotion.current
    if (!enabled || reduce) return 0
    val frame by produceState(0, seed) {
        val offset = (seed.mod(5000L)).toInt()
        while (true) {
            val t = SystemClock.uptimeMillis() + offset
            val blink = t % 4300L < 170L
            val bob = (t / 700L) % 2L == 1L
            val next = (if (bob) HumanSprite.FRAME_BOB else 0) or (if (blink) HumanSprite.FRAME_BLINK else 0)
            if (value != next) value = next
            delay(80)
        }
    }
    return frame
}

// --- Type particles --------------------------------------------------------------------

enum class ParticleKind { RISE, FALL, DRIFT, TWINKLE, RAIN }

class ParticleSpec(val kind: ParticleKind, val colors: List<Color>, val count: Int = 14, val speed: Float = 0.18f)

fun BroType.particles(): ParticleSpec = when (this) {
    BroType.ROAD_RAGER -> ParticleSpec(ParticleKind.RISE, listOf(Color(0xFFFF5A36), Color(0xFFFFB347), Color(0xFFFFE08A)), speed = 0.3f)
    BroType.BAD_DRIVER -> ParticleSpec(ParticleKind.DRIFT, listOf(Color(0xFFB8BCC8), Color(0xFFFFA64D)), speed = 0.5f)
    BroType.YAPPER -> ParticleSpec(ParticleKind.RISE, listOf(Color(0xFFFFE08A), Color.White), speed = 0.22f)
    BroType.GHOST -> ParticleSpec(ParticleKind.DRIFT, listOf(Color(0xFFE6ECF7), Color(0xFF9AA5B8)), count = 10, speed = 0.3f)
    BroType.GYM_RAT -> ParticleSpec(ParticleKind.FALL, listOf(Color(0xFF9AD8FF), Color(0xFFFF7A90)), speed = 0.2f)
    BroType.FOODIE -> ParticleSpec(ParticleKind.RISE, listOf(Color(0xFFF7E6D0), Color(0xFFF4A261)), count = 10, speed = 0.12f)
    BroType.GAMER -> ParticleSpec(ParticleKind.RAIN, listOf(Color(0xFFB69CFF), Color(0xFF6BFFB0)), count = 16, speed = 0.45f)
    BroType.NERD -> ParticleSpec(ParticleKind.TWINKLE, listOf(Color(0xFF8DB8FF), Color.White), speed = 0.5f)
    BroType.SPORTS_FAN -> ParticleSpec(ParticleKind.FALL, listOf(Color(0xFF9BE15D), Color.White), speed = 0.25f)
    BroType.PARTY_ANIMAL -> ParticleSpec(ParticleKind.FALL, listOf(Color(0xFFFF4FA3), Color(0xFFFFE08A), Color(0xFF5ED8FF), Color(0xFF9BE15D)), count = 18, speed = 0.28f)
    BroType.CHILL_GUY -> ParticleSpec(ParticleKind.DRIFT, listOf(Color(0xFF8FFFF2), Color(0xFF2EC4B6)), count = 10, speed = 0.15f)
    BroType.CHAOS_AGENT -> ParticleSpec(ParticleKind.RISE, listOf(Color(0xFFD62AD0), Color(0xFFFFE08A), Color(0xFF5ED8FF)), speed = 0.5f)
    BroType.MAIN_CHARACTER -> ParticleSpec(ParticleKind.TWINKLE, listOf(Color(0xFFD8C8FF), Color.White), speed = 0.4f)
    BroType.ALWAYS_LATE -> ParticleSpec(ParticleKind.DRIFT, listOf(Color(0xFFC9A27E), Color(0xFFF1D9B8)), count = 8, speed = 0.08f)
    BroType.CRYPTO_BRO -> ParticleSpec(ParticleKind.RISE, listOf(Color(0xFFFFD54A), Color(0xFFFFF0A0)), speed = 0.3f)
    BroType.OUTDOORSY -> ParticleSpec(ParticleKind.FALL, listOf(Color(0xFF4CAF50), Color(0xFF9BE15D), Color(0xFFC9A27E)), count = 10, speed = 0.14f)
    BroType.WINGMAN -> ParticleSpec(ParticleKind.RISE, listOf(Color(0xFF8DB8FF), Color.White), speed = 0.2f)
}

/** A calm particle layer behind the sprite. Draw-only: the clock is read inside the canvas. */
@Composable
fun ParticleLayer(type: BroType, seed: Long, clock: State<Long>, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    if (reduce) return
    val spec = remember(type) { type.particles() }
    val rnd = remember(type, seed) { Random(seed xor type.ordinal.toLong()).let { r -> FloatArray(spec.count * 3) { r.nextFloat() } } }
    Canvas(modifier) {
        val t = clock.value / 1000f
        val u = 2.dp.toPx()
        for (i in 0 until spec.count) {
            val a = rnd[i * 3]
            val b = rnd[i * 3 + 1]
            val c = rnd[i * 3 + 2]
            val color = spec.colors[(c * spec.colors.size).toInt().coerceAtMost(spec.colors.size - 1)]
            val s = if (c > 0.7f) u * 1.5f else u
            val speed = spec.speed * (0.6f + c)
            when (spec.kind) {
                ParticleKind.RISE -> {
                    val f = 1f - ((b + t * speed) % 1f)
                    val x = a * size.width + sin(t * 1.4f + a * 20f) * u * 2
                    pixel(snap(x, u), snap(f * size.height, u), s, color.copy(alpha = sin(f * PI.toFloat()).coerceIn(0f, 1f) * 0.85f))
                }
                ParticleKind.FALL -> {
                    val f = (b + t * speed) % 1f
                    val x = a * size.width + sin(t * 1.8f + a * 20f) * u * 3
                    pixel(snap(x, u), snap(f * size.height, u), s, color.copy(alpha = 0.85f * sin(f * PI.toFloat()).coerceIn(0.2f, 1f)))
                }
                ParticleKind.DRIFT -> {
                    val x = ((a + t * speed * 0.35f) % 1f) * size.width
                    val y = b * size.height + sin(t * 0.9f + a * 12f) * u * 3
                    val alpha = (sin(t * speed * 5f + c * 6.28f) * 0.5f + 0.5f) * 0.65f
                    pixel(snap(x, u), snap(y, u), s * 1.2f, color.copy(alpha = alpha))
                }
                ParticleKind.TWINKLE -> {
                    val w = sin(t * spec.speed * 6f + c * 6.28f)
                    if (w > 0.2f) star(Offset(snap(a * size.width, u), snap(b * size.height, u)), u, color.copy(alpha = w))
                }
                ParticleKind.RAIN -> {
                    val f = (b + t * speed * 1.6f) % 1f
                    val x = snap(a * size.width, u)
                    val y = snap(f * size.height, u)
                    for (k in 0..2) pixel(x, y - k * u, u, color.copy(alpha = (1f - k * 0.3f) * 0.7f))
                }
            }
        }
    }
}

private fun snap(v: Float, unit: Float) = (v / unit).toInt() * unit

private fun DrawScope.pixel(x: Float, y: Float, s: Float, color: Color) {
    drawRect(color, Offset(x, y), Size(s, s))
}

private fun DrawScope.star(c: Offset, u: Float, color: Color) {
    pixel(c.x, c.y, u, color)
    pixel(c.x - u, c.y, u, color.copy(alpha = color.alpha * 0.6f))
    pixel(c.x + u, c.y, u, color.copy(alpha = color.alpha * 0.6f))
    pixel(c.x, c.y - u, u, color.copy(alpha = color.alpha * 0.6f))
    pixel(c.x, c.y + u, u, color.copy(alpha = color.alpha * 0.6f))
}

// --- Illustrated rarity frames ----------------------------------------------------------

/**
 * Pixel art ornaments drawn over a card's border. Common is plain; Rare gets gems,
 * Epic gets gems plus an energy spark that runs around the edge, Legendary gets
 * ornate gold corners and a crest. The text label ("Rare") is always printed too,
 * so rarity never depends on these colors alone.
 */
fun Modifier.rarityFrame(rarity: Rarity, clock: State<Long>? = null): Modifier =
    if (rarity == Rarity.COMMON) this else drawWithContent {
        drawContent()
        val u = 2.dp.toPx()
        val w = size.width
        val h = size.height
        when (rarity) {
            Rarity.RARE -> {
                val gemColor = Color(0xFF5ED8FF)
                for (p in cornerPoints(w, h, u * 1.5f)) gem(p, u, gemColor)
                gem(Offset(w / 2, u * 0.5f), u, gemColor)
                gem(Offset(w / 2, h - u * 0.5f), u, gemColor)
            }
            Rarity.EPIC -> {
                val gemColor = DexColors.Epic
                for (p in cornerPoints(w, h, u * 1.5f)) gem(p, u, gemColor)
                val t = (clock?.value ?: 0L) / 1000f
                val perimeter = 2 * (w + h)
                for (k in 0..1) {
                    val d = ((t * 0.18f + k * 0.5f) % 1f) * perimeter
                    for (n in 0..5) {
                        val pt = onPerimeter((d - n * u * 1.6f).mod(perimeter), w, h)
                        drawRect(Color.White.copy(alpha = 1f - n * 0.16f), Offset(pt.x - u / 2, pt.y - u / 2), Size(u, u))
                    }
                }
            }
            Rarity.LEGENDARY -> {
                val gold = DexColors.Gold
                val dark = Color(0xFF8A5A00)
                val arm = u * 7
                for ((i, p) in cornerPoints(w, h, 0f).withIndex()) {
                    val sx = if (i % 2 == 0) 1f else -1f
                    val sy = if (i < 2) 1f else -1f
                    // L-shaped bracket with a studded tip.
                    drawRect(gold, Offset(minOf(p.x, p.x + sx * arm), minOf(p.y, p.y + sy * u * 1.5f)), Size(arm, u * 1.5f))
                    drawRect(gold, Offset(minOf(p.x, p.x + sx * u * 1.5f), minOf(p.y, p.y + sy * arm)), Size(u * 1.5f, arm))
                    drawRect(dark, Offset(p.x + sx * arm - (if (sx > 0) 0f else u), p.y + sy * 0f - (if (sy > 0) 0f else u * 1.5f)), Size(u, u * 1.5f))
                    gem(Offset(p.x + sx * u * 3.2f, p.y + sy * u * 3.2f), u, Color.White)
                }
                // A little crest at the top middle.
                val cx = w / 2
                drawRect(gold, Offset(cx - u * 3, -u * 0.5f), Size(u * 6, u * 1.5f))
                drawRect(gold, Offset(cx - u * 2, -u * 2f), Size(u * 4, u * 1.5f))
                drawRect(Color.White, Offset(cx - u * 0.5f, -u * 3f), Size(u, u))
            }
            Rarity.COMMON -> Unit
        }
    }

private fun cornerPoints(w: Float, h: Float, inset: Float) = listOf(
    Offset(inset, inset), Offset(w - inset, inset), Offset(inset, h - inset), Offset(w - inset, h - inset),
)

/** A tiny pixel diamond with a white glint. */
private fun DrawScope.gem(c: Offset, u: Float, color: Color) {
    drawRect(color, Offset(c.x - u / 2, c.y - u * 1.5f), Size(u, u))
    drawRect(color, Offset(c.x - u * 1.5f, c.y - u / 2), Size(u * 3, u))
    drawRect(color, Offset(c.x - u / 2, c.y + u / 2), Size(u, u))
    drawRect(Color.White.copy(alpha = 0.9f), Offset(c.x - u / 2, c.y - u / 2), Size(u, u))
}

private fun onPerimeter(d: Float, w: Float, h: Float): Offset = when {
    d < w -> Offset(d, 0f)
    d < w + h -> Offset(w, d - w)
    d < 2 * w + h -> Offset(w - (d - w - h), h)
    else -> Offset(0f, h - (d - 2 * w - h))
}

/** What a bro says when you poke their sprite. Short, silly, and tied to their main type. */
fun BroType.emote(): String = when (this) {
    BroType.ROAD_RAGER -> "HONK!"
    BroType.BAD_DRIVER -> "OOPS"
    BroType.YAPPER -> "BLAH!"
    BroType.GHOST -> "..."
    BroType.GYM_RAT -> "PUMP!"
    BroType.FOODIE -> "YUM!"
    BroType.GAMER -> "GG!"
    BroType.NERD -> "ACTUALLY"
    BroType.SPORTS_FAN -> "GOAL!"
    BroType.PARTY_ANIMAL -> "WOO!"
    BroType.CHILL_GUY -> "CHILL"
    BroType.CHAOS_AGENT -> "HAHA!"
    BroType.MAIN_CHARACTER -> "HI!"
    BroType.ALWAYS_LATE -> "5 MIN"
    BroType.CRYPTO_BRO -> "HODL"
    BroType.OUTDOORSY -> "HIKE!"
    BroType.WINGMAN -> "SMOOTH"
}
