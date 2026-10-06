package com.joecode.brokemon.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.ShinyHunt
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * The "SHINY!" moment: a burst of light, the card flips over and lands on
 * its shiny side. Shown when a logged memory turns a bro shiny.
 */
@Composable
fun ShinyMoment(bro: Bro, stage: EvolutionStage, onShare: () -> Unit, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val flip = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        burst.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        flip.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(120)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val spin by rememberInfiniteTransition(label = "rays").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by rememberInfiniteTransition(label = "title").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "pulse",
    )
    BackHandler(onBack = onDismiss)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        // Rotating gold rays + an expanding flash ring.
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val rays = 16
            for (i in 0 until rays) {
                val a = Math.toRadians((spin + i * 360.0 / rays)).toFloat()
                val len = size.maxDimension * 0.7f * burst.value
                drawLine(
                    Brush.linearGradient(
                        listOf(DexColors.Gold.copy(alpha = 0.35f), Color.Transparent),
                        start = c,
                        end = Offset(c.x + cos(a) * len, c.y + sin(a) * len),
                    ),
                    start = c,
                    end = Offset(c.x + cos(a) * len, c.y + sin(a) * len),
                    strokeWidth = 18.dp.toPx(),
                )
            }
            val ring = burst.value
            drawCircle(Color.White.copy(alpha = (1f - ring) * 0.8f), radius = size.minDimension * 0.7f * ring, center = c)
        }
        Sparkles(Modifier.fillMaxSize(), count = 22, seed = bro.id.toInt() + 3)
        Column(
            Modifier.padding(24.dp).semantics { liveRegion = LiveRegionMode.Assertive },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sparkles(Modifier.size(36.dp), count = 3, seed = 1)
                Text(
                    "SHINY!",
                    style = PixelText.Title.copy(fontSize = PixelText.Title.fontSize * 1.4f),
                    color = DexColors.Gold,
                    modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse },
                )
                Sparkles(Modifier.size(36.dp), count = 3, seed = 2)
            }
            Spacer(Modifier.height(16.dp))
            // Flip: the normal card turns away, the shiny side turns in.
            val t = flip.value
            val showShiny = t >= 0.5f
            BroCard(
                bro = bro.copy(isShiny = showShiny),
                stage = stage,
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .fillMaxWidth(0.66f)
                    .graphicsLayer {
                        // 0..90 degrees for the old side, then -90..0 for the shiny side.
                        rotationY = if (showShiny) (t - 1f) * 180f else t * 180f
                        cameraDistance = 14f * density
                        scaleX = 0.8f + 0.2f * burst.value
                        scaleY = 0.8f + 0.2f * burst.value
                    },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "${bro.name} is shining! A memory turned them shiny (1 in ${ShinyHunt.ODDS}).",
                color = DexColors.Text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 360.dp)) {
                PixelButton("Share", onShare, Modifier.weight(1f), color = DexColors.LedBlue.copy(alpha = 0.85f))
                PixelButton("Nice!", onDismiss, Modifier.weight(1f))
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
