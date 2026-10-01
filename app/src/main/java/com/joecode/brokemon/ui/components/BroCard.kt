package com.joecode.brokemon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

/** Collectible trading card used in the dex grid. */
@Composable
fun BroCard(
    bro: Bro,
    stage: EvolutionStage,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = CutCornerShape(10.dp)
    val type1 = bro.primaryType
    val type2 = bro.types.getOrNull(1)
    val borderBrush = Brush.linearGradient(listOf(type1.color, (type2 ?: type1).color))
    Column(
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "${bro.dexNumber} ${bro.name}, ${bro.rarity.label}" +
                    (if (bro.isShiny) ", shiny" else "") + ", ${stage.title}"
            }
            .rarityGlow(bro.rarity)
            .clip(shape)
            .background(DexColors.Surface)
            .border(3.dp, borderBrush, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CutCornerShape(4.dp))
                .background(
                    Brush.radialGradient(
                        listOf(type1.color.copy(alpha = 0.35f), DexColors.Screen),
                    ),
                )
                .scanlines(),
            contentAlignment = Alignment.Center,
        ) {
            BroSprite(
                seed = bro.avatarSeed,
                stage = stage.ordinal,
                type1 = type1,
                type2 = type2,
                shiny = bro.isShiny,
                modifier = Modifier.fillMaxSize(0.8f),
            )
            if (bro.isShiny) Sparkles(Modifier.fillMaxSize(), seed = bro.id.toInt())
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            bro.types.forEach { TypeBadge(it, compact = true) }
        }
        Spacer(Modifier.height(6.dp))
        Text(stage.title.uppercase(), style = PixelText.Tiny, color = DexColors.ScreenText)
    }
}

/** Compact one-line roster entry for lists (squads, pickers). */
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
            BroSprite(
                bro.avatarSeed,
                stage.ordinal,
                bro.primaryType,
                bro.types.getOrNull(1),
                bro.isShiny,
                Modifier.fillMaxSize(0.85f),
            )
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                bro.types.forEach { TypeBadge(it, compact = true) }
            }
        }
        trailing()
    }
}
