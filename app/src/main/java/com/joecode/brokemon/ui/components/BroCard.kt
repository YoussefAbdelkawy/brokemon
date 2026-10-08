package com.joecode.brokemon.ui.components

import com.joecode.brokemon.ui.theme.Spacing
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
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.joecode.brokemon.data.model.Rarity
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
    /** Phone tilt from the gyro; only the big card on a bro's page passes one. */
    deviceTilt: DeviceTilt? = null,
    onLongClick: (() -> Unit)? = null,
    /** Show the handheld-style dex entry under the types (big cards: detail, binder). */
    showDexEntry: Boolean = false,
    /** The number to print: National by default, or the bro's number in a regional dex. */
    number: String = bro.dexNumber,
    onClick: (() -> Unit)? = null,
) {
    val shape = DexShape(10.dp)
    val event = SeasonEvents.parse(bro.eventFrame)
    // Event cards wear the event's colors as a limited-edition frame.
    val borderBrush = bro.frameBrush()
    val holo = HoloStyle.of(bro.rarity, bro.isShiny)
    val light = if (tilt != null || deviceTilt != null) HoloLight(tilt, deviceTilt) else null
    val borderWidth = if (event != null) 4.dp else 3.dp
    val frameClock = if (bro.rarity == Rarity.EPIC) rememberClock(15) else null
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
            .border(borderWidth, borderBrush, shape)
            .holoFoilBorder(light, holo, shape, borderWidth)
            .rarityFrame(bro.rarity, frameClock)
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
            Text(number, style = PixelText.Tiny, color = DexColors.TextMuted)
            Spacer(Modifier.weight(1f))
            if (bro.isTraded) {
                Icon(Icons.Filled.SwapHoriz, "Traded", tint = DexColors.LedBlue, modifier = Modifier.size(12.dp))
            }
            if (bro.resolvedBattle.championships > 0) {
                Text("🏆", style = PixelText.Tiny, modifier = Modifier.semantics { contentDescription = "Tournament champion" })
            }
            if (!bro.isTradeable) {
                Icon(Icons.Filled.Lock, "Locked", tint = DexColors.TextMuted, modifier = Modifier.size(12.dp))
            }
            RarityStars(bro.rarity, size = 10.dp)
        }
        Spacer(Modifier.height(6.dp))
        SpriteWindow(bro, stage, Modifier.fillMaxWidth().aspectRatio(1f), eventIconSize = 16.dp, light = light)
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            bro.types.forEach { TypeBadge(it, compact = true) }
        }
        Spacer(Modifier.height(6.dp))
        Text(stage.title.uppercase(), style = PixelText.Tiny, color = DexColors.ScreenText)
        if (showDexEntry) {
            Spacer(Modifier.height(8.dp))
            DexEntryText(bro, Modifier.fillMaxWidth())
            bro.habitat?.let {
                Spacer(Modifier.height(6.dp))
                Text("HABITAT: ${it.uppercase()}", style = PixelText.Tiny, color = DexColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
            .background(DexColors.Screen, DexShape(3.dp))
            .border(1.dp, DexColors.ScreenBorder, DexShape(3.dp))
            .padding(horizontal = Spacing.sm, vertical = 6.dp),
    )
}

/**
 * The portrait window shared by every view (card grid, dex list, binder):
 * type-colored glow, scanlines, the sprite, shiny sparkles and event corners.
 */
@Composable
fun SpriteWindow(
    bro: Bro,
    stage: EvolutionStage,
    modifier: Modifier = Modifier,
    eventIconSize: Dp = 12.dp,
    light: HoloLight? = null,
    animated: Boolean = true,
) {
    val event = SeasonEvents.parse(bro.eventFrame)
    val clock = rememberClock(12)
    Box(
        modifier
            .clip(DexShape(4.dp))
            .background(Brush.radialGradient(listOf(bro.primaryType.color.copy(alpha = 0.35f), DexColors.Screen)))
            .scanlines(),
        contentAlignment = Alignment.Center,
    ) {
        // Parallax: the three layers slide a little against each other as the card tilts.
        ParticleLayer(
            bro.primaryType, bro.id, clock,
            Modifier.fillMaxSize().graphicsLayer { translationX = -(light?.x ?: 0f) * 5.dp.toPx(); translationY = -(light?.y ?: 0f) * 4.dp.toPx() },
        )
        BroSprite(
            bro, stage.ordinal,
            Modifier.fillMaxSize(0.86f).graphicsLayer { translationX = (light?.x ?: 0f) * 3.dp.toPx(); translationY = (light?.y ?: 0f) * 2.dp.toPx() },
            animated = animated,
        )
        if (bro.isShiny) {
            Sparkles(Modifier.fillMaxSize().graphicsLayer { translationX = (light?.x ?: 0f) * 8.dp.toPx() }, seed = bro.id.toInt())
        }
        event?.let { EventCorners(it.event, Modifier.fillMaxSize(), iconSize = eventIconSize) }
        // Foil over the art only (it's clipped to the window), never over the name or types.
        Box(Modifier.matchParentSize().holoFoil(light, HoloStyle.of(bro.rarity, bro.isShiny)))
    }
}

/** Type-colored frame used by both the card and the list row, so the two views read as one dex. */
fun Bro.frameBrush(): Brush {
    // Tournament champions wear a gold champion frame.
    if (resolvedBattle.championships > 0) {
        return Brush.linearGradient(listOf(DexColors.Gold, androidx.compose.ui.graphics.Color(0xFFFFF3B0), DexColors.Legendary, DexColors.Gold))
    }
    val event = SeasonEvents.parse(eventFrame)
    return event?.let { Brush.linearGradient(listOf(it.event.primaryColor, it.event.accentColor, it.event.primaryColor)) }
        ?: Brush.linearGradient(listOf(primaryType.color, (types.getOrNull(1) ?: primaryType).color))
}

/** Dex list view row: sprite, number + name, dex entry, types, rarity. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DexListRow(
    bro: Bro,
    stage: EvolutionStage,
    modifier: Modifier = Modifier,
    number: String = bro.dexNumber,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = DexShape(8.dp)
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
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpriteWindow(bro, stage, Modifier.size(64.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(number, style = PixelText.Tiny, color = DexColors.TextMuted)
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
    val shape = DexShape(10.dp)
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
                .clip(DexShape(4.dp))
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
        Box(Modifier.size(width = 64.dp, height = 14.dp).background(DexColors.SurfaceHigh, DexShape(3.dp)))
        Spacer(Modifier.height(6.dp))
        Text("NOT CAUGHT", style = PixelText.Tiny, color = DexColors.Outline)
    }
}

@Composable
fun MysteryRow(number: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = DexShape(8.dp)
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "Empty slot ${"#%03d".format(number)}. Tap to catch a bro." }
            .background(DexColors.Surface.copy(alpha = 0.6f), shape)
            .border(2.dp, DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).background(DexColors.Screen, DexShape(4.dp)), contentAlignment = Alignment.Center) {
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
    val shape = DexShape(6.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, bro.primaryType.color.copy(alpha = 0.6f), shape)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(DexColors.Screen, DexShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BroSprite(bro, stage.ordinal, Modifier.fillMaxSize(0.9f), animated = true)
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                bro.types.forEach { TypeBadge(it, compact = true) }
            }
        }
        trailing()
    }
}
