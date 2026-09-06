package com.factory.aquacoach.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.ui.components.ProBadge
import com.factory.aquacoach.ui.components.UnitFormat
import com.factory.aquacoach.ui.components.WaterProgressRing

private val QUICK_ADD_AMOUNTS_ML = listOf(100, 250, 350, 500)

@Composable
fun HomeScreen(viewModel: HomeViewModel, onUpgradeRequired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var showCustomDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading today's hydration" })
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text("AquaCoach", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        val progress = if (uiState.goalMl > 0) uiState.todayTotalMl.toFloat() / uiState.goalMl.toFloat() else 0f
        WaterProgressRing(
            progress = progress,
            modifier = Modifier
                .size(220.dp)
                .clearAndSetSemantics {
                    contentDescription = "${(progress.coerceIn(0f, 1f) * 100).toInt()} percent of daily goal reached, " +
                        "${UnitFormat.display(uiState.todayTotalMl, uiState.unit)} of ${UnitFormat.display(uiState.goalMl, uiState.unit)}"
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    UnitFormat.display(uiState.todayTotalMl, uiState.unit),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "of ${UnitFormat.display(uiState.goalMl, uiState.unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        val remaining = (uiState.goalMl - uiState.todayTotalMl).coerceAtLeast(0)
        Text(
            text = if (remaining == 0) {
                "Goal reached! Great job staying hydrated."
            } else {
                "${UnitFormat.display(remaining, uiState.unit)} left to reach your goal"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))
        Text(
            "Quick add",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QUICK_ADD_AMOUNTS_ML.forEach { amount ->
                QuickAddButton(
                    amountMl = amount,
                    unit = uiState.unit,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.addWater(amount)
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (uiState.isPremium) showCustomDialog = true else onUpgradeRequired()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (uiState.isPremium) {
                        "Add a custom amount"
                    } else {
                        "Add a custom amount, premium feature"
                    }
                }
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Custom amount")
            if (!uiState.isPremium) {
                Spacer(Modifier.width(8.dp))
                ProBadge()
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Today's log",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(12.dp))
        if (uiState.entries.isEmpty()) {
            Text(
                "No entries yet today. Add your first glass of water!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            uiState.entries.forEach { entry ->
                EntryRow(entry = entry, unit = uiState.unit, onDelete = { viewModel.deleteEntry(entry.id) })
            }
        }

        if (!uiState.isAdsRemoved) {
            Spacer(Modifier.height(24.dp))
            AdBanner(onRemoveAds = onUpgradeRequired)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showCustomDialog) {
        CustomAmountDialog(
            unit = uiState.unit,
            onDismiss = { showCustomDialog = false },
            onConfirm = { amountMl ->
                viewModel.addWater(amountMl)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun QuickAddButton(amountMl: Int, unit: MeasurementUnit, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.semantics {
            contentDescription = "Add ${UnitFormat.display(amountMl, unit)} of water"
        }
    ) {
        Text(UnitFormat.display(amountMl, unit))
    }
}

@Composable
private fun AdBanner(onRemoveAds: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(
                "Advertisement",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRemoveAds) { Text("Remove ads") }
        }
    }
}
