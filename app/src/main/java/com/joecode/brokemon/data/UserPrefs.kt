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

    // --- Earned shinies -----------------------------------------------------------

    suspend fun lastShinyRoll(broId: Long): Long? = context.dataStore.data.first()[longPreferencesKey("shiny_roll_$broId")]

    suspend fun setLastShinyRoll(broId: Long, epochDay: Long) {
        context.dataStore.edit { it[longPreferencesKey("shiny_roll_$broId")] = epochDay }
    }

    /** True once any bro has turned shiny through a memory (unlocks the cape accessory). */
    val shinyEarned: Flow<Boolean> = context.dataStore.data.map { it[SHINY_EARNED] ?: false }

    suspend fun setShinyEarned() {
        context.dataStore.edit { it[SHINY_EARNED] = true }
    }

    // --- Battles ------------------------------------------------------------------

    val battleWins: Flow<Int> = context.dataStore.data.map { it[BATTLE_WINS] ?: 0 }
    val dailyQuestsDone: Flow<Int> = context.dataStore.data.map { it[DAILY_QUESTS] ?: 0 }
    val trophies: Flow<Int> = context.dataStore.data.map { it[TROPHIES] ?: 0 }
    val dailyQuestDay: Flow<Long?> = context.dataStore.data.map { it[DAILY_QUEST_DAY] }

    suspend fun addBattleWin() {
        context.dataStore.edit { it[BATTLE_WINS] = (it[BATTLE_WINS] ?: 0) + 1 }
    }

    /** Returns true if this completed today's daily battle quest (it only counts once a day). */
    suspend fun completeDailyQuest(epochDay: Long): Boolean {
        var done = false
        context.dataStore.edit {
            if (it[DAILY_QUEST_DAY] != epochDay) {
                it[DAILY_QUEST_DAY] = epochDay
                it[DAILY_QUESTS] = (it[DAILY_QUESTS] ?: 0) + 1
                done = true
            }
        }
        return done
    }

    suspend fun addTrophy() {
        context.dataStore.edit {
            it[TROPHIES] = (it[TROPHIES] ?: 0) + 1
            it[TOURNAMENT_WON] = true
        }
    }

    /** Typewriter speed for battle text: 0 slow, 1 normal, 2 fast. */
    val textSpeed: Flow<Int> = context.dataStore.data.map { it[TEXT_SPEED] ?: 1 }

    suspend fun setTextSpeed(value: Int) {
        context.dataStore.edit { it[TEXT_SPEED] = value.coerceIn(0, 2) }
    }

    // --- Comfort settings -----------------------------------------------------------

    val soundsEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUNDS] ?: true }
    val musicEnabled: Flow<Boolean> = context.dataStore.data.map { it[MUSIC] ?: false }
    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTICS] ?: true }
    val reduceMotion: Flow<Boolean> = context.dataStore.data.map { it[REDUCE_MOTION] ?: false }
    val holoEnabled: Flow<Boolean> = context.dataStore.data.map { it[HOLO] ?: true }

    suspend fun setSounds(on: Boolean) { context.dataStore.edit { it[SOUNDS] = on } }
    suspend fun setMusic(on: Boolean) { context.dataStore.edit { it[MUSIC] = on } }
    suspend fun setHaptics(on: Boolean) { context.dataStore.edit { it[HAPTICS] = on } }
    suspend fun setReduceMotion(on: Boolean) { context.dataStore.edit { it[REDUCE_MOTION] = on } }
    suspend fun setHolo(on: Boolean) { context.dataStore.edit { it[HOLO] = on } }

    // --- Catch draft, recent searches, release notes ---------------------------------

    val catchDraft: Flow<String?> = context.dataStore.data.map { it[CATCH_DRAFT] }

    suspend fun catchDraftOnce(): String? = catchDraft.first()

    suspend fun setCatchDraft(json: String?) {
        context.dataStore.edit { if (json == null) it.remove(CATCH_DRAFT) else it[CATCH_DRAFT] = json }
    }

    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[RECENT_SEARCHES]?.let { runCatching { gson.fromJson(it, Array<String>::class.java).toList() }.getOrNull() }.orEmpty()
    }

    suspend fun addRecentSearch(query: String) {
        val q = query.trim().take(24)
        if (q.length < 2) return
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_SEARCHES]?.let { runCatching { gson.fromJson(it, Array<String>::class.java).toList() }.getOrNull() }.orEmpty()
            val next = (listOf(q) + current.filterNot { it.equals(q, ignoreCase = true) }).take(MAX_RECENT_SEARCHES)
            prefs[RECENT_SEARCHES] = gson.toJson(next)
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(RECENT_SEARCHES) }
    }

    /** The app version whose "What's new" the user has already seen (null on a fresh install). */
    suspend fun lastSeenVersion(): Int? = context.dataStore.data.first()[LAST_SEEN_VERSION]

    suspend fun setLastSeenVersion(code: Int) {
        context.dataStore.edit { it[LAST_SEEN_VERSION] = code }
    }

    // --- Cosmetics: Daily Pack, stickers, trainer room, secrets --------------------------

    val ownedCosmetics: Flow<Set<String>> = context.dataStore.data.map { it[OWNED_COSMETICS] ?: emptySet() }

    suspend fun addCosmetic(id: String) {
        context.dataStore.edit { it[OWNED_COSMETICS] = (it[OWNED_COSMETICS] ?: emptySet()) + id }
    }

    /** Epoch day the Daily Pack was last opened. A missed day costs nothing: the pack just waits. */
    val packDay: Flow<Long?> = context.dataStore.data.map { it[PACK_DAY] }

    suspend fun setPackDay(epochDay: Long) {
        context.dataStore.edit { it[PACK_DAY] = epochDay }
    }

    val trainerRoom: Flow<List<Int>?> = context.dataStore.data.map { prefs ->
        prefs[TRAINER_ROOM]?.let { runCatching { gson.fromJson(it, Array<Int>::class.java).toList() }.getOrNull() }
    }

    suspend fun setTrainerRoom(parts: List<Int>) {
        context.dataStore.edit { it[TRAINER_ROOM] = gson.toJson(parts) }
    }

    val evolutionCinematicSeen: Flow<Boolean> = context.dataStore.data.map { it[EVO_CINEMATIC_SEEN] ?: false }

    suspend fun setEvolutionCinematicSeen() {
        context.dataStore.edit { it[EVO_CINEMATIC_SEEN] = true }
    }

    /** Epoch day the mascot's tip card was dismissed, so it shows at most once a day. */
    val tipDismissedDay: Flow<Long?> = context.dataStore.data.map { it[TIP_DISMISSED_DAY] }

    suspend fun setTipDismissedDay(epochDay: Long) {
        context.dataStore.edit { it[TIP_DISMISSED_DAY] = epochDay }
    }

    /** True once the user (as a player) has won a tournament. */
    val tournamentWon: Flow<Boolean> = context.dataStore.data.map { it[TOURNAMENT_WON] ?: false }

    suspend fun setTournamentWon() {
        context.dataStore.edit { it[TOURNAMENT_WON] = true }
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
                listOf(ONBOARDING_DONE, BIRTHDAY_REMINDERS, WEEKLY_NUDGE, DEX_VIEW, SEEN_HINTS, TEXT_SPEED, SOUNDS, MUSIC, HAPTICS, REDUCE_MOTION, HOLO, LAST_SEEN_VERSION, EVO_CINEMATIC_SEEN, TIP_DISMISSED_DAY).forEach { key ->
                    prefs[key]?.let { put(key, it) }
                }
                if (keepProfile) {
                    listOf(TRAINER, CLAIMED_QUESTS, TRADED_QR, SHINY_EARNED, TOURNAMENT_WON, BATTLE_WINS, DAILY_QUESTS, DAILY_QUEST_DAY, TROPHIES, OWNED_COSMETICS, PACK_DAY, TRAINER_ROOM).forEach { key -> prefs[key]?.let { put(key, it) } }
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
        val SHINY_EARNED = booleanPreferencesKey("shiny_earned")
        val TOURNAMENT_WON = booleanPreferencesKey("tournament_won")
        val BATTLE_WINS = intPreferencesKey("battle_wins")
        val DAILY_QUESTS = intPreferencesKey("daily_quests")
        val DAILY_QUEST_DAY = longPreferencesKey("daily_quest_day")
        val TROPHIES = intPreferencesKey("trophies")
        val TEXT_SPEED = intPreferencesKey("text_speed")
        val SOUNDS = booleanPreferencesKey("sounds")
        val MUSIC = booleanPreferencesKey("music")
        val HAPTICS = booleanPreferencesKey("haptics")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HOLO = booleanPreferencesKey("holo")
        val CATCH_DRAFT = stringPreferencesKey("catch_draft")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        const val MAX_RECENT_SEARCHES = 6
        val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version")
        val OWNED_COSMETICS = stringSetPreferencesKey("owned_cosmetics")
        val PACK_DAY = longPreferencesKey("pack_day")
        val TRAINER_ROOM = stringPreferencesKey("trainer_room")
        val EVO_CINEMATIC_SEEN = booleanPreferencesKey("evo_cinematic_seen")
        val TIP_DISMISSED_DAY = longPreferencesKey("tip_dismissed_day")
    }
}
