package com.joecode.brokemon.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

/**
 * The device shell every screen sits in: a red Pokédex-style header with a
 * big pulsing lens, indicator LEDs and the screen title.
 */
@Composable
fun DexScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = DexColors.Background,
        topBar = { DexHeader(title, onBack, actions) },
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
private fun DexHeader(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DexColors.DexRed, DexColors.DexRedDark)))
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DexColors.Text)
                }
            } else {
                LensLed(Modifier.padding(start = 6.dp, end = 4.dp))
            }
            LedCluster(Modifier.padding(horizontal = 8.dp))
            Text(
                text = title.uppercase(),
                style = PixelText.Header,
                color = DexColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        // Hinge line under the header, like the seam of a flip device.
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.Black.copy(alpha = 0.45f)),
        )
    }
}

/** The big blue "camera lens" LED, gently pulsing. */
@Composable
fun LensLed(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val transition = rememberInfiniteTransition(label = "lens")
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "lensGlow",
    )
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(Color.White, r)
        drawCircle(Color(0xFF0B0B10), r * 0.86f)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFB8E3FF), DexColors.LedBlue, Color(0xFF0F4C8A)),
                center = center - Offset(r * 0.2f, r * 0.2f),
                radius = r * 0.8f,
            ),
            r * 0.72f,
            alpha = glow,
        )
        drawCircle(Color.White.copy(alpha = 0.8f), r * 0.16f, center - Offset(r * 0.28f, r * 0.28f))
    }
}

/** Three little status LEDs; the green one blinks. */
@Composable
fun LedCluster(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "leds")
    val blink by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "blink",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Led(DexColors.LedRed, 1f)
        Led(DexColors.LedYellow, 1f)
        Led(DexColors.LedGreen, blink)
    }
}

@Composable
fun Led(color: Color, alpha: Float, size: Dp = 9.dp) {
    Box(
        Modifier
            .size(size)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
    )
}

/** A screen-within-the-device panel: dark LCD with a bezel and scanlines. */
@Composable
fun ScreenPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = CutCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 4.dp)
    Column(
        modifier
            .background(DexColors.SurfaceHigh, shape)
            .border(2.dp, DexColors.Outline, shape)
            .padding(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Led(DexColors.LedRed, 0.9f, 6.dp)
            Spacer(Modifier.width(4.dp))
            Led(DexColors.LedRed, 0.9f, 6.dp)
            if (title != null) {
                Spacer(Modifier.width(8.dp))
                Text(title.uppercase(), style = PixelText.Tiny, color = DexColors.TextMuted)
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(3.dp))
                .background(DexColors.Screen)
                .border(1.dp, DexColors.ScreenBorder, CutCornerShape(3.dp))
                .scanlines()
                .padding(contentPadding),
            content = content,
        )
    }
}

/** Chunky retro button that physically "presses" down. [stacked] puts the icon above the label. */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DexColors.DexRed,
    enabled: Boolean = true,
    stacked: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 4.dp
    val offset by animateFloatAsState(if (pressed) 1f else 0f, tween(60), label = "press")
    val shape = CutCornerShape(6.dp)
    val face = if (enabled) color else DexColors.Outline
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = depth)
                .background(Color.Black.copy(alpha = 0.55f), shape),
        )
        val faceModifier = Modifier
            .offset { IntOffset(0, (depth * offset).roundToPx()) }
            .fillMaxWidth()
            .background(face, shape)
            .border(2.dp, Color.White.copy(alpha = 0.18f), shape)
        if (stacked) {
            Column(
                faceModifier.padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.height(6.dp))
                }
                Text(text.uppercase(), style = PixelText.Tiny, color = DexColors.Text, textAlign = TextAlign.Center, maxLines = 1)
            }
        } else {
            Row(
                faceModifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                }
                Text(text.uppercase(), style = PixelText.Label, color = DexColors.Text, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun TypeBadge(type: BroType, modifier: Modifier = Modifier, compact: Boolean = false) {
    val shape = CutCornerShape(4.dp)
    Text(
        text = type.label.uppercase(),
        style = if (compact) PixelText.Tiny else PixelText.Label,
        color = Color(0xFF101014),
        maxLines = 1,
        modifier = modifier
            .background(type.color, shape)
            .border(1.dp, Color.Black.copy(alpha = 0.4f), shape)
            .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = if (compact) 3.dp else 5.dp),
    )
}

@Composable
fun RarityStars(rarity: Rarity, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(rarity.ordinal + 1) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = rarity.color, modifier = Modifier.size(size))
        }
    }
}

