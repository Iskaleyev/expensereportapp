package com.timur.receiptlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timur.receiptlogger.data.Trip
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    existingTrip: Trip? = null,
    onBack: () -> Unit,
    onCreate: suspend (name: String, startDate: Long, endDate: Long) -> Long,
    onCreated: (tripId: Long) -> Unit
) {
    val isEditing = existingTrip != null
    var name by remember { mutableStateOf(existingTrip?.name ?: "") }
    var startDate by remember { mutableStateOf(existingTrip?.startDate) }
    var endDate by remember { mutableStateOf(existingTrip?.endDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val start = startDate
    val end = endDate
    val datesInOrder = start == null || end == null || start <= end
    val canSave = name.isNotBlank() && start != null && end != null && datesInOrder

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit trip" else "New trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip name") },
                placeholder = { Text("e.g. Chicago conference") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            DateField(
                label = "Start date",
                dateMillis = startDate,
                onClick = { showStartPicker = true }
            )

            DateField(
                label = "End date",
                dateMillis = endDate,
                onClick = { showEndPicker = true }
            )

            if (!datesInOrder) {
                Text(
                    "End date can't be before the start date",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val trimmed = name.trim()
                    val s = startDate
                    val e = endDate
                    if (trimmed.isNotEmpty() && s != null && e != null && s <= e) {
                        scope.launch {
                            val tripId = onCreate(trimmed, s, e)
                            onCreated(tripId)
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Save changes" else "Create trip")
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = state.selectedDateMillis
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate ?: startDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = state.selectedDateMillis
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateField(label: String, dateMillis: Long?, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                dateMillis?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
                    ?: "Select date",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
