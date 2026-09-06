package com.factory.aquacoach.ui.paywall

import android.app.Activity
import android.content.Context
import app.cash.turbine.test
import com.factory.aquacoach.data.billing.BillingConnectionState
import com.factory.aquacoach.data.billing.BillingManager
import com.factory.aquacoach.data.billing.NetworkUtils
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.billing.PremiumProduct
import com.factory.aquacoach.data.billing.PremiumState
import com.factory.aquacoach.data.billing.PurchaseEvent
import com.factory.aquacoach.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PaywallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var billingManager: BillingManager
    private lateinit var premiumManager: PremiumManager
    private lateinit var context: Context
    private lateinit var purchaseEvents: MutableSharedFlow<PurchaseEvent>

    @Before
    fun setup() {
        mockkObject(NetworkUtils)
        every { NetworkUtils.isOnline(any()) } returns true

        purchaseEvents = MutableSharedFlow(extraBufferCapacity = 1)
        billingManager = mockk(relaxed = true)
        every { billingManager.connectionState } returns MutableStateFlow(BillingConnectionState.Disconnected)
        every { billingManager.productDetails } returns MutableStateFlow(emptyMap())
        every { billingManager.purchaseEvents } returns purchaseEvents

        premiumManager = mockk(relaxed = true)
        every { premiumManager.premiumState } returns flowOf(PremiumState())
        coEvery { premiumManager.markOnboardingPaywallSeen() } returns Unit

        context = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        unmockkObject(NetworkUtils)
    }

    private fun createViewModel() = PaywallViewModel(billingManager, premiumManager, context)

    @Test
    fun `init starts billing connection and subscribes to purchase events`() = runTest {
        createViewModel()
        advanceUntilIdle()

        verify(exactly = 1) { billingManager.startConnection() }
    }

    @Test
    fun `initial state reflects connection state and product details`() = runTest {
        val products = mapOf("id" to mockk<com.android.billingclient.api.ProductDetails>(relaxed = true))
        every { billingManager.connectionState } returns MutableStateFlow(BillingConnectionState.Connected)
        every { billingManager.productDetails } returns MutableStateFlow(products)
        every { premiumManager.premiumState } returns flowOf(PremiumState(isPremium = true))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val loaded = awaitItem()
            assertEquals(BillingConnectionState.Connected, loaded.connectionState)
            assertEquals(products, loaded.products)
            assertEquals(true, loaded.isPremium)
        }
    }

    @Test
    fun `retryConnection delegates to billing manager`() = runTest {
        val viewModel = createViewModel()

        viewModel.retryConnection()

        verify(exactly = 2) { billingManager.startConnection() } // once on init, once on retry
    }

    @Test
    fun `purchase when offline shows message and never calls billing manager`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false
        val viewModel = createViewModel()
        val activity = mockk<Activity>(relaxed = true)

        viewModel.uiState.test {
            awaitItem()
            viewModel.purchase(activity, PremiumProduct.Monthly)
            val state = awaitItem()
            assertEquals("No internet connection. Please check your network and try again.", state.message)
            assertEquals(false, state.isBusy)
        }
        verify(exactly = 0) { billingManager.purchase(any(), any()) }
    }

    @Test
    fun `purchase when online sets busy and delegates to billing manager`() = runTest {
        val viewModel = createViewModel()
        val activity = mockk<Activity>(relaxed = true)

        viewModel.uiState.test {
            awaitItem()
            viewModel.purchase(activity, PremiumProduct.Monthly)
            val state = awaitItem()
            assertEquals(true, state.isBusy)
        }
        verify(exactly = 1) { billingManager.purchase(activity, PremiumProduct.Monthly) }
    }

    @Test
    fun `restore when offline shows message and never calls billing manager`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.restore()
            val state = awaitItem()
            assertEquals("No internet connection. Please check your network and try again.", state.message)
        }
        verify(exactly = 0) { billingManager.restorePurchases() }
    }

    @Test
    fun `restore when online sets busy and delegates to billing manager`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.restore()
            val state = awaitItem()
            assertEquals(true, state.isBusy)
        }
        verify(exactly = 1) { billingManager.restorePurchases() }
    }

    @Test
    fun `dismissMessage clears current message`() = runTest {
        every { NetworkUtils.isOnline(any()) } returns false
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.restore()
            awaitItem() // message set
            viewModel.dismissMessage()
            val state = awaitItem()
            assertEquals(null, state.message)
        }
    }

    @Test
    fun `markOnboardingPaywallSeen delegates to premium manager`() = runTest {
        val viewModel = createViewModel()

        viewModel.markOnboardingPaywallSeen()
        advanceUntilIdle()

        coVerify(exactly = 1) { premiumManager.markOnboardingPaywallSeen() }
    }

    @Test
    fun `success purchase event clears busy state and sets confirmation message`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            purchaseEvents.emit(PurchaseEvent.Success(listOf(PremiumProduct.Monthly.productId)))
            val state = awaitItem()
            assertEquals(false, state.isBusy)
            assertEquals("You're all set — premium features are unlocked.", state.message)
        }
    }

    @Test
    fun `user cancelled purchase event clears busy state without a message`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val activity = mockk<Activity>(relaxed = true)
        viewModel.purchase(activity, PremiumProduct.Monthly)

        viewModel.uiState.test {
            awaitItem()
            purchaseEvents.emit(PurchaseEvent.UserCancelled)
            val state = awaitItem()
            assertEquals(false, state.isBusy)
            assertEquals(null, state.message)
        }
    }

    @Test
    fun `pending purchase event sets pending message`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            purchaseEvents.emit(PurchaseEvent.Pending)
            val state = awaitItem()
            assertEquals(
                "Your purchase is pending approval. Premium unlocks automatically once it completes.",
                state.message
            )
        }
    }

    @Test
    fun `error purchase event surfaces the error message`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            purchaseEvents.emit(PurchaseEvent.Error("card declined"))
            val state = awaitItem()
            assertEquals("card declined", state.message)
            assertEquals(false, state.isBusy)
        }
    }

    @Test
    fun `restore completed event sets confirmation message`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            purchaseEvents.emit(PurchaseEvent.RestoreCompleted)
            val state = awaitItem()
            assertEquals("Purchases restored.", state.message)
        }
    }
}
