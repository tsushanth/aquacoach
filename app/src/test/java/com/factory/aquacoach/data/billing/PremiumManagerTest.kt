package com.factory.aquacoach.data.billing

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class PremiumManagerTest {

    private lateinit var premiumManager: PremiumManager

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        premiumManager = PremiumManager(context)
        // The DataStore backing this manager is a process-wide singleton, so tests reset known
        // state explicitly rather than relying on a pristine store.
        premiumManager.syncEntitlements(emptySet())
    }

    @Test
    fun `syncEntitlements with no active products clears premium state`() = runTest {
        val state = premiumManager.premiumState.first()

        assertFalse(state.isPremium)
        assertFalse(state.isAdsRemoved)
        assertFalse(state.isLifetime)
        assertNull(state.activeSubscriptionId)
    }

    @Test
    fun `syncEntitlements with active subscription marks premium and stores subscription id`() = runTest {
        premiumManager.syncEntitlements(setOf(PremiumProduct.Monthly.productId))

        val state = premiumManager.premiumState.first()

        assertTrue(state.isPremium)
        assertTrue(state.isAdsRemoved)
        assertFalse(state.isLifetime)
        assertEquals(PremiumProduct.Monthly.productId, state.activeSubscriptionId)
    }

    @Test
    fun `syncEntitlements with lifetime product marks premium and lifetime without subscription id`() = runTest {
        premiumManager.syncEntitlements(setOf(PremiumProduct.Lifetime.productId))

        val state = premiumManager.premiumState.first()

        assertTrue(state.isPremium)
        assertTrue(state.isAdsRemoved)
        assertTrue(state.isLifetime)
        assertNull(state.activeSubscriptionId)
    }

    @Test
    fun `syncEntitlements with remove-ads product only removes ads without granting premium`() = runTest {
        premiumManager.syncEntitlements(setOf(PremiumProduct.RemoveAds.productId))

        val state = premiumManager.premiumState.first()

        assertFalse(state.isPremium)
        assertTrue(state.isAdsRemoved)
        assertFalse(state.isLifetime)
        assertNull(state.activeSubscriptionId)
    }

    @Test
    fun `syncEntitlements dropping a previously active subscription revokes premium`() = runTest {
        premiumManager.syncEntitlements(setOf(PremiumProduct.Yearly.productId))
        assertTrue(premiumManager.premiumState.first().isPremium)

        premiumManager.syncEntitlements(emptySet())

        val state = premiumManager.premiumState.first()
        assertFalse(state.isPremium)
        assertNull(state.activeSubscriptionId)
    }

    @Test
    fun `syncEntitlements with unrelated product id grants no entitlement`() = runTest {
        premiumManager.syncEntitlements(setOf("some_unknown_product"))

        val state = premiumManager.premiumState.first()

        assertFalse(state.isPremium)
        assertFalse(state.isAdsRemoved)
        assertFalse(state.isLifetime)
        assertNull(state.activeSubscriptionId)
    }

    @Test
    fun `markOnboardingPaywallSeen updates hasSeenOnboardingPaywall to true`() = runTest {
        premiumManager.markOnboardingPaywallSeen()

        assertTrue(premiumManager.hasSeenOnboardingPaywall.first())
    }
}
