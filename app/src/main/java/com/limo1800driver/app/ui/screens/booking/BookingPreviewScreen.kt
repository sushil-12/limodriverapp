package com.limo1800driver.app.ui.screens.booking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewExtraStop
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.LimoRed
import com.limo1800driver.app.ui.components.ErrorAlertDialog
import com.limo1800driver.app.ui.components.SuccessAlertDialog
import com.limo1800driver.app.ui.viewmodel.BookingPreviewViewModel

// Standard gray for labels
private val LabelGray = Color(0xFF757575)
private val DividerColor = Color(0xFFEEEEEE)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingPreviewScreen(
    bookingId: Int,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    onNavigateToFinalizeRates: (bookingId: Int, mode: String, source: String) -> Unit,
    source: String = "prearranged"
) {
    val viewModel: BookingPreviewViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookingId) {
        if (bookingId != 0) viewModel.load(bookingId)
    }

    var successDialogMessage by remember { mutableStateOf<String?>(null) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.successMessage) {
        if (!state.successMessage.isNullOrBlank()) {
            successDialogMessage = state.successMessage
        }
    }

    LaunchedEffect(state.error) {
        if (!state.error.isNullOrBlank()) {
            errorDialogMessage = state.error
        }
    }

    val preview = state.preview
    val reservationIdDisplay = preview?.reservationId ?: bookingId
    val isPending = preview?.bookingStatus.equals("pending", ignoreCase = true)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            PreviewBookingHeader(
                bookingNumber = reservationIdDisplay,
                onBack = onBack,
                onClose = onBack
            )
        },
        bottomBar = {
            if (preview != null) {
                PreviewBottomBar(
                    isPending = isPending,
                    isAccepting = state.isAccepting,
                    isRejecting = state.isRejecting,
                    isFinalizing = false,
                    onAccept = { viewModel.accept(bookingId) },
                    onReject = { viewModel.reject(bookingId) },
                    onFinalize = { onNavigateToFinalizeRates(bookingId, "finalizeOnly", source) }
                )
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShimmerCircle(size = 32.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading details...", color = LabelGray)
                }
            }

            preview == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No booking data available", color = LabelGray)
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    TravelInformationSection(preview)
                    MapSection(preview)
                    SectionDivider()
                    PassengerInformationSection(preview)
                    SpecialInstructionsSection(preview)
                    MeetAndGreetSection(preview)
                    CancellationPeriodSection(preview)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Success Dialog
    SuccessAlertDialog(
        isVisible = successDialogMessage != null,
        onDismiss = {
            successDialogMessage = null
            viewModel.consumeSuccess()
            onCompleted()
        },
        title = "Success",
        message = successDialogMessage.orEmpty()
    )

    // Error Dialog
    ErrorAlertDialog(
        isVisible = errorDialogMessage != null && !state.isLoading,
        onDismiss = { errorDialogMessage = null },
        title = "Error",
        message = errorDialogMessage.orEmpty()
    )
}

@Composable
private fun PreviewBookingHeader(
    bookingNumber: Int,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }

            Text(
                text = "Preview Booking #$bookingNumber",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
            }
        }
    }
}

@Composable
private fun PreviewBottomBar(
    isPending: Boolean,
    isAccepting: Boolean,
    isRejecting: Boolean,
    isFinalizing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onFinalize: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // REJECT Button
                    Button(
                        onClick = onReject,
                        enabled = !isAccepting && !isRejecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRejecting) Color.Gray else LimoRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (isRejecting) {
                            ShimmerCircle(
                                size = 24.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isRejecting) "Rejecting..." else "Reject",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // ACCEPT Button
                    Button(
                        onClick = onAccept,
                        enabled = !isAccepting && !isRejecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccepting) Color.Gray else LimoGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (isAccepting) {
                            ShimmerCircle(
                                size = 24.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isAccepting) "Accepting..." else "Accept",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Button(
                    onClick = onFinalize,
                    enabled = !isFinalizing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFinalizing) Color.Gray else LimoGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isFinalizing) {
                        ShimmerCircle(size = 20.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = if (isFinalizing) "Finalizing..." else "Finalize")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TravelInformationSection(p: AdminBookingPreviewData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SectionTitle("Travel Information")
        Spacer(modifier = Modifier.height(16.dp))

        // DATE & TIME (Side by Side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                InlineDetail(label = "DATE", value = formatPickupDate(p.pickupDate))
            }
            Box(modifier = Modifier.weight(1f)) {
                InlineDetail(label = "TIME", value = formatPickupTime(p.pickupTime))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = DividerColor)
        Spacer(modifier = Modifier.height(12.dp))

        // COMPACT ADDRESSES
        if (p.isAirportTransfer()) {
            DetailRow(label = "PICKUP", value = p.getPickupLocationName())
            val airline = p.getPickupAirlineInfo()
            if (airline.isNotBlank()) {
                DetailRow(label = "AIRLINE", value = "$airline (Update Arrival Time)")
            }
        } else {
            DetailRow(label = "PICKUP", value = p.pickupAddress.orEmpty().ifBlank { "N/A" })
        }

        DetailRow(label = "DROP OFF", value = p.getDropoffLocationName())

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = DividerColor)
        Spacer(modifier = Modifier.height(12.dp))

        // VEHICLE
        DetailRow(
            label = "VEHICLE",
            value = p.vehicleTypeName.orEmpty().ifBlank { "N/A" }
        )
    }
}

