package com.joecode.brokemon.ui.engage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.Wrapped
import com.joecode.brokemon.domain.WrappedSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Year

data class WrappedUiState(
    val isLoading: Boolean = true,
    val years: List<Int> = emptyList(),
    val selectedYear: Int = Year.now().value,
    val summary: WrappedSummary? = null,
    val stages: Map<Long, EvolutionStage> = emptyMap(),
)

class WrappedViewModel(repository: BroRepository) : ViewModel() {
    private val selectedYear = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<WrappedUiState> = combine(repository.bros, selectedYear) { bros, chosen ->
        val years = (Wrapped.availableYears(bros) + Year.now().value).distinct().sortedDescending()
        val year = chosen ?: years.first()
        WrappedUiState(
            isLoading = false,
            years = years,
            selectedYear = year,
            summary = Wrapped.summarize(bros, year),
            stages = bros.associate { it.id to Evolution.info(it).stage },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WrappedUiState())

    fun selectYear(year: Int) {
        selectedYear.value = year
    }
}
