package com.joecode.brokemon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.EventClock
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
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

data class DexEntry(val bro: Bro, val stage: EvolutionStage)

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
        SortOrder.DEX -> entries.sortedBy { it.bro.id }
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
)

class HomeViewModel(repository: BroRepository, events: EventClock, private val prefs: UserPrefs) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(BroFilter())

    val uiState: StateFlow<HomeUiState> =
        combine(repository.bros, query, filter, events.current, prefs.roomHintSeen) { bros, q, f, event, hintSeen ->
            val all = bros.map { DexEntry(it, Evolution.info(it).stage) }
            val shown = all.filter { e -> (q.isBlank() || e.bro.name.contains(q.trim(), ignoreCase = true)) && f.matches(e) }
            HomeUiState(
                isLoading = false,
                entries = f.sorted(shown),
                totalCaught = bros.size,
                shinyCount = bros.count { it.isShiny },
                legendaryCount = bros.count { it.rarity == Rarity.LEGENDARY },
                query = q,
                filter = f,
                // Wrapped is a New Year thing only. (A forced New Year event in debug builds also opens it.)
                wrappedYear = Wrapped.seasonYear() ?: if (event == SeasonEvent.NEW_YEAR) Year.now().value else null,
                event = event,
                showRoomHint = !hintSeen && bros.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChanged(value: String) = query.update { value }

    fun onFilterChanged(value: BroFilter) = filter.update { value }

    fun clearFilters() = filter.update { BroFilter() }

    fun dismissRoomHint() {
        viewModelScope.launch { prefs.setRoomHintSeen() }
    }
}
