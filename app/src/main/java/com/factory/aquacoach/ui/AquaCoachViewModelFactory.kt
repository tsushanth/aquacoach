package com.factory.aquacoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.factory.aquacoach.AquaCoachApp
import com.factory.aquacoach.ui.history.HistoryViewModel
import com.factory.aquacoach.ui.home.HomeViewModel
import com.factory.aquacoach.ui.paywall.PaywallViewModel
import com.factory.aquacoach.ui.settings.SettingsViewModel

class AquaCoachViewModelFactory(private val app: AquaCoachApp) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(app.waterRepository, app.settingsRepository, app.premiumManager) as T

            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(app.waterRepository, app.settingsRepository, app.premiumManager) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app.settingsRepository, app.premiumManager, app.applicationContext) as T

            modelClass.isAssignableFrom(PaywallViewModel::class.java) ->
                PaywallViewModel(app.billingManager, app.premiumManager, app.applicationContext) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
