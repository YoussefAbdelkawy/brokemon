package com.joecode.brokemon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.ui.theme.DexColors
import kotlin.math.sin
import kotlin.random.Random

/**
 * Common = nothing, Rare = a soft light sweep across the card,
 * Legendary = a pulsing outer glow plus a rotating gold rim.
 */
fun Modifier.rarityGlow(rarity: Rarity, cornerRadius: Float = 18f): Modifier = when (rarity) {
    Rarity.COMMON -> this
    Rarity.RARE -> this.shimmer()
    Rarity.EPIC -> this.shimmer()
    Rarity.LEGENDARY -> this.legendaryGlow(cornerRadius)
}

private fun Modifier.shimmer(): Modifier = composed {
    if (com.joecode.brokemon.ui.feedback.LocalReduceMotion.current) return@composed this
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "shimmerProgress",
    )
    drawWithContent {
        drawContent()
        val x = size.width * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent),
                start = Offset(x - size.width * 0.3f, 0f),
                end = Offset(x + size.width * 0.3f, size.height),
            ),
        )
    }
}

private fun Modifier.legendaryGlow(cornerRadius: Float): Modifier = composed {
    val still = com.joecode.brokemon.ui.feedback.LocalReduceMotion.current
    val pulseState: androidx.compose.runtime.State<Float>
    val angleState: androidx.compose.runtime.State<Float>
    if (still) {
        pulseState = remember { androidx.compose.runtime.mutableStateOf(0.7f) }
        angleState = remember { androidx.compose.runtime.mutableStateOf(0f) }
    } else {
        val transition = rememberInfiniteTransition(label = "legendary")
        pulseState = transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "pulse",
        )
        angleState = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
            label = "angle",
        )
    }
    val pulse by pulseState
    val angle by angleState
    this
        .drawBehind {
            // Fake bloom: stacked strokes fading outward.
            val layers = 6
            for (i in layers downTo 1) {
                val spread = i * 3.dp.toPx() * pulse
                drawRoundRect(
                    color = DexColors.Legendary.copy(alpha = 0.10f * pulse),
                    topLeft = Offset(-spread, -spread),
                    size = Size(size.width + spread * 2, size.height + spread * 2),
                    cornerRadius = CornerRadius(cornerRadius + spread),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        .drawWithContent {
            drawContent()
            drawRoundRect(
                brush = Brush.sweepGradient(
                    listOf(
                        DexColors.Legendary.copy(alpha = pulse),
                        Color.White.copy(alpha = pulse),
                        DexColors.Legendary.copy(alpha = 0.4f),
                        DexColors.Legendary.copy(alpha = pulse),
                    ),
                    center = center + Offset(sin(Math.toRadians(angle.toDouble())).toFloat() * size.width / 3, 0f),
                ),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
}

/** Twinkling 4-point pixel stars, used on shiny cards. */
@Composable
fun Sparkles(
    modifier: Modifier = Modifier,
    count: Int = 9,
    color: Color = DexColors.Gold,
    seed: Int = 7,
) {
    val points = remember(count, seed) {
        val r = Random(seed)
        List(count) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }
    val tState: androidx.compose.runtime.State<Float> = if (com.joecode.brokemon.ui.feedback.LocalReduceMotion.current) {
        remember { androidx.compose.runtime.mutableStateOf(0.2f) }
    } else {
        rememberInfiniteTransition(label = "sparkles").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
            label = "sparkleTime",
        )
    }
    val t by tState
    Canvas(modifier) {
        points.forEach { (px, py, phase) ->
            val wave = sin(((t + phase) % 1f) * Math.PI * 2).toFloat()
            if (wave > 0f) drawPixelStar(Offset(px * size.width, py * size.height), wave, color)
        }
    }
}

private fun DrawScope.drawPixelStar(center: Offset, strength: Float, color: Color) {
    val p = 2.dp.toPx()
    val arm = (1 + (strength * 2).toInt())
    val c = color.copy(alpha = strength)
    drawRect(Color.White.copy(alpha = strength), Offset(center.x - p / 2, center.y - p / 2), Size(p, p))
    for (i in 1..arm) {
        drawRect(c, Offset(center.x - p / 2 + i * p, center.y - p / 2), Size(p, p))
        drawRect(c, Offset(center.x - p / 2 - i * p, center.y - p / 2), Size(p, p))
        drawRect(c, Offset(center.x - p / 2, center.y - p / 2 + i * p), Size(p, p))
        drawRect(c, Offset(center.x - p / 2, center.y - p / 2 - i * p), Size(p, p))
    }
}

/** Faint horizontal scanlines for the LCD "screen" panels. */
fun Modifier.scanlines(alpha: Float = 0.07f): Modifier = drawWithContent {
    drawContent()
    val step = 3.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawRect(Color.Black.copy(alpha = alpha), Offset(0f, y), Size(size.width, step / 3))
        y += step
    }
}
