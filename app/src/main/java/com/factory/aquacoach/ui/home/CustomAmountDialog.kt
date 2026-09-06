package com.factory.aquacoach.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.factory.aquacoach.data.preferences.MeasurementUnit
import com.factory.aquacoach.ui.components.UnitFormat
import kotlin.math.roundToInt

@Composable
fun CustomAmountDialog(
    unit: MeasurementUnit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val label = if (unit == MeasurementUnit.ML) "Amount (ml)" else "Amount (oz)"
    val haptics = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    fun submit() {
        val value = text.toFloatOrNull()
        if (value != null && value > 0f) {
            val amountMl = if (unit == MeasurementUnit.ML) value.roundToInt() else UnitFormat.ozToMl(value)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onConfirm(amountMl)
        } else {
            isError = true
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom amount") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' }) {
                            text = newValue
                            isError = false
                        }
                    },
                    label = { Text(label) },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Enter an amount greater than 0") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
