package com.joecode.brokemon.ui.engage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.domain.CheckInPrompts
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.ui.home.DexEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckOnUiState(
    val isLoading: Boolean = true,
    val pick: DexEntry? = null,
    val daysSince: Long = 0,
    val prompts: List<String> = emptyList(),
    val canReroll: Boolean = false,
    val checkedIn: Boolean = false,
)

class CheckOnBroViewModel(
    private val repository: BroRepository,
    private val prefs: UserPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckOnUiState())
    val state: StateFlow<CheckOnUiState> = _state.asStateFlow()

    init {
        recommend()
    }

    fun recommend() {
        viewModelScope.launch {
            val bros = repository.allBrosOnce()
            val pick = CheckOnBro.recommend(bros, prefs.lastRecommendedBroId())
            pick?.let { prefs.setLastRecommendedBroId(it.id) }
            _state.value = CheckOnUiState(
                isLoading = false,
                pick = pick?.let { DexEntry(it, Evolution.info(it).stage) },
                daysSince = pick?.let { CheckOnBro.daysSinceContact(it) } ?: 0,
                prompts = pick?.let { CheckInPrompts.forBro(it) }.orEmpty(),
                canReroll = bros.size > 1,
            )
        }
    }

    fun checkIn() {
        val bro = _state.value.pick?.bro ?: return
        viewModelScope.launch {
            val fresh = repository.findBro(bro.id) ?: return@launch
            repository.update(fresh.copy(checkInCount = fresh.checkInCount + 1, lastCheckIn = System.currentTimeMillis()))
            _state.update { it.copy(checkedIn = true) }
        }
    }
}