@Composable
private fun MapSection(p: AdminBookingPreviewData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        val pickup = p.getFinalPickupLatLng()
        val dropoff = p.getFinalDropoffLatLng()
        val stops = p.getExtraStopLatLngs()

        if (pickup != null && dropoff != null) {
            // MAP SURFACE
            Surface(
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                BookingRouteMap(
                    pickup = pickup,
                    dropoff = dropoff,
                    extraStops = stops,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TRIP INFO (Duration / Distance) - Moved under map
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    InlineDetail(label = "DURATION", value = formatDuration(p.duration))
                }
                Box(modifier = Modifier.weight(1f)) {
                    InlineDetail(label = "DISTANCE", value = formatDistance(p.distance))
                }
            }

        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F2F2)),
                contentAlignment = Alignment.Center
            ) {
                Text("Location data not available", color = LabelGray)
            }
        }
    }
}

@Composable
private fun PassengerInformationSection(p: AdminBookingPreviewData) {
    Column(modifier = Modifier.padding(16.dp)) {
        SectionTitle("Passenger Information")
        Spacer(modifier = Modifier.height(16.dp))



        // Row 2+: Contact Details
        DetailRow(label = "NAME", value = p.passengerName.orEmpty().ifBlank { "N/A" })
        DetailRow(label = "PHONE", value = p.getPassengerPhone())
        DetailRow(label = "EMAIL", value = p.passengerEmail.orEmpty().ifBlank { "N/A" })
        // Row 1: Passengers and Luggage grouped side-by-side
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                InlineDetail(label = "PASSENGERS", value = (p.totalPassengers ?: 0).toString())
            }
            Box(modifier = Modifier.weight(1f)) {
                InlineDetail(label = "LUGGAGE", value = (p.luggageCount ?: 0).toString())
            }
        }
    }
    SectionDivider()
}

@Composable
private fun SpecialInstructionsSection(p: AdminBookingPreviewData) {
    val instructions = p.bookingInstructions.orEmpty().trim()
    Column(modifier = Modifier.padding(16.dp)) {
        SectionTitle("Special Instructions")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (instructions.isBlank()) "N/A" else instructions,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            lineHeight = 20.sp
        )
    }
    SectionDivider()
}

@Composable
private fun MeetAndGreetSection(p: AdminBookingPreviewData) {
    Column(modifier = Modifier.padding(16.dp)) {
        SectionTitle("Meet and Greet")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = p.meetGreetChoiceName.orEmpty().ifBlank { "N/A" },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
    }
    SectionDivider()
}

@Composable
private fun CancellationPeriodSection(p: AdminBookingPreviewData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CANCELLATION PERIOD",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = LimoRed
        )
        Text(
            text = p.getCancellationPeriodDisplay(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = LimoRed
        )
    }
}

// ---------------- UI Helpers (Compact & Aligned) ----------------

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = LimoOrange
    )
}

