package com.joecode.brokemon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.domain.Cosmetic
import com.joecode.brokemon.domain.StickerArt
import com.joecode.brokemon.ui.feedback.LocalFeedback
import com.joecode.brokemon.ui.feedback.LocalReduceMotion
import com.joecode.brokemon.ui.feedback.Sfx
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

// Dexy: the little handheld-robot guide. An original character: a boxy teal gadget with a
// screen face, an antenna bulb and stubby legs. Not based on any existing creature.

enum class DexyMood { HAPPY, WOW, SLEEPY, THINK }

private const val DEXY_BODY = """
.......yy.......
.......yy.......
.......##.......
..############..
.#bbbbbbbbbbbb#.
.#blbbbbbbbbbB#.
.#b##########B#.
.#b#ssssssss#B#.
.#b#ssssssss#B#.
.#b#ssssssss#B#.
.#b#ssssssss#B#.
.#b##########B#.
g#bbbbbbbbbbbB#g
g#bbywbbbbrrbB#g
.#bbbbbbbbbbBB#.
..############..
...#gg#..#gg#...
...####..####...
"""

// The 8x4 inside of the screen, per mood. 'e' is the glowing face, 'r' an open mouth.
private val DEXY_FACE = mapOf(
    DexyMood.HAPPY to listOf("ssssssss", "seessees", "sesssses", "sseeeess"),
    DexyMood.WOW to listOf("seessees", "seessees", "ssssssss", "sssrrsss"),
    DexyMood.SLEEPY to listOf("ssssssss", "seessees", "ssssssss", "ssseesss"),
    DexyMood.THINK to listOf("ssssseee", "seessess", "sessssss", "sseessss"),
)

private val dexyColors = mapOf(
    '#' to Color(0xFF0B0B10), 'b' to Color(0xFF2EC4B6), 'B' to Color(0xFF1E8E84), 'l' to Color(0xFF8FFFF2),
    's' to Color(0xFF0F1D33), 'e' to Color(0xFF7CFFCB), 'y' to Color(0xFFFFD54A), 'w' to Color(0xFFF5F5F7),
    'r' to Color(0xFFFF5A6E), 'g' to Color(0xFF9A9AA8),
)

private fun dexyRows(mood: DexyMood): List<String> {
    val rows = DEXY_BODY.trim().lines().toMutableList()
    val face = DEXY_FACE.getValue(mood)
    for (i in face.indices) rows[7 + i] = rows[7 + i].replaceRange(4, 12, face[i])
    // Closed bulb for the sleepy mood.
    if (mood == DexyMood.SLEEPY) { rows[0] = ".......gg......."; rows[1] = ".......gg......." }
    return rows
}

@Composable
fun DexyImage(mood: DexyMood, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val rows = remember(mood) { dexyRows(mood) }
    Canvas(modifier.size(size)) {
        val px = this.size.minDimension / 18f
        val xOffset = (this.size.width - px * 16f) / 2f
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = dexyColors[ch] ?: return@forEachIndexed
                drawRect(color, Offset(xOffset + x * px, y * px), Size(px + 0.5f, px + 0.5f))
            }
        }
    }
}

/** Dexy, bobbing gently. Tap for another tip or a joke. */
@Composable
fun Dexy(
    mood: DexyMood = DexyMood.HAPPY,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onTap: (() -> Unit)? = null,
) {
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val squash = remember { Animatable(1f) }
    val feedback = LocalFeedback.current
    val clock = rememberClock(8)
    Column(
        modifier
            .semantics { contentDescription = "Dexy, your guide" }
            .then(
                if (onTap != null) Modifier.clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                    feedback?.play(Sfx.TAP, 0.4f)
                    if (!reduce) scope.launch {
                        squash.animateTo(0.85f, tween(60))
                        squash.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
                    }
                    onTap()
                } else Modifier,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DexyImage(
            mood, size,
            Modifier.graphicsLayer {
                translationY = if (reduce) 0f else -(((clock.value / 450) % 2).toInt()) * 2.dp.toPx()
                scaleY = squash.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
        )
    }
}

/** Dexy with a speech bubble. Used for hints, empty states and results. */
@Composable
fun DexySays(
    text: String,
    modifier: Modifier = Modifier,
    mood: DexyMood = DexyMood.HAPPY,
    onTap: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Dexy(mood, size = 64.dp, onTap = onTap)
        Spacer(Modifier.width(Spacing.md))
        PixelPanel(Modifier.weight(1f), fill = DexColors.Surface) {
            Text(text, color = DexColors.Text, style = MaterialTheme.typography.bodyMedium)
            trailing?.let { Spacer(Modifier.size(Spacing.sm)); it() }
        }
    }
}

