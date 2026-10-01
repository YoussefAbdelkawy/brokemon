package com.joecode.brokemon.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.joecode.brokemon.AppContainer
import com.joecode.brokemon.BrokemonApplication
import com.joecode.brokemon.ui.catchbro.BroViewModel
import com.joecode.brokemon.ui.detail.BroDetailViewModel
import com.joecode.brokemon.ui.engage.CheckOnBroViewModel
import com.joecode.brokemon.ui.engage.WrappedViewModel
import com.joecode.brokemon.ui.home.HomeViewModel
import com.joecode.brokemon.ui.settings.SettingsViewModel
import com.joecode.brokemon.ui.squads.SquadDetailViewModel
import com.joecode.brokemon.ui.squads.SquadsViewModel
import com.joecode.brokemon.ui.trade.TradeViewModel

/** One factory for every screen's ViewModel; dependencies come from [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(container().repository) }
        initializer { BroViewModel(container().repository) }
        initializer {
            BroDetailViewModel(createSavedStateHandle(), container().repository, container().media, container().prefs)
        }
        initializer { SquadsViewModel(container().repository) }
        initializer { SquadDetailViewModel(createSavedStateHandle(), container().repository) }
        initializer { TradeViewModel(container().repository) }
        initializer { CheckOnBroViewModel(container().repository, container().prefs) }
        initializer { WrappedViewModel(container().repository) }
        initializer { SettingsViewModel(container().repository, container().backup, container().prefs) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as BrokemonApplication).container
