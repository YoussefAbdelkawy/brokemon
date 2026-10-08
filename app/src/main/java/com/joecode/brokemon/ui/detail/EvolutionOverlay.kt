package com.joecode.brokemon.ui.detail

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.statusBarsPadding
import com.joecode.brokemon.ui.theme.Spacing
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.EvolutionStyle
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The evolution sequence. Every bro builds up for ~3.5 seconds, then crossfades
 * into their evolved look with a shockwave, but HOW it builds up depends on
 * their type (see [EvolutionStyle]): a power-up aura, a lightning storm, a
 * glitchy software update, or a spotlight drumroll.
 */
@Composable
fun EvolutionOverlay(bro: Bro, event: EvolutionEvent, onFinished: () -> Unit) {
    val style = remember(bro.primaryType) { EvolutionStyle.forType(bro.primaryType) }
    val charge = remember { Animatable(0f) }
    val shock = remember { Animatable(0f) }
    var revealed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val newStage = EvolutionStage.entries[event.toStage]
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { (context.applicationContext as com.joecode.brokemon.BrokemonApplication).container.prefs }
    val seenBefore by prefs.evolutionCinematicSeen.collectAsStateWithLifecycle(initialValue = null)
    val feedback = com.joecode.brokemon.ui.feedback.LocalFeedback.current
    val reduceMotion = com.joecode.brokemon.ui.feedback.LocalReduceMotion.current
    var skip by remember { mutableStateOf(false) }

    BackHandler { if (revealed) onFinished() else skip = seenBefore == true }

    LaunchedEffect(Unit) {
        // First time: the full build-up. After that it can be skipped. Reduce motion shortens it.
        val duration = if (reduceMotion) 900 else 3600
        coroutineScope {
            val anim = launch { charge.animateTo(1f, tween(duration, easing = LinearEasing)) }
            val watcher = launch { snapshotFlow { skip }.first { it }; anim.cancel() }
            anim.join()
            watcher.cancel()
        }
        charge.snapTo(1f)
        feedback?.play(com.joecode.brokemon.ui.feedback.Sfx.LEVEL_UP, 0.7f)
        feedback?.thump() ?: haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        revealed = true
        prefs.setEvolutionCinematicSeen()
        shock.animateTo(1f, tween(if (reduceMotion) 300 else 900, easing = FastOutSlowInEasing))
    }

    val transition = rememberInfiniteTransition(label = "evo")
    val pulse by transition.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "pulse")
    val time by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "time")

    val c = charge.value
    val accent = when (style) {
        EvolutionStyle.POWER_UP -> lerp(bro.primaryType.color, DexColors.Gold, if (revealed) 1f else c)
        EvolutionStyle.LIGHTNING -> lerp(Color(0xFF5CE1E6), Color(0xFFFFF59D), if (revealed) 1f else c)
        EvolutionStyle.GLITCH -> lerp(Color(0xFF7CFFCB), Color(0xFFFF4FA3), (time * 2f) % 1f)
        EvolutionStyle.SPOTLIGHT -> lerp(Color(0xFFFFE6A8), DexColors.Gold, c)
    }
    val particles = remember { List(40) { Triple(Random.nextFloat(), Random.nextFloat(), 0.5f + Random.nextFloat()) } }
    // A flicker that speeds up as the charge builds — the classic "something's happening" tell.
    val flashWhite = !revealed && style != EvolutionStyle.GLITCH && sin(c * c * 70f) > 0.2f && c > 0.15f
    // Lightning flashes the whole screen a few times.
    val boltPhase = (c * 7f) % 1f
    val bolt = style == EvolutionStyle.LIGHTNING && !revealed && c > 0.2f && boltPhase < 0.12f
    val glitchSwap = style == EvolutionStyle.GLITCH && !revealed && c > 0.55f && ((time * 10).toInt() % 3 == 0)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.93f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (revealed) onFinished() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (style) {
                EvolutionStyle.POWER_UP -> powerUp(c, pulse, time, accent, particles)
                EvolutionStyle.LIGHTNING -> lightning(c, time, accent, bolt, revealed)
                EvolutionStyle.GLITCH -> glitch(c, time, revealed)
                EvolutionStyle.SPOTLIGHT -> spotlight(c, time, accent, revealed)
            }
            if (shock.value > 0f) {
                drawCircle(
                    Color.White.copy(alpha = (1f - shock.value) * 0.9f),
                    radius = shock.value * size.maxDimension * 0.7f,
                    style = Stroke(width = 12.dp.toPx() * (1f - shock.value)),
                )
            }
            if (revealed && style == EvolutionStyle.SPOTLIGHT) confetti(shock.value, particles)
        }
        if (revealed) Sparkles(Modifier.fillMaxSize(), count = 30, color = DexColors.Gold, seed = bro.id.toInt())

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Spacing.xxl)) {
            Text(if (revealed) "WHOA!" else "?!", style = PixelText.Title, color = DexColors.LedYellow)
            Spacer(Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center) {
                if (style == EvolutionStyle.GLITCH && !revealed && c > 0.1f) {
                    // RGB split ghosts behind the sprite.
                    val split = (sin(time * 40f) * 8f * c)
                    BroSprite(bro.resolvedLook, event.fromStage, bro.isShiny, Modifier.size(180.dp).graphicsLayer { translationX = split; alpha = 0.6f }, tint = Color(0xFFFF2E63))
                    BroSprite(bro.resolvedLook, event.fromStage, bro.isShiny, Modifier.size(180.dp).graphicsLayer { translationX = -split; alpha = 0.6f }, tint = Color(0xFF08D9D6))
                }
                Crossfade(targetState = revealed, animationSpec = tween(500), label = "evolve") { done ->
                    BroSprite(
                        look = bro.resolvedLook,
                        stage = if (done || glitchSwap) event.toStage else event.fromStage,
                        shiny = bro.isShiny,
                        tint = if (flashWhite || bolt) Color.White else null,
                        modifier = Modifier
                            .size(180.dp)
                            .graphicsLayer {
                                val shake = if (done) 0f else when (style) {
                                    EvolutionStyle.LIGHTNING -> (Random.nextFloat() - 0.5f) * 16f * c
                                    EvolutionStyle.SPOTLIGHT -> 0f
                                    else -> (Random.nextFloat() - 0.5f) * 10f * c
                                }
                                translationX = shake
                                translationY = if (!done && style == EvolutionStyle.SPOTLIGHT) -kotlin.math.abs(sin(time * 12.56f)) * 14f * c else 0f
                                val s = if (done) 1f + (1f - shock.value) * 0.2f else 1f
                                scaleX = s
                                scaleY = s
                            },
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                if (revealed) "${bro.name} reached ${newStage.title.uppercase()}!" else "${bro.name} ${style.evolvingText}",
                style = PixelText.Label,
                color = DexColors.Text,
                textAlign = TextAlign.Center,
            )
            AnimatedVisibility(revealed, enter = fadeIn(tween(600, delayMillis = 300)) + scaleIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    event.learnedMoves.forEach { move ->
                        Text(
                            "LEARNED ${move.uppercase()}!",
                            style = PixelText.Label,
                            color = DexColors.LedYellow,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }
            AnimatedVisibility(revealed, enter = fadeIn(tween(600, delayMillis = 900)) + scaleIn()) {
                Text("TAP TO CONTINUE", style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.padding(top = 20.dp))
            }
        }
        if (bolt && !reduceMotion) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.35f)))
        if (!revealed && seenBefore == true) {
            com.joecode.brokemon.ui.components.PixelButton(
                "Skip",
                onClick = { skip = true },
                color = com.joecode.brokemon.ui.theme.DexColors.SurfaceHigh,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Spacing.lg),
            )
        }
    }
}

