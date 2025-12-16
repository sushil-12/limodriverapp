package com.limo1800driver.app.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewExtraStop
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.LimoRed
import com.limo1800driver.app.ui.viewmodel.BookingPreviewViewModel

// Standard gray for labels
private val LabelGray = Color(0xFF757575)
private val DividerColor = Color(0xFFEEEEEE)

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
                    CircularProgressIndicator(color = LimoOrange)
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
                    RatesDistributionSection(preview)
                    CancellationPeriodSection(preview)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Dialog handling remains same
    if (successDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Success") },
            text = { Text(successDialogMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    successDialogMessage = null
                    viewModel.consumeSuccess()
                    onCompleted()
                }) { Text("OK", color = LimoOrange) }
            },
            containerColor = Color.White
        )
    }

    if (errorDialogMessage != null && !state.isLoading) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("Error") },
            text = { Text(errorDialogMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { errorDialogMessage = null }) { Text("OK", color = LimoOrange) }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun PreviewBookingHeader(
    bookingNumber: Int,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp
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
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // REJECT Button (Left Side)
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
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = if (isRejecting) "Rejecting..." else "Reject")
                    }

                    // ACCEPT Button (Right Side)
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
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = if (isAccepting) "Accepting..." else "Accept")
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
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = if (isFinalizing) "Finalizing..." else "Finalize")
                }
            }
        }
    }
}

