package com.joecode.brokemon.ui.wild

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.CheckInResult
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionInfo
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroFullBody
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.SegmentedBar
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.components.TypeBadge
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

private enum class WildPhase { INTRO, APPEARED, ASK, RESULT }

/**
 * Shake the phone (or tap WILD) and a random bro jumps out, retro battle
 * style. Then it nudges you to actually message them.
 */
@Composable
fun WildBroScreen(
    onBack: () -> Unit,
    onOpenBro: (Long) -> Unit,
    onCatch: () -> Unit,
    viewModel: WildBroViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var phase by remember(state.encounter) { mutableStateOf(WildPhase.INTRO) }
    val intro = remember(state.encounter) { Animatable(0f) }
    val bro = state.bro

    LaunchedEffect(state.encounter, bro?.id) {
        if (bro == null) return@LaunchedEffect
        intro.snapTo(0f)
        intro.animateTo(1f, tween(INTRO_MS, easing = LinearEasing))
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        phase = WildPhase.APPEARED
    }
    LaunchedEffect(state.result) { if (state.result != null) phase = WildPhase.RESULT }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF16323A), Color(0xFF1E3A2A), Color(0xFF14241A)))),
    ) {
        when {
            state.loading -> CircularProgressIndicator(color = DexColors.DexRed, modifier = Modifier.align(Alignment.Center))
            bro == null -> NoWildBros(onBack, onCatch)
            else -> {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DexColors.Text)
                        }
                        Text("WILD ENCOUNTER", style = PixelText.Label, color = DexColors.TextMuted)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        BattleField(
                            bro = bro,
                            evolution = state.evolution!!,
                            trainerLook = state.trainer?.look,
                            progress = intro.value,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).fillMaxSize(),
                        )
                    }
                    BattleText(
                        phase = phase,
                        bro = bro,
                        result = state.result,
                        onNext = { if (phase == WildPhase.APPEARED) phase = WildPhase.ASK },
                        onWhatsApp = {
                            openWhatsApp(context, "Yo ${bro.name}! A wild you just showed up in my Brodex. How you doing? 👀")
                            viewModel.checkIn()
                        },
                        onCheckIn = viewModel::checkIn,
                        onAnother = viewModel::encounter,
                        onRun = onBack,
                        onOpenCard = { onOpenBro(bro.id) },
                    )
                }
                // The classic flash + closing bars, drawn over everything.
                IntroOverlay(intro.value, Modifier.fillMaxSize())
            }
        }
    }
}

private const val INTRO_MS = 1900

/** 0..1 of the window [start, end] of the intro timeline. */
private fun window(t: Float, start: Float, end: Float) = ((t - start) / (end - start)).coerceIn(0f, 1f)

@Composable
private fun IntroOverlay(t: Float, modifier: Modifier) {
    if (t >= 1f) return
    Canvas(modifier) {
        // 1) Three quick white flashes.
        val flash = window(t, 0f, 0.28f)
        if (flash in 0.001f..0.999f) {
            val pulse = abs(sin(flash * Math.PI * 3)).toFloat()
            drawRect(Color.White.copy(alpha = pulse * 0.9f))
        }
        // 2) Bars slam shut from alternating sides, then 3) slide away to reveal the field.
        val close = window(t, 0.28f, 0.55f)
        val open = window(t, 0.62f, 0.85f)
        if (t >= 0.28f) {
            val bars = 10
            val barH = size.height / bars
            for (i in 0 until bars) {
                val fromLeft = i % 2 == 0
                val cover = (close * (1f - open)).coerceIn(0f, 1f)
                val w = size.width * cover
                val x = if (fromLeft) 0f else size.width - w
                drawRect(Color.Black, Offset(x, i * barH), Size(w, barH + 1f))
            }
        }
    }
}

