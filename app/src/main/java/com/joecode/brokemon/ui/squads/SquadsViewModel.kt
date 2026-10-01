package com.joecode.brokemon.ui.squads

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Squad
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.TypeMatchups
import com.joecode.brokemon.ui.home.DexEntry
import com.joecode.brokemon.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SquadSummary(val squad: Squad, val members: List<DexEntry>)

data class SquadsUiState(val isLoading: Boolean = true, val squads: List<SquadSummary> = emptyList())

class SquadsViewModel(private val repository: BroRepository) : ViewModel() {

    val uiState: StateFlow<SquadsUiState> = combine(repository.squads, repository.bros) { squads, bros ->
        val byId = bros.associateBy { it.id }
        SquadsUiState(
            isLoading = false,
            squads = squads.map { squad ->
                SquadSummary(squad, squad.memberIds.mapNotNull { byId[it] }.map { DexEntry(it, Evolution.info(it).stage) })
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SquadsUiState())

    fun createSquad(name: String, onCreated: (Long) -> Unit) {
        val clean = name.trim().take(24)
        if (clean.isEmpty()) return
        viewModelScope.launch { onCreated(repository.insertSquad(Squad(name = clean))) }
    }
}

data class SquadDetailUiState(
    val isLoading: Boolean = true,
    val squad: Squad? = null,
    val members: List<DexEntry> = emptyList(),
    val allBros: List<DexEntry> = emptyList(),
    val otherSquads: List<SquadSummary> = emptyList(),
    val report: List<TypeMatchups.TypeReport> = emptyList(),
    val rival: SquadSummary? = null,
    val clash: TypeMatchups.SquadClash? = null,
)

class SquadDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BroRepository,
) : ViewModel() {

    private val squadId: Long = checkNotNull(savedStateHandle[Routes.ARG_SQUAD_ID])
    private val rivalId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<SquadDetailUiState> =
        combine(repository.squad(squadId), repository.squads, repository.bros, rivalId) { squad, squads, bros, rid ->
            val byId = bros.associateBy { it.id }
            fun entries(ids: List<Long>) = ids.mapNotNull { byId[it] }.map { DexEntry(it, Evolution.info(it).stage) }
            val members = entries(squad?.memberIds.orEmpty())
            val others = squads.filter { it.id != squadId }.map { SquadSummary(it, entries(it.memberIds)) }
            val rival = others.firstOrNull { it.squad.id == rid }
            SquadDetailUiState(
                isLoading = false,
                squad = squad,
                members = members,
                allBros = bros.map { DexEntry(it, Evolution.info(it).stage) },
                otherSquads = others,
                report = TypeMatchups.squadReport(members.map { it.bro }),
                rival = rival,
                clash = rival?.takeIf { members.isNotEmpty() && it.members.isNotEmpty() }
                    ?.let { TypeMatchups.clash(members.map { m -> m.bro }, it.members.map { m -> m.bro }) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SquadDetailUiState())

    fun selectRival(id: Long?) {
        rivalId.value = id
    }

    fun setMembers(ids: List<Long>) = editSquad { it.copy(memberIds = ids.distinct().take(Squad.MAX_MEMBERS)) }

    fun removeMember(bro: Bro) = editSquad { it.copy(memberIds = it.memberIds - bro.id) }

    fun rename(name: String) {
        val clean = name.trim().take(24)
        if (clean.isNotEmpty()) editSquad { it.copy(name = clean) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            uiState.value.squad?.let { repository.deleteSquad(it) }
            onDeleted()
        }
    }

    private fun editSquad(transform: (Squad) -> Squad) {
        val squad = uiState.value.squad ?: return
        viewModelScope.launch { repository.updateSquad(transform(squad)) }
    }
}
