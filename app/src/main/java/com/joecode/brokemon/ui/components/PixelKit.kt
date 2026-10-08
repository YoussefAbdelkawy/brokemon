package com.joecode.brokemon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.joecode.brokemon.ui.theme.Borders
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.MinTouch
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing

// The pixel UI kit: PixelPanel, PixelDialog, PixelChip, PixelProgressBar (and PixelButton in DexChrome).
// All of them use a chunky stepped-corner border instead of Material's rounded look.

/** A rectangle with stepped ("pixel") corners instead of smooth or chamfered ones. */
class PixelShape(private val step: Dp = 2.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { step.toPx() }.coerceAtMost(minOf(size.width, size.height) / 6f)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 2 * s); lineTo(s, 2 * s); lineTo(s, s); lineTo(2 * s, s); lineTo(2 * s, 0f)
            lineTo(w - 2 * s, 0f); lineTo(w - 2 * s, s); lineTo(w - s, s); lineTo(w - s, 2 * s); lineTo(w, 2 * s)
            lineTo(w, h - 2 * s); lineTo(w - s, h - 2 * s); lineTo(w - s, h - s); lineTo(w - 2 * s, h - s); lineTo(w - 2 * s, h)
            lineTo(2 * s, h); lineTo(2 * s, h - s); lineTo(s, h - s); lineTo(s, h - 2 * s); lineTo(0f, h - 2 * s)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Fill + chunky stepped border, clipped to the pixel shape. */
fun Modifier.pixelBox(
    fill: Color,
    border: Color,
    width: Dp = Borders.chunky,
    step: Dp = 2.dp,
): Modifier {
    val shape = PixelShape(step)
    return this
        .clip(shape)
        .background(fill)
        .drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this) as Outline.Generic
            clipPath(outline.path) {
                drawPath(outline.path, border, style = Stroke(width = width.toPx() * 2f, join = StrokeJoin.Miter))
            }
        }
}

/** A general pixel-bordered panel (lists, settings groups, dialogs). */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    border: Color = DexColors.Outline,
    fill: Color = DexColors.SurfaceHigh,
    contentPadding: Dp = Spacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.pixelBox(fill, border).padding(contentPadding)) {
        if (title != null) {
            Text(title.uppercase(), style = PixelText.Tiny, color = DexColors.TextMuted)
            Spacer(Modifier.size(Spacing.sm))
        }
        content()
    }
}

/**
 * A pixel chip: filters, tags, quick picks. Looks compact but always has a 48dp tall touch target.
 * Pair it with a label (and optionally an icon) so it never relies on color alone.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PixelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DexColors.LedYellow,
    icon: PixelIcon? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier
            .heightIn(min = MinTouch)
            .semantics { this.selected = selected }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .pixelBox(if (selected) color else DexColors.Surface, if (selected) color else DexColors.Outline, Borders.normal, 1.dp)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                PixelIconImage(icon, tint = if (selected) DexColors.OnBright else color, size = 14.dp)
                Spacer(Modifier.width(Spacing.xs + 2.dp))
            }
            Text(
                label.uppercase(),
                style = PixelText.Tiny,
                color = if (selected) DexColors.OnBright else DexColors.Text,
                maxLines = 1,
            )
        }
    }
}

/** A pixel progress bar with a bordered track. For thin inline bars use [SegmentedBar]. */
@Composable
fun PixelProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    segments: Int = 20,
    height: Dp = 14.dp,
) {
    Box(
        modifier
            .pixelBox(DexColors.Screen, DexColors.ScreenBorder, Borders.normal, 1.dp)
            .padding(Borders.normal + 1.dp),
    ) {
        SegmentedBar(fraction, color, Modifier.fillMaxWidth().height(height - 6.dp), segments)
    }
}

/** A dialog in the pixel style: bordered panel, pixel-font title, content and a row of buttons. */
@Composable
fun PixelDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier
                .padding(horizontal = Spacing.xl)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .pixelBox(DexColors.SurfaceHigh, DexColors.Outline, Borders.heavy, 3.dp)
                .padding(Spacing.lg),
        ) {
            if (title != null) {
                Text(title.uppercase(), style = PixelText.Label, color = DexColors.Text)
                Spacer(Modifier.size(Spacing.md))
            }
            content()
            Spacer(Modifier.size(Spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                buttons()
            }
        }
    }
}

/** Drop-in replacement for Material's AlertDialog with the same parameter names. */
@Composable
fun PixelAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier
                .padding(horizontal = Spacing.xl)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .pixelBox(DexColors.SurfaceHigh, DexColors.Outline, Borders.heavy, 3.dp)
                .padding(Spacing.lg),
        ) {
            title?.let {
                it()
                Spacer(Modifier.size(Spacing.md))
            }
            text?.let {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides DexColors.Text,
                ) { it() }
                Spacer(Modifier.size(Spacing.md))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
