package com.joecode.brokemon.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.ui.components.PixelIcon
import com.joecode.brokemon.ui.components.PixelIconImage
import com.joecode.brokemon.ui.theme.Borders
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.MinTouch
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.hypot

enum class Tab(val route: String, val label: String, val icon: PixelIcon) {
    BRODEX(Routes.HOME, "Brodex", PixelIcon.BRODEX),
    BATTLE(Routes.BATTLE_HUB, "Battle", PixelIcon.BATTLE),
    TRAINER(Routes.TRAINER, "Trainer", PixelIcon.TRAINER),
}

/**
 * Bottom bar: Brodex, Battle, a raised round CATCH button, Trainer.
 * The CATCH orb is an original design: a glowing round button with a plus.
 */
@Composable
fun PixelBottomBar(currentRoute: String?, onTab: (Tab) -> Unit, onCatch: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(DexColors.Surface)
                .drawBehind {
                    // Chunky pixel top edge with a darker lip.
                    drawRect(DexColors.Outline, Offset.Zero, Size(size.width, Borders.chunky.toPx()))
                    drawRect(Color.Black.copy(alpha = 0.35f), Offset(0f, Borders.chunky.toPx()), Size(size.width, 2.dp.toPx()))
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = Borders.chunky),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(Tab.BRODEX, currentRoute, onTab, Modifier.weight(1f))
            TabItem(Tab.BATTLE, currentRoute, onTab, Modifier.weight(1f))
            Spacer(Modifier.width(100.dp)) // room for the raised button
            TabItem(Tab.TRAINER, currentRoute, onTab, Modifier.weight(1f))
        }
        CatchOrb(onCatch, Modifier.align(Alignment.TopCenter).offset(y = (-32).dp))
    }
}

@Composable
private fun TabItem(tab: Tab, currentRoute: String?, onTab: (Tab) -> Unit, modifier: Modifier) {
    val selected = currentRoute == tab.route
    val feedback = com.joecode.brokemon.ui.feedback.LocalFeedback.current
    Column(
        modifier
            .heightIn(min = 64.dp)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClickLabel = "Open ${tab.label}") { feedback?.tap(); onTab(tab) }
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // A little pixel pip over the selected tab, so the state never relies on color alone.
        Box(
            Modifier
                .width(14.dp)
                .height(3.dp)
                .background(if (selected) DexColors.LedYellow else Color.Transparent),
        )
        Spacer(Modifier.height(Spacing.xs))
        PixelIconImage(tab.icon, tint = if (selected) DexColors.LedYellow else DexColors.TextMuted, accent = if (selected) DexColors.Text else DexColors.TextMuted, size = 24.dp)
        Spacer(Modifier.height(Spacing.xs))
        Text(tab.label.uppercase(), style = PixelText.Tiny, color = if (selected) DexColors.LedYellow else DexColors.TextMuted, maxLines = 1)
    }
}

/** The raised round CATCH button. Bounces when tapped. */
@Composable
fun CatchOrb(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val feedback = com.joecode.brokemon.ui.feedback.LocalFeedback.current
    val scale = remember { Animatable(1f) }
    Box(
        modifier
            .size(MinTouch + 28.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .semantics { contentDescription = "Catch a bro" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                feedback?.play(com.joecode.brokemon.ui.feedback.Sfx.TAP, 0.5f)
                scope.launch {
                    scale.animateTo(0.78f, tween(70))
                    launch { onClick() }
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(MinTouch + 28.dp)) {
            val n = 20
            val px = size.minDimension / n
            val c = (n - 1) / 2f
            // Soft shadow under the button.
            for (y in 0 until n) for (x in 0 until n) {
                val d = hypot(x - c, y - c - 0.8f)
                if (d <= n / 2f - 0.2f) drawRect(Color.Black.copy(alpha = 0.4f), Offset(x * px, (y + 1) * px), Size(px + 0.5f, px + 0.5f))
            }
            for (y in 0 until n) for (x in 0 until n) {
                val d = hypot(x - c, y - c)
                if (d > n / 2f - 0.2f) continue
                val color = when {
                    d > n / 2f - 1.6f -> Color(0xFF0B0B10) // outline
                    d > n / 2f - 2.6f -> Color(0xFFFFE08A) // gold rim
                    // Shading: light from the top-left.
                    x + y < n * 0.62f && d < n / 2f - 4f -> DexColors.DexRedLight
                    x + y > n * 1.35f -> DexColors.DexRedDark
                    else -> DexColors.DexRed
                }
                drawRect(color, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
            }
            // A plus sign in cream.
            val arm = 5
            for (i in -arm..arm) for (t in 0..1) {
                val cx = (c - 0.5f + t + 0f)
                drawRect(DexColors.Paper, Offset((c - 0.5f + i + 0.5f) * px, (cx + 0.5f) * px), Size(px + 0.5f, px + 0.5f))
                drawRect(DexColors.Paper, Offset((cx + 0.5f) * px, (c - 0.5f + i + 0.5f) * px), Size(px + 0.5f, px + 0.5f))
            }
            // A small shine.
            drawRect(Color.White.copy(alpha = 0.85f), Offset(5 * px, 4 * px), Size(px * 2, px))
            drawRect(Color.White.copy(alpha = 0.85f), Offset(4 * px, 5 * px), Size(px, px * 2))
        }
    }
}
