package com.factory.aquacoach.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.factory.aquacoach.data.local.DailyTotalRow
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.ui.components.ProBadge
import com.factory.aquacoach.ui.components.UnitFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onUpgradeRequired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading history" })
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (!uiState.isPremium) {
                Spacer(Modifier.width(8.dp))
                ProBadge()
            }
        }
        Spacer(Modifier.height(16.dp))

        if (uiState.dailyTotals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No history yet. Start logging water to see your trends here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.dailyTotals, key = { it.dayKey }) { day ->
                    DailyHistoryRow(day = day, goalMl = uiState.goalMl, unit = uiState.unit)
                }
            }
            if (uiState.hasMoreHistory) {
                UnlockHistoryCard(onUpgradeRequired = onUpgradeRequired)
            }
        }
    }
}

@Composable
private fun UnlockHistoryCard(onUpgradeRequired: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Unlock your full hydration history", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Go Premium to see trends beyond the last $FREE_HISTORY_DAYS days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onUpgradeRequired()
            }) { Text("Upgrade to Premium") }
        }
    }
}

@Composable
private fun DailyHistoryRow(day: DailyTotalRow, goalMl: Int, unit: MeasurementUnit) {
    val dateLabel = remember(day.dayKey) {
        LocalDate.parse(day.dayKey).format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    val progress = if (goalMl > 0) (day.totalMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) else 0f
    val goalMet = day.totalMl >= goalMl

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$dateLabel, ${UnitFormat.display(day.totalMl, unit)} of ${UnitFormat.display(goalMl, unit)}" +
                    if (goalMet) ", goal met" else ""
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateLabel, style = MaterialTheme.typography.titleMedium)
                if (goalMet) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${UnitFormat.display(day.totalMl, unit)} of ${UnitFormat.display(goalMl, unit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
