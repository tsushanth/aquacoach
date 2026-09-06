package com.factory.aquacoach.ui.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.factory.aquacoach.data.billing.BillingConnectionState
import com.factory.aquacoach.data.billing.PremiumProduct
import com.factory.aquacoach.data.billing.priceLabel

private const val TERMS_OF_SERVICE_URL = "https://aquacoach.example.com/terms"
private const val PRIVACY_POLICY_URL = "https://aquacoach.example.com/privacy"
private const val MANAGE_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions?package=com.factory.aquacoach"

private val PREMIUM_FEATURES = listOf(
    "Unlimited custom water amounts",
    "Full hydration history & trends",
    "Custom reminder schedules & quiet hours",
    "Personalized daily goals",
    "An ad-free experience"
)

/**
 * @param trigger where the paywall was opened from ("onboarding", "settings", or a feature name);
 * only changes the header copy and whether closing marks onboarding as seen.
 */
@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    trigger: String,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    var selectedTier by remember { mutableStateOf<PremiumProduct>(PremiumProduct.Yearly) }

    LaunchedEffect(trigger) {
        if (trigger == "onboarding") {
            viewModel.markOnboardingPaywallSeen()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(8.dp))
            Icon(
                Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "AquaCoach Premium",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (trigger == "onboarding") {
                    "Start your hydration journey with every feature unlocked."
                } else {
                    "Unlock the full AquaCoach experience."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            PREMIUM_FEATURES.forEach { feature -> FeatureRow(feature) }

            Spacer(Modifier.height(24.dp))
            if (uiState.isPremium) {
                PremiumActiveSection(
                    onManageSubscription = { uriHandler.openUri(MANAGE_SUBSCRIPTIONS_URL) },
                    onClose = onClose
                )
            } else {
                when (val connection = uiState.connectionState) {
                    BillingConnectionState.Connecting, BillingConnectionState.Disconnected -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is BillingConnectionState.Unavailable -> {
                        Text(
                            connection.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.retryConnection()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry")
                        }
                    }
                    BillingConnectionState.Connected -> {
                        PremiumProduct.subscriptionTiers.forEach { tier ->
                            PricingCard(
                                product = tier,
                                priceLabel = tier.priceLabel(uiState.products),
                                selected = selectedTier == tier,
                                onSelect = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedTier = tier
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                (context as? Activity)?.let { viewModel.purchase(it, selectedTier) }
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Continue with ${selectedTier.displayName}" }
                        ) {
                            if (uiState.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Continue")
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                (context as? Activity)?.let { viewModel.purchase(it, PremiumProduct.RemoveAds) }
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Just remove ads for ${PremiumProduct.RemoveAds.priceLabel(uiState.products)}")
                        }

                        TextButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.restore()
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restore purchases")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { uriHandler.openUri(TERMS_OF_SERVICE_URL) }) {
                    Text("Terms of Service", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }) {
                    Text("Privacy Policy", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        uiState.message?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clickable { viewModel.dismissMessage() }
                    .semantics { contentDescription = "$message. Tap to dismiss." }
            ) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PricingCard(
    product: PremiumProduct,
    priceLabel: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val badge = when (product) {
        PremiumProduct.Yearly -> "Best value"
        PremiumProduct.Lifetime -> "Pay once"
        else -> null
    }

    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${product.displayName}, $priceLabel ${product.billingPeriodLabel}" +
                    if (badge != null) ", $badge" else ""
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Text(
                "$priceLabel ${product.billingPeriodLabel}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PremiumActiveSection(onManageSubscription: () -> Unit, onClose: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("You're a Premium member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "All premium features are unlocked. Thank you for supporting AquaCoach!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    TextButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onManageSubscription()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Manage subscription")
    }
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Close")
    }
}
