package com.joecode.brokemon

import android.app.Application
import androidx.work.Configuration
import com.joecode.brokemon.notify.Notifications
import com.joecode.brokemon.notify.ReminderWorker

/**
 * Implements [Configuration.Provider] so WorkManager can initialize on demand
 * (it's used for the reminders + widget refresh worker).
 */
class BrokemonApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        ReminderWorker.schedule(this)
    }
}
