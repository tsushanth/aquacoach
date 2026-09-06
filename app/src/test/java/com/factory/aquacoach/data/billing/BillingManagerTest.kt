package com.factory.aquacoach.data.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import app.cash.turbine.test
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Runs under Robolectric (rather than a plain JVM unit test) because BillingManager.purchase()
// builds a real android.billingclient BillingFlowParams, whose Builder.build() always validates
// a default SubscriptionUpdateParams via android.text.TextUtils.isEmpty(). The AGP unit test
// stub for that method always returns false regardless of input, which makes the SDK's
// mutual-exclusivity check misfire; Robolectric's shadow implements it correctly.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerTest {

    private lateinit var billingClient: BillingClient
    private lateinit var premiumManager: PremiumManager
    private lateinit var context: Context
    private lateinit var externalScope: CoroutineScope
    private lateinit var capturedListener: PurchasesUpdatedListener
    private lateinit var manager: BillingManager

    private val okResult: BillingResult = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.OK)
        .build()

    @Before
    fun setup() {
        mockkObject(NetworkUtils)
        every { NetworkUtils.isOnline(any()) } returns true
        // queryProductDetails/queryPurchasesAsync/acknowledgePurchase are top-level billing-ktx
        // suspend extensions (compiled into this facade class), not BillingClient members, so
        // they must be statically mocked or coEvery would fall through to the real
        // suspendCancellableCoroutine body and hang waiting on a callback a mock never invokes.
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")

        billingClient = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        externalScope = CoroutineScope(UnconfinedTestDispatcher())

        manager = BillingManager(context, premiumManager, externalScope) { _, listener ->
            capturedListener = listener
            billingClient
        }
    }

    @After
    fun teardown() {
        unmockkObject(NetworkUtils)
        unmockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
    }

    private fun mockProductDetails(id: String, offerToken: String? = "token-$id"): ProductDetails {
        val details = mockk<ProductDetails>(relaxed = true)
        every { details.productId } returns id
        if (offerToken != null) {
            val offer = mockk<ProductDetails.SubscriptionOfferDetails>(relaxed = true)
            every { offer.offerToken } returns offerToken
            every { details.subscriptionOfferDetails } returns listOf(offer)
        } else {
            every { details.subscriptionOfferDetails } returns emptyList()
        }
        return details
    }

    private fun mockPurchase(productIds: List<String>): Purchase {
        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.products } returns productIds
        every { purchase.isAcknowledged } returns false
        every { purchase.purchaseToken } returns "token-${productIds.joinToString()}"
        return purchase
    }

    private fun stubQueryProductDetails(products: List<ProductDetails>) {
        coEvery { billingClient.queryProductDetails(any()) } returns ProductDetailsResult(okResult, products)
    }

    private fun stubQueryExistingPurchases(subs: List<Purchase> = emptyList(), inApp: List<Purchase> = emptyList()) {
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returnsMany listOf(
            PurchasesResult(okResult, subs),
            PurchasesResult(okResult, inApp)
        )
    }

    // --- Connection / product loading ---------------------------------------------------

    @Test
    fun `startConnection when offline sets Unavailable state`() = runTest {
        every { billingClient.isReady } returns false
        every { NetworkUtils.isOnline(any()) } returns false

        manager.startConnection()

        assertEquals(BillingConnectionState.Unavailable("No internet connection."), manager.connectionState.value)
        verify(exactly = 0) { billingClient.startConnection(any()) }
    }

    @Test
    fun `startConnection when already ready marks Connected without reconnecting`() = runTest {
        every { billingClient.isReady } returns true

        manager.startConnection()

        assertEquals(BillingConnectionState.Connected, manager.connectionState.value)
        verify(exactly = 0) { billingClient.startConnection(any()) }
    }

    @Test
    fun `startConnection success loads product details and syncs existing purchases`() = runTest {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }
        stubQueryProductDetails(listOf(mockProductDetails(PremiumProduct.Monthly.productId)))
        stubQueryExistingPurchases()

        manager.startConnection()
        assertEquals(BillingConnectionState.Connecting, manager.connectionState.value)

        listenerSlot.captured.onBillingSetupFinished(okResult)

        assertEquals(BillingConnectionState.Connected, manager.connectionState.value)
        assertEquals(setOf(PremiumProduct.Monthly.productId), manager.productDetails.value.keys)
        coVerify { premiumManager.syncEntitlements(emptySet()) }
    }

    @Test
    fun `startConnection failure sets Unavailable state with debug message`() = runTest {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }

        manager.startConnection()
        listenerSlot.captured.onBillingSetupFinished(
            BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("boom")
                .build()
        )

        assertEquals(BillingConnectionState.Unavailable("boom"), manager.connectionState.value)
    }

    @Test
    fun `billing service disconnection sets Disconnected state`() = runTest {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }

        manager.startConnection()
        listenerSlot.captured.onBillingServiceDisconnected()

        assertEquals(BillingConnectionState.Disconnected, manager.connectionState.value)
    }

    // --- Purchase flow --------------------------------------------------------------------

    @Test
    fun `purchase when offline emits error and never launches billing flow`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false
        val activity = mockk<Activity>(relaxed = true)

        manager.purchaseEvents.test {
            manager.purchase(activity, PremiumProduct.Monthly)
            assertEquals(PurchaseEvent.Error("No internet connection. Please try again."), awaitItem())
        }
        verify(exactly = 0) { billingClient.launchBillingFlow(any(), any()) }
    }

    @Test
    fun `purchase when product details missing emits error`() = runTest {
        val activity = mockk<Activity>(relaxed = true)

        manager.purchaseEvents.test {
            manager.purchase(activity, PremiumProduct.Monthly)
            assertEquals(
                PurchaseEvent.Error("This item isn't available right now. Please try again later."),
                awaitItem()
            )
        }
    }

    @Test
    fun `purchase of subscription without offer token emits error`() = runTest {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }
        stubQueryProductDetails(listOf(mockProductDetails(PremiumProduct.Monthly.productId, offerToken = null)))
        stubQueryExistingPurchases()
        manager.startConnection()
        listenerSlot.captured.onBillingSetupFinished(okResult)

        val activity = mockk<Activity>(relaxed = true)
        manager.purchaseEvents.test {
            manager.purchase(activity, PremiumProduct.Monthly)
            assertEquals(PurchaseEvent.Error("This subscription isn't available right now."), awaitItem())
        }
    }

    @Test
    fun `purchase launches billing flow when product details and offer token available`() = runTest {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }
        stubQueryProductDetails(listOf(mockProductDetails(PremiumProduct.Monthly.productId)))
        stubQueryExistingPurchases()
        manager.startConnection()
        listenerSlot.captured.onBillingSetupFinished(okResult)
        every { billingClient.launchBillingFlow(any(), any()) } returns okResult

        val activity = mockk<Activity>(relaxed = true)
        manager.purchase(activity, PremiumProduct.Monthly)

        verify(exactly = 1) { billingClient.launchBillingFlow(activity, any()) }
    }

    @Test
    fun `purchase of one-time product does not require offer token`() = runTest {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }
        stubQueryProductDetails(listOf(mockProductDetails(PremiumProduct.Lifetime.productId, offerToken = null)))
        stubQueryExistingPurchases()
        manager.startConnection()
        listenerSlot.captured.onBillingSetupFinished(okResult)
        every { billingClient.launchBillingFlow(any(), any()) } returns okResult

        val activity = mockk<Activity>(relaxed = true)
        manager.purchase(activity, PremiumProduct.Lifetime)

        verify(exactly = 1) { billingClient.launchBillingFlow(activity, any()) }
    }

    @Test
    fun `purchase emits error when launchBillingFlow fails`() = runTest {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }
        stubQueryProductDetails(listOf(mockProductDetails(PremiumProduct.Monthly.productId)))
        stubQueryExistingPurchases()
        manager.startConnection()
        listenerSlot.captured.onBillingSetupFinished(okResult)
        every { billingClient.launchBillingFlow(any(), any()) } returns BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .setDebugMessage("flow failed")
            .build()

        val activity = mockk<Activity>(relaxed = true)
        manager.purchaseEvents.test {
            manager.purchase(activity, PremiumProduct.Monthly)
            assertEquals(PurchaseEvent.Error("flow failed"), awaitItem())
        }
    }

    // --- Purchases updated listener --------------------------------------------------------

    @Test
    fun `purchases updated listener with success purchases emits Success and syncs entitlements`() = runTest {
        stubQueryExistingPurchases(subs = listOf(mockPurchase(listOf(PremiumProduct.Monthly.productId))))
        coEvery { billingClient.acknowledgePurchase(any()) } returns okResult

        val purchase = mockPurchase(listOf(PremiumProduct.Monthly.productId))
        manager.purchaseEvents.test {
            capturedListener.onPurchasesUpdated(okResult, listOf(purchase))
            assertEquals(PurchaseEvent.Success(listOf(PremiumProduct.Monthly.productId)), awaitItem())
        }
        coVerify { premiumManager.syncEntitlements(setOf(PremiumProduct.Monthly.productId)) }
    }

    @Test
    fun `purchases updated listener with pending purchase emits Pending`() = runTest {
        stubQueryExistingPurchases()
        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.purchaseState } returns Purchase.PurchaseState.PENDING
        every { purchase.products } returns listOf(PremiumProduct.Monthly.productId)

        manager.purchaseEvents.test {
            capturedListener.onPurchasesUpdated(okResult, listOf(purchase))
            assertEquals(PurchaseEvent.Pending, awaitItem())
        }
    }

    @Test
    fun `purchases updated listener with user cancellation emits UserCancelled`() = runTest {
        manager.purchaseEvents.test {
            capturedListener.onPurchasesUpdated(
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED).build(),
                null
            )
            assertEquals(PurchaseEvent.UserCancelled, awaitItem())
        }
    }

    @Test
    fun `purchases updated listener with error code emits Error`() = runTest {
        manager.purchaseEvents.test {
            capturedListener.onPurchasesUpdated(
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                    .setDebugMessage("network hiccup")
                    .build(),
                null
            )
            assertEquals(PurchaseEvent.Error("network hiccup"), awaitItem())
        }
    }

    // --- Restore purchases ------------------------------------------------------------------

    @Test
    fun `restorePurchases when offline emits error and does not query purchases`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false

        manager.purchaseEvents.test {
            manager.restorePurchases()
            assertEquals(PurchaseEvent.Error("No internet connection. Please try again."), awaitItem())
        }
        coVerify(exactly = 0) { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) }
    }

    @Test
    fun `restorePurchases when not ready starts connection and emits error`() = runTest {
        every { billingClient.isReady } returns false

        manager.purchaseEvents.test {
            manager.restorePurchases()
            assertEquals(
                PurchaseEvent.Error("Not connected to Google Play. Please try again."),
                awaitItem()
            )
        }
        verify(exactly = 1) { billingClient.startConnection(any()) }
    }

    @Test
    fun `restorePurchases when ready queries purchases and emits RestoreCompleted`() = runTest {
        every { billingClient.isReady } returns true
        stubQueryExistingPurchases(
            subs = listOf(mockPurchase(listOf(PremiumProduct.Yearly.productId))),
            inApp = listOf(mockPurchase(listOf(PremiumProduct.RemoveAds.productId)))
        )
        coEvery { billingClient.acknowledgePurchase(any()) } returns okResult

        manager.purchaseEvents.test {
            manager.restorePurchases()
            assertEquals(PurchaseEvent.RestoreCompleted, awaitItem())
        }
        coVerify {
            premiumManager.syncEntitlements(setOf(PremiumProduct.Yearly.productId, PremiumProduct.RemoveAds.productId))
        }
    }

    @Test
    fun `endConnection delegates to underlying billing client`() {
        manager.endConnection()

        verify(exactly = 1) { billingClient.endConnection() }
    }
}
