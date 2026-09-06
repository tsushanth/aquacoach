package com.factory.aquacoach.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.factory.aquacoach.data.local.AppDatabase
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.data.repository.WaterRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val settings = settingsRepository.settings.first()

        if (!settings.remindersEnabled) return Result.success()

        val now = LocalTime.now()
        if (isInQuietHours(now, settings.quietHoursStartHour, settings.quietHoursEndHour)) {
            return Result.success()
        }

        val waterRepository = WaterRepository(AppDatabase.getInstance(applicationContext).waterEntryDao())
        val todayTotal = waterRepository.todayTotalMl()
        if (todayTotal >= settings.dailyGoalMl) return Result.success()

        val remainingMl = settings.dailyGoalMl - todayTotal
        val message = "You're at ${todayTotal}ml of ${settings.dailyGoalMl}ml today. $remainingMl ml to go — take a sip!"
        NotificationHelper.showReminderNotification(applicationContext, message)

        return Result.success()
    }

    private fun isInQuietHours(now: LocalTime, startHour: Int, endHour: Int): Boolean {
        val start = LocalTime.of(startHour, 0)
        val end = LocalTime.of(endHour, 0)
        return if (start <= end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }
}
