package com.joecode.brokemon

import android.content.Context
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.MediaStorage
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.backup.BackupManager
import com.joecode.brokemon.data.local.BroDatabase
import com.joecode.brokemon.widget.BroOfTheDayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Tiny manual DI container. One instance lives on [BrokemonApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = BroDatabase.get(appContext)
    val media = MediaStorage(appContext)
    val prefs = UserPrefs(appContext)
    val repository = BroRepository(database, media, prefs, onChanged = ::refreshWidgets)
    val backup = BackupManager(appContext, repository, media)

    fun refreshWidgets() {
        appScope.launch { runCatching { BroOfTheDayWidget.refresh(appContext) } }
    }
}
