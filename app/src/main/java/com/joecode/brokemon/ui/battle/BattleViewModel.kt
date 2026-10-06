package com.joecode.brokemon.ui.battle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.battle.LinkEvent
import com.joecode.brokemon.battle.NearbyLink
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.BattleLoadout
import com.joecode.brokemon.data.model.BattleMode as RecordMode
import com.joecode.brokemon.data.model.BattleRecord
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.CustomMoveSpec
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.data.model.RegionalDex
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.battle.BattleAction
import com.joecode.brokemon.domain.battle.BattleAi
import com.joecode.brokemon.domain.battle.BattleCodec
import com.joecode.brokemon.domain.battle.BattleEngine
import com.joecode.brokemon.domain.battle.BattleEvent
import com.joecode.brokemon.domain.battle.BattleLines
import com.joecode.brokemon.domain.battle.BattleMath
import com.joecode.brokemon.domain.battle.BattleMessage
import com.joecode.brokemon.domain.battle.BattleState
import com.joecode.brokemon.domain.battle.Brackets
import com.joecode.brokemon.domain.battle.DailyBattleQuest
import com.joecode.brokemon.domain.battle.FighterSnapshot
import com.joecode.brokemon.domain.battle.MoveCatalog
import com.joecode.brokemon.domain.battle.RoomCodes
import com.joecode.brokemon.domain.battle.Snapshots
import com.joecode.brokemon.domain.battle.StatusEffect
import com.joecode.brokemon.domain.battle.TournamentFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

enum class BattleKind { CPU, LOCAL, HOST, JOIN, REPLAY }

enum class BattlePhase { SETUP, LOBBY, BATTLE, OVER }

/** One fighter as currently drawn (HP animates step by step during playback). */
data class ShownFighter(
    val name: String,
    val look: BroLook,
    val shiny: Boolean,
    val types: List<BroType>,
    val level: Int,
    val hp: Int,
    val maxHp: Int,
    val statuses: Set<StatusEffect>,
    val fainted: Boolean,
    val teamLeft: Int,
    val teamSize: Int,
)

enum class EffectKind { HIT, BIG_HIT, FAINT, SWITCH, STATUS, END }

data class PlaybackStep(val text: String, val shown: List<ShownFighter>, val effect: EffectKind? = null, val effectSide: Int = 0)

data class Emote(val side: Int, val text: String, val id: Long)

data class LobbyState(
    val code: String = "",
    val connected: Boolean = false,
    val opponentName: String? = null,
    val opponentTeam: List<FighterSnapshot>? = null,
    val meReady: Boolean = false,
    val themReady: Boolean = false,
    val error: String? = null,
    val reconnectSecondsLeft: Int? = null,
)

data class BattleResult(
    val winnerName: String,
    val youWon: Boolean?,
    val mvp: FighterSnapshot,
    val mvpDamage: Int,
    val turns: Int,
    val dailyQuestDone: Boolean = false,
    val bonusFor: String? = null,
    val champion: String? = null,
)

data class BattleUiState(
    val kind: BattleKind = BattleKind.CPU,
    val phase: BattlePhase = BattlePhase.SETUP,
    val format: Int = 3,
    val fairMode: Boolean = true,
    val bros: List<Bro> = emptyList(),
    val dexes: List<RegionalDex> = emptyList(),
    val dexFilter: Long? = null,
    val dexMembers: Set<Long>? = null,
    val picked: List<Long> = emptyList(),
    val picked2: List<Long> = emptyList(),
    /** Same-phone mode: which player is picking their team (1 or 2). */
    val pickingPlayer: Int = 1,
    val myName: String = "You",
    val opponentName: String = "CPU",
    val lobby: LobbyState = LobbyState(),
    val battle: BattleState? = null,
    val mySide: Int = 0,
    val shown: List<ShownFighter> = emptyList(),
    val steps: List<PlaybackStep> = emptyList(),
    /** Side whose input is needed now (null while animating or waiting on the other phone). */
    val awaiting: Int? = null,
    /** Same-phone mode: cover the screen until the next player takes the phone. */
    val passTo: Int? = null,
    val waitingForOpponent: Boolean = false,
    val timerLeft: Int = TURN_SECONDS,
    val emotes: List<Emote> = emptyList(),
    val result: BattleResult? = null,
    val textSpeed: Int = 1,
    val tournamentLabel: String? = null,
    val dailyType: BroType = DailyBattleQuest.typeFor(),
) {
    val teamSize: Int get() = format
    val currentPicks: List<Long> get() = if (pickingPlayer == 2) picked2 else picked
}

