package com.joecode.brokemon.ui.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.CheckInResult
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroRoom
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoomUiState(
    val isLoading: Boolean = true,
    val bro: Bro? = null,
    val stage: EvolutionStage = EvolutionStage.ROOKIE,
)

class RoomViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BroRepository,
    prefs: UserPrefs,
) : ViewModel() {
    private val broId: Long = checkNotNull(savedStateHandle[Routes.ARG_BRO_ID])

    val uiState: StateFlow<RoomUiState> = repository.bro(broId)
        .map { bro -> RoomUiState(false, bro, bro?.let { Evolution.info(it).stage } ?: EvolutionStage.ROOKIE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoomUiState())

    init {
        viewModelScope.launch { prefs.setRoomHintSeen() }
    }

    fun hangOut(onResult: (CheckInResult) -> Unit) {
        viewModelScope.launch { onResult(repository.checkIn(broId)) }
    }

    fun setRoom(room: BroRoom) {
        viewModelScope.launch {
            val bro = repository.findBro(broId) ?: return@launch
            repository.update(bro.copy(room = room))
        }
    }
}
