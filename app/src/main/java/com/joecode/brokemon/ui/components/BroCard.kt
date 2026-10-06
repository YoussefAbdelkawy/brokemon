package com.joecode.brokemon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.SeasonEvents
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

/** Collectible trading card used in the dex grid. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BroCard(
    bro: Bro,
    stage: EvolutionStage,
    modifier: Modifier = Modifier,
    tilt: TiltState? = null,
    tiltDegrees: Float = 7f,
    holoFloor: Float = 0f,
    onLongClick: (() -> Unit)? = null,
    /** Show the Pokédex-style dex entry under the types (big cards: detail, binder). */
    showDexEntry: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = CutCornerShape(10.dp)
    val event = SeasonEvents.parse(bro.eventFrame)
    // Event cards wear the event's colors as a limited-edition frame.
    val borderBrush = bro.frameBrush()
    Column(
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "${bro.dexNumber} ${bro.name}, ${bro.rarity.label}" +
                    (if (bro.isShiny) ", shiny" else "") + ", ${stage.title}" +
                    (event?.let { ", limited ${it.label} frame" } ?: "")
            }
            .tilt3d(tilt, tiltDegrees)
            .rarityGlow(bro.rarity)
            .clip(shape)
            .background(DexColors.Surface)
            .holoSheen(tilt, maxOf(holoFloor, holoStrength(bro.rarity, bro.isShiny)))
            .border(if (event != null) 4.dp else 3.dp, borderBrush, shape)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                        onLongClickLabel = onLongClick?.let { "Enter ${bro.name}'s room" },
                    )
                } else Modifier,
            )
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bro.dexNumber, style = PixelText.Tiny, color = DexColors.TextMuted)
            Spacer(Modifier.weight(1f))
            if (bro.isTraded) {
                Icon(Icons.Filled.SwapHoriz, "Traded", tint = DexColors.LedBlue, modifier = Modifier.size(12.dp))
            }
            if (!bro.isTradeable) {
                Icon(Icons.Filled.Lock, "Locked", tint = DexColors.TextMuted, modifier = Modifier.size(12.dp))
            }
            RarityStars(bro.rarity, size = 10.dp)
        }
        Spacer(Modifier.height(6.dp))
        SpriteWindow(bro, stage, Modifier.fillMaxWidth().aspectRatio(1f), eventIconSize = 16.dp)
        event?.let {
            Spacer(Modifier.height(6.dp))
            EventRibbon(it, compact = true)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            bro.name,
            style = PixelText.Label,
            color = DexColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        // Long type names like "Main Character" wrap instead of overflowing the card.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            bro.types.forEach { TypeBadge(it, compact = true) }
        }
        Spacer(Modifier.height(6.dp))
        Text(stage.title.uppercase(), style = PixelText.Tiny, color = DexColors.ScreenText)
        if (showDexEntry) {
            Spacer(Modifier.height(8.dp))
            DexEntryText(bro, Modifier.fillMaxWidth())
        }
    }
}

