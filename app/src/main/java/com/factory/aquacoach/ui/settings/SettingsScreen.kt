package com.factory.aquacoach.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.ui.components.ProBadge
import com.factory.aquacoach.ui.components.UnitFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onUpgradeRequired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val haptics = LocalHapticFeedback.current
    var showGoalDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showQuietHoursDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* NotificationHelper re-checks the permission before posting either way */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        if (uiState.isPremium) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("You're a Premium member", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUpgradeRequired()
                    })
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Upgrade to Premium. Unlock full history, custom reminders, and more"
                        role = Role.Button
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Upgrade to Premium", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Unlock full history, custom reminders, and more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Upgrade", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        SettingsSectionTitle("Goal")
        SettingsRow(
            title = "Daily goal",
            value = UnitFormat.display(settings.dailyGoalMl, settings.unit),
            onClick = { showGoalDialog = true }
        )

        SettingsSectionTitle("Units")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Measurement unit", modifier = Modifier.weight(1f))
            SegmentedUnitPicker(selected = settings.unit, onSelect = viewModel::setUnit)
        }

        SettingsSectionTitle("Reminders")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Gentle reminders", modifier = Modifier.weight(1f))
            Switch(
                checked = settings.remindersEnabled,
                onCheckedChange = { enabled ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.setRemindersEnabled(enabled)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Gentle reminders"
                }
            )
        }
        if (settings.remindersEnabled) {
            SettingsRow(
                title = "Reminder interval",
                value = "Every ${settings.reminderIntervalMinutes} min",
                locked = !uiState.isPremium,
                onClick = { if (uiState.isPremium) showIntervalDialog = true else onUpgradeRequired() }
            )
            SettingsRow(
                title = "Quiet hours",
                value = "${formatHour(settings.quietHoursStartHour)} – ${formatHour(settings.quietHoursEndHour)}",
                locked = !uiState.isPremium,
                onClick = { if (uiState.isPremium) showQuietHoursDialog = true else onUpgradeRequired() }
            )
        }
    }

    if (showGoalDialog) {
        GoalPickerDialog(
            currentMl = settings.dailyGoalMl,
            unit = settings.unit,
            onDismiss = { showGoalDialog = false },
            onConfirm = {
                viewModel.setDailyGoal(it)
                showGoalDialog = false
            }
        )
    }
    if (showIntervalDialog) {
        IntervalPickerDialog(
            currentMinutes = settings.reminderIntervalMinutes,
            onDismiss = { showIntervalDialog = false },
            onConfirm = {
                viewModel.setReminderInterval(it)
                showIntervalDialog = false
            }
        )
    }
    if (showQuietHoursDialog) {
        QuietHoursDialog(
            startHour = settings.quietHoursStartHour,
            endHour = settings.quietHoursEndHour,
            onDismiss = { showQuietHoursDialog = false },
            onConfirm = { start, end ->
                viewModel.setQuietHours(start, end)
                showQuietHoursDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(title: String, value: String, locked: Boolean = false, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            })
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = if (locked) "$title, $value, premium feature" else "$title, $value"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title)
            if (locked) {
                Spacer(Modifier.width(6.dp))
                ProBadge()
            }
        }
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SegmentedUnitPicker(selected: MeasurementUnit, onSelect: (MeasurementUnit) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val options = listOf(MeasurementUnit.ML to "ml", MeasurementUnit.OZ to "oz")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.semantics { contentDescription = "Measurement unit" }
    ) {
        options.forEachIndexed { index, (unit, label) ->
            SegmentedButton(
                selected = selected == unit,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(unit)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.semantics { contentDescription = "Use $label" }
            ) {
                Text(label)
            }
        }
    }
}

internal fun formatHour(hour: Int): String =
    LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("h a"))
