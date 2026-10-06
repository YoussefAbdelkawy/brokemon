package com.joecode.brokemon.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.gson.Gson
import com.joecode.brokemon.data.model.Trainer
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "brokemon_prefs")

/** How the Brodex lays out cards. */
enum class DexView { CARDS, LIST, BINDER }

/** One-time coach marks ("tap here!"). Each id shows once, then never again. */
interface HintStore {
    val seenHints: Flow<Set<String>>
    suspend fun markHintSeen(id: String)
}

class UserPrefs(private val context: Context) : HintStore {

    private val gson = Gson()

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

    /** Whether the "hold a card to enter their room" tip can be retired. */
    val roomHintSeen: Flow<Boolean> = context.dataStore.data.map { it[ROOM_HINT_SEEN] ?: false }

    suspend fun setRoomHintSeen() {
        context.dataStore.edit { it[ROOM_HINT_SEEN] = true }
    }

    /** Debug-only event override: null = real date, "NONE" = no event, else a SeasonEvent name. */
    val debugEvent: Flow<String?> = context.dataStore.data.map { it[DEBUG_EVENT] }

    suspend fun setDebugEvent(value: String?) {
        context.dataStore.edit { if (value == null) it.remove(DEBUG_EVENT) else it[DEBUG_EVENT] = value }
    }

    // --- Trainer Card, Journal, hints, dex view ---------------------------------

    val trainer: Flow<Trainer?> = context.dataStore.data.map { prefs ->
        prefs[TRAINER]?.let { json -> runCatching { gson.fromJson(json, Trainer::class.java)?.sanitized() }.getOrNull() }
    }

    suspend fun trainerOnce(): Trainer? = trainer.first()

    suspend fun setTrainer(trainer: Trainer?) {
        context.dataStore.edit { if (trainer == null) it.remove(TRAINER) else it[TRAINER] = gson.toJson(trainer) }
    }

    val claimedQuests: Flow<Set<String>> = context.dataStore.data.map { it[CLAIMED_QUESTS] ?: emptySet() }

    suspend fun claimQuest(name: String) {
        context.dataStore.edit { it[CLAIMED_QUESTS] = (it[CLAIMED_QUESTS] ?: emptySet()) + name }
    }

    /** Set the first time a QR card is shown or scanned (Journal quest). */
    val tradedQr: Flow<Boolean> = context.dataStore.data.map { it[TRADED_QR] ?: false }

    suspend fun setTradedQr() {
        context.dataStore.edit { it[TRADED_QR] = true }
    }

    override val seenHints: Flow<Set<String>> = context.dataStore.data.map { it[SEEN_HINTS] ?: emptySet() }

    override suspend fun markHintSeen(id: String) {
        context.dataStore.edit { it[SEEN_HINTS] = (it[SEEN_HINTS] ?: emptySet()) + id }
    }

    val dexView: Flow<DexView> = context.dataStore.data.map { prefs ->
        DexView.entries.firstOrNull { it.name == prefs[DEX_VIEW] } ?: DexView.CARDS
    }

    suspend fun setDexView(view: DexView) {
        context.dataStore.edit { it[DEX_VIEW] = view.name }
    }

    suspend fun lastExport(): Long? = context.dataStore.data.first()[LAST_EXPORT]

    suspend fun setLastExport(millis: Long) {
        context.dataStore.edit { it[LAST_EXPORT] = millis }
    }

    /**
     * Drops everything tied to specific bros but keeps app settings.
     * [keepProfile] also keeps the Trainer Card and Journal progress (used when
     * restoring a backup, which replaces the bros but not you).
     */
    suspend fun clear(keepProfile: Boolean = false) {
        context.dataStore.edit { prefs ->
            val keep: Map<Preferences.Key<*>, Any> = buildMap {
                listOf(ONBOARDING_DONE, BIRTHDAY_REMINDERS, WEEKLY_NUDGE, DEX_VIEW, SEEN_HINTS).forEach { key ->
                    prefs[key]?.let { put(key, it) }
                }
                if (keepProfile) {
                    listOf(TRAINER, CLAIMED_QUESTS, TRADED_QR).forEach { key -> prefs[key]?.let { put(key, it) } }
                }
            }
            prefs.clear()
            @Suppress("UNCHECKED_CAST")
            keep.forEach { (key, value) -> prefs[key as Preferences.Key<Any>] = value }
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
        val DEBUG_EVENT = stringPreferencesKey("debug_event")
        val ROOM_HINT_SEEN = booleanPreferencesKey("room_hint_seen")
        val TRAINER = stringPreferencesKey("trainer")
        val CLAIMED_QUESTS = stringSetPreferencesKey("claimed_quests")
        val TRADED_QR = booleanPreferencesKey("traded_qr")
        val SEEN_HINTS = stringSetPreferencesKey("seen_hints")
        val DEX_VIEW = stringPreferencesKey("dex_view")
    }
}
