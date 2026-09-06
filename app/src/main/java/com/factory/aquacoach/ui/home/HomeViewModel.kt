package com.factory.aquacoach.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.local.WaterEntryEntity
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.data.repository.WaterRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayTotalMl: Int = 0,
    val goalMl: Int = 2000,
    val unit: MeasurementUnit = MeasurementUnit.ML,
    val entries: List<WaterEntryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val isAdsRemoved: Boolean = false
)

class HomeViewModel(
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val dayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val uiState: StateFlow<HomeUiState> = combine(
        waterRepository.observeEntriesForDay(dayKey),
        settingsRepository.settings,
        premiumManager.premiumState
    ) { entries, settings, premium ->
        HomeUiState(
            todayTotalMl = entries.sumOf { it.amountMl },
            goalMl = settings.dailyGoalMl,
            unit = settings.unit,
            entries = entries,
            isLoading = false,
            isPremium = premium.isPremium,
            isAdsRemoved = premium.isAdsRemoved
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun addWater(amountMl: Int) {
        viewModelScope.launch { waterRepository.addEntry(amountMl) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { waterRepository.deleteEntry(id) }
    }
}
