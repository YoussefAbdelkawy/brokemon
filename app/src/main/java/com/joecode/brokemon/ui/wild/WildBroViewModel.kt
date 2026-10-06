package com.joecode.brokemon.ui.wild

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.CheckInResult
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Trainer
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class WildState(
    val loading: Boolean = true,
    val bro: Bro? = null,
    val evolution: EvolutionInfo? = null,
    val trainer: Trainer? = null,
    /** Bumped for every new encounter so the battle intro replays. */
    val encounter: Int = 0,
    val result: CheckInResult? = null,
)

/**
 * Wild bro encounters: picks a random bro, leaning toward whoever you
 * haven't talked to in a while, and skipping anyone you already checked in
 * with today when possible.
 */
class WildBroViewModel(
    private val repository: BroRepository,
    private val prefs: UserPrefs,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(WildState())
    val state: StateFlow<WildState> = _state.asStateFlow()

    init {
        encounter()
    }

    fun encounter() {
        viewModelScope.launch {
            val bros = repository.allBrosOnce()
            val fresh = bros.filterNot { CheckOnBro.checkedInToday(it) }.ifEmpty { bros }
            val pick = CheckOnBro.recommend(fresh, lastRecommendedId = _state.value.bro?.id, random = random)
            _state.update {
                WildState(
                    loading = false,
                    bro = pick,
                    evolution = pick?.let(Evolution::info),
                    trainer = prefs.trainerOnce(),
                    encounter = it.encounter + 1,
                )
            }
        }
    }

    fun checkIn() {
        val bro = _state.value.bro ?: return
        viewModelScope.launch {
            val result = repository.checkIn(bro.id)
            val updated = repository.findBro(bro.id) ?: bro
            _state.update { it.copy(result = result, bro = updated, evolution = Evolution.info(updated)) }
        }
    }
}
