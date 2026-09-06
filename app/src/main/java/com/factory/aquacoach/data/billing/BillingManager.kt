package com.factory.aquacoach.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Wraps a single [BillingClient] connection for the app's lifetime so premium entitlements can be
 * resolved (and re-validated) before any screen needs to gate a feature, not just while the
 * paywall is open.
 */
class BillingManager(
    context: Context,
    private val premiumManager: PremiumManager,
    private val externalScope: CoroutineScope,
    billingClientFactory: (Context, PurchasesUpdatedListener) -> BillingClient = { appContext, listener ->
        BillingClient.newBuilder(appContext)
            .setListener(listener)
            .enablePendingPurchases()
            .build()
    }
) {
    private val appContext = context.applicationContext

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val updated = purchases
                if (!updated.isNullOrEmpty()) {
                    externalScope.launch { handlePurchases(updated) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _purchaseEvents.tryEmit(PurchaseEvent.UserCancelled)
            else ->
                _purchaseEvents.tryEmit(
                    PurchaseEvent.Error(billingResult.debugMessage.ifBlank { "Purchase failed. Please try again." })
                )
        }
    }

    private val billingClient: BillingClient = billingClientFactory(appContext, purchasesUpdatedListener)

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 1)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    fun startConnection() {
        if (billingClient.isReady) {
            _connectionState.value = BillingConnectionState.Connected
            return
        }
        if (!NetworkUtils.isOnline(appContext)) {
            _connectionState.value = BillingConnectionState.Unavailable("No internet connection.")
            return
        }

        _connectionState.value = BillingConnectionState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.Connected
                    externalScope.launch {
                        queryProductDetails()
                        queryExistingPurchases()
                    }
                } else {
                    _connectionState.value = BillingConnectionState.Unavailable(
                        result.debugMessage.ifBlank { "Unable to connect to Google Play." }
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.Disconnected
            }
        })
    }

    private suspend fun queryProductDetails() {
        val productGroups = listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)
            .map { type -> PremiumProduct.all.filter { it.productType == type } }
            .filter { it.isNotEmpty() }
            .map { productsOfType ->
                productsOfType.map { product ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(product.productId)
                        .setProductType(product.productType)
                        .build()
                }
            }

        val results = productGroups.map { products ->
            billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(products).build()
            )
        }

        _productDetails.value = results.flatMap { it.productDetailsList.orEmpty() }.associateBy { it.productId }
    }

    private suspend fun queryExistingPurchases() {
        val subsPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ).purchasesList
        val inAppPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ).purchasesList

        val purchased = (subsPurchases + inAppPurchases).filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchased.forEach { acknowledgeIfNeeded(it) }
        premiumManager.syncEntitlements(purchased.flatMap { it.products }.toSet())
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            )
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        val hasPending = purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { acknowledgeIfNeeded(it) }

        // Re-derive entitlements from Play rather than trusting only this callback's purchases,
        // so state stays correct even if the user has multiple products active at once.
        queryExistingPurchases()

        if (hasPending) {
            _purchaseEvents.emit(PurchaseEvent.Pending)
        } else {
            _purchaseEvents.emit(PurchaseEvent.Success(purchases.flatMap { it.products }))
        }
    }

    fun purchase(activity: Activity, product: PremiumProduct) {
        if (!NetworkUtils.isOnline(appContext)) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error("No internet connection. Please try again."))
            return
        }
        val details = _productDetails.value[product.productId]
        if (details == null) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error("This item isn't available right now. Please try again later."))
            return
        }

        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        if (product.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                _purchaseEvents.tryEmit(PurchaseEvent.Error("This subscription isn't available right now."))
                return
            }
            paramsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error(result.debugMessage.ifBlank { "Unable to start purchase." }))
        }
    }

    fun restorePurchases() {
        if (!NetworkUtils.isOnline(appContext)) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error("No internet connection. Please try again."))
            return
        }
        if (!billingClient.isReady) {
            startConnection()
            _purchaseEvents.tryEmit(PurchaseEvent.Error("Not connected to Google Play. Please try again."))
            return
        }
        externalScope.launch {
            queryExistingPurchases()
            _purchaseEvents.emit(PurchaseEvent.RestoreCompleted)
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
