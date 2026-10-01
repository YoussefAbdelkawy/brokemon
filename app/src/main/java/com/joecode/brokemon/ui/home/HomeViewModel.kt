package com.joecode.brokemon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class DexEntry(val bro: Bro, val stage: EvolutionStage)

data class HomeUiState(
    val isLoading: Boolean = true,
    val entries: List<DexEntry> = emptyList(),
    val totalCaught: Int = 0,
    val shinyCount: Int = 0,
    val legendaryCount: Int = 0,
    val query: String = "",
    val typeFilter: BroType? = null,
)

class HomeViewModel(repository: BroRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<BroType?>(null)

    val uiState: StateFlow<HomeUiState> = combine(repository.bros, query, typeFilter) { bros, q, type ->
        val filtered = bros.filter { bro ->
            (q.isBlank() || bro.name.contains(q.trim(), ignoreCase = true)) &&
                (type == null || type in bro.types)
        }
        HomeUiState(
            isLoading = false,
            entries = filtered.map { DexEntry(it, Evolution.info(it).stage) },
            totalCaught = bros.size,
            shinyCount = bros.count { it.isShiny },
            legendaryCount = bros.count { it.rarity == Rarity.LEGENDARY },
            query = q,
            typeFilter = type,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChanged(value: String) = query.update { value }

    fun onTypeFilterSelected(type: BroType?) = typeFilter.update { if (it == type) null else type }
}
