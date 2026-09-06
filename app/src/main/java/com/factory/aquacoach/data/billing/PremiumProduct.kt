package com.factory.aquacoach.data.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

sealed class PremiumProduct(
    val productId: String,
    val productType: String,
    val displayName: String,
    val fallbackPrice: String,
    val billingPeriodLabel: String
) {
    data object Weekly : PremiumProduct(
        productId = "aquacoach_sub_weekly",
        productType = BillingClient.ProductType.SUBS,
        displayName = "Weekly",
        fallbackPrice = "$2.99",
        billingPeriodLabel = "/week"
    )

    data object Monthly : PremiumProduct(
        productId = "aquacoach_sub_monthly",
        productType = BillingClient.ProductType.SUBS,
        displayName = "Monthly",
        fallbackPrice = "$4.99",
        billingPeriodLabel = "/month"
    )

    data object Yearly : PremiumProduct(
        productId = "aquacoach_sub_yearly",
        productType = BillingClient.ProductType.SUBS,
        displayName = "Yearly",
        fallbackPrice = "$29.99",
        billingPeriodLabel = "/year"
    )

    data object Lifetime : PremiumProduct(
        productId = "aquacoach_lifetime",
        productType = BillingClient.ProductType.INAPP,
        displayName = "Lifetime",
        fallbackPrice = "$49.99",
        billingPeriodLabel = "one-time"
    )

    data object RemoveAds : PremiumProduct(
        productId = "aquacoach_remove_ads",
        productType = BillingClient.ProductType.INAPP,
        displayName = "Remove Ads",
        fallbackPrice = "$1.99",
        billingPeriodLabel = "one-time"
    )

    companion object {
        // Lazy so this list is never built while the JVM is still initializing PremiumProduct's
        // class hierarchy: eagerly evaluating it here would re-enter class init for whichever
        // data object happened to trigger loading the sealed class first, and the JVM resolves
        // that reentrant reference to its still-null INSTANCE field (KT-30146).
        val subscriptionTiers by lazy { listOf(Weekly, Monthly, Yearly, Lifetime) }
        val all by lazy { listOf(Weekly, Monthly, Yearly, Lifetime, RemoveAds) }

        fun fromProductId(productId: String): PremiumProduct? = all.find { it.productId == productId }
    }
}

fun PremiumProduct.priceLabel(products: Map<String, ProductDetails>): String {
    val details = products[productId] ?: return fallbackPrice
    val formattedPrice = when (productType) {
        BillingClient.ProductType.SUBS ->
            details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
        else ->
            details.oneTimePurchaseOfferDetails?.formattedPrice
    }
    return formattedPrice ?: fallbackPrice
}

sealed class BillingConnectionState {
    data object Disconnected : BillingConnectionState()
    data object Connecting : BillingConnectionState()
    data object Connected : BillingConnectionState()
    data class Unavailable(val message: String) : BillingConnectionState()
}

sealed class PurchaseEvent {
    data class Success(val productIds: List<String>) : PurchaseEvent()
    data object UserCancelled : PurchaseEvent()
    data object Pending : PurchaseEvent()
    data class Error(val message: String) : PurchaseEvent()
    data object RestoreCompleted : PurchaseEvent()
}
