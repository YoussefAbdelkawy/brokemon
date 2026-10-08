package com.joecode.brokemon.ui.catchbro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.google.gson.Gson
import com.joecode.brokemon.data.EventClock
import com.joecode.brokemon.data.UserPrefs
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import com.joecode.brokemon.domain.SeasonEvents
import java.time.LocalDate
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
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

data class CaughtResult(val id: Long, val name: String, val isShiny: Boolean, val eventLabel: String? = null)

data class BroState(
    val name: String = "",
    val catchLocation: String = "",
    val habitat: String = "",
    val flavorText: String = "",
    val type1: BroType? = null,
    val type2: BroType? = null,
    val rarity: Rarity = Rarity.COMMON,
    val stats: BroStats = BroStats(),
    val selectedMoves: List<String> = emptyList(),
    val customMoveInput: String = "",
    val avatarSeed: Long = 0L,
    val look: BroLook = BroLook(),
    val isSaving: Boolean = false,
    val caught: CaughtResult? = null,
    val step: CatchStep = CatchStep.NAME,
    /** A saved draft waiting for "Continue catching X?". */
    val offeredDraft: CatchDraft? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && type1 != null && !isSaving
    val movesFull: Boolean get() = selectedMoves.size >= MAX_MOVES

    companion object {
        /** Picked at catch; evolution teaches up to two more. */
        const val MAX_MOVES = 4
        const val MAX_NAME = QrCodec.MAX_NAME
        const val MAX_MOVE_LENGTH = QrCodec.MAX_MOVE_LENGTH
    }
}

/** The five steps of the catch wizard. */
enum class CatchStep(val title: String) {
    NAME("Name"), TYPES("Types"), AVATAR("Avatar"), MOVES("Moves"), REVEAL("Reveal");

    val next: CatchStep? get() = entries.getOrNull(ordinal + 1)
    val previous: CatchStep? get() = entries.getOrNull(ordinal - 1)
}

/** What gets auto-saved while the wizard is open, so a closed app never loses a half-made bro. */
data class CatchDraft(
    val name: String = "",
    val location: String = "",
    val habitat: String = "",
    val flavor: String = "",
    val type1: String? = null,
    val type2: String? = null,
    val rarity: String = Rarity.COMMON.name,
    val stats: List<Int> = emptyList(),
    val moves: List<String> = emptyList(),
    val look: List<Int> = emptyList(),
    val avatarSeed: Long = 0L,
    val step: String = CatchStep.NAME.name,
)

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

@OptIn(FlowPreview::class)
class BroViewModel(
    private val repository: BroRepository,
    private val events: EventClock,
    private val prefs: UserPrefs? = null,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val gson = Gson()

    private val _state = MutableStateFlow(
        random.nextLong().let { seed ->
            BroState(stats = BroStats.random(random), avatarSeed = seed, look = BroLook.random(seed))
        },
    )
    val state: StateFlow<BroState> = _state.asStateFlow()

    init {
        // Offer last time's draft, then auto-save every change (debounced) while the wizard is open.
        viewModelScope.launch {
            val saved = prefs?.catchDraftOnce()?.let { runCatching { gson.fromJson(it, CatchDraft::class.java) }.getOrNull() }
            if (saved != null && saved.name.isNotBlank()) _state.update { it.copy(offeredDraft = saved) }
            _state.drop(1).debounce(400).collect { st ->
                if (st.caught != null || st.isSaving) return@collect
                if (st.offeredDraft != null) return@collect // don't overwrite the draft before they choose
                prefs?.setCatchDraft(if (st.name.isBlank()) null else gson.toJson(st.toDraft()))
            }
        }
    }

    private fun BroState.toDraft() = CatchDraft(
        name, catchLocation, habitat, flavorText, type1?.name, type2?.name, rarity.name,
        stats.toArray().toList(), selectedMoves, look.toList(), avatarSeed, step.name,
    )

    fun continueDraft() = _state.update { st ->
        val d = st.offeredDraft ?: return@update st
        st.copy(
            name = d.name, catchLocation = d.location, habitat = d.habitat, flavorText = d.flavor,
            type1 = d.type1?.let(BroType::from), type2 = d.type2?.let(BroType::from),
            rarity = Rarity.entries.firstOrNull { it.name == d.rarity } ?: Rarity.COMMON,
            stats = if (d.stats.size == BroStats().toArray().size) BroStats.fromArray(d.stats) else st.stats,
            selectedMoves = d.moves, look = if (d.look.isEmpty()) st.look else BroLook.fromList(d.look),
            avatarSeed = d.avatarSeed, step = CatchStep.entries.firstOrNull { it.name == d.step } ?: CatchStep.NAME,
            offeredDraft = null,
        )
    }

    fun discardDraft() {
        _state.update { it.copy(offeredDraft = null) }
        viewModelScope.launch { prefs?.setCatchDraft(null) }
    }

    fun goNext() = _state.update { st -> st.step.next?.takeIf { canLeave(st) }?.let { st.copy(step = it) } ?: st }
    fun goBack(): Boolean {
        val prev = _state.value.step.previous ?: return false
        _state.update { it.copy(step = prev) }
        return true
    }

    /** Name is required to leave step 1; a type is required to leave step 2. */
    private fun canLeave(st: BroState) = when (st.step) {
        CatchStep.NAME -> st.name.isNotBlank()
        CatchStep.TYPES -> st.type1 != null
        else -> true
    }

    /** Quick Catch: only a name. Type defaults to Chill Guy; everything else can be filled in later. */
    fun quickCatch() {
        if (_state.value.name.isBlank()) return
        _state.update { it.copy(type1 = it.type1 ?: BroType.CHILL_GUY, selectedMoves = emptyList()) }
        saveBro()
    }

    fun onNameChanged(value: String) = _state.update { it.copy(name = value.take(BroState.MAX_NAME)) }

    fun onLocationChanged(value: String) = _state.update { it.copy(catchLocation = value.take(40)) }

    fun onHabitatChanged(value: String) = _state.update { it.copy(habitat = value.take(Bro.MAX_HABITAT)) }

    fun onFlavorChanged(value: String) = _state.update { it.copy(flavorText = value.take(Bro.MAX_FLAVOR)) }

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

    fun randomizeLook() = _state.update { it.copy(look = BroLook.random(random.nextLong())) }

    fun onLookChanged(look: BroLook) = _state.update { it.copy(look = look) }

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
            // Shinies are earned later through memories (ShinyHunt), never rolled at catch.
            val isShiny = false
            // Caught during a limited event? Stamp the event frame on forever.
            val eventFrame = events.now()?.let { SeasonEvents.stamp(it, LocalDate.now()) }
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
                look = current.look,
                eventFrame = eventFrame,
                flavorText = current.flavorText.trim(),
                habitat = current.habitat.trim().ifBlank { null },
            )
            val id = repository.insert(bro)
            prefs?.setCatchDraft(null)
            val eventLabel = SeasonEvents.parse(eventFrame)?.label
            _state.update { it.copy(isSaving = false, caught = CaughtResult(id, bro.name, isShiny, eventLabel)) }
        }
    }

}
