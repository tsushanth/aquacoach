package com.factory.aquacoach.data.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.BillingClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.premiumDataStore by preferencesDataStore(name = "aquacoach_premium")

data class PremiumState(
    val isPremium: Boolean = false,
    val isAdsRemoved: Boolean = false,
    val activeSubscriptionId: String? = null,
    val isLifetime: Boolean = false
)

class PremiumManager(context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val ACTIVE_SUBSCRIPTION_ID = stringPreferencesKey("active_subscription_id")
        val IS_LIFETIME = booleanPreferencesKey("is_lifetime")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val HAS_SEEN_ONBOARDING_PAYWALL = booleanPreferencesKey("has_seen_onboarding_paywall")
    }

    val premiumState: Flow<PremiumState> = appContext.premiumDataStore.data.map { prefs ->
        val activeSubscriptionId = prefs[Keys.ACTIVE_SUBSCRIPTION_ID]
        val isLifetime = prefs[Keys.IS_LIFETIME] ?: false
        val adsRemoved = prefs[Keys.ADS_REMOVED] ?: false
        PremiumState(
            isPremium = activeSubscriptionId != null || isLifetime,
            isAdsRemoved = adsRemoved || activeSubscriptionId != null || isLifetime,
            activeSubscriptionId = activeSubscriptionId,
            isLifetime = isLifetime
        )
    }

    val hasSeenOnboardingPaywall: Flow<Boolean> = appContext.premiumDataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_ONBOARDING_PAYWALL] ?: false
    }

    suspend fun markOnboardingPaywallSeen() {
        appContext.premiumDataStore.edit { it[Keys.HAS_SEEN_ONBOARDING_PAYWALL] = true }
    }

    /**
     * Replaces stored entitlements with exactly what Google Play currently reports as owned.
     * Subscriptions that fell off this set (expired, cancelled, refunded) lose access; one-time
     * purchases (lifetime, remove ads) remain granted as long as they keep appearing.
     */
    suspend fun syncEntitlements(activeProductIds: Set<String>) {
        appContext.premiumDataStore.edit { prefs ->
            val activeSubscription = PremiumProduct.subscriptionTiers.firstOrNull {
                it.productType == BillingClient.ProductType.SUBS && activeProductIds.contains(it.productId)
            }
            if (activeSubscription != null) {
                prefs[Keys.ACTIVE_SUBSCRIPTION_ID] = activeSubscription.productId
            } else {
                prefs.remove(Keys.ACTIVE_SUBSCRIPTION_ID)
            }
            prefs[Keys.IS_LIFETIME] = activeProductIds.contains(PremiumProduct.Lifetime.productId)
            prefs[Keys.ADS_REMOVED] = activeProductIds.contains(PremiumProduct.RemoveAds.productId)
        }
    }
}
