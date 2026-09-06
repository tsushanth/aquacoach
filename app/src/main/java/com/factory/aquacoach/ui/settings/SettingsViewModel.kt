package com.factory.aquacoach.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.preferences.AppSettings
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isPremium: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    premiumManager: PremiumManager,
    private val appContext: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        premiumManager.premiumState
    ) { settings, premium ->
        SettingsUiState(settings = settings, isPremium = premium.isPremium)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDailyGoal(ml: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoalMl(ml) }
    }

    fun setUnit(unit: MeasurementUnit) {
        viewModelScope.launch { settingsRepository.setUnit(unit) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            if (enabled) {
                val interval = settingsRepository.settings.first().reminderIntervalMinutes
                ReminderScheduler.schedule(appContext, interval)
            } else {
                ReminderScheduler.cancel(appContext)
            }
        }
    }

    fun setReminderInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderIntervalMinutes(minutes)
            if (settingsRepository.settings.first().remindersEnabled) {
                ReminderScheduler.schedule(appContext, minutes)
            }
        }
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursStart(startHour)
            settingsRepository.setQuietHoursEnd(endHour)
        }
    }
}
