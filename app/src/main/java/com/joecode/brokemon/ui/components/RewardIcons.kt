package com.joecode.brokemon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.LookOptions
import com.joecode.brokemon.data.model.TrainerFrame
import com.joecode.brokemon.domain.AvatarLocks
import com.joecode.brokemon.domain.Reward
import com.joecode.brokemon.domain.RewardKind
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

/**
 * Pixel art for every Journal reward. Badges are drawn from tiny character
 * grids ('o' outline, 'a' main, 'b' shade, 'c' highlight), frames as a mini
 * card in the frame's colors, and the sprite part as a bro wearing it.
 * Locked rewards show as a dark silhouette.
 */
@Composable
fun RewardIcon(reward: Reward, unlocked: Boolean, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (reward.kind) {
            RewardKind.BADGE -> PixelGrid(badgeGrid(reward), badgePalette(reward), unlocked, Modifier.fillMaxSize())
            RewardKind.FRAME -> FrameIcon(reward.frame ?: TrainerFrame.BASIC, unlocked, Modifier.fillMaxSize(0.8f))
            RewardKind.PART -> BroSprite(
                look = AvatarLocks.showcase(reward),
                stage = 0,
                shiny = false,
                modifier = Modifier.fillMaxSize(),
                tint = if (unlocked) null else DexColors.Outline,
            )
        }
        if (!unlocked) Text("?", style = PixelText.Label, color = DexColors.TextMuted)
    }
}

@Composable
fun FrameIcon(frame: TrainerFrame, unlocked: Boolean, modifier: Modifier = Modifier) {
    val shape = DexShape(3.dp)
    val colors = frame.colors.map { Color(it) }
    Box(
        modifier
            .padding(horizontal = 4.dp)
            .background(DexColors.Surface, shape)
            .border(
                4.dp,
                if (unlocked) Brush.linearGradient(colors) else Brush.linearGradient(listOf(DexColors.Outline, DexColors.Outline)),
                shape,
            ),
    )
}

private fun badgeGrid(reward: Reward): List<String> = when (reward) {
    // Scholar: a graduation cap with a tassel.
    Reward.SCHOLAR_BADGE -> listOf(
        "....oo....",
        "..ooccoo..",
        "ooaacccaoo",
        "oaaaaaaaao",
        ".ooaaaaoo.",
        "..obbbbooc",
        "..oaaaao.c",
        "..oaaaao.c",
        "...oooo.cc",
        "..........",
    )
    Reward.MASTER_BADGE -> listOf(
        "o...oo...o",
        "oo.oaao.oo",
        "oaoaaaaoao",
        "oaaacaaaao",
        "oaacccaaao",
        "oaaacaaaao",
        "oaaaaaaaao",
        "obbbbbbbbo",
        "oooooooooo",
        "..........",
    )
    // Rookie: a shield with a star.
    else -> listOf(
        "..oooooo..",
        ".oaaaaaao.",
        "oaaaacaaao",
        "oaaacccaao",
        "oaccccccco",
        "oaaacccabo",
        ".oaacacabo",
        "..oaaaabo.",
        "...oabbo..",
        "....oo....",
    )
}

private data class BadgePalette(val main: Color, val shade: Color, val light: Color)

private fun badgePalette(reward: Reward): BadgePalette = when (reward) {
    Reward.SCHOLAR_BADGE -> BadgePalette(Color(0xFF3FA7FF), Color(0xFF1F5FA8), Color(0xFFE8F3FF))
    Reward.MASTER_BADGE -> BadgePalette(Color(0xFFFFD23F), Color(0xFFC99A1E), Color(0xFFFFF6C8))
    else -> BadgePalette(Color(0xFFD7263D), Color(0xFF8C1427), Color(0xFFFFD23F))
}

@Composable
private fun PixelGrid(grid: List<String>, palette: BadgePalette, unlocked: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val cols = grid.maxOf { it.length }
        val cell = minOf(size.width / cols, size.height / grid.size)
        val left = (size.width - cell * cols) / 2f
        val top = (size.height - cell * grid.size) / 2f
        grid.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = when (ch) {
                    'o' -> if (unlocked) Color(0xFF0B0B10) else DexColors.Outline
                    'a' -> if (unlocked) palette.main else DexColors.SurfaceHigh
                    'b' -> if (unlocked) palette.shade else DexColors.Surface
                    'c' -> if (unlocked) palette.light else DexColors.SurfaceHigh
                    else -> null
                }
                if (color != null) {
                    // +0.5 overlap hides hairline gaps between cells.
                    drawRect(color, Offset(left + x * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