@Composable
private fun BattleField(bro: Bro, evolution: EvolutionInfo, trainerLook: BroLook?, progress: Float, modifier: Modifier) {
    val slide = window(progress, 0.7f, 1f)
    val bob by rememberInfiniteTransition(label = "idle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "bob",
    )
    BoxWithConstraints(modifier.padding(horizontal = 16.dp)) {
        val w = constraints.maxWidth.toFloat()
        // Wild bro: top right, slides in from the left edge.
        Column(Modifier.align(Alignment.TopEnd).padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Platform(Modifier.size(200.dp, 44.dp))
                Box(
                    Modifier
                        .padding(bottom = 18.dp)
                        .size(170.dp)
                        .graphicsLayer {
                            translationX = -(1f - slide) * w
                            translationY = bob * 3f
                        }
                        .semantics { contentDescription = "Wild ${bro.name}" },
                ) {
                    BroSprite(bro, evolution.stage.ordinal, Modifier.fillMaxSize())
                    if (bro.isShiny && slide >= 1f) Sparkles(Modifier.fillMaxSize(), seed = bro.id.toInt())
                }
            }
        }
        // Enemy info box: top left.
        InfoBox(
            bro,
            evolution,
            Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp)
                .graphicsLayer { alpha = slide; translationX = -(1f - slide) * 80f },
        )
        // You: bottom left, slides in from the right.
        Box(Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp), contentAlignment = Alignment.BottomCenter) {
            Platform(Modifier.size(170.dp, 38.dp))
            BroFullBody(
                trainerLook ?: BroLook(),
                stage = 0,
                shiny = false,
                tint = if (trainerLook == null) DexColors.Outline else null,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(width = 96.dp, height = 156.dp)
                    .graphicsLayer {
                        translationX = (1f - slide) * w
                        translationY = -bob * 2f
                    },
            )
        }
    }
}

@Composable
private fun Platform(modifier: Modifier) {
    Canvas(modifier) {
        drawOval(Color(0xFF2F5A3A), Offset.Zero, size)
        drawOval(Color(0xFF3F7A4B), Offset(size.width * 0.06f, size.height * 0.08f), Size(size.width * 0.88f, size.height * 0.62f))
    }
}

@Composable
private fun InfoBox(bro: Bro, evolution: EvolutionInfo, modifier: Modifier) {
    val shape = DexShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 2.dp, bottomEnd = 14.dp)
    Column(
        modifier
            .width(196.dp)
            .background(Color(0xFFF7F4E8), shape)
            .border(3.dp, Color(0xFF16161C), shape)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                bro.name.uppercase(),
                style = PixelText.Tiny,
                color = Color(0xFF16161C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(evolution.stage.title.uppercase(), style = PixelText.Tiny, color = DexColors.DexRed)
        }
        Spacer(Modifier.height(6.dp))
        TypeBadge(bro.primaryType, compact = true)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BOND", style = PixelText.Tiny, color = DexColors.DexRed)
            Spacer(Modifier.width(6.dp))
            SegmentedBar(evolution.progress, bro.primaryType.color, Modifier.weight(1f).height(8.dp), segments = 12)
        }
    }
}