/** Original tips and jokes. Short, kind, no streak pressure. */
object DexyLines {
    val tips = listOf(
        "Hold a card for quick actions: check in, add a memory, share.",
        "Swipe a card left or right on its page to flip to the next bro.",
        "A Quick Catch only needs a name. Fill in the rest whenever.",
        "Add a memory to a bro and they might turn shiny.",
        "Open the Daily Pack when you feel like it. It waits for you.",
        "Tilt your phone to make a Rare card shine.",
        "Stickers go on the back of a card. Tap a slot to place one.",
        "Check in on a bro you haven't talked to in a while.",
        "Your Trainer Room shows off your trophies and your favorite bros.",
        "Shake the phone to meet a wild bro.",
    )
    val jokes = listOf(
        "Why did the bro bring a ladder? To reach the next level.",
        "I tried to catch a bro. They said they were on their way. Still on their way.",
        "What is a ghost bro's favorite app? Read receipts.",
        "My battery is at 1%, but my friendship is at 100%.",
        "Bros are like pixels: small alone, a whole picture together.",
        "Beep boop. That is robot for 'nice catch'.",
        "I would tell you a joke about UDP, but you might not get it.",
    )

    /** One tip per day, picked by date, so it feels fresh without being nagging. */
    fun tipOfTheDay(day: LocalDate = LocalDate.now()): String = tips[(day.toEpochDay() % tips.size).toInt()]

    fun random(avoid: String? = null): String = (tips + jokes).filter { it != avoid }.random()
}

/** Tap-for-a-line Dexy: shows a tip first, then a new tip or joke on each tap. */
@Composable
fun DexyGuide(modifier: Modifier = Modifier, first: String = DexyLines.tipOfTheDay(), mood: DexyMood = DexyMood.HAPPY) {
    var line by remember { androidx.compose.runtime.mutableStateOf(first) }
    var taps by remember { mutableIntStateOf(0) }
    DexySays(
        text = line,
        mood = if (taps > 0 && taps % 3 == 0) DexyMood.WOW else mood,
        onTap = { taps++; line = DexyLines.random(line) },
        modifier = modifier,
    )
}

/** A pixel sticker (8x8 art on a paper-white sticker edge). */
@Composable
fun StickerImage(sticker: Cosmetic, size: Dp = 40.dp, modifier: Modifier = Modifier, locked: Boolean = false) {
    val rows = remember(sticker) { StickerArt.rows(sticker) }
    Canvas(modifier.size(size)) {
        val px = this.size.minDimension / 10f
        fun filled(x: Int, y: Int) = y in 0..7 && x in 0..7 && rows[y][x] != '.'
        // Sticker edge: any cell next to art gets a paper border.
        for (y in -1..8) for (x in -1..8) {
            val near = (-1..1).any { dy -> (-1..1).any { dx -> filled(x + dx, y + dy) } }
            if (near) drawRect(if (locked) Color(0xFF3A3A46) else Color(0xFFF5F2EA), Offset((x + 1) * px, (y + 1) * px), Size(px + 0.5f, px + 0.5f))
        }
        if (!locked) for (y in 0..7) for (x in 0..7) {
            val color = StickerArt.palette[rows[y][x]] ?: continue
            drawRect(Color(color), Offset((x + 1) * px, (y + 1) * px), Size(px + 0.5f, px + 0.5f))
        }
    }
}
