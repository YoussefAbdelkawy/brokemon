package com.joecode.brokemon.ui.trainer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Trainer
import com.joecode.brokemon.domain.LevelInfo
import com.joecode.brokemon.domain.Reward
import com.joecode.brokemon.domain.RewardKind
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.RewardIcon
import com.joecode.brokemon.ui.components.SegmentedBar
import com.joecode.brokemon.ui.components.TiltState
import com.joecode.brokemon.ui.components.TypeBadge
import com.joecode.brokemon.ui.components.DeviceTilt
import com.joecode.brokemon.ui.components.HoloLevel
import com.joecode.brokemon.ui.components.HoloLight
import com.joecode.brokemon.ui.components.HoloStyle
import com.joecode.brokemon.ui.components.holoFoil
import com.joecode.brokemon.ui.components.holoFoilBorder
import com.joecode.brokemon.ui.components.scanlines
import com.joecode.brokemon.ui.components.tilt3d
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

data class TrainerTotals(val bros: Int, val checkIns: Int, val memories: Int)

/** The user's own card: ID-card layout with their sprite, level, XP and badges. */
@Composable
fun TrainerCard(
    trainer: Trainer,
    level: LevelInfo,
    totals: TrainerTotals,
    unlocked: Set<Reward>,
    modifier: Modifier = Modifier,
    trophies: Int = 0,
    tilt: TiltState? = null,
    deviceTilt: DeviceTilt? = null,
) {
    val frame = trainer.resolvedFrame
    val shape = DexShape(12.dp)
    val frameBrush = Brush.linearGradient(frame.colors.map { Color(it) })
    val xpProgress by animateFloatAsState(level.progress, tween(900), label = "xp")
    Column(
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Trainer card: ${trainer.name}, Trainer level ${level.level}, ${frame.label} frame"
            }
            .tilt3d(tilt, 14f)
            .clip(shape)
            .background(DexColors.Surface)
            .border(5.dp, frameBrush, shape)
            .holoFoilBorder(
                light = HoloLight(tilt, deviceTilt),
                style = HoloStyle(if (frame.holo) HoloLevel.FULL else HoloLevel.SUBTLE, shiny = frame.holo),
                shape = shape,
                width = 5.dp,
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TRAINER CARD", style = PixelText.Tiny, color = DexColors.TextMuted)
            Spacer(Modifier.weight(1f))
            Text("LV.${level.level}", style = PixelText.Header, color = DexColors.LedYellow)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(104.dp)
                    .clip(DexShape(6.dp))
                    .background(Brush.radialGradient(listOf(trainer.type.color.copy(alpha = 0.4f), DexColors.Screen)))
                    .border(2.dp, frameBrush, DexShape(6.dp))
                    .scanlines()
                    .holoFoil(
                        HoloLight(tilt, deviceTilt).takeIf { tilt != null || deviceTilt != null },
                        HoloStyle(if (frame.holo) HoloLevel.FULL else HoloLevel.NONE, shiny = frame.holo),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BroSprite(trainer.look, 0, false, Modifier.fillMaxSize(0.9f))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(trainer.name.uppercase(), style = PixelText.Header, color = DexColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                TypeBadge(trainer.type, compact = true)
                if (trainer.motto.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"${trainer.motto}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = DexColors.ScreenText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("XP", style = PixelText.Tiny, color = DexColors.TextMuted)
            Spacer(Modifier.width(8.dp))
            SegmentedBar(xpProgress, DexColors.LedBlue, Modifier.weight(1f).height(10.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                level.nextLevelXp?.let { "${level.xp}/$it" } ?: "MAX",
                style = PixelText.Tiny,
                color = DexColors.Text,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Total("BROS", totals.bros)
            Total("CHECK-INS", totals.checkIns)
            Total("MEMORIES", totals.memories)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("BADGES", style = PixelText.Tiny, color = DexColors.TextMuted)
            Reward.entries.filter { it.kind == RewardKind.BADGE }.forEach { RewardIcon(it, it in unlocked, size = 26.dp) }
            if (trophies > 0) {
                Spacer(Modifier.weight(1f))
                Text("🏆 x$trophies", style = PixelText.Label, color = DexColors.Gold)
            }
        }
    }
}

@Composable
private fun Total(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("%03d".format(value), style = PixelText.Label, color = DexColors.ScreenText)
        Spacer(Modifier.height(4.dp))
        Text(label, style = PixelText.Tiny, color = DexColors.TextMuted)
    }
}

/**
 * Home-screen strip: your sprite, name and level, plus the Journal progress.
 * Before you've made a Trainer Card it invites you to make one.
 */
@Composable
fun TrainerStrip(
    trainer: Trainer?,
    level: LevelInfo,
    questsDone: Int,
    questsTotal: Int,
    rewardsReady: Int,
    onOpenTrainer: () -> Unit,
    onOpenJournal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = DexShape(8.dp)
    val pulse by rememberInfiniteTransition(label = "ready").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, trainer?.resolvedFrame?.let { f -> Brush.linearGradient(f.colors.map { Color(it) }) }
                ?: Brush.linearGradient(listOf(DexColors.Outline, DexColors.Outline)), shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClickLabel = "Open Trainer Card", onClick = onOpenTrainer)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(DexColors.Screen, DexShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (trainer != null) {
                    BroSprite(trainer.look, 0, false, Modifier.fillMaxSize(0.9f))
                } else {
                    Icon(Icons.Filled.PersonAdd, null, tint = DexColors.LedYellow)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (trainer != null) {
                    Text(trainer.name.uppercase(), style = PixelText.Label, color = DexColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TRAINER LV.${level.level}", style = PixelText.Tiny, color = DexColors.LedYellow)
                        Spacer(Modifier.width(8.dp))
                        SegmentedBar(level.progress, DexColors.LedBlue, Modifier.weight(1f).height(6.dp), segments = 10)
                    }
                } else {
                    Text("MAKE YOUR TRAINER CARD", style = PixelText.Tiny, color = DexColors.LedYellow)
                    Spacer(Modifier.height(4.dp))
                    Text("Catch yourself first.", style = MaterialTheme.typography.bodySmall, color = DexColors.TextMuted)
                }
            }
        }
        // Journal chip.
        Column(
            Modifier
                .clickable(role = Role.Button, onClickLabel = "Open Trainer's Journal", onClick = onOpenJournal)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Icon(Icons.AutoMirrored.Filled.MenuBook, "Journal", tint = DexColors.ScreenText)
                if (rewardsReady > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .graphicsLayer { alpha = pulse }
                            .background(DexColors.LedRed, DexShape(2.dp)),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (rewardsReady > 0) "CLAIM!" else "$questsDone/$questsTotal",
                style = PixelText.Tiny,
                color = if (rewardsReady > 0) DexColors.LedRed else DexColors.TextMuted,
            )
        }
    }
}
