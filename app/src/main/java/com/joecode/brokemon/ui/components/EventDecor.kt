package com.joecode.brokemon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.domain.EventIcon
import com.joecode.brokemon.domain.EventStamp
import com.joecode.brokemon.domain.PixelIcons
import com.joecode.brokemon.domain.SeasonEvent
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

val SeasonEvent.primaryColor: Color get() = Color(primary)
val SeasonEvent.accentColor: Color get() = Color(accent)

fun DrawScope.drawPixelIcon(icon: EventIcon, topLeft: Offset, px: Float, primary: Color, accent: Color) {
    PixelIcons.rows(icon).forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
            val color = when (ch) {
                'P' -> primary
                'A' -> accent
                'W' -> Color.White
                'D' -> Color(0xFF0B0B10)
                else -> null
            }
            if (color != null) drawRect(color, Offset(topLeft.x + x * px, topLeft.y + y * px), Size(px + 0.5f, px + 0.5f))
        }
    }
}

@Composable
fun EventIconView(event: SeasonEvent, size: Dp, modifier: Modifier = Modifier) {
    val rows = PixelIcons.rows(event.icon)
    Canvas(modifier.size(size)) {
        val px = this.size.minDimension / maxOf(rows.size, rows[0].length)
        drawPixelIcon(event.icon, Offset.Zero, px, event.primaryColor, event.accentColor)
    }
}

/** "★ RAMADAN 2027" limited-edition ribbon shown on event cards. */
@Composable
fun EventRibbon(stamp: EventStamp, modifier: Modifier = Modifier, compact: Boolean = false) {
    val shape = CutCornerShape(3.dp)
    Row(
        modifier
            .background(stamp.event.primaryColor.copy(alpha = 0.25f), shape)
            .border(1.dp, stamp.event.accentColor, shape)
            .padding(horizontal = 6.dp, vertical = if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EventIconView(stamp.event, if (compact) 10.dp else 14.dp)
        Spacer(Modifier.width(5.dp))
        Text(
            (if (compact) stamp.label else "LIMITED · ${stamp.label}"),
            style = PixelText.Tiny,
            color = stamp.event.accentColor,
            maxLines = 1,
        )
    }
}

/** Corner icons drawn over the sprite window of an event card. */
@Composable
fun EventCorners(event: SeasonEvent, modifier: Modifier = Modifier, iconSize: Dp = 22.dp) {
    Canvas(modifier) {
        val rows = PixelIcons.rows(event.icon)
        val px = iconSize.toPx() / maxOf(rows.size, rows[0].length)
        val w = rows[0].length * px
        val pad = 4.dp.toPx()
        drawPixelIcon(event.icon, Offset(pad, pad), px, event.primaryColor, event.accentColor)
        drawPixelIcon(event.icon, Offset(size.width - w - pad, pad), px, event.primaryColor, event.accentColor)
    }
}

/** Home-screen banner while an event is running. */
@Composable
fun EventBanner(event: SeasonEvent, modifier: Modifier = Modifier) {
    ScreenPanel(title = "Limited event", modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EventIconView(event, 40.dp)
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text("${event.label.uppercase()} EVENT", style = PixelText.Label, color = event.accentColor)
                Spacer(Modifier.size(6.dp))
                Text(event.tagline, color = DexColors.ScreenText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }
}
