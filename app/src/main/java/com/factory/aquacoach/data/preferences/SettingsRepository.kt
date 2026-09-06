package com.factory.aquacoach.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aquacoach_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DAILY_GOAL_ML = intPreferencesKey("daily_goal_ml")
        val UNIT = stringPreferencesKey("unit")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INTERVAL_MIN = intPreferencesKey("reminder_interval_min")
        val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val default = AppSettings()
        AppSettings(
            dailyGoalMl = prefs[Keys.DAILY_GOAL_ML] ?: default.dailyGoalMl,
            unit = prefs[Keys.UNIT]?.let { runCatching { MeasurementUnit.valueOf(it) }.getOrNull() } ?: default.unit,
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: default.remindersEnabled,
            reminderIntervalMinutes = prefs[Keys.REMINDER_INTERVAL_MIN] ?: default.reminderIntervalMinutes,
            quietHoursStartHour = prefs[Keys.QUIET_START_HOUR] ?: default.quietHoursStartHour,
            quietHoursEndHour = prefs[Keys.QUIET_END_HOUR] ?: default.quietHoursEndHour
        )
    }

    suspend fun setDailyGoalMl(value: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL_ML] = value }
    }

    suspend fun setUnit(unit: MeasurementUnit) {
        context.dataStore.edit { it[Keys.UNIT] = unit.name }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setReminderIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REMINDER_INTERVAL_MIN] = minutes }
    }

    suspend fun setQuietHoursStart(hour: Int) {
        context.dataStore.edit { it[Keys.QUIET_START_HOUR] = hour }
    }

    suspend fun setQuietHoursEnd(hour: Int) {
        context.dataStore.edit { it[Keys.QUIET_END_HOUR] = hour }
    }
}
