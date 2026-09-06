package com.factory.aquacoach.ui.settings

import android.content.Context
import app.cash.turbine.test
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.billing.PremiumState
import com.factory.aquacoach.data.preferences.AppSettings
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.notification.ReminderScheduler
import com.factory.aquacoach.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var premiumManager: PremiumManager
    private lateinit var context: Context
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>

    @Before
    fun setup() {
        mockkObject(ReminderScheduler)
        every { ReminderScheduler.schedule(any(), any()) } returns Unit
        every { ReminderScheduler.cancel(any()) } returns Unit

        settingsFlow = MutableStateFlow(AppSettings())
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.settings } returns settingsFlow
        premiumManager = mockk(relaxed = true)
        every { premiumManager.premiumState } returns flowOf(PremiumState())
        context = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        unmockkObject(ReminderScheduler)
    }

    private fun createViewModel() = SettingsViewModel(settingsRepository, premiumManager, context)

    @Test
    fun `initial state combines settings and premium status`() = runTest {
        every { premiumManager.premiumState } returns flowOf(PremiumState(isPremium = true))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val loaded = awaitItem()
            assertEquals(AppSettings(), loaded.settings)
            assertEquals(true, loaded.isPremium)
        }
    }

    @Test
    fun `setDailyGoal delegates to repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.setDailyGoal(3000)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setDailyGoalMl(3000) }
    }

    @Test
    fun `setUnit delegates to repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.setUnit(MeasurementUnit.OZ)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setUnit(MeasurementUnit.OZ) }
    }

    @Test
    fun `setRemindersEnabled true schedules reminders with current interval`() = runTest {
        settingsFlow.value = AppSettings(reminderIntervalMinutes = 45)
        coEvery { settingsRepository.setRemindersEnabled(true) } returns Unit
        val viewModel = createViewModel()

        viewModel.setRemindersEnabled(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setRemindersEnabled(true) }
        verify(exactly = 1) { ReminderScheduler.schedule(context, 45) }
    }

    @Test
    fun `setRemindersEnabled false cancels reminders`() = runTest {
        coEvery { settingsRepository.setRemindersEnabled(false) } returns Unit
        val viewModel = createViewModel()

        viewModel.setRemindersEnabled(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setRemindersEnabled(false) }
        verify(exactly = 1) { ReminderScheduler.cancel(context) }
        verify(exactly = 0) { ReminderScheduler.schedule(any(), any()) }
    }

    @Test
    fun `setReminderInterval reschedules only when reminders are enabled`() = runTest {
        settingsFlow.value = AppSettings(remindersEnabled = true)
        val viewModel = createViewModel()

        viewModel.setReminderInterval(60)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setReminderIntervalMinutes(60) }
        verify(exactly = 1) { ReminderScheduler.schedule(context, 60) }
    }

    @Test
    fun `setReminderInterval does not reschedule when reminders are disabled`() = runTest {
        settingsFlow.value = AppSettings(remindersEnabled = false)
        val viewModel = createViewModel()

        viewModel.setReminderInterval(60)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setReminderIntervalMinutes(60) }
        verify(exactly = 0) { ReminderScheduler.schedule(any(), any()) }
    }

    @Test
    fun `setQuietHours delegates start and end hour to repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.setQuietHours(23, 6)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setQuietHoursStart(23) }
        coVerify(exactly = 1) { settingsRepository.setQuietHoursEnd(6) }
    }
}
