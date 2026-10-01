package com.joecode.brokemon.ui.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.share.QrCodec
import com.joecode.brokemon.ui.home.DexEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TradeUiState(
    val tradeable: List<DexEntry> = emptyList(),
    val allBros: List<Bro> = emptyList(),
    /** A decoded card waiting for the user to accept. */
    val incoming: Bro? = null,
    val error: String? = null,
)

class TradeViewModel(private val repository: BroRepository) : ViewModel() {

    private val incoming = MutableStateFlow<Bro?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TradeUiState> = combine(repository.bros, incoming, error) { bros, inc, err ->
        TradeUiState(
            tradeable = bros.filter { it.isTradeable }.map { DexEntry(it, Evolution.info(it).stage) },
            allBros = bros,
            incoming = inc,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TradeUiState())

    fun onScanned(raw: String?) {
        val bro = raw?.let { QrCodec.decode(it) }
        if (bro == null) {
            error.update { "That QR code isn't a Brokemon card." }
        } else {
            error.update { null }
            incoming.update { bro }
        }
    }

    fun onScanFailed(message: String) = error.update { message }

    fun clearError() = error.update { null }

    fun declineIncoming() = incoming.update { null }

    fun acceptIncoming(onSaved: (Long) -> Unit) {
        val bro = incoming.value ?: return
        incoming.update { null }
        viewModelScope.launch { onSaved(repository.insert(bro.copy(catchDate = System.currentTimeMillis()))) }
    }
}
