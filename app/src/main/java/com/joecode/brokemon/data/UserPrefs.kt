package com.joecode.brokemon.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "brokemon_prefs")

class UserPrefs(private val context: Context) {

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun lastRecommendedBroId(): Long? =
        context.dataStore.data.first()[LAST_RECOMMENDED]

    suspend fun setLastRecommendedBroId(id: Long) {
        context.dataStore.edit { it[LAST_RECOMMENDED] = id }
    }

    /**
     * The evolution stage the user has already watched the animation for.
     * This is NOT the stage itself (that's always computed) — it only decides
     * whether to play the "evolving!" animation again.
     */
    suspend fun seenStage(broId: Long): Int =
        context.dataStore.data.first()[seenStageKey(broId)] ?: 0

    suspend fun setSeenStage(broId: Long, stage: Int) {
        context.dataStore.edit { it[seenStageKey(broId)] = stage }
    }

    // --- Bro of the Day (widget) ---------------------------------------------

    suspend fun broOfTheDay(): Pair<Long, Long>? {
        val prefs = context.dataStore.data.first()
        val day = prefs[BOTD_DAY] ?: return null
        val id = prefs[BOTD_ID] ?: return null
        return day to id
    }

    suspend fun setBroOfTheDay(epochDay: Long, broId: Long) {
        context.dataStore.edit {
            it[BOTD_DAY] = epochDay
            it[BOTD_ID] = broId
        }
    }

    // --- Reminders -------------------------------------------------------------

    val birthdayReminders: Flow<Boolean> = context.dataStore.data.map { it[BIRTHDAY_REMINDERS] ?: true }
    val weeklyNudge: Flow<Boolean> = context.dataStore.data.map { it[WEEKLY_NUDGE] ?: true }

    suspend fun setBirthdayReminders(on: Boolean) {
        context.dataStore.edit { it[BIRTHDAY_REMINDERS] = on }
    }

    suspend fun setWeeklyNudge(on: Boolean) {
        context.dataStore.edit { it[WEEKLY_NUDGE] = on }
    }

    /** Epoch day each reminder kind last fired, so a reminder never fires twice in one day. */
    suspend fun lastFired(kind: String): Long? = context.dataStore.data.first()[longPreferencesKey("fired_$kind")]

    suspend fun setLastFired(kind: String, epochDay: Long) {
        context.dataStore.edit { it[longPreferencesKey("fired_$kind")] = epochDay }
    }

    suspend fun lastExport(): Long? = context.dataStore.data.first()[LAST_EXPORT]

    suspend fun setLastExport(millis: Long) {
        context.dataStore.edit { it[LAST_EXPORT] = millis }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            // Keep app settings; drop everything tied to specific bros.
            val keepOnboarding = prefs[ONBOARDING_DONE] ?: false
            val keepBirthdays = prefs[BIRTHDAY_REMINDERS]
            val keepNudge = prefs[WEEKLY_NUDGE]
            prefs.clear()
            prefs[ONBOARDING_DONE] = keepOnboarding
            keepBirthdays?.let { prefs[BIRTHDAY_REMINDERS] = it }
            keepNudge?.let { prefs[WEEKLY_NUDGE] = it }
        }
    }

    private fun seenStageKey(id: Long): Preferences.Key<Int> = intPreferencesKey("seen_stage_$id")

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_RECOMMENDED = longPreferencesKey("last_recommended_bro")
        val BOTD_DAY = longPreferencesKey("botd_day")
        val BOTD_ID = longPreferencesKey("botd_id")
        val BIRTHDAY_REMINDERS = booleanPreferencesKey("birthday_reminders")
        val WEEKLY_NUDGE = booleanPreferencesKey("weekly_nudge")
        val LAST_EXPORT = longPreferencesKey("last_export")
    }
}