const val TURN_SECONDS = 30

class BattleViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BroRepository,
    private val prefs: UserPrefs,
    linkFactory: () -> NearbyLink,
) : ViewModel() {

    /** Only Nearby battles create the link, so CPU and same-phone battles never touch Play services. */
    private val link: NearbyLink by lazy(linkFactory)

    private val kind: BattleKind = runCatching { BattleKind.valueOf(savedStateHandle.get<String>("kind") ?: "CPU") }.getOrDefault(BattleKind.CPU)
    private val replayId: Long = savedStateHandle.get<Long>("replayId") ?: 0L
    private val tournamentId: Long = savedStateHandle.get<Long>("tournamentId") ?: 0L
    private val matchRound: Int = savedStateHandle.get<Int>("round") ?: -1
    private val matchIndex: Int = savedStateHandle.get<Int>("index") ?: -1

    private val _state = MutableStateFlow(BattleUiState(kind = kind, fairMode = true, format = if (kind == BattleKind.CPU) 3 else 3))
    val state: StateFlow<BattleUiState> = _state.asStateFlow()

    val allDexRefs = repository.dexRefs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var teams: List<List<FighterSnapshot>> = emptyList()
    private val history = mutableListOf<Pair<BattleAction, BattleAction>>()
    private val pending = arrayOfNulls<BattleAction>(2)
    private var timerJob: Job? = null
    private var reconnectJob: Job? = null
    private var linkJob: Job? = null
    private val random = Random.Default
    private var recorded = false
    private var rematchMine = false
    private var rematchTheirs = false

    init {
        viewModelScope.launch {
            val trainer = prefs.trainerOnce()
            _state.update { it.copy(myName = trainer?.name ?: "You", textSpeed = prefs.textSpeed.first()) }
        }
        viewModelScope.launch {
            repository.bros.collect { bros -> _state.update { it.copy(bros = bros) } }
        }
        viewModelScope.launch {
            repository.dexes.collect { dexes -> _state.update { it.copy(dexes = dexes) } }
        }
        if (tournamentId > 0) viewModelScope.launch { loadTournamentMatch() }
        if (kind == BattleKind.REPLAY) viewModelScope.launch { loadReplay() }
        if (kind == BattleKind.HOST || kind == BattleKind.JOIN) listenToLink()
    }

    // --- Setup --------------------------------------------------------------------

    fun setFormat(format: Int) = _state.update { s ->
        s.copy(format = format, picked = s.picked.take(format), picked2 = s.picked2.take(format))
    }

    fun setFair(on: Boolean) = _state.update { it.copy(fairMode = on) }

    fun setDexFilter(id: Long?) = _state.update { s ->
        s.copy(dexFilter = id, dexMembers = id?.let { d -> allDexRefs.value.filter { it.dexId == d }.map { it.broId }.toSet() })
    }

    fun setOpponentName(name: String) = _state.update { it.copy(opponentName = name.take(16)) }

    fun togglePick(id: Long) = _state.update { s ->
        val list = s.currentPicks
        val next = when {
            id in list -> list - id
            list.size >= s.format -> list
            else -> list + id
        }
        if (s.pickingPlayer == 2) s.copy(picked2 = next) else s.copy(picked = next)
    }

    fun nextPicker() = _state.update { it.copy(pickingPlayer = 2) }
    fun backToPicker1() = _state.update { it.copy(pickingPlayer = 1) }

    /** Saves a bro's 4 equipped moves (by key). */
    fun saveLoadout(bro: Bro, equipped: List<String>, customs: List<CustomMoveSpec>) {
        viewModelScope.launch {
            val fresh = repository.findBro(bro.id) ?: return@launch
            repository.update(fresh.copy(battle = fresh.resolvedBattle.copy(equipped = equipped.take(4), customs = customs)))
        }
    }

    private fun snapshotsOf(ids: List<Long>): List<FighterSnapshot> =
        ids.mapNotNull { id -> _state.value.bros.firstOrNull { it.id == id } }.map(Snapshots::of)

    // --- Starting -------------------------------------------------------------------

    fun startCpu() {
        val mine = snapshotsOf(_state.value.picked)
        if (mine.isEmpty()) return
        val avg = mine.map { it.level }.average().toInt()
        teams = listOf(mine, cpuTeam(mine.size, avg))
        _state.update { it.copy(opponentName = CPU_NAMES.random(random)) }
        begin(seed = random.nextLong(), mySide = 0)
    }

    fun startLocal() {
        val p1 = snapshotsOf(_state.value.picked)
        val p2 = snapshotsOf(_state.value.picked2)
        if (p1.isEmpty() || p2.isEmpty()) return
        teams = listOf(p1, p2)
        begin(seed = random.nextLong(), mySide = 0)
    }

    private fun begin(seed: Long, mySide: Int) {
        val s = _state.value
        val names = sideNames(s, mySide)
        val battle = BattleEngine.start(teams[0], teams[1], names[0], names[1], seed, s.fairMode)
        history.clear()
        pending.fill(null)
        recorded = false
        rematchMine = false
        rematchTheirs = false
        _state.update {
            it.copy(
                phase = BattlePhase.BATTLE,
                battle = battle,
                mySide = mySide,
                shown = shownOf(battle),
                steps = listOf(
                    PlaybackStep("${names[1 - mySide]} wants to battle!", shownOf(battle)),
                    PlaybackStep("Go, ${battle.sides[mySide].current.snap.name}!", shownOf(battle), EffectKind.SWITCH, mySide),
                ),
                awaiting = null,
                passTo = null,
                result = null,
                emotes = emptyList(),
            )
        }
    }

    private fun sideNames(s: BattleUiState, mySide: Int): List<String> {
        val me = s.myName
        val them = when (s.kind) {
            BattleKind.LOCAL -> s.opponentName.ifBlank { "Player 2" }
            BattleKind.HOST, BattleKind.JOIN -> s.lobby.opponentName ?: "Rival"
            else -> s.opponentName
        }
        return if (mySide == 0) listOf(me, them) else listOf(them, me)
    }

    private fun cpuTeam(size: Int, level: Int): List<FighterSnapshot> = List(size) { i ->
        val types = BroType.entries.shuffled(random).take(if (random.nextBoolean()) 1 else 2)
        val score = BattleMath.scoreForLevel(level)
        val fake = Bro(
            id = -(i + 1L),
            name = CPU_BROS.random(random),
            type1 = types[0].name,
            type2 = types.getOrNull(1)?.name,
            stats = BroStats.random(random),
            moves = emptyList(),
            rarity = Rarity.COMMON,
            catchDate = System.currentTimeMillis(),
            avatarSeed = random.nextLong(),
        )
        FighterSnapshot(
            name = fake.name,
            look = BroLook.random(fake.avatarSeed).toList(),
            types = fake.types.map { it.name },
            level = level,
            base = fake.stats.toArray(),
            moves = MoveCatalog.equipped(fake, score).map(MoveCatalog::specOf),
        )
    }

    // --- Turns ------------------------------------------------------------------------

    /** Called by the UI once every playback step has been shown. */
    fun onPlaybackDone() {
        val s = _state.value
        val battle = s.battle ?: return
        _state.update { it.copy(steps = emptyList(), shown = shownOf(battle)) }
        if (battle.over) {
            finish(battle)
            return
        }
        when (kind) {
            BattleKind.LOCAL -> _state.update { it.copy(passTo = 0) }
            BattleKind.REPLAY -> Unit
            else -> awaitInput(s.mySide)
        }
    }

    fun onPassAccepted() {
        val to = _state.value.passTo ?: return
        _state.update { it.copy(passTo = null) }
        awaitInput(to)
    }

    private fun awaitInput(side: Int) {
        _state.update { it.copy(awaiting = side, timerLeft = TURN_SECONDS, waitingForOpponent = false) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (t in TURN_SECONDS downTo 1) {
                _state.update { it.copy(timerLeft = t) }
                delay(1000)
            }
            // Time's up: a random legal move (never forfeit).
            val battle = _state.value.battle ?: return@launch
            val options = BattleEngine.legalActions(battle, side).filterIsInstance<BattleAction.Move>()
            options.randomOrNull(random)?.let { choose(it) }
        }
    }

    /** The player at [_state].awaiting picked an action. */
    fun choose(action: BattleAction) {
        val s = _state.value
        val side = s.awaiting ?: return
        val battle = s.battle ?: return
        if (!BattleEngine.isLegal(battle, side, action)) return
        timerJob?.cancel()
        pending[side] = action
        _state.update { it.copy(awaiting = null) }
        when (kind) {
            BattleKind.CPU -> {
                pending[1] = BattleAi.choose(battle, 1, random)
                resolveTurn()
            }
            BattleKind.LOCAL -> if (side == 0) _state.update { it.copy(passTo = 1) } else resolveTurn()
            BattleKind.HOST, BattleKind.JOIN -> {
                link.send(BattleMessage(BattleMessage.ACTION, turn = battle.turn, action = BattleAction.encode(action)))
                if (pending[1 - side] != null) resolveTurn() else _state.update { it.copy(waitingForOpponent = true) }
            }
            BattleKind.REPLAY -> Unit
        }
    }

    private fun resolveTurn() {
        val before = _state.value.battle ?: return
        val a = pending[0] ?: return
        val b = pending[1] ?: return
        pending.fill(null)
        history += a to b
        val after = BattleEngine.resolve(before, a, b)
        val steps = playback(before, after, _state.value.mySide)
        _state.update { it.copy(battle = after, steps = steps, waitingForOpponent = false) }
        if (kind == BattleKind.CPU && random.nextInt(100) < 25 && steps.any { it.effect == EffectKind.BIG_HIT && it.effectSide == 1 }) {
            emote(1, BattleLines.emotes.random(random), send = false)
        }
    }

    private val lineRandom = Random(7)

    /** Turns the engine's events into text + HP snapshots to animate one by one. */
    private fun playback(before: BattleState, after: BattleState, mySide: Int): List<PlaybackStep> {
        val shown = shownOf(before).toMutableList()
        val names = after.sides.map { it.name }
        val steps = mutableListOf<PlaybackStep>()
        for (event in after.events) {
            var effect: EffectKind? = null
            var effectSide = 0
            when (event) {
                is BattleEvent.Hit -> {
                    shown[event.side] = shown[event.side].copy(hp = event.hpLeft)
                    val big = event.effectiveness > 1f || event.crit || event.damage * 3 > event.maxHp
                    effect = if (big) EffectKind.BIG_HIT else EffectKind.HIT
                    effectSide = event.side
                }
                is BattleEvent.StatusApplied -> {
                    shown[event.side] = shown[event.side].copy(statuses = shown[event.side].statuses + event.status)
                    effect = EffectKind.STATUS
                    effectSide = event.side
                }
                is BattleEvent.StatusEnded -> shown[event.side] = shown[event.side].copy(statuses = shown[event.side].statuses - event.status)
                is BattleEvent.Fainted -> {
                    shown[event.side] = shown[event.side].copy(hp = 0, fainted = true, teamLeft = shown[event.side].teamLeft - 1)
                    effect = EffectKind.FAINT
                    effectSide = event.side
                }
                is BattleEvent.SwitchedIn -> {
                    val side = after.sides[event.side]
                    val f = side.fighters.firstOrNull { it.snap.name == event.fighter && !it.fainted } ?: side.current
                    shown[event.side] = shownFighter(f, side)
                    effect = EffectKind.SWITCH
                    effectSide = event.side
                }
                is BattleEvent.Won, BattleEvent.Draw, is BattleEvent.Forfeited -> effect = EffectKind.END
                else -> Unit
            }
            BattleLines.describe(event, names, lineRandom).forEachIndexed { i, line ->
                steps += PlaybackStep(line, shown.toList(), if (i == 0) effect else null, effectSide)
            }
        }
        return steps
    }

    private fun shownOf(battle: BattleState): List<ShownFighter> = battle.sides.map { shownFighter(it.current, it) }

    private fun shownFighter(f: com.joecode.brokemon.domain.battle.Fighter, side: com.joecode.brokemon.domain.battle.Side) = ShownFighter(
        name = f.snap.name,
        // Backdrops are for cards; in battle the arena is the backdrop.
        look = BroLook.fromList(f.snap.look).copy(background = 0),
        shiny = f.snap.shiny,
        types = f.types,
        level = f.snap.level,
        hp = f.hp,
        maxHp = f.stats.hp,
        statuses = f.statuses.keys,
        fainted = f.fainted,
        teamLeft = side.fighters.count { !it.fainted },
        teamSize = side.fighters.size,
    )

    fun forfeit() = choose(BattleAction.Forfeit)

    fun emote(side: Int, text: String, send: Boolean = true) {
        val e = Emote(side, text, System.nanoTime())
        _state.update { it.copy(emotes = it.emotes + e) }
        if (send && (kind == BattleKind.HOST || kind == BattleKind.JOIN)) link.send(BattleMessage(BattleMessage.EMOTE, emote = text))
        viewModelScope.launch {
            delay(2200)
            _state.update { s -> s.copy(emotes = s.emotes - e) }
        }
    }

    // --- End of battle ------------------------------------------------------------------

    private fun finish(battle: BattleState) {
        timerJob?.cancel()
        val s = _state.value
        val winnerSide = battle.winner ?: return
        val mySide = s.mySide
        val youWon: Boolean? = when {
            winnerSide < 0 -> null
            kind == BattleKind.LOCAL || kind == BattleKind.REPLAY -> null
            else -> winnerSide == mySide
        }
        val mvpSide = if (winnerSide >= 0) winnerSide else mySide
        val mvp = BattleEngine.mvp(battle, mvpSide)
        val winnerName = if (winnerSide >= 0) battle.sides[winnerSide].name else "Nobody"
        _state.update {
            it.copy(
                phase = BattlePhase.OVER,
                result = BattleResult(winnerName, youWon, mvp.snap, mvp.damageDealt, battle.turn),
            )
        }
        if (kind != BattleKind.REPLAY && !recorded) {
            recorded = true
            viewModelScope.launch { record(battle, winnerSide, youWon) }
        }
    }

    private suspend fun record(battle: BattleState, winnerSide: Int, youWon: Boolean?) {
        val s = _state.value
        val mySide = s.mySide
        val myMvp = BattleEngine.mvp(battle, mySide)
        repository.insertBattle(
            BattleRecord(
                date = System.currentTimeMillis(),
                mode = when (kind) {
                    BattleKind.CPU -> RecordMode.CPU.name
                    BattleKind.LOCAL -> RecordMode.LOCAL.name
                    else -> RecordMode.NEARBY.name
                },
                opponentName = battle.sides[1 - mySide].name,
                won = youWon,
                mvpBroId = myMvp.snap.localId.takeIf { it > 0 },
                mvpName = myMvp.snap.name,
                seed = battle.seed,
                fairMode = battle.fairMode,
                snapshotsJson = BattleCodec.teamsToJson(teams),
                actionsJson = BattleCodec.actionsToJson(history),
                tournamentId = tournamentId.takeIf { it > 0 },
                mySide = mySide,
                sideNames = battle.sides.map { it.name },
            ),
        ).let { recordId -> if (tournamentId > 0) recordTournament(battle, winnerSide, recordId) }

        if (youWon == true) {
            prefs.addBattleWin()
            // Daily quest: win with a bro of today's type on your team.
            val questType = DailyBattleQuest.typeFor()
            val daily = battle.sides[mySide].fighters.any { f -> questType in f.types } &&
                prefs.completeDailyQuest(LocalDate.now().toEpochDay())
            // Friendship bonus for the MVP: one point a day, capped.
            var bonusFor: String? = null
            val id = myMvp.snap.localId
            if (id > 0) {
                repository.findBro(id)?.let { bro ->
                    val loadout = bro.resolvedBattle
                    val today = LocalDate.now().toEpochDay()
                    if (loadout.lastBonusDay != today && loadout.winBonus < Evolution.BATTLE_BONUS_CAP) {
                        repository.update(bro.copy(battle = loadout.copy(winBonus = loadout.winBonus + 1, lastBonusDay = today)))
                        bonusFor = bro.name
                    }
                }
            }
            _state.update { it.copy(result = it.result?.copy(dailyQuestDone = daily, bonusFor = bonusFor)) }
        }
    }

    private suspend fun recordTournament(battle: BattleState, winnerSide: Int, recordId: Long) {
        if (winnerSide < 0) return
        val t = repository.tournament(tournamentId).first() ?: return
        val matches = BattleCodec.matchesFromJson(t.matchesJson)
        val match = matches.firstOrNull { it.round == matchRound && it.index == matchIndex } ?: return
        // Side 0 is always the match's first player, side 1 the second (see loadTournamentMatch).
        val winner = if (winnerSide == 0) match.a else match.b
        winner ?: return
        val updated = Brackets.record(matches, matchRound, matchIndex, winner, recordId)
        val format = TournamentFormat.entries.firstOrNull { it.name == t.format } ?: TournamentFormat.BRACKET
        val champion = Brackets.champion(format, t.players, updated)
        repository.updateTournament(t.copy(matchesJson = BattleCodec.matchesToJson(updated), champion = champion))
        if (champion != null) {
            _state.update { it.copy(result = it.result?.copy(champion = champion)) }
            val me = _state.value.myName
            val mySideName = battle.sides[_state.value.mySide].name
            if (champion == me || champion == mySideName) {
                prefs.addTrophy()
                val mvp = BattleEngine.mvp(battle, _state.value.mySide).snap.localId
                repository.findBro(mvp)?.let { bro ->
                    repository.update(bro.copy(battle = bro.resolvedBattle.copy(championships = bro.resolvedBattle.championships + 1)))
                }
            }
        }
    }

    fun rematch() {
        when (kind) {
            BattleKind.CPU -> startCpu()
            BattleKind.LOCAL -> startLocal()
            BattleKind.HOST, BattleKind.JOIN -> {
                rematchMine = true
                link.send(BattleMessage(BattleMessage.REMATCH))
                maybeRematch()
            }
            BattleKind.REPLAY -> viewModelScope.launch { loadReplay() }
        }
    }

    private fun maybeRematch() {
        if (!rematchMine || !rematchTheirs) return
        if (kind == BattleKind.HOST) {
            val seed = random.nextLong()
            link.send(BattleMessage(BattleMessage.CONFIG, format = _state.value.format, fair = _state.value.fairMode, seed = seed))
            begin(seed, mySide = 0)
        }
    }

    // --- Tournament context -----------------------------------------------------------

    private suspend fun loadTournamentMatch() {
        val t = repository.tournament(tournamentId).first() ?: return
        val match = BattleCodec.matchesFromJson(t.matchesJson).firstOrNull { it.round == matchRound && it.index == matchIndex } ?: return
        // Tournaments are always Fair Mode by default.
        _state.update {
            it.copy(
                fairMode = true,
                myName = match.a ?: it.myName,
                opponentName = match.b ?: it.opponentName,
                tournamentLabel = "${t.name}: ${match.a} vs ${match.b}",
            )
        }
    }

    // --- Replays ---------------------------------------------------------------------

    private suspend fun loadReplay() {
        val record = repository.findBattle(replayId) ?: return
        val loaded = BattleCodec.teamsFromJson(record.snapshotsJson) ?: return
        teams = loaded
        val actions = BattleCodec.actionsFromJson(record.actionsJson)
        val names = record.sideNames.takeIf { it.size == 2 } ?: listOf("Side 1", "Side 2")
        val start = BattleEngine.start(teams[0], teams[1], names[0], names[1], record.seed, record.fairMode)
        val states = BattleEngine.replay(start, actions)
        val steps = mutableListOf(PlaybackStep("Replay: ${names[0]} vs ${names[1]}", shownOf(start)))
        states.zipWithNext().forEach { (a, b) -> steps += playback(a, b, record.mySide) }
        _state.update {
            it.copy(phase = BattlePhase.BATTLE, battle = states.last(), mySide = record.mySide, shown = shownOf(start), steps = steps, fairMode = record.fairMode)
        }
    }

    // --- Nearby -----------------------------------------------------------------------

    fun openRoom() {
        val code = RoomCodes.generate(random)
        _state.update { it.copy(phase = BattlePhase.LOBBY, lobby = LobbyState(code = code)) }
        link.host(code, _state.value.myName)
        // Codes expire after 10 minutes if nobody joins.
        viewModelScope.launch {
            delay(10 * 60_000L)
            if (!_state.value.lobby.connected && _state.value.phase == BattlePhase.LOBBY) {
                link.stop()
                _state.update { it.copy(lobby = it.lobby.copy(error = "This code expired. Open a new room.")) }
            }
        }
    }

    fun joinRoom(rawCode: String) {
        val code = RoomCodes.normalize(rawCode)
        if (!RoomCodes.isValid(code)) {
            _state.update { it.copy(lobby = it.lobby.copy(error = "Codes are 6 letters/numbers, like K7Q2XM.")) }
            return
        }
        _state.update { it.copy(phase = BattlePhase.LOBBY, lobby = LobbyState(code = code)) }
        link.join(code, _state.value.myName)
    }

    fun setReady() {
        _state.update { it.copy(lobby = it.lobby.copy(meReady = true)) }
        link.send(BattleMessage(BattleMessage.READY))
        maybeStartNearby()
    }

    fun leaveLobby() {
        link.send(BattleMessage(BattleMessage.LEAVE))
        link.stop()
        _state.update { it.copy(phase = BattlePhase.SETUP, lobby = LobbyState()) }
    }

    private var hostSeed: Long? = null

    private fun listenToLink() {
        linkJob = viewModelScope.launch {
            link.events.collect { event ->
                when (event) {
                    LinkEvent.Connected -> onConnected()
                    LinkEvent.Disconnected -> onDisconnected()
                    is LinkEvent.Failed -> _state.update { it.copy(lobby = it.lobby.copy(error = event.reason)) }
                    is LinkEvent.Message -> onMessage(event.message)
                }
            }
        }
    }

    private fun onConnected() {
        reconnectJob?.cancel()
        _state.update { it.copy(lobby = it.lobby.copy(connected = true, error = null, reconnectSecondsLeft = null)) }
        val s = _state.value
        link.send(BattleMessage(BattleMessage.HELLO, trainer = s.myName))
        if (s.phase == BattlePhase.LOBBY || s.phase == BattlePhase.SETUP) {
            link.send(BattleMessage(BattleMessage.TEAM, team = snapshotsOf(s.picked)))
            if (kind == BattleKind.HOST) {
                val seed = hostSeed ?: random.nextLong().also { hostSeed = it }
                link.send(BattleMessage(BattleMessage.CONFIG, format = s.format, fair = s.fairMode, seed = seed))
            }
        } else {
            // Back after a drop: resend this turn's action if we already picked one.
            val battle = s.battle
            val mine = pending[s.mySide]
            if (battle != null && mine != null) {
                link.send(BattleMessage(BattleMessage.ACTION, turn = battle.turn, action = BattleAction.encode(mine)))
            }
        }
    }

    private fun onDisconnected() {
        val s = _state.value
        if (s.phase == BattlePhase.OVER) return
        if (s.phase != BattlePhase.BATTLE) {
            _state.update { it.copy(lobby = it.lobby.copy(connected = false, error = "Lost the connection. Waiting for them to come back...")) }
            link.reconnect()
            return
        }
        // 60 seconds to come back, then whoever stayed wins.
        link.reconnect()
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            for (t in 60 downTo 1) {
                _state.update { it.copy(lobby = it.lobby.copy(connected = false, reconnectSecondsLeft = t)) }
                delay(1000)
            }
            val battle = _state.value.battle ?: return@launch
            val mySide = _state.value.mySide
            val actions = Array<BattleAction>(2) { BattleAction.Move(0) }
            actions[1 - mySide] = BattleAction.Forfeit
            actions[mySide] = BattleEngine.legalActions(battle, mySide).first()
            val after = BattleEngine.resolve(battle, actions[0], actions[1])
            history += actions[0] to actions[1]
            _state.update {
                it.copy(
                    battle = after,
                    steps = listOf(PlaybackStep("${battle.sides[1 - mySide].name} disconnected. You win!", shownOf(after), EffectKind.END)),
                    lobby = it.lobby.copy(reconnectSecondsLeft = null),
                    awaiting = null,
                )
            }
            link.stop()
        }
    }

    private fun onMessage(m: BattleMessage) {
        when (m.type) {
            BattleMessage.HELLO -> _state.update { it.copy(lobby = it.lobby.copy(opponentName = m.trainer?.take(16))) }
            BattleMessage.TEAM -> {
                val team = m.team?.let { Snapshots.validate(it) }
                _state.update {
                    it.copy(lobby = it.lobby.copy(opponentTeam = team, error = if (team == null) "Their team didn't pass the rules check." else it.lobby.error))
                }
            }
            BattleMessage.CONFIG -> if (kind == BattleKind.JOIN) {
                hostSeed = m.seed
                _state.update { it.copy(format = m.format ?: 3, fairMode = m.fair ?: true) }
                if (_state.value.phase == BattlePhase.OVER && m.seed != null) {
                    begin(m.seed, mySide = 1) // rematch
                }
            }
            BattleMessage.READY -> {
                _state.update { it.copy(lobby = it.lobby.copy(themReady = true)) }
                maybeStartNearby()
            }
            BattleMessage.ACTION -> {
                val battle = _state.value.battle ?: return
                val action = m.action?.let { BattleAction.decode(it) } ?: return
                if (m.turn != battle.turn) return
                val theirSide = 1 - _state.value.mySide
                pending[theirSide] = action
                if (pending[_state.value.mySide] != null) resolveTurn()
            }
            BattleMessage.EMOTE -> m.emote?.takeIf { it in BattleLines.emotes }?.let { emote(1 - _state.value.mySide, it, send = false) }
            BattleMessage.REMATCH -> {
                rematchTheirs = true
                maybeRematch()
            }
            BattleMessage.LEAVE -> _state.update { it.copy(lobby = it.lobby.copy(error = "They left the room.")) }
        }
    }

    private fun maybeStartNearby() {
        val s = _state.value
        val lobby = s.lobby
        val theirs = lobby.opponentTeam ?: return
        val seed = hostSeed ?: return
        if (!lobby.meReady || !lobby.themReady) return
        val mine = snapshotsOf(s.picked).take(s.format)
        val them = theirs.take(s.format)
        val mySide = if (kind == BattleKind.HOST) 0 else 1
        teams = if (mySide == 0) listOf(mine, them) else listOf(them, mine)
        begin(seed, mySide)
    }

    override fun onCleared() {
        timerJob?.cancel()
        if (kind == BattleKind.HOST || kind == BattleKind.JOIN) link.stop()
    }

    companion object {
        private val CPU_NAMES = listOf("Rival Rami", "Cousin Karim", "Coach Dina", "Gym Bro Gary", "Neighbor Nour", "Uni Rival Sam")
        private val CPU_BROS = listOf("Zizo", "Hamada", "Tarek", "Mido", "Sherif", "Bassel", "Yara", "Lina", "Omar B.", "Kiki", "Adam", "Jojo")
    }
}

/** Battle loadout helper used by the moves sheet. */
fun Bro.withLoadout(equipped: List<String>, customs: List<CustomMoveSpec>): Bro =
    copy(battle = (battle ?: BattleLoadout()).copy(equipped = equipped, customs = customs))
