package com.joecode.brokemon.ui.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joecode.brokemon.ui.components.PixelIcon
import com.joecode.brokemon.ui.components.PixelIconImage
import com.joecode.brokemon.ui.components.pixelBox
import com.joecode.brokemon.ui.theme.Borders
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Shows the controller's current toast near the bottom of the screen, above the tab bar. */
@Composable
fun PixelToastHost(controller: FeedbackController, modifier: Modifier = Modifier) {
    val toast by controller.toast.collectAsStateWithLifecycle()
    LaunchedEffect(toast?.id) {
        val t = toast ?: return@LaunchedEffect
        delay(if (t.kind == ToastKind.REWARD) 3200 else 2200)
        controller.dismissToast(t.id)
    }
    Box(modifier.fillMaxWidth().padding(horizontal = Spacing.lg), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
        ) {
            // Keep showing the last text while it animates out.
            val shown = remember(toast?.id) { toast }
            shown?.let { t ->
                val (icon, color) = when (t.kind) {
                    ToastKind.SUCCESS -> PixelIcon.STAR to DexColors.LedGreen
                    ToastKind.ERROR -> PixelIcon.LOCK to DexColors.LedRed
                    ToastKind.INFO -> PixelIcon.MAIL to DexColors.LedBlue
                    ToastKind.REWARD -> PixelIcon.TROPHY to DexColors.Gold
                }
                Row(
                    Modifier
                        .widthIn(max = 420.dp)
                        .pixelBox(DexColors.Surface, color, Borders.heavy, 2.dp)
                        .padding(horizontal = Spacing.md, vertical = Spacing.md)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PixelIconImage(icon, tint = color, accent = DexColors.Text, size = 20.dp)
                    Spacer(Modifier.width(Spacing.md))
                    Text(t.text, style = PixelText.Tiny.copy(lineHeight = PixelText.Header.lineHeight), color = DexColors.Text)
                }
            }
        }
    }
}
