package com.joecode.brokemon.ui.catchbro

import com.joecode.brokemon.ui.theme.Spacing
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.ui.components.CatchCube
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** Drop → three shakes → click → star burst → "GOTCHA!". */
@Composable
fun CatchOverlay(result: CaughtResult, onViewBro: () -> Unit, onDone: () -> Unit) {
    val drop = remember { Animatable(-1f) }
    val shake = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val core = remember { Animatable(0.3f) }
    var revealed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val feedback = com.joecode.brokemon.ui.feedback.LocalFeedback.current

    BackHandler(onBack = onDone)

    LaunchedEffect(Unit) {
        drop.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
        repeat(3) {
            delay(280)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            core.animateTo(1f, tween(80))
            shake.animateTo(1f, tween(90))
            shake.animateTo(-1f, tween(160))
            shake.animateTo(0f, tween(90))
            core.animateTo(0.3f, tween(120))
        }
        delay(350)
        feedback?.play(com.joecode.brokemon.ui.feedback.Sfx.CATCH, 0.6f)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        core.snapTo(1f)
        burst.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        revealed = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            // Swallow taps so the form underneath can't be touched mid-animation.
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        val rayColor = if (result.isShiny) DexColors.Gold else DexColors.LedBlue
        Canvas(Modifier.fillMaxSize()) {
            val b = burst.value
            if (b <= 0f) return@Canvas
            val px = 6.dp.toPx()
            val alpha = (1f - b).coerceIn(0f, 1f)
            for (i in 0 until 16) {
                val angle = Math.toRadians(i * 22.5)
                val dist = b * size.minDimension * 0.55f
                val pos = center + Offset((cos(angle) * dist).toFloat(), (sin(angle) * dist).toFloat())
                drawRect(rayColor.copy(alpha = alpha), pos, Size(px, px))
                drawRect(Color.White.copy(alpha = alpha), pos + Offset(px / 3, px / 3), Size(px / 3, px / 3))
            }
            drawCircle(Color.White.copy(alpha = alpha * 0.35f), radius = b * size.minDimension * 0.3f)
        }
        if (revealed && result.isShiny) Sparkles(Modifier.fillMaxSize(), count = 24, seed = result.id.toInt())

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CatchCube(
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        translationY = drop.value * 1400f
                        rotationZ = shake.value * 18f
                        val pop = 1f + burst.value * 0.15f
                        scaleX = pop
                        scaleY = pop
                    },
                coreGlow = core.value,
            )
            Spacer(Modifier.height(32.dp))
            AnimatedVisibility(
                visible = revealed,
                enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    com.joecode.brokemon.ui.components.Dexy(com.joecode.brokemon.ui.components.DexyMood.WOW, size = 56.dp)
                    Text("GOTCHA!", style = PixelText.Title, color = DexColors.LedYellow)
                    Text(
                        "${result.name} joined your Brodex!",
                        style = PixelText.Label,
                        color = DexColors.Text,
                        textAlign = TextAlign.Center,
                    )
                    if (result.isShiny) {
                        Text("IT'S SHINY!", style = PixelText.Header, color = DexColors.Gold)
                    }
                    result.eventLabel?.let {
                        Text("LIMITED $it FRAME!", style = PixelText.Label, color = DexColors.LedYellow, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(8.dp))
                    PixelButton("View card", onViewBro, Modifier.fillMaxWidth())
                    PixelButton("Done", onDone, Modifier.fillMaxWidth(), color = DexColors.SurfaceHigh)
                }
            }
        }
    }
}
