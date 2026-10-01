package com.joecode.brokemon.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.joecode.brokemon.R
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.widget.BroOfTheDayWidget
import com.joecode.brokemon.ui.theme.DexColors
import androidx.compose.ui.graphics.toArgb

object Notifications {
    private const val CHANNEL_BIRTHDAYS = "birthdays"
    private const val CHANNEL_CHECK_INS = "check_ins"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_BIRTHDAYS, "Birthdays", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "A heads-up on your bros' birthdays"
                },
                NotificationChannel(CHANNEL_CHECK_INS, "Check-in nudges", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "A weekly nudge to check on a bro"
                },
            ),
        )
    }

    fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (android.os.Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun birthday(context: Context, bro: Bro) {
        val n = NotificationCompat.Builder(context, CHANNEL_BIRTHDAYS)
            .setSmallIcon(R.drawable.ic_stat_brokemon)
            .setColor(DexColors.DexRed.toArgb())
            .setContentTitle("🎂 It's ${bro.name}'s birthday!")
            .setContentText("Send them something, then log a memory (+${Evolution.MEMORY_POINTS} bond).")
            .setContentIntent(openCard(context, bro.id))
            .setAutoCancel(true)
            .build()
        post(context, birthdayId(bro.id), n)
    }

    fun nudge(context: Context, bro: Bro, days: Long, prompt: String?) {
        val n = NotificationCompat.Builder(context, CHANNEL_CHECK_INS)
            .setSmallIcon(R.drawable.ic_stat_brokemon)
            .setColor(DexColors.DexRed.toArgb())
            .setContentTitle("Check on ${bro.name}?")
            .setContentText(if (days > 0) "It's been $days days. ${prompt.orEmpty()}" else prompt.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (days > 0) "It's been $days days. ${prompt.orEmpty()}" else prompt.orEmpty()))
            .setContentIntent(openCard(context, bro.id))
            .addAction(0, "Checked in ✓", checkInAction(context, bro.id))
            .setAutoCancel(true)
            .build()
        post(context, NUDGE_ID, n)
    }

    fun cancelNudge(context: Context) = NotificationManagerCompat.from(context).cancel(NUDGE_ID)

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        if (!canNotify(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
    }

    private fun openCard(context: Context, broId: Long): PendingIntent = PendingIntent.getActivity(
        context,
        broId.toInt(),
        BroOfTheDayWidget.openIntent(context, broId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun checkInAction(context: Context, broId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        broId.toInt(),
        Intent(context, CheckInReceiver::class.java).putExtra(CheckInReceiver.EXTRA_BRO_ID, broId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private const val NUDGE_ID = 1
    private fun birthdayId(broId: Long) = 1_000 + broId.toInt()
}
