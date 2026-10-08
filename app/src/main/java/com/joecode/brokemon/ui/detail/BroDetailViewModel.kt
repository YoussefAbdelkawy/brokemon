package com.joecode.brokemon.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.CheckInResult
import com.joecode.brokemon.data.MediaStorage
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.data.model.MediaType
import com.joecode.brokemon.data.model.Memory
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionInfo
import com.joecode.brokemon.domain.EvolutionMoves
import com.joecode.brokemon.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import com.joecode.brokemon.data.BroOrder
import kotlinx.coroutines.flow.MutableStateFlow
import com.joecode.brokemon.domain.ShinyHunt
import com.joecode.brokemon.data.model.RegionalDex
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DetailUiState(
    val isLoading: Boolean = true,
    val bro: Bro? = null,
    val evolution: EvolutionInfo? = null,
)

data class EvolutionEvent(val fromStage: Int, val toStage: Int, val learnedMoves: List<String> = emptyList())

class BroDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: BroRepository,
    private val media: MediaStorage,
    private val prefs: UserPrefs,
    private val order: BroOrder,
) : ViewModel() {

    /** The bro on screen. Swiping left/right through the dex changes it without leaving the screen. */
    private val broIdFlow = MutableStateFlow<Long>(checkNotNull(savedStateHandle[Routes.ARG_BRO_ID]))
    private val broId: Long get() = broIdFlow.value
    val currentBroId: StateFlow<Long> = broIdFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> = broIdFlow.flatMapLatest { repository.bro(it) }
        .map { bro -> DetailUiState(isLoading = false, bro = bro, evolution = bro?.let { Evolution.info(it) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    /** Position in the current dex view, e.g. 3 of 12; null if this bro isn't in it. */
    val position: StateFlow<Pair<Int, Int>?> = kotlinx.coroutines.flow.combine(broIdFlow, order.ids) { id, _ -> order.position(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** +1 = next bro, -1 = previous. Returns false if there's nobody to swipe to. */
    fun swipe(step: Int): Boolean {
        val next = order.neighbor(broId, step) ?: return false
        _evolutionEvent.value = null
        _captionTarget.value = null
        savedStateHandle[Routes.ARG_BRO_ID] = next
        broIdFlow.value = next
        return true
    }

    private val _evolutionEvent = MutableStateFlow<EvolutionEvent?>(null)
    val evolutionEvent: StateFlow<EvolutionEvent?> = _evolutionEvent.asStateFlow()

    /** Set right after a capture/import so the screen can ask for a caption. */
    private val _captionTarget = MutableStateFlow<Memory?>(null)
    val captionTarget: StateFlow<Memory?> = _captionTarget.asStateFlow()

    init {
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            broIdFlow.flatMapLatest { repository.bro(it) }.filterNotNull().collect { bro ->
                val stage = Evolution.info(bro).stage.ordinal
                val seen = prefs.seenStage(bro.id)
                when {
                    stage > seen && _evolutionEvent.value == null -> {
                        // Evolving teaches a bonus move per stage gained.
                        val learned = EvolutionMoves.unlocked(bro.primaryType, seen, stage).filterNot { it in bro.moves }
                        _evolutionEvent.value = EvolutionEvent(seen, stage, learned)
                        if (learned.isNotEmpty()) repository.update(bro.copy(moves = bro.moves + learned))
                    }
                    stage < seen -> prefs.setSeenStage(bro.id, stage)
                }
            }
        }
    }

    fun onEvolutionFinished() {
        val event = _evolutionEvent.value ?: return
        _evolutionEvent.value = null
        viewModelScope.launch { prefs.setSeenStage(broId, event.toStage) }
    }

    private fun edit(transform: (Bro) -> Bro) {
        viewModelScope.launch {
            val bro = repository.findBro(broId) ?: return@launch
            repository.update(transform(bro))
        }
    }

    fun checkIn(onResult: (CheckInResult) -> Unit) {
        viewModelScope.launch { onResult(repository.checkIn(broId)) }
    }

    fun addMoves(moves: List<String>) = edit { b ->
        b.copy(moves = (b.moves + moves.map { it.trim() }.filter { it.isNotEmpty() }).distinctBy { it.lowercase() }.take(com.joecode.brokemon.ui.catchbro.BroState.MAX_MOVES + 2))
    }

    fun setTradeable(tradeable: Boolean) = edit { it.copy(isTradeable = tradeable) }

    fun setRealMeetDate(millis: Long?) = edit { it.copy(realMeetDate = millis) }

    fun setRarity(rarity: Rarity) = edit { it.copy(rarity = rarity) }

    fun setLook(look: BroLook) = edit { it.copy(look = look) }

    fun setFlavorText(text: String) = edit { it.copy(flavorText = text.trim().take(Bro.MAX_FLAVOR)) }

    fun rename(name: String) {
        val clean = name.trim().take(24)
        if (clean.isNotEmpty()) edit { it.copy(name = clean) }
    }

    fun addFact(category: String, value: String, monthDay: String? = null) {
        val c = category.trim().take(32)
        val v = value.trim().take(80)
        if (c.isEmpty() || v.isEmpty()) return
        edit { bro ->
            // Only one birthday per bro: a new one replaces the old.
            val kept = if (monthDay != null) bro.facts.filterNot { it.monthDay != null } else bro.facts
            bro.copy(facts = kept + Fact(category = c, value = v, monthDay = monthDay))
        }
    }

    fun removeFact(fact: Fact) = edit { bro -> bro.copy(facts = bro.facts.filterNot { it.id == fact.id }) }

    // --- Memories -----------------------------------------------------------

    /** Creates the file the camera will write into and remembers it across process death. */
    fun prepareCapture(type: MediaType): Uri {
        val (file, uri) = media.newCaptureTarget(type)
        savedStateHandle[KEY_PENDING_PATH] = file.path
        savedStateHandle[KEY_PENDING_TYPE] = type.name
        return uri
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCaptureResult(success: Boolean) {
        val path = savedStateHandle.get<String>(KEY_PENDING_PATH) ?: return
        val type = savedStateHandle.get<String>(KEY_PENDING_TYPE)?.let { MediaType.valueOf(it) } ?: return
        savedStateHandle.remove<String>(KEY_PENDING_PATH)
        savedStateHandle.remove<String>(KEY_PENDING_TYPE)
        val file = File(path)
        // Some camera apps return "cancelled" for video even after recording, so trust the file.
        if (file.length() > 0L) addMemory(file, type) else media.discardIfEmpty(file)
    }

    fun onPicked(uri: Uri, isVideo: Boolean) {
        val type = if (isVideo) MediaType.VIDEO else MediaType.PHOTO
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { media.importFromPicker(uri, type) } ?: return@launch
            addMemory(file, type)
        }
    }

    fun newVoiceFile(prefix: String = ""): File = media.newAudioFile(prefix)

    fun addVoiceMemory(file: File) = addMemory(file, MediaType.AUDIO)

    /** Replaces the bro's voice line, deleting the old recording. */
    fun setVoiceLine(file: File?) {
        viewModelScope.launch {
            val bro = repository.findBro(broId) ?: return@launch
            bro.voiceLine?.let { media.delete(it) }
            repository.update(bro.copy(voiceLine = file?.let { Uri.fromFile(it).toString() }))
        }
    }

    private fun addMemory(file: File, type: MediaType) {
        val memory = Memory(fileUri = Uri.fromFile(file).toString(), mediaType = type)
        _captionTarget.value = memory
        viewModelScope.launch {
            val bro = repository.findBro(broId) ?: return@launch
            // Every memory is a chance to go shiny (one roll per bro per day).
            val today = LocalDate.now().toEpochDay()
            var turnedShiny = false
            if (ShinyHunt.canRoll(bro.isShiny, prefs.lastShinyRoll(broId), today)) {
                prefs.setLastShinyRoll(broId, today)
                turnedShiny = ShinyHunt.roll()
            }
            repository.update(bro.copy(memories = bro.memories + memory, isShiny = bro.isShiny || turnedShiny))
            if (turnedShiny) {
                prefs.setShinyEarned()
                _shinyMoment.value = true
            }
        }
    }

    private val _shinyMoment = MutableStateFlow(false)
    val shinyMoment: StateFlow<Boolean> = _shinyMoment.asStateFlow()

    fun dismissShinyMoment() {
        _shinyMoment.value = false
    }

    /** Each regional dex and whether this bro is in it. */
    val dexMembership: StateFlow<List<Pair<RegionalDex, Boolean>>> =
        kotlinx.coroutines.flow.combine(repository.dexes, repository.dexRefs) { dexes, refs ->
            dexes.map { dex -> dex to refs.any { it.dexId == dex.id && it.broId == broId } }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setInDex(dexId: Long, member: Boolean) {
        viewModelScope.launch { if (member) repository.addToDex(broId, dexId) else repository.removeFromDex(broId, dexId) }
    }

    fun createDexWithBro(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createDex(name, colorIndex = dexMembership.value.size)
            repository.addToDex(broId, id)
        }
    }

    fun setHabitat(value: String) = edit { it.copy(habitat = value.trim().take(Bro.MAX_HABITAT).ifBlank { null }) }

    fun setCaption(memory: Memory, caption: String) {
        _captionTarget.value = null
        val clean = caption.trim().take(140)
        edit { bro -> bro.copy(memories = bro.memories.map { if (it.id == memory.id) it.copy(caption = clean) else it }) }
    }

    fun dismissCaption() {
        _captionTarget.value = null
    }

    fun deleteMemory(memory: Memory) {
        edit { bro -> bro.copy(memories = bro.memories.filterNot { it.id == memory.id }) }
        media.delete(memory.fileUri)
    }

    fun shareableUri(memory: Memory): Uri? = media.shareableUri(memory.fileUri)

    fun deleteBro(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.findBro(broId)?.let { repository.delete(it) }
            onDeleted()
        }
    }

    private companion object {
        const val KEY_PENDING_PATH = "pending_capture_path"
        const val KEY_PENDING_TYPE = "pending_capture_type"
    }
}
