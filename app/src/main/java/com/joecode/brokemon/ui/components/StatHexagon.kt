package com.joecode.brokemon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar chart of the six base stats, drawn on a Canvas. The stat shape grows
 * out from the middle the first time it appears.
 */
@Composable
fun StatHexagon(stats: BroStats, color: Color, modifier: Modifier = Modifier) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(stats) { grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val measurer = rememberTextMeasurer()
    val entries = stats.asList()
    val labelStyle = PixelText.Tiny.copy(color = DexColors.TextMuted)
    val valueStyle = PixelText.Tiny.copy(color = DexColors.ScreenText)

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .semantics {
                contentDescription = "Stat hexagon: " + entries.joinToString { (info, v) -> "${info.label} $v" }
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.33f
        val count = entries.size
        fun vertex(i: Int, fraction: Float): Offset {
            // First stat points straight up, then clockwise.
            val angle = -PI / 2 + 2 * PI * i / count
            return Offset(
                center.x + (cos(angle) * radius * fraction).toFloat(),
                center.y + (sin(angle) * radius * fraction).toFloat(),
            )
        }

        // Grid: four rings plus spokes.
        for (ring in 1..4) {
            drawPolygon((0 until count).map { vertex(it, ring / 4f) }, DexColors.ScreenBorder, Stroke(1.dp.toPx()))
        }
        for (i in 0 until count) drawLine(DexColors.ScreenBorder, center, vertex(i, 1f), 1.dp.toPx())

        // The stats themselves.
        val points = entries.mapIndexed { i, (_, v) -> vertex(i, (v / BroStats.MAX.toFloat()).coerceIn(0.04f, 1f) * grow.value) }
        val path = polygonPath(points)
        drawPath(path, color.copy(alpha = 0.38f))
        drawPath(path, color, style = Stroke(2.dp.toPx()))
        val dot = 5.dp.toPx()
        points.forEach { p ->
            drawRect(DexColors.Text, Offset(p.x - dot / 2, p.y - dot / 2), Size(dot, dot))
            drawRect(color, Offset(p.x - dot / 2 + 1.dp.toPx(), p.y - dot / 2 + 1.dp.toPx()), Size(dot - 2.dp.toPx(), dot - 2.dp.toPx()))
        }

        // Labels just outside each corner.
        entries.forEachIndexed { i, (info, value) ->
            val anchor = vertex(i, 1.28f)
            val label = measurer.measure(info.label, labelStyle)
            val number = measurer.measure("$value", valueStyle)
            val blockHeight = label.size.height + number.size.height
            val top = anchor.y - blockHeight / 2f
            drawText(label, topLeft = Offset(anchor.x - label.size.width / 2f, top))
            drawText(number, topLeft = Offset(anchor.x - number.size.width / 2f, top + label.size.height))
        }
    }
}

private fun polygonPath(points: List<Offset>) = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    close()
}

private fun DrawScope.drawPolygon(points: List<Offset>, color: Color, stroke: Stroke) =
    drawPath(polygonPath(points), color, style = stroke)
