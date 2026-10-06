package com.joecode.brokemon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.LookOptions
import com.joecode.brokemon.data.model.LookPart
import com.joecode.brokemon.domain.AvatarLocks
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

/**
 * Character creator: pick a part (hair, glasses, hat...), then pick an option.
 * Every option tile is a live preview of your bro wearing it.
 */
@Composable
fun AvatarBuilder(look: BroLook, onLookChange: (BroLook) -> Unit, modifier: Modifier = Modifier) {
    var part by rememberSaveable { mutableStateOf(LookPart.HAIR) }
    val optionsState = rememberLazyListState()
    LaunchedEffect(part) { optionsState.scrollToItem((look[part] - 1).coerceAtLeast(0)) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(LookPart.entries) { p ->
                val selected = p == part
                val shape = DexShape(4.dp)
                Text(
                    p.label.uppercase(),
                    style = PixelText.Tiny,
                    color = if (selected) Color(0xFF101014) else DexColors.Text,
                    modifier = Modifier
                        .semantics { this.selected = selected }
                        .background(if (selected) DexColors.LedYellow else DexColors.SurfaceHigh, shape)
                        .border(1.dp, if (selected) DexColors.LedYellow else DexColors.Outline, shape)
                        .clickable { part = p }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        val unlocked = LocalUnlockedRewards.current
        LazyRow(state = optionsState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until part.count).toList()) { i ->
                // Journal rewards stay visible but locked until they're earned.
                val locked = AvatarLocks.requiredReward(part, i)?.let { it !in unlocked } ?: false
                OptionTile(look, part, i, selected = look[part] == i, locked = locked) {
                    if (!locked) onLookChange(look.with(part, i))
                }
            }
        }
    }
}

@Composable
private fun OptionTile(look: BroLook, part: LookPart, index: Int, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val shape = DexShape(4.dp)
    val label = if (locked) "Locked" else part.optionLabel(index)
    Column(
        Modifier
            .width(72.dp)
            .semantics {
                contentDescription = "${part.label} ${label ?: "color ${index + 1}"}" + if (locked) ", locked: see Rewards on your Trainer Card" else ""
                this.selected = selected
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(DexColors.Screen, shape)
                .border(2.dp, if (selected) DexColors.LedYellow else DexColors.ScreenBorder, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (part.isColor) {
                val color = when (part) {
                    LookPart.SKIN -> LookOptions.skinTones[index]
                    LookPart.HAIR_COLOR -> LookOptions.hairColors[index]
                    else -> LookOptions.outfitColors[index]
                }
                // Swatch in the corner + the bro wearing it.
                BroSprite(look.with(part, index), 0, false, Modifier.fillMaxSize().padding(4.dp))
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(14.dp)
                        .background(Color(color), DexShape(2.dp))
                        .border(1.dp, Color.Black.copy(alpha = 0.6f), DexShape(2.dp)),
                )
            } else if (locked) {
                BroSprite(look.with(part, index), 0, false, Modifier.fillMaxSize().padding(4.dp), tint = DexColors.Outline)
                Icon(Icons.Filled.Lock, null, tint = DexColors.LedYellow, modifier = Modifier.size(18.dp))
            } else {
                BroSprite(look.with(part, index), 0, false, Modifier.fillMaxSize().padding(4.dp))
            }
        }
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                label.uppercase(),
                style = PixelText.Tiny,
                color = if (selected) DexColors.LedYellow else DexColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
