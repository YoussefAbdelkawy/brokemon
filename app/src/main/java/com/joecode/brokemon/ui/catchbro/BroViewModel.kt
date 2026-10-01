package com.joecode.brokemon.ui.catchbro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.share.QrCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CaughtResult(val id: Long, val name: String, val isShiny: Boolean)

data class BroState(
    val name: String = "",
    val catchLocation: String = "",
    val type1: BroType? = null,
    val type2: BroType? = null,
    val rarity: Rarity = Rarity.COMMON,
    val stats: BroStats = BroStats(),
    val selectedMoves: List<String> = emptyList(),
    val customMoveInput: String = "",
    val avatarSeed: Long = 0L,
    val isSaving: Boolean = false,
    val caught: CaughtResult? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && type1 != null && !isSaving
    val movesFull: Boolean get() = selectedMoves.size >= MAX_MOVES

    companion object {
        const val MAX_MOVES = QrCodec.MAX_MOVES
        const val MAX_NAME = QrCodec.MAX_NAME
        const val MAX_MOVE_LENGTH = QrCodec.MAX_MOVE_LENGTH
    }
}

object PresetMoves {
    val all = listOf(
        "Aux Cord Takeover",
        "Group Chat Spam",
        "Late-Night Drive",
        "Hype Speech",
        "Meme Barrage",
        "Snack Run",
        "Roast Session",
        "Clutch Assist",
        "Deep Talk",
        "Wingman Assist",
        "Ghost Mode",
        "Plan Cancel",
        "Road Trip Rally",
        "Couch Crash",
        "Spot Me",
        "Playlist Drop",
    )
}

class BroViewModel(
    private val repository: BroRepository,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BroState(stats = BroStats.random(random), avatarSeed = random.nextLong()),
    )
    val state: StateFlow<BroState> = _state.asStateFlow()

    fun onNameChanged(value: String) = _state.update { it.copy(name = value.take(BroState.MAX_NAME)) }

    fun onLocationChanged(value: String) = _state.update { it.copy(catchLocation = value.take(40)) }

    fun onType1Selected(type: BroType) = _state.update {
        it.copy(type1 = type, type2 = it.type2.takeUnless { t -> t == type })
    }

    /** Tapping the selected secondary type again clears it. */
    fun onType2Selected(type: BroType?) = _state.update {
        if (type == it.type1) it else it.copy(type2 = if (it.type2 == type) null else type)
    }

    fun onRarityChanged(rarity: Rarity) = _state.update { it.copy(rarity = rarity) }

    fun onStatChanged(index: Int, value: Int) = _state.update {
        val values = it.stats.toArray().toMutableList()
        values[index] = value
        it.copy(stats = BroStats.fromArray(values))
    }

    fun rerollStats() = _state.update { it.copy(stats = BroStats.random(random)) }

    fun rerollAvatar() = _state.update { it.copy(avatarSeed = random.nextLong()) }

    fun onMoveToggled(move: String) = _state.update {
        when {
            move in it.selectedMoves -> it.copy(selectedMoves = it.selectedMoves - move)
            it.movesFull -> it
            else -> it.copy(selectedMoves = it.selectedMoves + move)
        }
    }

    fun onCustomMoveInputChanged(value: String) =
        _state.update { it.copy(customMoveInput = value.take(BroState.MAX_MOVE_LENGTH)) }

    fun addCustomMove() = _state.update {
        val move = it.customMoveInput.trim()
        val duplicate = it.selectedMoves.any { m -> m.equals(move, ignoreCase = true) }
        if (move.isEmpty() || duplicate || it.movesFull) it
        else it.copy(selectedMoves = it.selectedMoves + move, customMoveInput = "")
    }

    fun saveBro() {
        val current = _state.value
        val type1 = current.type1 ?: return
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val isShiny = random.nextInt(SHINY_ODDS) == 0
            val bro = Bro(
                name = current.name.trim(),
                type1 = type1.name,
                type2 = current.type2?.name,
                stats = current.stats,
                moves = current.selectedMoves,
                catchLocation = current.catchLocation.trim(),
                rarity = current.rarity,
                isShiny = isShiny,
                catchDate = System.currentTimeMillis(),
                checkInCount = 0,
                memories = emptyList(),
                facts = emptyList(),
                avatarSeed = current.avatarSeed,
            )
            val id = repository.insert(bro)
            _state.update { it.copy(isSaving = false, caught = CaughtResult(id, bro.name, isShiny)) }
        }
    }

    companion object {
        /** 1 in 10 catches is shiny. Purely cosmetic. */
        const val SHINY_ODDS = 10
    }
}
