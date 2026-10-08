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
import com.joecode.brokemon.ui.room.RoomViewModel
import com.joecode.brokemon.ui.settings.SettingsViewModel
import com.joecode.brokemon.ui.squads.SquadDetailViewModel
import com.joecode.brokemon.ui.squads.SquadsViewModel
import com.joecode.brokemon.ui.trade.TradeViewModel
import com.joecode.brokemon.ui.trainer.TrainerViewModel
import com.joecode.brokemon.ui.wild.WildBroViewModel
import com.joecode.brokemon.battle.NearbyLink
import com.joecode.brokemon.ui.battle.BattleRecordsViewModel
import com.joecode.brokemon.ui.battle.BattleViewModel
import com.joecode.brokemon.ui.battle.TournamentsViewModel

/** One factory for every screen's ViewModel; dependencies come from [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(container().repository, container().events, container().prefs, container().broOrder) }
        initializer { BroViewModel(container().repository, container().events, container().prefs) }
        initializer {
            BroDetailViewModel(createSavedStateHandle(), container().repository, container().media, container().prefs, container().broOrder)
        }
        initializer { com.joecode.brokemon.ui.pack.PackViewModel(container().prefs) }
        initializer { SquadsViewModel(container().repository) }
        initializer { SquadDetailViewModel(createSavedStateHandle(), container().repository) }
        initializer { TradeViewModel(container().repository, container().prefs) }
        initializer { CheckOnBroViewModel(container().repository, container().prefs) }
        initializer { WrappedViewModel(container().repository) }
        initializer { RoomViewModel(createSavedStateHandle(), container().repository, container().prefs) }
        initializer { TrainerViewModel(container().repository, container().prefs) }
        initializer { WildBroViewModel(container().repository, container().prefs) }
        initializer {
            val app = this[APPLICATION_KEY] as BrokemonApplication
            BattleViewModel(createSavedStateHandle(), container().repository, container().prefs) { NearbyLink(app) }
        }
        initializer { BattleRecordsViewModel(container().repository, container().prefs) }
        initializer { TournamentsViewModel(createSavedStateHandle(), container().repository, container().prefs) }
        initializer { SettingsViewModel(container().repository, container().backup, container().prefs) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as BrokemonApplication).container
