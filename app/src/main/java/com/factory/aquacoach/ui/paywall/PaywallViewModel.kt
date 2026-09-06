package com.factory.aquacoach.ui.paywall

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.factory.aquacoach.data.billing.BillingConnectionState
import com.factory.aquacoach.data.billing.BillingManager
import com.factory.aquacoach.data.billing.NetworkUtils
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.data.billing.PremiumProduct
import com.factory.aquacoach.data.billing.PurchaseEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaywallUiState(
    val connectionState: BillingConnectionState = BillingConnectionState.Disconnected,
    val products: Map<String, ProductDetails> = emptyMap(),
    val isPremium: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null
)

class PaywallViewModel(
    private val billingManager: BillingManager,
    private val premiumManager: PremiumManager,
    private val appContext: Context
) : ViewModel() {

    private val isBusy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PaywallUiState> = combine(
        billingManager.connectionState,
        billingManager.productDetails,
        premiumManager.premiumState,
        isBusy,
        message
    ) { connectionState, products, premiumState, busy, msg ->
        PaywallUiState(
            connectionState = connectionState,
            products = products,
            isPremium = premiumState.isPremium,
            isBusy = busy,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaywallUiState())

    init {
        billingManager.startConnection()
        viewModelScope.launch {
            billingManager.purchaseEvents.collect { event -> onPurchaseEvent(event) }
        }
    }

    fun retryConnection() {
        billingManager.startConnection()
    }

    fun purchase(activity: Activity, product: PremiumProduct) {
        if (!NetworkUtils.isOnline(appContext)) {
            message.value = "No internet connection. Please check your network and try again."
            return
        }
        isBusy.value = true
        billingManager.purchase(activity, product)
    }

    fun restore() {
        if (!NetworkUtils.isOnline(appContext)) {
            message.value = "No internet connection. Please check your network and try again."
            return
        }
        isBusy.value = true
        billingManager.restorePurchases()
    }

    fun dismissMessage() {
        message.value = null
    }

    fun markOnboardingPaywallSeen() {
        viewModelScope.launch { premiumManager.markOnboardingPaywallSeen() }
    }

    private fun onPurchaseEvent(event: PurchaseEvent) {
        isBusy.value = false
        message.value = when (event) {
            is PurchaseEvent.Success -> "You're all set — premium features are unlocked."
            PurchaseEvent.UserCancelled -> null
            PurchaseEvent.Pending -> "Your purchase is pending approval. Premium unlocks automatically once it completes."
            is PurchaseEvent.Error -> event.message
            PurchaseEvent.RestoreCompleted -> "Purchases restored."
        }
    }
}
