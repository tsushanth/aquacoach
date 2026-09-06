package com.factory.aquacoach.ui.home

import app.cash.turbine.test
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.billing.PremiumState
import com.factory.aquacoach.data.local.WaterEntryEntity
import com.factory.aquacoach.data.preferences.AppSettings
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.data.repository.WaterRepository
import com.factory.aquacoach.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

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

    private fun createViewModel() = HomeViewModel(waterRepository, settingsRepository, premiumManager)

    @Test
    fun `initial state is default before any collector subscribes`() = runTest {
        every { waterRepository.observeEntriesForDay(any()) } returns flowOf(emptyList())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        every { premiumManager.premiumState } returns flowOf(PremiumState())

        val viewModel = createViewModel()

        assertEquals(HomeUiState(), viewModel.uiState.value)
    }

    @Test
    fun `state reflects entries, settings and premium status once loaded`() = runTest {
        val entries = listOf(
            WaterEntryEntity(id = 1, amountMl = 250, timestampEpochMillis = 1_000L, dayKey = "2026-08-29"),
            WaterEntryEntity(id = 2, amountMl = 300, timestampEpochMillis = 2_000L, dayKey = "2026-08-29")
        )
        every { waterRepository.observeEntriesForDay(any()) } returns flowOf(entries)
        every { settingsRepository.settings } returns flowOf(AppSettings(dailyGoalMl = 2500, unit = MeasurementUnit.OZ))
        every { premiumManager.premiumState } returns flowOf(PremiumState(isPremium = true, isAdsRemoved = true))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val loaded = awaitItem()
            assertEquals(550, loaded.todayTotalMl)
            assertEquals(2500, loaded.goalMl)
            assertEquals(MeasurementUnit.OZ, loaded.unit)
            assertEquals(entries, loaded.entries)
            assertEquals(false, loaded.isLoading)
            assertEquals(true, loaded.isPremium)
            assertEquals(true, loaded.isAdsRemoved)
        }
    }

    @Test
    fun `addWater delegates to repository with given amount`() = runTest {
        every { waterRepository.observeEntriesForDay(any()) } returns flowOf(emptyList())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        every { premiumManager.premiumState } returns flowOf(PremiumState())
        coEvery { waterRepository.addEntry(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.addWater(350)
        advanceUntilIdle()

        coVerify(exactly = 1) { waterRepository.addEntry(350) }
    }

    @Test
    fun `deleteEntry delegates to repository with given id`() = runTest {
        every { waterRepository.observeEntriesForDay(any()) } returns flowOf(emptyList())
        every { settingsRepository.settings } returns flowOf(AppSettings())
        every { premiumManager.premiumState } returns flowOf(PremiumState())
        coEvery { waterRepository.deleteEntry(any()) } returns Unit
        val viewModel = createViewModel()

        viewModel.deleteEntry(7L)
        advanceUntilIdle()

        coVerify(exactly = 1) { waterRepository.deleteEntry(7L) }
    }
}
