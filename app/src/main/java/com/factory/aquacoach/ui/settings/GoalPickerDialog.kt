package com.factory.aquacoach.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.ui.components.UnitFormat
import kotlin.math.roundToInt

@Composable
fun GoalPickerDialog(
    currentMl: Int,
    unit: MeasurementUnit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentMl.toFloat()) }
    val haptics = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily goal") },
        text = {
            Column {
                Text(UnitFormat.display(sliderValue.roundToInt(), unit), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    valueRange = 500f..5000f,
                    steps = 44,
                    modifier = Modifier.semantics {
                        contentDescription = "Daily goal, ${UnitFormat.display(sliderValue.roundToInt(), unit)}"
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirm(sliderValue.roundToInt())
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun IntervalPickerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentMinutes.toFloat()) }
    val haptics = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder interval") },
        text = {
            Column {
                Text("Every ${sliderValue.roundToInt()} min", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    valueRange = 15f..240f,
                    steps = 14,
                    modifier = Modifier.semantics {
                        contentDescription = "Reminder interval, every ${sliderValue.roundToInt()} minutes"
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirm(sliderValue.roundToInt())
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuietHoursDialog(
    startHour: Int,
    endHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (start: Int, end: Int) -> Unit
) {
    var startSlider by remember { mutableStateOf(startHour.toFloat()) }
    var endSlider by remember { mutableStateOf(endHour.toFloat()) }
    val haptics = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quiet hours") },
        text = {
            Column {
                Text("Start", style = MaterialTheme.typography.labelLarge)
                Row {
                    Slider(
                        value = startSlider,
                        onValueChange = { startSlider = it },
                        onValueChangeFinished = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Quiet hours start, ${formatHour(startSlider.roundToInt())}" }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatHour(startSlider.roundToInt()))
                }
                Spacer(Modifier.height(16.dp))
                Text("End", style = MaterialTheme.typography.labelLarge)
                Row {
                    Slider(
                        value = endSlider,
                        onValueChange = { endSlider = it },
                        onValueChangeFinished = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Quiet hours end, ${formatHour(endSlider.roundToInt())}" }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatHour(endSlider.roundToInt()))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirm(startSlider.roundToInt(), endSlider.roundToInt())
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