// --- Styles ---------------------------------------------------------------------

private fun DrawScope.powerUp(c: Float, pulse: Float, time: Float, color: Color, flames: List<Triple<Float, Float, Float>>) {
    val r = size.minDimension * 0.32f * (0.6f + 0.4f * c) * pulse
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.75f), color.copy(alpha = 0.15f), Color.Transparent), center, r * 1.4f), r * 1.4f)
    val px = 6.dp.toPx()
    flames.forEach { (fx, phase, speed) ->
        val t = ((time * speed) + phase) % 1f
        val x = center.x + (fx - 0.5f) * r * 1.6f
        val y = center.y + r - t * r * 2.4f
        val a = (1f - t) * (0.3f + 0.7f * c)
        drawRect(color.copy(alpha = a), Offset(x, y), Size(px, px * 1.5f))
        drawRect(Color.White.copy(alpha = a * 0.6f), Offset(x + px / 3, y + px / 3), Size(px / 3, px / 2))
    }
}

private fun DrawScope.lightning(c: Float, time: Float, color: Color, bolt: Boolean, revealed: Boolean) {
    // Storm clouds at the top.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1B2440), Color.Transparent), 0f, size.height * 0.4f))
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.35f * c), Color.Transparent), center, size.minDimension * 0.4f), size.minDimension * 0.4f)
    if (bolt || revealed) {
        val seed = (c * 7f).toInt()
        repeat(if (revealed) 4 else 2) { i ->
            val r = Random(seed * 31 + i)
            val path = Path().apply {
                var x = size.width * (0.2f + r.nextFloat() * 0.6f)
                var y = 0f
                moveTo(x, y)
                while (y < center.y - 40.dp.toPx()) {
                    y += size.height * 0.06f
                    x += (r.nextFloat() - 0.5f) * 60.dp.toPx() + (center.x - x) * 0.18f
                    lineTo(x, y)
                }
            }
            drawPath(path, color, style = Stroke(width = 5.dp.toPx()))
            drawPath(path, Color.White, style = Stroke(width = 2.dp.toPx()))
        }
    }
    // Static sparks circling the bro.
    val px = 5.dp.toPx()
    repeat(10) { i ->
        val a = (time + i / 10f) * 6.283f
        val rr = size.minDimension * 0.28f
        drawRect(color.copy(alpha = c), Offset(center.x + cos(a) * rr, center.y + sin(a) * rr), Size(px, px))
    }
}

