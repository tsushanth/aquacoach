package com.factory.aquacoach

import android.app.Application
import com.factory.aquacoach.data.billing.BillingManager
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.local.AppDatabase
import com.factory.aquacoach.data.preferences.SettingsRepository
import com.factory.aquacoach.data.repository.WaterRepository
import com.factory.aquacoach.notification.NotificationHelper
import com.factory.aquacoach.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AquaCoachApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var waterRepository: WaterRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var premiumManager: PremiumManager
        private set

    lateinit var billingManager: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getInstance(this)
        waterRepository = WaterRepository(database.waterEntryDao())
        settingsRepository = SettingsRepository(this)
        premiumManager = PremiumManager(this)
        billingManager = BillingManager(this, premiumManager, appScope)
        billingManager.startConnection()

        NotificationHelper.createNotificationChannel(this)

        appScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.remindersEnabled) {
                ReminderScheduler.schedule(this@AquaCoachApp, settings.reminderIntervalMinutes)
            }
        }
    }
}
