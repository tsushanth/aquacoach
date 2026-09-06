package com.factory.aquacoach.ui.history

import app.cash.turbine.test
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.billing.PremiumState
import com.factory.aquacoach.data.local.DailyTotalRow
import com.factory.aquacoach.data.preferences.AppSettings
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.data.repository.WaterRepository
import com.factory.aquacoach.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var waterRepository: WaterRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var premiumManager: PremiumManager

    @Before
    fun setup() {
        waterRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)
    }

    private fun createViewModel() = HistoryViewModel(waterRepository, settingsRepository, premiumManager)

    private fun dailyTotals(days: Int) = (1..days).map { DailyTotalRow("2026-08-$it", 1000 + it) }

    @Test
    fun `initial state is default before any collector subscribes`() = runTest {
        every { waterRepository.observeDailyTotals(30) } returns flowOf(emptyList())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        every { premiumManager.premiumState } returns flowOf(PremiumState())

        val viewModel = createViewModel()

        assertEquals(HistoryUiState(), viewModel.uiState.value)
    }

    @Test
    fun `free users see only the most recent FREE_HISTORY_DAYS entries`() = runTest {
        val totals = dailyTotals(10)
        every { waterRepository.observeDailyTotals(30) } returns flowOf(totals)
        every { settingsRepository.settings } returns flowOf(AppSettings())
        every { premiumManager.premiumState } returns flowOf(PremiumState(isPremium = false))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val loaded = awaitItem()
            assertEquals(FREE_HISTORY_DAYS, loaded.dailyTotals.size)
            assertEquals(totals.take(FREE_HISTORY_DAYS), loaded.dailyTotals)
            assertEquals(false, loaded.isPremium)
            assertEquals(true, loaded.hasMoreHistory)
        }
    }

    @Test
    fun `premium users see full history and hasMoreHistory is false`() = runTest {
        val totals = dailyTotals(10)
        every { waterRepository.observeDailyTotals(30) } returns flowOf(totals)
        every { settingsRepository.settings } returns flowOf(AppSettings())
        every { premiumManager.premiumState } returns flowOf(PremiumState(isPremium = true))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val loaded = awaitItem()
            assertEquals(10, loaded.dailyTotals.size)
            assertEquals(totals, loaded.dailyTotals)
            assertEquals(true, loaded.isPremium)
            assertEquals(false, loaded.hasMoreHistory)
        }
    }

    @Test
    fun `hasMoreHistory is false for free users when history is within the free limit`() = runTest {
        val totals = dailyTotals(FREE_HISTORY_DAYS)
        every { waterRepository.observeDailyTotals(30) } returns flowOf(totals)
        every { settingsRepository.settings } returns flowOf(AppSettings())
        every { premiumManager.premiumState } returns flowOf(PremiumState(isPremium = false))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val loaded = awaitItem()
            assertEquals(false, loaded.hasMoreHistory)
            assertEquals(FREE_HISTORY_DAYS, loaded.dailyTotals.size)
        }
    }
}
