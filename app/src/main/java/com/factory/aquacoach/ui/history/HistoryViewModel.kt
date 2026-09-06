package com.factory.aquacoach.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.local.DailyTotalRow
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.data.repository.WaterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

const val FREE_HISTORY_DAYS = 7

data class HistoryUiState(
    val dailyTotals: List<DailyTotalRow> = emptyList(),
    val goalMl: Int = 2000,
    val unit: MeasurementUnit = MeasurementUnit.ML,
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val hasMoreHistory: Boolean = false
)

class HistoryViewModel(
    waterRepository: WaterRepository,
    settingsRepository: SettingsRepository,
    premiumManager: PremiumManager
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        waterRepository.observeDailyTotals(30),
        settingsRepository.settings,
        premiumManager.premiumState
    ) { totals, settings, premium ->
        HistoryUiState(
            dailyTotals = if (premium.isPremium) totals else totals.take(FREE_HISTORY_DAYS),
            goalMl = settings.dailyGoalMl,
            unit = settings.unit,
            isLoading = false,
            isPremium = premium.isPremium,
            hasMoreHistory = !premium.isPremium && totals.size > FREE_HISTORY_DAYS
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
