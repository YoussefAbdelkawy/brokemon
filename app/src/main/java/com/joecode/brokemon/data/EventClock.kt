package com.joecode.brokemon.data

import com.joecode.brokemon.domain.SeasonEvent
import com.joecode.brokemon.domain.SeasonEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Which limited event is running right now. Uses only the device date.
 * Debug builds can force an event from Settings so it can be tested any day.
 */
class EventClock(private val prefs: UserPrefs, private val debuggable: Boolean) {

    val current: Flow<SeasonEvent?> = prefs.debugEvent.map { resolve(it) }

    suspend fun now(): SeasonEvent? = resolve(prefs.debugEvent.first())

    private fun resolve(override: String?): SeasonEvent? = when {
        !debuggable || override == null -> SeasonEvents.active(LocalDate.now())
        override == NONE -> null
        else -> SeasonEvent.entries.firstOrNull { it.name == override } ?: SeasonEvents.active(LocalDate.now())
    }

    companion object {
        const val NONE = "NONE"
    }
}
