package com.factory.aquacoach.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.factory.aquacoach.R
import com.factory.aquacoach.data.local.WaterEntryEntity
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.ui.components.UnitFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EntryRow(entry: WaterEntryEntity, unit: MeasurementUnit, onDelete: () -> Unit) {
    val timeText = remember(entry.timestampEpochMillis) {
        Instant.ofEpochMilli(entry.timestampEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_water_drop),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(UnitFormat.display(entry.amountMl, unit), style = MaterialTheme.typography.bodyLarge)
            Text(timeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onDelete()
        }) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete entry of ${UnitFormat.display(entry.amountMl, unit)} at $timeText"
            )
        }
    }
}