private fun DrawScope.glitch(c: Float, time: Float, revealed: Boolean) {
    // Shifting data bars and scanlines.
    val r = Random((time * 20).toInt())
    repeat(if (revealed) 0 else (4 + (c * 10).toInt())) {
        val y = r.nextFloat() * size.height
        val h = (2 + r.nextInt(14)).dp.toPx()
        val color = listOf(Color(0xFF08D9D6), Color(0xFFFF2E63), Color(0xFF7CFFCB), Color.White)[r.nextInt(4)]
        drawRect(color.copy(alpha = 0.18f + 0.3f * c), Offset(r.nextFloat() * size.width * 0.3f, y), Size(size.width * (0.3f + r.nextFloat() * 0.7f), h))
    }
    var y = 0f
    while (y < size.height) {
        drawRect(Color.White.copy(alpha = 0.04f), Offset(0f, y), Size(size.width, 1.dp.toPx()))
        y += 4.dp.toPx()
    }
    if (!revealed) {
        val pct = (c * 100).toInt()
        val barW = size.width * 0.6f
        drawRect(Color(0xFF223344), Offset(center.x - barW / 2, size.height * 0.82f), Size(barW, 10.dp.toPx()))
        drawRect(Color(0xFF7CFFCB), Offset(center.x - barW / 2, size.height * 0.82f), Size(barW * pct / 100f, 10.dp.toPx()))
    }
}

private fun DrawScope.spotlight(c: Float, time: Float, color: Color, revealed: Boolean) {
    val sway = sin(time * 6.283f) * size.width * 0.12f * (1f - c)
    for (side in listOf(-1f, 1f)) {
        val top = Offset(center.x + side * size.width * 0.45f, 0f)
        val target = Offset(center.x + side * sway, center.y + 60.dp.toPx())
        val spread = 90.dp.toPx() * (if (revealed) 1.4f else 1f)
        val path = Path().apply {
            moveTo(top.x - 10.dp.toPx(), top.y)
            lineTo(top.x + 10.dp.toPx(), top.y)
            lineTo(target.x + spread, target.y)
            lineTo(target.x - spread, target.y)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.05f)), 0f, target.y))
    }
    drawOval(color.copy(alpha = 0.25f + 0.25f * c), Offset(center.x - 120.dp.toPx(), center.y + 60.dp.toPx()), Size(240.dp.toPx(), 40.dp.toPx()))
}

private fun DrawScope.confetti(t: Float, bits: List<Triple<Float, Float, Float>>) {
    val colors = listOf(Color(0xFFFF4FA3), Color(0xFFFFD54A), Color(0xFF5CE1E6), Color(0xFF7CFFCB), Color(0xFFB892FF))
    bits.forEachIndexed { i, (fx, fy, speed) ->
        val x = size.width * fx
        val y = -20f + (size.height * 0.9f) * (t * speed).coerceAtMost(1.2f) * (0.5f + fy * 0.5f)
        drawRect(colors[i % colors.size].copy(alpha = (1.2f - t).coerceIn(0f, 1f)), Offset(x, y), Size(6.dp.toPx(), 10.dp.toPx()))
    }
}