/** The dex entry in a little LCD box, like the description on a real dex page. */
@Composable
fun DexEntryText(bro: Bro, modifier: Modifier = Modifier, maxLines: Int = 4) {
    Text(
        bro.dexEntry,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = if (bro.flavorText.isBlank()) FontStyle.Italic else FontStyle.Normal,
        color = DexColors.ScreenText,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(DexColors.Screen, CutCornerShape(3.dp))
            .border(1.dp, DexColors.ScreenBorder, CutCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/**
 * The portrait window shared by every view (card grid, Pokédex list, binder):
 * type-colored glow, scanlines, the sprite, shiny sparkles and event corners.
 */
@Composable
fun SpriteWindow(bro: Bro, stage: EvolutionStage, modifier: Modifier = Modifier, eventIconSize: Dp = 12.dp) {
    val event = SeasonEvents.parse(bro.eventFrame)
    Box(
        modifier
            .clip(CutCornerShape(4.dp))
            .background(Brush.radialGradient(listOf(bro.primaryType.color.copy(alpha = 0.35f), DexColors.Screen)))
            .scanlines(),
        contentAlignment = Alignment.Center,
    ) {
        BroSprite(bro, stage.ordinal, Modifier.fillMaxSize(0.86f))
        if (bro.isShiny) Sparkles(Modifier.fillMaxSize(), seed = bro.id.toInt())
        event?.let { EventCorners(it.event, Modifier.fillMaxSize(), iconSize = eventIconSize) }
    }
}

/** Type-colored frame used by both the card and the list row, so the two views read as one dex. */
fun Bro.frameBrush(): Brush {
    val event = SeasonEvents.parse(eventFrame)
    return event?.let { Brush.linearGradient(listOf(it.event.primaryColor, it.event.accentColor, it.event.primaryColor)) }
        ?: Brush.linearGradient(listOf(primaryType.color, (types.getOrNull(1) ?: primaryType).color))
}

/** Pokédex list view row: sprite, number + name, dex entry, types, rarity. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DexListRow(
    bro: Bro,
    stage: EvolutionStage,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = CutCornerShape(8.dp)
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${bro.dexNumber} ${bro.name}, ${bro.rarity.label}" +
                    (if (bro.isShiny) ", shiny" else "") + ", ${stage.title}. ${bro.dexEntry}"
            }
            .rarityGlow(bro.rarity, cornerRadius = 12f)
            .clip(shape)
            .background(DexColors.Surface)
            .border(2.dp, bro.frameBrush(), shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClick?.let { "Enter ${bro.name}'s room" },
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpriteWindow(bro, stage, Modifier.size(64.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bro.dexNumber, style = PixelText.Tiny, color = DexColors.TextMuted)
                Spacer(Modifier.width(6.dp))
                Text(
                    bro.name,
                    style = PixelText.Label,
                    color = DexColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                bro.dexEntry,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = if (bro.flavorText.isBlank()) FontStyle.Italic else FontStyle.Normal,
                color = DexColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bro.types.forEach { TypeBadge(it, compact = true) }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            RarityStars(bro.rarity, size = 10.dp)
            Spacer(Modifier.height(8.dp))
            Text(stage.title.uppercase(), style = PixelText.Tiny, color = DexColors.ScreenText)
        }
    }
}

private val mysteryShade = Color(0xFF24242C)

/** An uncaught dex slot: dark "???" silhouette, so the Brodex is never just empty. */
@Composable
fun MysteryCard(number: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = CutCornerShape(10.dp)
    Column(
        modifier
            .semantics(mergeDescendants = true) { contentDescription = "Empty slot ${"#%03d".format(number)}. Tap to catch a bro." }
            .clip(shape)
            .background(DexColors.Surface.copy(alpha = 0.6f))
            .border(3.dp, DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Text("#%03d".format(number), style = PixelText.Tiny, color = DexColors.Outline)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CutCornerShape(4.dp))
                .background(DexColors.Screen)
                .scanlines(),
            contentAlignment = Alignment.Center,
        ) {
            BroSprite(BroLook.random(number * 7919), 0, false, Modifier.fillMaxSize(0.86f), tint = mysteryShade)
            Text("?", style = PixelText.Title, color = DexColors.Outline)
        }
        Spacer(Modifier.height(8.dp))
        Text("???", style = PixelText.Label, color = DexColors.Outline)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.size(width = 64.dp, height = 14.dp).background(DexColors.SurfaceHigh, CutCornerShape(3.dp)))
        Spacer(Modifier.height(6.dp))
        Text("NOT CAUGHT", style = PixelText.Tiny, color = DexColors.Outline)
    }
}

@Composable
fun MysteryRow(number: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = CutCornerShape(8.dp)
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "Empty slot ${"#%03d".format(number)}. Tap to catch a bro." }
            .background(DexColors.Surface.copy(alpha = 0.6f), shape)
            .border(2.dp, DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).background(DexColors.Screen, CutCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            BroSprite(BroLook.random(number * 7919), 0, false, Modifier.fillMaxSize(0.86f), tint = mysteryShade)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("#%03d  ???".format(number), style = PixelText.Label, color = DexColors.Outline)
            Spacer(Modifier.height(6.dp))
            Text("Not caught yet", style = MaterialTheme.typography.bodySmall, color = DexColors.Outline)
        }
    }
}

/** Compact one-line roster entry for lists (squads, pickers). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BroRow(
    bro: Bro,
    stage: EvolutionStage,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val shape = CutCornerShape(6.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, bro.primaryType.color.copy(alpha = 0.6f), shape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(DexColors.Screen, CutCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BroSprite(bro, stage.ordinal, Modifier.fillMaxSize(0.9f))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                "${bro.dexNumber} ${bro.name}",
                style = PixelText.Tiny,
                color = DexColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bro.types.forEach { TypeBadge(it, compact = true) }
            }
        }
        trailing()
    }
}
