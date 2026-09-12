package com.timur.receiptlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timur.receiptlogger.data.Trip
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    trips: List<Trip>,
    totalForTrip: (Long) -> Flow<Double>,
    onTripClick: (Long) -> Unit,
    onAddTripClick: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Business Trips") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTripClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add trip")
            }
        }
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyTripsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripRow(
                        trip = trip,
                        // remember() keeps this the same Flow instance across recompositions
                        // (unless the trip id changes) so collectAsState doesn't resubscribe every frame.
                        totalFlow = remember(trip.id) { totalForTrip(trip.id) },
                        onClick = { onTripClick(trip.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTripsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Flight,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "No trips yet",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                "Tap + to start a new business trip",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TripRow(trip: Trip, totalFlow: Flow<Double>, onClick: () -> Unit) {
    val total by totalFlow.collectAsState(initial = 0.0)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(trip.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDateRange(trip.startDate, trip.endDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatAmount(total), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatAmount(amount: Double): String =
    NumberFormat.getCurrencyInstance().format(amount)

private fun formatDateRange(startDate: Long, endDate: Long): String {
    val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
    return "${df.format(Date(startDate))} – ${df.format(Date(endDate))}"
}
