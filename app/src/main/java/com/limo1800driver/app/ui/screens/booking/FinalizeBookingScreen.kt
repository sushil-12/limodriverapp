package com.limo1800driver.app.ui.screens.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.ui.viewmodel.BookingPreviewViewModel

@Composable
fun FinalizeBookingScreen(
    bookingId: Int,
    source: String,
    onBack: () -> Unit,
    onNext: (bookingId: Int, mode: String, source: String) -> Unit
) {
    val viewModel: BookingPreviewViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookingId) {
        if (bookingId != 0) viewModel.load(bookingId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Finalize Booking #$bookingId",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
            state.preview != null -> {
                val p = state.preview!!
                Text(text = "Service Type: ${p.serviceType.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Transfer Type: ${p.transferType.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Pickup: ${p.pickupAddress.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Dropoff: ${p.dropoffAddress.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Passenger: ${p.passengerName.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quote Amt: ${p.currencySymbol.orEmpty()}${p.grandTotal ?: 0.0}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = { onNext(bookingId, "finalizeOnly", source) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Next") }
                }
            }
            else -> Text(text = "No booking data available")
        }
    }
}


