package com.joecode.brokemon

import android.content.Context
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.MediaStorage
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.local.BroDatabase

/** Tiny manual DI container. One instance lives on [BrokemonApplication]. */
class AppContainer(context: Context) {
    private val database = BroDatabase.get(context)
    val media = MediaStorage(context)
    val prefs = UserPrefs(context)
    val repository = BroRepository(database.broDao(), database.squadDao(), media, prefs)
}
