package com.joecode.brokemon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.ui.feedback.LocalReduceMotion
import com.joecode.brokemon.ui.theme.DexColors
import java.time.LocalDateTime
import kotlin.random.Random

/** Part of the day, used for the Home sky. */
enum class DayPhase(val top: Color, val bottom: Color) {
    NIGHT(Color(0xFF0B1030), Color(0xFF1B2250)),
    DAWN(Color(0xFF3A3A78), Color(0xFFE98F6F)),
    DAY(Color(0xFF3E78D6), Color(0xFF9ED2FF)),
    DUSK(Color(0xFF3C2A6B), Color(0xFFE0605A));

    companion object {
        fun at(hour: Int): DayPhase = when (hour) {
            in 5..6 -> DAWN
            in 7..16 -> DAY
            in 17..19 -> DUSK
            else -> NIGHT
        }
    }
}

/** A faint tint for the season, so the sky also tells you what time of year it is. */
fun seasonTint(month: Int): Color = when (month) {
    12, 1, 2 -> Color(0xFF9FD4FF) // winter: icy
    3, 4, 5 -> Color(0xFF9BE15D) // spring: fresh green
    6, 7, 8 -> Color(0xFFFFD54A) // summer: warm
    else -> Color(0xFFFF9A4D) // autumn: amber
}

/**
 * The pixel sky behind the Home screen: stepped gradient bands, a sun or moon, stars at night
 * and a few drifting clouds. It fades into the normal background so lists stay readable.
 */
@Composable
fun SkyBackground(modifier: Modifier = Modifier, clock: State<Long>, now: LocalDateTime = LocalDateTime.now()) {
    val phase = remember(now.hour) { DayPhase.at(now.hour) }
    val tint = remember(now.monthValue) { seasonTint(now.monthValue) }
    val stars = remember { Random(42).let { r -> List(26) { Triple(r.nextFloat(), r.nextFloat() * 0.7f, r.nextFloat()) } } }
    val reduce = LocalReduceMotion.current
    Canvas(modifier.fillMaxSize()) {
        val u = 4.dp.toPx()
        val bands = 9
        val bandH = size.height / bands
        for (i in 0 until bands) {
            val base = lerp(phase.top, phase.bottom, i / (bands - 1f))
            drawRect(lerp(base, tint, 0.10f), Offset(0f, i * bandH), Size(size.width, bandH + 1f))
        }
        val t = if (reduce) 0f else clock.value / 1000f
        if (phase == DayPhase.NIGHT || phase == DayPhase.DAWN || phase == DayPhase.DUSK) {
            for ((x, y, p) in stars) {
                val a = if (phase == DayPhase.NIGHT) 0.5f + 0.5f * kotlin.math.sin(t * 1.5f + p * 6.28f) else 0.25f
                drawRect(Color.White.copy(alpha = a.coerceIn(0.1f, 1f)), Offset((x * size.width / u).toInt() * u, (y * size.height / u).toInt() * u), Size(u / 2, u / 2))
            }
        }
        // Sun or moon, as a chunky pixel disc.
        val orbX = size.width * when (phase) { DayPhase.DAWN -> 0.2f; DayPhase.DAY -> 0.75f; DayPhase.DUSK -> 0.85f; DayPhase.NIGHT -> 0.8f }
        val orbY = size.height * when (phase) { DayPhase.DAWN -> 0.6f; DayPhase.DAY -> 0.22f; DayPhase.DUSK -> 0.55f; DayPhase.NIGHT -> 0.25f }
        val orbColor = if (phase == DayPhase.NIGHT) Color(0xFFF1F3D8) else Color(0xFFFFE08A)
        val r = 4
        for (dy in -r..r) for (dx in -r..r) {
            if (dx * dx + dy * dy <= r * r + 1) drawRect(orbColor.copy(alpha = 0.95f), Offset(orbX + dx * u / 1.6f, orbY + dy * u / 1.6f), Size(u / 1.6f + 0.5f, u / 1.6f + 0.5f))
        }
        // Clouds: blocky puffs that drift slowly.
        val cloud = Color.White.copy(alpha = if (phase == DayPhase.NIGHT) 0.10f else 0.35f)
        for (k in 0..2) {
            val cx = (((k * 0.37f) + t * 0.01f * (1 + k * 0.4f)) % 1.2f - 0.1f) * size.width
            val cy = size.height * (0.28f + k * 0.17f)
            drawRect(cloud, Offset(cx, cy), Size(u * 7, u))
            drawRect(cloud, Offset(cx + u * 1.5f, cy - u), Size(u * 4, u))
            drawRect(cloud, Offset(cx + u * 3f, cy - u * 2), Size(u * 2, u))
        }
        // Fade into the screen background.
        drawRect(Brush.verticalGradient(0.45f to Color.Transparent, 1f to DexColors.Background))
    }
}

/** Convenience for screens that just want the sky with its own slow clock. */
@Composable
fun HomeSky(modifier: Modifier = Modifier) {
    val clock = rememberClock(4)
    SkyBackground(modifier, clock)
}
