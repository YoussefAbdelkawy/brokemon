package com.joecode.brokemon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.HintStore
import com.joecode.brokemon.domain.Reward
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Where one-time hints are remembered. Null (previews, tests) = never show hints. */
val LocalHintStore = staticCompositionLocalOf<HintStore?> { null }

/** Journal rewards the user owns, e.g. to unlock the Trainer cap in the character builder. */
val LocalUnlockedRewards = compositionLocalOf<Set<Reward>> { emptySet() }

/** Hint ids. Each coach mark shows once per install. */
object Hints {
    const val DETAIL_CHECK_IN = "detail_check_in"
    const val HOME_VIEWS = "home_views"
    const val HOME_WILD = "home_wild"
    const val HOME_JOURNAL = "home_journal"
    const val DETAIL_TILT = "detail_tilt"
}

/** The seen-hint set, or null while it's still loading (so nothing flashes on screen). */
@Composable
fun rememberSeenHints(): Set<String>? {
    val store = LocalHintStore.current ?: return null
    val flow: Flow<Set<String>?> = remember(store) { store.seenHints.map { it } }
    val seen by flow.collectAsState(initial = null)
    return seen
}

/** Marks a hint as seen, e.g. when the user does the thing the hint was pointing at. */
@Composable
fun rememberHintDismisser(): (String) -> Unit {
    val store = LocalHintStore.current
    val scope = rememberCoroutineScope()
    return remember(store, scope) {
        val dismiss: (String) -> Unit = { id -> if (store != null) scope.launch { store.markHintSeen(id) } }
        dismiss
    }
}

enum class ArrowSide { TOP, BOTTOM }

/**
 * A pixel speech bubble that points at a feature the first time the user
 * sees it. Tap it to dismiss; it never comes back.
 */
@Composable
fun CoachMark(
    id: String,
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    arrow: ArrowSide = ArrowSide.BOTTOM,
    /** 0 = arrow at the left edge, 1 = right edge. */
    arrowBias: Float = 0.5f,
) {
    val dismiss = rememberHintDismisser()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(250)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically(),
    ) {
        HintBubble(text, arrow, arrowBias, onDismiss = { dismiss(id) })
    }
}

@Composable
fun HintBubble(text: String, arrow: ArrowSide, arrowBias: Float, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val bob by rememberInfiniteTransition(label = "hint").animateFloat(
        initialValue = 0f,
        targetValue = if (arrow == ArrowSide.BOTTOM) 4f else -4f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "bob",
    )
    val shape = DexShape(6.dp)
    val paper = Color(0xFFF7F4E8)
    val ink = Color(0xFF16161C)
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = bob * density }
            .semantics { liveRegion = LiveRegionMode.Polite }
            .clickable(role = Role.Button, onClickLabel = "Dismiss tip", onClick = onDismiss),
    ) {
        if (arrow == ArrowSide.TOP) PixelArrow(arrowBias, pointingUp = true, fill = paper, edge = DexColors.LedYellow)
        Row(
            Modifier
                .fillMaxWidth()
                .background(paper, shape)
                .border(3.dp, DexColors.LedYellow, shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text, color = ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text("GOT IT", style = PixelText.Tiny, color = DexColors.DexRed, modifier = Modifier.padding(top = 4.dp))
        }
        if (arrow == ArrowSide.BOTTOM) PixelArrow(arrowBias, pointingUp = false, fill = paper, edge = DexColors.LedYellow)
    }
}

/** A stair-stepped triangle, so the pointer looks like pixel art instead of a smooth vector. */
@Composable
private fun PixelArrow(bias: Float, pointingUp: Boolean, fill: Color, edge: Color) {
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val px = 3.dp.toPx()
        val rows = 4
        val center = (size.width * bias.coerceIn(0.06f, 0.94f))
        for (r in 0 until rows) {
            // Row 0 touches the bubble and is widest.
            val half = (rows - r) * px
            val y = if (pointingUp) size.height - (r + 1) * px else r * px
            drawRect(edge, Offset(center - half - px, y), Size(half * 2 + px * 2, px))
            if (r < rows - 1) drawRect(fill, Offset(center - half + px, y), Size(half * 2 - px * 2, px))
        }
    }
}
