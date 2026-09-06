package com.factory.aquacoach.data.preferences

data class AppSettings(
    val dailyGoalMl: Int = 2000,
    val unit: MeasurementUnit = MeasurementUnit.ML,
    val remindersEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 90,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7
)
