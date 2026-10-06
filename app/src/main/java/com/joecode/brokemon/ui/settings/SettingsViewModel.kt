package com.joecode.brokemon.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.backup.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val busy: Boolean = false,
    val lastExport: Long? = null,
    val birthdayReminders: Boolean = true,
    val weeklyNudge: Boolean = true,
    /** Debug-only event override (see EventClock). */
    val debugEvent: String? = null,
    /** One-shot message for a snackbar. */
    val message: String? = null,
    /** Battle text speed: 0 slow, 1 normal, 2 fast. */
    val textSpeed: Int = 1,
)

class SettingsViewModel(
    private val repository: BroRepository,
    private val backup: BackupManager,
    private val prefs: UserPrefs,
) : ViewModel() {

    private val local = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> =
        combine(local, prefs.birthdayReminders, prefs.weeklyNudge, prefs.debugEvent, prefs.textSpeed) { s, birthdays, nudge, debugEvent, speed ->
            s.copy(birthdayReminders = birthdays, weeklyNudge = nudge, debugEvent = debugEvent, textSpeed = speed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch { local.update { it.copy(lastExport = prefs.lastExport()) } }
    }

    fun export(target: Uri) = runBusy {
        backup.export(target).fold(
            onSuccess = {
                val now = System.currentTimeMillis()
                prefs.setLastExport(now)
                local.update { s -> s.copy(lastExport = now) }
                "Backed up ${it.bros} bros and ${it.memories} memories."
            },
            onFailure = { "Backup failed: ${it.message ?: "unknown error"}" },
        )
    }

    fun restore(source: Uri) = runBusy {
        backup.restore(source).fold(
            onSuccess = { "Restored ${it.bros} bros and ${it.memories} memories." },
            onFailure = { "Restore failed: ${it.message ?: "unknown error"}" },
        )
    }

    fun setBirthdayReminders(on: Boolean) = viewModelScope.launch { prefs.setBirthdayReminders(on) }

    fun setWeeklyNudge(on: Boolean) = viewModelScope.launch { prefs.setWeeklyNudge(on) }

    fun setTextSpeed(value: Int) = viewModelScope.launch { prefs.setTextSpeed(value) }

    fun setDebugEvent(value: String?) = viewModelScope.launch { prefs.setDebugEvent(value) }

    fun messageShown() = local.update { it.copy(message = null) }

    fun wipeEverything(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.wipeEverything()
            onDone()
        }
    }

    private fun runBusy(block: suspend () -> String) {
        if (local.value.busy) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = block()
            local.update { it.copy(busy = false, message = message) }
        }
    }
}