@Composable
private fun BattleText(
    phase: WildPhase,
    bro: Bro,
    result: CheckInResult?,
    onNext: () -> Unit,
    onWhatsApp: () -> Unit,
    onCheckIn: () -> Unit,
    onAnother: () -> Unit,
    onRun: () -> Unit,
    onOpenCard: () -> Unit,
) {
    val days = CheckOnBro.daysSinceContact(bro)
    val message = when (phase) {
        WildPhase.INTRO -> ""
        WildPhase.APPEARED -> "${bro.name.uppercase()} jumped out of the group chat!"
        WildPhase.ASK -> "Check in with ${bro.name}? " + when (days) {
            0L -> "You talked today. Legend."
            1L -> "Last check-in: yesterday."
            else -> "It's been $days days."
        }
        WildPhase.RESULT -> when (result) {
            CheckInResult.CHECKED_IN -> "You checked in with ${bro.name}! +${Evolution.CHECK_IN_POINTS} bond pts. It hit DIFFERENT."
            CheckInResult.ALREADY_TODAY -> "Already checked in with ${bro.name} today. Still, nice to say hi!"
            else -> "${bro.name} got away..."
        }
    }
    var shown by remember(message) { mutableIntStateOf(0) }
    LaunchedEffect(message) {
        while (shown < message.length) {
            delay(22)
            shown++
        }
    }
    val paper = Color(0xFFF7F4E8)
    val ink = Color(0xFF16161C)
    val shape = DexShape(6.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(paper, shape)
            .border(4.dp, ink, shape)
            .padding(4.dp)
            .border(2.dp, DexColors.DexRed, DexShape(4.dp))
            .clickable(enabled = phase == WildPhase.APPEARED, role = Role.Button, onClickLabel = "Next") {
                if (shown < message.length) shown = message.length else onNext()
            }
            .padding(14.dp)
            .heightIn(min = 72.dp),
    ) {
        Text(
            message.take(shown),
            color = ink,
            style = PixelText.Label.copy(lineHeight = PixelText.Header.lineHeight),
            modifier = Modifier.semantics { contentDescription = message },
        )
        when (phase) {
            WildPhase.APPEARED -> if (shown >= message.length) {
                Text("TAP TO CONTINUE", style = PixelText.Tiny, color = DexColors.DexRed, modifier = Modifier.align(Alignment.End).padding(top = 8.dp))
            }
            WildPhase.ASK -> {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(
                        "WhatsApp",
                        onWhatsApp,
                        Modifier.weight(1f),
                        color = Color(0xFF1FA855),
                        stacked = true,
                        leading = { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = DexColors.Text, modifier = Modifier.size(20.dp)) },
                    )
                    PixelButton(
                        "Check in",
                        onCheckIn,
                        Modifier.weight(1f),
                        stacked = true,
                        leading = { Icon(Icons.Filled.WavingHand, null, tint = DexColors.LedYellow, modifier = Modifier.size(20.dp)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(
                        "Another",
                        onAnother,
                        Modifier.weight(1f),
                        color = DexColors.SurfaceHigh,
                        stacked = true,
                        leading = { Icon(Icons.Filled.Refresh, null, tint = DexColors.LedBlue, modifier = Modifier.size(20.dp)) },
                    )
                    PixelButton(
                        "Run",
                        onRun,
                        Modifier.weight(1f),
                        color = DexColors.SurfaceHigh,
                        stacked = true,
                        leading = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = DexColors.TextMuted, modifier = Modifier.size(20.dp)) },
                    )
                }
            }
            WildPhase.RESULT -> {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton("Open card", onOpenCard, Modifier.weight(1f), color = DexColors.SurfaceHigh)
                    PixelButton("Another", onAnother, Modifier.weight(1f))
                }
            }
            WildPhase.INTRO -> Unit
        }
    }
}

@Composable
private fun NoWildBros(onBack: () -> Unit, onCatch: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BroSprite(BroLook(), 0, false, Modifier.size(120.dp), tint = DexColors.Outline)
        Spacer(Modifier.height(20.dp))
        Text("THE GRASS IS QUIET...", style = PixelText.Header, color = DexColors.Text)
        Spacer(Modifier.height(10.dp))
        Text("No wild bros yet. Catch your first one and they'll start popping up.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        PixelButton("Catch a bro", onCatch, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        PixelButton("Back", onBack, Modifier.fillMaxWidth(), color = DexColors.SurfaceHigh)
    }
}

/**
 * Opens WhatsApp with a ready-made message (you pick the chat there), or
 * falls back to the share sheet if WhatsApp isn't installed.
 */
fun openWhatsApp(context: Context, message: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        try {
            context.startActivity(Intent(send).setPackage(pkg))
            return
        } catch (_: ActivityNotFoundException) {
            // Not installed; try the next one.
        }
    }
    runCatching { context.startActivity(Intent.createChooser(send, "Say hi")) }
}