@Composable
private fun TravelInformationSection(p: AdminBookingPreviewData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SectionTitle("Travel Information")
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PICKUP DETAILS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
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

        if (p.isAirportTransfer()) {
            DetailRow(label = "AIRPORT", value = p.getPickupLocationName())
            val airline = p.getPickupAirlineInfo()
            if (airline.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = "AIRLINE", value = "$airline (Update Arrival Time)")
            }
        } else {
            DetailRow(label = "ADDRESS", value = p.pickupAddress.orEmpty().ifBlank { "N/A" })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DROP OFF DETAILS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "ADDRESS", value = p.getDropoffLocationName())

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = DividerColor)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "VEHICLE DETAILS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        DetailRow(
            label = "Vehicle Type",
            value = p.vehicleTypeName.orEmpty().ifBlank { "N/A" }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Trip Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(12.dp))


        Text(
            text = "Trip Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                InlineDetail(label = "TOTAL TIME", value = formatDuration(p.duration))
            }
            Box(modifier = Modifier.weight(1f)) {
                InlineDetail(label = "TOTAL DISTANCE", value = formatDistance(p.distance))
            }
        }
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
        SectionTitle("Map")
        Spacer(modifier = Modifier.height(12.dp))

        val pickup = p.getFinalPickupLatLng()
        val dropoff = p.getFinalDropoffLatLng()
        val stops = p.getExtraStopLatLngs()

        if (pickup != null && dropoff != null) {
            Surface(
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                BookingRouteMap(
                    pickup = pickup,
                    dropoff = dropoff,
                    extraStops = stops,
                    modifier = Modifier.fillMaxSize()
                )
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

        DetailRow(label = "NAME", value = p.passengerName.orEmpty().ifBlank { "N/A" })
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "PHONE", value = p.getPassengerPhone())
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "EMAIL", value = p.passengerEmail.orEmpty().ifBlank { "N/A" })
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
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
            color = Color.Black
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
private fun VehicleTypeSection(p: AdminBookingPreviewData) {
    Column(modifier = Modifier.padding(16.dp)) {
        SectionTitle("Vehicle Type")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = p.vehicleTypeName.orEmpty().ifBlank { "N/A" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
    SectionDivider()
}

@Composable
private fun RatesDistributionSection(p: AdminBookingPreviewData) {
    Column(modifier = Modifier.padding(16.dp)) {
        SectionTitle("Rates Distribution")
        Spacer(modifier = Modifier.height(16.dp))

        val symbol = p.currencySymbol.orEmpty()
        val rates = p.ratesPreview
        
        // Use fallbacks to grandTotal if ratesPreview is missing
        val baseRate = rates?.baseRate?.toDoubleOrNull() ?: 0.0
        val adminShare = rates?.adminShare?.toDoubleOrNull() ?: 0.0
        val stripeFee = rates?.stripeFee?.toDoubleOrNull() ?: 0.0
        val affiliateShare = rates?.affiliateShare?.toDoubleOrNull() ?: 0.0
        val totalClient = rates?.grandTotal?.toDoubleOrNull() ?: (p.grandTotal ?: 0.0)

        RatesRow(label = "BASE RATE", value = formatCurrency(symbol, baseRate), subText = "Fare Base Rate")
        Spacer(modifier = Modifier.height(10.dp))
        
        RatesRow(label = "TOTAL CLIENT COST", value = formatCurrency(symbol, totalClient), subText = "Actual Cost To Customer")
        Spacer(modifier = Modifier.height(10.dp))

        RatesRow(label = "AFFILIATE PAYOUT", value = formatCurrency(symbol, affiliateShare), subText = "Net Payout", isGreen = true)
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

// ---------------- UI Helpers (Fixed Layout Issues) ----------------

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
private fun InlineDetail(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    // Using a Row with weights to ensure alignment on different screen sizes
    // instead of fixed width which causes layout issues.
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray,
            modifier = Modifier.width(140.dp) // Slightly reduced from 170dp for better fit
        )
        Text(
            text = value.ifBlank { "N/A" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.weight(1f) // Takes remaining space
        )
    }
}

@Composable
private fun RatesRow(
    label: String, 
    value: String, 
    subText: String? = null, 
    isGreen: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.width(140.dp)) {
            Text(
                text = "$label:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isGreen) LimoGreen else LabelGray
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGreen) LimoGreen else Color.Black
            )
            if (subText != null) {
                Text(
                    text = "($subText)",
                    fontSize = 11.sp,
                    color = LabelGray,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun BookingRouteMap(
    pickup: LatLng,
    dropoff: LatLng,
    extraStops: List<LatLng>,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()
    val mapUiSettings = remember {
        MapUiSettings(zoomControlsEnabled = false, compassEnabled = false, myLocationButtonEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false)
    }

    LaunchedEffect(pickup, dropoff, extraStops) {
        fitCameraToPoints(
            cameraPositionState = cameraPositionState,
            points = listOf(pickup) + extraStops + listOf(dropoff)
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = mapUiSettings
    ) {
        Marker(
            state = MarkerState(position = pickup),
            title = "Pickup",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        )

        extraStops.forEachIndexed { index, stop ->
            Marker(
                state = MarkerState(position = stop),
                title = "Stop ${index + 1}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            )
        }

        Marker(
            state = MarkerState(position = dropoff),
            title = "Dropoff",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )

        val routePoints = listOf(pickup) + extraStops + listOf(dropoff)
        if (routePoints.size >= 2) {
            Polyline(
                points = routePoints,
                color = Color(0xFF2196F3),
                width = 8f
            )
        }
    }
}

private suspend fun fitCameraToPoints(
    cameraPositionState: CameraPositionState,
    points: List<LatLng>
) {
    if (points.isEmpty()) return
    val builder = LatLngBounds.Builder()
    points.forEach { builder.include(it) }
    
    try {
        val bounds = builder.build()
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngBounds(bounds, 100), // Increased padding
            durationMs = 700
        )
    } catch (e: Exception) {
        // Handle edge cases where map isn't laid out yet
    }
}

// ---------------- Helpers (Identical logic to before) ----------------

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

private fun formatPickupDate(date: String?): String {
    val raw = date.orEmpty().trim()
    if (raw.isBlank()) return "N/A"
    return try {
        val input = java.time.LocalDate.parse(raw)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        input.format(formatter)
    } catch (_: Exception) { raw }
}

private fun formatPickupTime(time: String?): String {
    val raw = time.orEmpty().trim()
    if (raw.isBlank()) return "N/A"
    return try {
        val t = java.time.LocalTime.parse(raw) // Assuming hh:mm:ss format
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        t.format(formatter)
    } catch (_: Exception) { 
        // Fallback for flexible formats
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

private fun formatCurrency(symbol: String, amount: Double): String =
    "$symbol${String.format("%.2f", amount)}"