/** Segmented stat bar that fills up on first appearance. */
@Composable
fun StatBar(label: String, value: Int, color: Color, modifier: Modifier = Modifier, max: Int = 100) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fill by animateFloatAsState(
        targetValue = if (started) value / max.toFloat() else 0f,
        animationSpec = tween(900),
        label = "statFill",
    )
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = PixelText.Tiny, color = DexColors.ScreenText, modifier = Modifier.width(64.dp))
        Text(
            value.toString().padStart(3, ' '),
            style = PixelText.Tiny,
            color = DexColors.Text,
            modifier = Modifier.width(32.dp),
        )
        SegmentedBar(fill, color, Modifier.weight(1f).height(10.dp))
    }
}

@Composable
fun SegmentedBar(fraction: Float, color: Color, modifier: Modifier = Modifier, segments: Int = 20) {
    Box(
        modifier.drawBehind {
            val gap = 2.dp.toPx()
            val w = (size.width - gap * (segments - 1)) / segments
            val filled = fraction * segments
            for (i in 0 until segments) {
                val x = i * (w + gap)
                val a = when {
                    i + 1 <= filled -> 1f
                    i < filled -> filled - i
                    else -> 0f
                }
                drawRect(Color.White.copy(alpha = 0.06f), Offset(x, 0f), Size(w, size.height))
                if (a > 0f) drawRect(color.copy(alpha = a), Offset(x, 0f), Size(w, size.height))
            }
        },
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = DexColors.TextMuted) {
    Text(text.uppercase(), style = PixelText.Label, color = color, modifier = modifier.padding(vertical = 6.dp))
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CatchCube(Modifier.size(96.dp))
        Spacer(Modifier.height(20.dp))
        Text(title.uppercase(), style = PixelText.Header, color = DexColors.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, color = DexColors.TextMuted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

/**
 * The "Bro Cube": an original catch device — a chunky pixel cube with a red
 * lid, a seam and a glowing core. Drawn on a 12x12 pixel grid.
 */
@Composable
fun CatchCube(modifier: Modifier = Modifier, coreGlow: Float = 1f, lidOpen: Float = 0f) {
    Canvas(modifier) {
        val px = size.minDimension / 12f
        fun cell(x: Int, y: Int, c: Color, dy: Float = 0f) =
            drawRect(c, Offset(x * px, y * px + dy), Size(px + 0.5f, px + 0.5f))

        val outline = Color(0xFF0B0B10)
        val lift = -lidOpen * px * 3
        // Lid (rows 1..5)
        for (y in 1..5) for (x in 1..10) {
            val edge = y == 1 || x == 1 || x == 10
            cell(x, y, if (edge) outline else if (y == 2 && x in 3..5) DexColors.DexRedLight else DexColors.DexRed, lift)
        }
        // Base (rows 6..10)
        for (y in 6..10) for (x in 1..10) {
            val edge = y == 10 || x == 1 || x == 10
            cell(x, y, if (edge) outline else if (y == 9) Color(0xFF1C1C24) else Color(0xFF2E2E3A))
        }
        // Seam
        for (x in 1..10) cell(x, 6, outline)
        // Core button
        for (y in 5..7) for (x in 5..6) cell(x, y, outline)
        cell(5, 6, DexColors.LedBlue.copy(alpha = coreGlow))
        cell(6, 6, Color(0xFFB8E3FF).copy(alpha = coreGlow))
    }
}
