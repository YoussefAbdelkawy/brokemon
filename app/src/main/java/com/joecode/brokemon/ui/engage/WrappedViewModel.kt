package com.joecode.brokemon.ui.engage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.Wrapped
import com.joecode.brokemon.domain.WrappedSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Year

data class WrappedUiState(
    val isLoading: Boolean = true,
    val year: Int = Year.now().value,
    /** True outside the New Year window (only reachable in debug builds). */
    val isPreview: Boolean = false,
    val summary: WrappedSummary? = null,
    val stages: Map<Long, EvolutionStage> = emptyMap(),
)

class WrappedViewModel(repository: BroRepository) : ViewModel() {
    private val season = Wrapped.seasonYear()

    val uiState: StateFlow<WrappedUiState> = repository.bros.map { bros ->
        val year = season ?: Year.now().value
        WrappedUiState(
            isLoading = false,
            year = year,
            isPreview = season == null,
            summary = Wrapped.summarize(bros, year),
            stages = bros.associate { it.id to Evolution.info(it).stage },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WrappedUiState())
}
