package com.joecode.brokemon.ui.battle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.BattleRecord
import com.joecode.brokemon.data.model.Tournament
import com.joecode.brokemon.domain.battle.BattleCodec
import com.joecode.brokemon.domain.battle.Brackets
import com.joecode.brokemon.domain.battle.RoomCodes
import com.joecode.brokemon.domain.battle.TournamentFormat
import com.joecode.brokemon.domain.battle.TournamentMatch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** A row on the private leaderboard: how you've done against one friend. */
data class RivalRow(val name: String, val wins: Int, val losses: Int, val streak: Int)

data class RecordsUiState(
    val records: List<BattleRecord> = emptyList(),
    val wins: Int = 0,
    val losses: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val seasonWins: Int = 0,
    val seasonLosses: Int = 0,
    val seasonLabel: String = "",
    val trophies: Int = 0,
    val mvpName: String? = null,
    val mvpCount: Int = 0,
    val rivals: List<RivalRow> = emptyList(),
)

/**
 * Local battle records and a private leaderboard built from them. Seasons
 * are calendar months: the season table resets on the 1st.
 */
class BattleRecordsViewModel(repository: BroRepository, prefs: UserPrefs) : ViewModel() {

    val state: StateFlow<RecordsUiState> = combine(repository.battleRecords, prefs.trophies) { records, trophies ->
        val decided = records.filter { it.won != null && it.mode != "LOCAL" }
        val chronological = decided.sortedBy { it.date }
        var streak = 0
        var best = 0
        chronological.forEach { r -> streak = if (r.won == true) streak + 1 else 0; best = maxOf(best, streak) }
        val season = YearMonth.now()
        fun monthOf(r: BattleRecord) = YearMonth.from(Instant.ofEpochMilli(r.date).atZone(ZoneId.systemDefault()))
        val inSeason = decided.filter { monthOf(it) == season }
        val mvp = decided.filter { it.won == true && it.mvpName.isNotBlank() }.groupingBy { it.mvpName }.eachCount().maxByOrNull { it.value }
        val rivals = decided.groupBy { it.opponentName }.map { (name, rs) ->
            var s = 0
            rs.sortedBy { it.date }.forEach { r -> s = if (r.won == true) s + 1 else 0 }
            RivalRow(name, rs.count { it.won == true }, rs.count { it.won == false }, s)
        }.sortedWith(compareByDescending<RivalRow> { it.wins - it.losses }.thenByDescending { it.wins })
        RecordsUiState(
            records = records,
            wins = decided.count { it.won == true },
            losses = decided.count { it.won == false },
            currentStreak = streak,
            bestStreak = best,
            seasonWins = inSeason.count { it.won == true },
            seasonLosses = inSeason.count { it.won == false },
            seasonLabel = season.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${season.year}",
            trophies = trophies,
            mvpName = mvp?.key,
            mvpCount = mvp?.value ?: 0,
            rivals = rivals,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState())
}

data class TournamentView(val tournament: Tournament, val format: TournamentFormat, val matches: List<TournamentMatch>) {
    val standings get() = Brackets.standings(tournament.players, matches)
}

class TournamentsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BroRepository,
    private val prefs: UserPrefs,
) : ViewModel() {
    private val id: Long = savedStateHandle.get<Long>("tournamentId") ?: 0L

    val tournaments: StateFlow<List<Tournament>> =
        repository.tournaments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val current: StateFlow<TournamentView?> = (if (id > 0) repository.tournament(id) else flowOf(null)).map { t ->
        t?.let {
            TournamentView(it, TournamentFormat.entries.firstOrNull { f -> f.name == it.format } ?: TournamentFormat.BRACKET, BattleCodec.matchesFromJson(it.matchesJson))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dexes = repository.dexes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val squads = repository.squads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bros = repository.bros.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dexRefs = repository.dexRefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun trainerName(): String = prefs.trainerOnce()?.name ?: "You"

    fun create(name: String, format: TournamentFormat, players: List<String>, dexId: Long?, onCreated: (Long) -> Unit) {
        val clean = players.map { it.trim().take(16) }.filter { it.isNotBlank() }.distinct()
        if (clean.size < 2) return
        viewModelScope.launch {
            val newId = repository.insertTournament(
                Tournament(
                    name = name.trim().ifBlank { "Bro Cup" }.take(24),
                    code = RoomCodes.generate(),
                    format = format.name,
                    players = clean,
                    matchesJson = BattleCodec.matchesToJson(Brackets.create(format, clean)),
                    dexId = dexId,
                ),
            )
            onCreated(newId)
        }
    }

    /** For matches played on two other phones: the organizer just taps who won. */
    fun recordWinner(match: TournamentMatch, winner: String) {
        val view = current.value ?: return
        viewModelScope.launch {
            val updated = Brackets.record(view.matches, match.round, match.index, winner)
            val champion = Brackets.champion(view.format, view.tournament.players, updated)
            repository.updateTournament(view.tournament.copy(matchesJson = BattleCodec.matchesToJson(updated), champion = champion))
            if (champion != null && champion == trainerName()) prefs.addTrophy()
        }
    }

    fun delete(onDone: () -> Unit) {
        val view = current.value ?: return
        viewModelScope.launch {
            repository.deleteTournament(view.tournament)
            onDone()
        }
    }

    suspend fun allOnce() = repository.tournaments.first()
}
