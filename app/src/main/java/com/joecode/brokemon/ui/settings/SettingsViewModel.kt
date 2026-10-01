package com.joecode.brokemon.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: BroRepository) : ViewModel() {
    fun wipeEverything(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.wipeEverything()
            onDone()
        }
    }
}
