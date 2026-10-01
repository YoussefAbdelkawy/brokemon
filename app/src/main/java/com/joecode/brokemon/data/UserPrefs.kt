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

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            val keepOnboarding = prefs[ONBOARDING_DONE] ?: false
            prefs.clear()
            prefs[ONBOARDING_DONE] = keepOnboarding
        }
    }

    private fun seenStageKey(id: Long): Preferences.Key<Int> = intPreferencesKey("seen_stage_$id")

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_RECOMMENDED = longPreferencesKey("last_recommended_bro")
    }
}
