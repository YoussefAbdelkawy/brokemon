package com.joecode.brokemon.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.core.graphics.scale
import com.joecode.brokemon.BrokemonApplication
import com.joecode.brokemon.MainActivity
import com.joecode.brokemon.data.broOfTheDay
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.HumanSprite
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Home-screen widget: today's bro, a nudge, and a one-tap check-in. */
class BroOfTheDayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as BrokemonApplication).container
        val bro = container.repository.broOfTheDay(container.prefs)
        val sprite = bro?.let { spriteBitmap(it) }
        val open = openIntent(context, bro?.id)
        provideContent { Content(bro, sprite, open) }
    }

    @Composable
    private fun Content(bro: Bro?, sprite: Bitmap?, open: Intent) {
        Column(
            GlanceModifier
                .fillMaxSize()
                .background(DexColors.Background)
                .cornerRadius(18.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(open)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Box(GlanceModifier.size(10.dp).background(DexColors.LedBlue).cornerRadius(5.dp)) {}
                Spacer(GlanceModifier.width(6.dp))
                Text("BRO OF THE DAY", style = TextStyle(color = provider(DexColors.DexRedLight), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.defaultWeight())
                if (bro != null) Text(bro.dexNumber, style = TextStyle(color = provider(DexColors.TextMuted), fontSize = 11.sp))
            }
            Spacer(GlanceModifier.height(8.dp))
            if (bro == null || sprite == null) {
                Text(
                    "Catch your first bro and they'll show up here.",
                    style = TextStyle(color = provider(DexColors.Text), fontSize = 13.sp),
                )
                return@Column
            }
            val checkedToday = bro.lastCheckIn?.let { isToday(it) } == true
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                Box(
                    GlanceModifier.size(76.dp).background(DexColors.Screen).cornerRadius(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(ImageProvider(sprite), contentDescription = bro.name, modifier = GlanceModifier.size(70.dp))
                }
                Spacer(GlanceModifier.width(10.dp))
                Column {
                    Text(bro.name, style = TextStyle(color = provider(DexColors.Text), fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    Text(
                        bro.types.joinToString(" / ") { it.label },
                        style = TextStyle(color = provider(bro.primaryType.color), fontSize = 11.sp),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(nudge(bro, checkedToday), style = TextStyle(color = provider(DexColors.TextMuted), fontSize = 11.sp), maxLines = 2)
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Box(
                GlanceModifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .cornerRadius(8.dp)
                    .background(if (checkedToday) DexColors.SurfaceHigh else DexColors.DexRed)
                    .clickable(actionRunCallback<CheckInFromWidget>(actionParametersOf(BRO_ID to bro.id))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (checkedToday) "CHECKED IN TODAY ✓" else "CHECK IN  +${Evolution.CHECK_IN_POINTS}",
                    style = TextStyle(color = provider(DexColors.Text), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }

    private fun nudge(bro: Bro, checkedToday: Boolean): String {
        if (checkedToday) return "Nice. You checked in today."
        val days = CheckOnBro.daysSinceContact(bro)
        return when {
            days == 0L -> "New to the dex. Say hi!"
            days < 7 -> "Last check-in: $days day${if (days == 1L) "" else "s"} ago."
            days < 30 -> "You haven't checked on them in ${days / 7} week${if (days < 14) "" else "s"}."
            else -> "It's been ${days / 30}+ month${if (days < 60) "" else "s"}. Send a text?"
        }
    }

    companion object {
        val BRO_ID = ActionParameters.Key<Long>("bro_id")

        suspend fun refresh(context: Context) {
            if (GlanceAppWidgetManager(context).getGlanceIds(BroOfTheDayWidget::class.java).isNotEmpty()) {
                BroOfTheDayWidget().updateAll(context)
            }
        }

        fun openIntent(context: Context, broId: Long?): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (broId != null) putExtra(MainActivity.EXTRA_BRO_ID, broId)
            }

        private fun provider(color: Color) = ColorProvider(color)

        private fun isToday(millis: Long): Boolean =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()

        /** Pre-scaled with nearest neighbor so the launcher can't blur the pixels much. */
        private fun spriteBitmap(bro: Bro): Bitmap {
            val bmp = com.joecode.brokemon.ui.components.AvatarBitmaps.portrait(bro.resolvedLook, Evolution.info(bro).stage.ordinal, bro.isShiny)
            return if (com.joecode.brokemon.ui.components.AvatarBitmaps.smooth) bmp else bmp.scale(bmp.width * 8, bmp.height * 8, filter = false)
        }
    }
}

class CheckInFromWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[BroOfTheDayWidget.BRO_ID] ?: return
        val container = (context.applicationContext as BrokemonApplication).container
        container.repository.checkIn(id)
        BroOfTheDayWidget().update(context, glanceId)
    }
}

class BroOfTheDayReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BroOfTheDayWidget()
}
