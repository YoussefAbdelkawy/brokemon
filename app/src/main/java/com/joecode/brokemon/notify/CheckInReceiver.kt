package com.joecode.brokemon.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joecode.brokemon.BrokemonApplication
import kotlinx.coroutines.launch

/** Handles the "Checked in ✓" button on the weekly nudge notification. */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_BRO_ID, -1L).takeIf { it > 0 } ?: return
        val container = (context.applicationContext as BrokemonApplication).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.repository.checkIn(id)
                Notifications.cancelNudge(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_BRO_ID = "com.joecode.brokemon.extra.CHECK_IN_BRO_ID"
    }
}