@Composable
private fun SectionDivider() {
    Divider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun InlineDetail(
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = LabelGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Label Column (Fixed ratio for alignment)
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray,
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .padding(top = 2.dp) // Optical alignment
        )
        // Value Column
        Text(
            text = value.ifBlank { "N/A" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------- Helpers ----------------

private fun AdminBookingPreviewData.isAirportTransfer(): Boolean =
    transferType?.contains("airport", ignoreCase = true) == true

private fun AdminBookingPreviewData.getPickupLocationName(): String {
    val airport = pickupAirportName.orEmpty().trim()
    val addr = pickupAddress.orEmpty().trim()
    return if (isAirportTransfer() && airport.isNotBlank()) airport else addr.ifBlank { "N/A" }
}

private fun AdminBookingPreviewData.getDropoffLocationName(): String {
    val airport = dropoffAirportName.orEmpty().trim()
    val addr = dropoffAddress.orEmpty().trim()
    return if (isAirportTransfer() && airport.isNotBlank()) airport else addr.ifBlank { "N/A" }
}

private fun AdminBookingPreviewData.getPickupAirlineInfo(): String {
    val airline = pickupAirlineName.orEmpty().trim()
    val flight = pickupFlight.orEmpty().trim()
    return if (airline.isNotBlank() && flight.isNotBlank()) "$airline #$flight" else ""
}

private fun AdminBookingPreviewData.getPassengerPhone(): String {
    val isd = passengerCellIsd.orEmpty().trim()
    val cell = passengerCell.orEmpty().trim()
    if (isd.isBlank() && cell.isBlank()) return "N/A"
    return "$isd $cell".trim()
}

private fun AdminBookingPreviewData.getCancellationPeriodDisplay(): String {
    val hours = cancellationHours
    return if (hours != null) "$hours Hours" else "N/A"
}

private fun AdminBookingPreviewData.getFinalPickupLatLng(): LatLng? {
    return if (isAirportTransfer() && pickupAirportLatitude != null && pickupAirportLongitude != null) {
        LatLng(pickupAirportLatitude, pickupAirportLongitude)
    } else {
        val lat = pickupLatitude?.toDoubleOrNull()
        val lng = pickupLongitude?.toDoubleOrNull()
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }
}

private fun AdminBookingPreviewData.getFinalDropoffLatLng(): LatLng? {
    return if (isAirportTransfer() && dropoffAirportLatitude != null && dropoffAirportLongitude != null) {
        LatLng(dropoffAirportLatitude, dropoffAirportLongitude)
    } else {
        val lat = dropoffLatitude?.toDoubleOrNull()
        val lng = dropoffLongitude?.toDoubleOrNull()
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }
}

private fun AdminBookingPreviewData.getExtraStopLatLngs(): List<LatLng> =
    extraStops.orEmpty().mapNotNull { it.toLatLngOrNull() }

private fun AdminBookingPreviewExtraStop.toLatLngOrNull(): LatLng? {
    val lat = latitude?.toDoubleOrNull()
    val lng = longitude?.toDoubleOrNull()
    return if (lat != null && lng != null) LatLng(lat, lng) else null
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatPickupDate(date: String?): String {
    val raw = date.orEmpty().trim()
    if (raw.isBlank()) return "N/A"
    return try {
        val input = java.time.LocalDate.parse(raw)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        input.format(formatter)
    } catch (_: Exception) { raw }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatPickupTime(time: String?): String {
    val raw = time.orEmpty().trim()
    if (raw.isBlank()) return "N/A"
    return try {
        val t = java.time.LocalTime.parse(raw) // Assuming hh:mm:ss format
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        t.format(formatter)
    } catch (_: Exception) {
        raw
    }
}

private fun formatDistance(distance: String?): String {
    val raw = distance.orEmpty().trim()
    val v = raw.toDoubleOrNull()
    if (v != null) {
        return if (v > 1000) {
            val km = v / 1000.0
            val miles = v / 1609.0
            String.format("%.2f Miles / %.2f Km", miles, km)
        } else {
            val km = v * 1.60934
            String.format("%.2f Miles / %.2f Km", v, km)
        }
    }
    return raw
}

private fun formatDuration(duration: String?): String {
    val raw = duration.orEmpty().trim()
    val seconds = raw.toDoubleOrNull()?.toInt()
    if (seconds != null && seconds > 0) {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return "$hours hrs, $mins mins"
    }
    return raw
}

// ---------------- PREVIEW ----------------

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewBookingDetail() {
    val dummyData = AdminBookingPreviewData(
        reservationId = 12345,
        bookingStatus = "pending",
        pickupDate = "2023-12-25",
        pickupTime = "14:30:00",
        pickupAddress = "123 Main St, New York, NY",
        dropoffAddress = "JFK Airport Terminal 4",
        vehicleTypeName = "SUV / Cadillac Escalade",
        passengerName = "John Doe",
        passengerCell = "555-0199",
        totalPassengers = 2,
        luggageCount = 3,
        duration = "3600",
        distance = "15.5",
        currencySymbol = "$"
    )

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        TravelInformationSection(dummyData)
        MapSection(dummyData)
        SectionDivider()
        PassengerInformationSection(dummyData)
    }
}