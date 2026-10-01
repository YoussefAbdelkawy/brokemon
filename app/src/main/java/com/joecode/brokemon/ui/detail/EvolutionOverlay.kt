package com.joecode.brokemon.ui.detail

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Powering up" sequence: a pulsing aura and rising pixel flames that shift
 * from the bro's type color to gold, the sprite flickering white faster and
 * faster, then a crossfade into the evolved sprite with a shockwave.
 */
@Composable
fun EvolutionOverlay(bro: Bro, event: EvolutionEvent, onFinished: () -> Unit) {
    val charge = remember { Animatable(0f) }
    val shock = remember { Animatable(0f) }
    var revealed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val newStage = EvolutionStage.entries[event.toStage]

    BackHandler { if (revealed) onFinished() }

    LaunchedEffect(Unit) {
        charge.animateTo(1f, tween(3400, easing = LinearEasing))
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        revealed = true
        shock.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    val transition = rememberInfiniteTransition(label = "aura")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
        label = "pulse",
    )
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "flames",
    )

    val c = charge.value
    val auraColor = lerp(bro.primaryType.color, DexColors.Gold, if (revealed) 1f else c)
    val flames = remember { List(36) { Triple(Random.nextFloat(), Random.nextFloat(), 0.5f + Random.nextFloat()) } }
    // Flicker gets faster as the charge builds — classic transformation tell.
    val flashWhite = !revealed && sin(c * c * 70f) > 0.2f && c > 0.15f

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                if (revealed) onFinished()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val auraRadius = size.minDimension * 0.32f * (0.6f + 0.4f * c) * pulse
            drawCircle(
                Brush.radialGradient(
                    listOf(auraColor.copy(alpha = 0.75f), auraColor.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = auraRadius * 1.4f,
                ),
                radius = auraRadius * 1.4f,
            )
            // Rising pixel flames.
            val px = 6.dp.toPx()
            flames.forEach { (fx, phase, speed) ->
                val t = ((time * speed) + phase) % 1f
                val x = center.x + (fx - 0.5f) * auraRadius * 1.6f
                val y = center.y + auraRadius - t * auraRadius * 2.4f
                val alpha = (1f - t) * (0.3f + 0.7f * c)
                drawRect(auraColor.copy(alpha = alpha), Offset(x, y), Size(px, px * 1.5f))
                drawRect(Color.White.copy(alpha = alpha * 0.6f), Offset(x + px / 3, y + px / 3), Size(px / 3, px / 2))
            }
            if (shock.value > 0f) {
                drawCircle(
                    Color.White.copy(alpha = (1f - shock.value) * 0.9f),
                    radius = shock.value * size.maxDimension * 0.7f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx() * (1f - shock.value)),
                )
            }
        }
        if (revealed) Sparkles(Modifier.fillMaxSize(), count = 30, color = DexColors.Gold, seed = bro.id.toInt())

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                if (revealed) "WHOA!" else "?!",
                style = PixelText.Title,
                color = DexColors.LedYellow,
            )
            Spacer(Modifier.height(24.dp))
            Crossfade(targetState = revealed, animationSpec = tween(500), label = "evolve") { done ->
                BroSprite(
                    look = bro.resolvedLook,
                    stage = if (done) event.toStage else event.fromStage,
                    shiny = bro.isShiny,
                    tint = if (flashWhite) Color.White else null,
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer {
                            val jitter = if (!done) (Random.nextFloat() - 0.5f) * 10f * c else 0f
                            translationX = jitter
                            val s = if (done) 1f + (1f - shock.value) * 0.2f else 1f
                            scaleX = s
                            scaleY = s
                        },
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                if (revealed) "${bro.name} reached ${newStage.title.uppercase()}!"
                else "${bro.name} is powering up...",
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
                Text(
                    "TAP TO CONTINUE",
                    style = PixelText.Tiny,
                    color = DexColors.TextMuted,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
    }
}
