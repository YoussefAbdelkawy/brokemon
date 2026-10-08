package com.joecode.brokemon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.DexView
import com.joecode.brokemon.data.EventClock
import com.joecode.brokemon.ui.trainer.TrainerProgress
import com.joecode.brokemon.ui.trainer.trainerProgressFlow
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.data.model.RegionalDex
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.SeasonEvent
import com.joecode.brokemon.domain.Wrapped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Year

/** A bro as shown in the current dex: [number] is the National number or the regional one. */
data class DexEntry(val bro: Bro, val stage: EvolutionStage, val number: String = bro.dexNumber)

enum class SortOrder(val label: String) {
    DEX("Dex number"),
    NAME("Name A–Z"),
    NEWEST("Newest catches"),
    NEEDS_CHECK_IN("Needs a check-in"),
    BOND("Strongest bond"),
    RARITY("Rarest first"),
}

/** Everything besides the name search. Empty sets mean "any". */
data class BroFilter(
    val types: Set<BroType> = emptySet(),
    val rarities: Set<Rarity> = emptySet(),
    val stages: Set<EvolutionStage> = emptySet(),
    val shinyOnly: Boolean = false,
    val limitedOnly: Boolean = false,
    val tradeableOnly: Boolean = false,
    val sort: SortOrder = SortOrder.DEX,
) {
    val activeCount: Int
        get() = types.size + rarities.size + stages.size +
            listOf(shinyOnly, limitedOnly, tradeableOnly).count { it } + (if (sort != SortOrder.DEX) 1 else 0)

    fun matches(entry: DexEntry): Boolean {
        val bro = entry.bro
        return (types.isEmpty() || bro.types.any { it in types }) &&
            (rarities.isEmpty() || bro.rarity in rarities) &&
            (stages.isEmpty() || entry.stage in stages) &&
            (!shinyOnly || bro.isShiny) &&
            (!limitedOnly || bro.eventFrame != null) &&
            (!tradeableOnly || bro.isTradeable)
    }

    fun sorted(entries: List<DexEntry>): List<DexEntry> = when (sort) {
        SortOrder.DEX -> entries.sortedBy { it.number }
        SortOrder.NAME -> entries.sortedBy { it.bro.name.lowercase() }
        SortOrder.NEWEST -> entries.sortedByDescending { it.bro.catchDate }
        SortOrder.NEEDS_CHECK_IN -> entries.sortedByDescending { CheckOnBro.daysSinceContact(it.bro) }
        SortOrder.BOND -> entries.sortedByDescending { Evolution.score(it.bro) }
        SortOrder.RARITY -> entries.sortedWith(compareByDescending<DexEntry> { it.bro.rarity.ordinal }.thenByDescending { it.bro.isShiny })
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val entries: List<DexEntry> = emptyList(),
    val totalCaught: Int = 0,
    val shinyCount: Int = 0,
    val legendaryCount: Int = 0,
    val query: String = "",
    val filter: BroFilter = BroFilter(),
    /** Only set during the New Year window (Dec 20 – Jan 15). */
    val wrappedYear: Int? = null,
    /** Limited event running right now (from the device date). */
    val event: SeasonEvent? = null,
    val showRoomHint: Boolean = false,
    /** Uncaught "???" slots shown after your bros, so the dex is never empty (#001–#006). */
    val mysterySlots: List<Long> = emptyList(),
    val view: DexView = DexView.CARDS,
    val progress: TrainerProgress = TrainerProgress(),
    /** All bros, whatever dex is selected (decides the first-run empty state). */
    val nationalCount: Int = 0,
    val dexes: List<RegionalDex> = emptyList(),
    /** Null = the National Dex (every bro). */
    val selectedDex: RegionalDex? = null,
)

/** The bros in the selected dex, each with its number in that dex. */
private data class DexScope(val dex: RegionalDex?, val dexes: List<RegionalDex>, val entries: List<DexEntry>, val national: Int)

class HomeViewModel(
    private val repository: BroRepository,
    events: EventClock,
    private val prefs: UserPrefs,
    private val broOrder: com.joecode.brokemon.data.BroOrder,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(BroFilter())

    private val selectedDexId = MutableStateFlow<Long?>(null)

    private val scope = combine(repository.bros, repository.dexes, repository.dexRefs, selectedDexId) { bros, dexes, refs, sel ->
        val dex = dexes.firstOrNull { it.id == sel }
        val entries = if (dex == null) {
            bros.map { DexEntry(it, Evolution.info(it).stage) }
        } else {
            val numbers = refs.filter { it.dexId == dex.id }.associate { it.broId to it.regionalNumber }
            bros.filter { it.id in numbers }.map { DexEntry(it, Evolution.info(it).stage, "#%03d".format(numbers.getValue(it.id))) }
        }
        DexScope(dex, dexes, entries, bros.size)
    }

    private val dexState =
        combine(scope, query, filter, events.current, prefs.roomHintSeen) { sc, q, f, event, hintSeen ->
            val all = sc.entries
            val bros = all.map { it.bro }
            val shown = all.filter { e -> (q.isBlank() || e.bro.name.contains(q.trim(), ignoreCase = true)) && f.matches(e) }
            val sorted = f.sorted(shown)
            // The detail screen swipes through exactly what the user is looking at.
            broOrder.set(sorted.map { it.bro.id })
            HomeUiState(
                isLoading = false,
                entries = sorted,
                totalCaught = bros.size,
                shinyCount = bros.count { it.isShiny },
                legendaryCount = bros.count { it.rarity == Rarity.LEGENDARY },
                query = q,
                filter = f,
                // Wrapped is a New Year thing only. (A forced New Year event in debug builds also opens it.)
                wrappedYear = Wrapped.seasonYear() ?: if (event == SeasonEvent.NEW_YEAR) Year.now().value else null,
                event = event,
                showRoomHint = !hintSeen && bros.isNotEmpty(),
                mysterySlots = if (q.isBlank() && f == BroFilter()) {
                    ((all.maxOfOrNull { it.number.drop(1).toLongOrNull() ?: 0L } ?: 0L) + 1..MIN_SLOTS).toList()
                } else emptyList(),
                nationalCount = sc.national,
                dexes = sc.dexes,
                selectedDex = sc.dex,
            )
        }

    val uiState: StateFlow<HomeUiState> =
        combine(dexState, trainerProgressFlow(repository, prefs), prefs.dexView) { state, progress, view ->
            state.copy(progress = progress, view = view)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectDex(id: Long?) = selectedDexId.update { id }

    fun createDex(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createDex(name, colorIndex = uiState.value.dexes.size)
            selectedDexId.update { id }
        }
    }

    fun renameDex(dex: RegionalDex, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameDex(dex, name) }
    }

    fun deleteDex(dex: RegionalDex) {
        viewModelScope.launch {
            repository.deleteDex(dex)
            selectedDexId.update { null }
        }
    }

    /** Puts [broIds] in the dex (new ones get the next numbers) and takes everyone else out. */
    fun setDexMembers(dex: RegionalDex, broIds: Set<Long>) {
        viewModelScope.launch {
            val current = repository.allDexRefsOnce().filter { it.dexId == dex.id }.map { it.broId }.toSet()
            (current - broIds).forEach { repository.removeFromDex(it, dex.id) }
            (broIds - current).sorted().forEach { repository.addToDex(it, dex.id) }
        }
    }

    val dexRefs: StateFlow<List<com.joecode.brokemon.data.model.BroDexCrossRef>> =
        repository.dexRefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every bro (National Dex), for the "add bros" picker. */
    val allBros: StateFlow<List<Bro>> = repository.bros.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setView(view: DexView) {
        viewModelScope.launch { prefs.setDexView(view) }
    }

    companion object {
        const val MIN_SLOTS = 6L
    }

    fun onQueryChanged(value: String) = query.update { value }

    fun onFilterChanged(value: BroFilter) = filter.update { value }

    fun clearFilters() = filter.update { BroFilter() }

    fun dismissRoomHint() {
        viewModelScope.launch { prefs.setRoomHintSeen() }
    }
}
