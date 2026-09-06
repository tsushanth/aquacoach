package com.factory.aquacoach.data.preferences

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = SettingsRepository(context)
        // The DataStore backing this repository is a process-wide singleton, so tests reset
        // known state explicitly rather than relying on a pristine store.
        val defaults = AppSettings()
        repository.setDailyGoalMl(defaults.dailyGoalMl)
        repository.setUnit(defaults.unit)
        repository.setRemindersEnabled(defaults.remindersEnabled)
        repository.setReminderIntervalMinutes(defaults.reminderIntervalMinutes)
        repository.setQuietHoursStart(defaults.quietHoursStartHour)
        repository.setQuietHoursEnd(defaults.quietHoursEndHour)
    }

    @Test
    fun `settings reflect defaults after reset`() = runTest {
        val settings = repository.settings.first()

        assertEquals(AppSettings(), settings)
    }

    @Test
    fun `setDailyGoalMl persists new goal`() = runTest {
        repository.setDailyGoalMl(3000)

        assertEquals(3000, repository.settings.first().dailyGoalMl)
    }

    @Test
    fun `setUnit persists new unit`() = runTest {
        repository.setUnit(MeasurementUnit.OZ)

        assertEquals(MeasurementUnit.OZ, repository.settings.first().unit)
    }

    @Test
    fun `setRemindersEnabled persists flag`() = runTest {
        repository.setRemindersEnabled(false)

        assertEquals(false, repository.settings.first().remindersEnabled)
    }

    @Test
    fun `setReminderIntervalMinutes persists interval`() = runTest {
        repository.setReminderIntervalMinutes(45)

        assertEquals(45, repository.settings.first().reminderIntervalMinutes)
    }

    @Test
    fun `setQuietHours persists start and end hour independently`() = runTest {
        repository.setQuietHoursStart(23)
        repository.setQuietHoursEnd(6)

        val settings = repository.settings.first()
        assertEquals(23, settings.quietHoursStartHour)
        assertEquals(6, settings.quietHoursEndHour)
    }

    @Test
    fun `settings survives repeated reads without reset`() = runTest {
        repository.setDailyGoalMl(2500)

        assertEquals(2500, repository.settings.first().dailyGoalMl)
        assertEquals(2500, repository.settings.first().dailyGoalMl)
    }
}
