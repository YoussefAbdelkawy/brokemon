package com.joecode.brokemon.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.joecode.brokemon.BrokemonApplication
import com.joecode.brokemon.domain.Birthdays
import com.joecode.brokemon.domain.CheckInPrompts
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.ReminderSchedule
import com.joecode.brokemon.widget.BroOfTheDayWidget
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Runs every few hours, entirely on-device: refreshes the widget (so the Bro of
 * the Day flips over after midnight) and posts any reminders that are due.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BrokemonApplication).container
        val prefs = container.prefs
        val repo = container.repository
        val now = LocalDateTime.now()
        val today = now.toLocalDate().toEpochDay()

        runCatching { BroOfTheDayWidget.refresh(applicationContext) }

        val bros = repo.allBrosOnce()
        if (bros.isEmpty()) return Result.success()

        if (prefs.birthdayReminders.first() && ReminderSchedule.birthdaysDue(now, prefs.lastFired(KIND_BIRTHDAY))) {
            bros.filter { Birthdays.isToday(it, now.toLocalDate()) }.forEach { Notifications.birthday(applicationContext, it) }
            prefs.setLastFired(KIND_BIRTHDAY, today)
        }

        if (prefs.weeklyNudge.first() && ReminderSchedule.nudgeDue(now, prefs.lastFired(KIND_NUDGE))) {
            CheckOnBro.recommend(bros, prefs.lastRecommendedBroId())?.let { bro ->
                prefs.setLastRecommendedBroId(bro.id)
                Notifications.nudge(
                    applicationContext,
                    bro,
                    CheckOnBro.daysSinceContact(bro),
                    CheckInPrompts.forBro(bro).firstOrNull(),
                )
            }
            prefs.setLastFired(KIND_NUDGE, today)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "brokemon_reminders"
        private const val KIND_BIRTHDAY = "birthday"
        private const val KIND_NUDGE = "nudge"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(3, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
