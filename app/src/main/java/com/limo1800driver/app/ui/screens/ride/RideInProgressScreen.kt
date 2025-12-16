package com.limo1800driver.app.ui.screens.ride

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.BuildConfig
import com.limo1800driver.app.ui.components.RideInProgressMap
import com.limo1800driver.app.ui.viewmodel.RideInProgressViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideInProgressScreen(
    bookingId: Int,
    onBack: () -> Unit,
    onNavigateToFinalizeRates: (bookingId: Int, mode: String, source: String) -> Unit = { _, _, _ -> },
    onNavigateToChat: (bookingId: Int, customerId: Int, customerName: String) -> Unit = { _, _, _ -> },
    viewModel: RideInProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // UI States
    var otp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showCompleteConfirm by remember { mutableStateOf(false) }
    var arrivedSeconds by remember { mutableStateOf(0) }
    var debugForcedArrive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // -- Location Permissions Logic --
    val requestLocationPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val fine = grantMap[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grantMap[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            val ride = uiState.activeRide
            if (ride != null) viewModel.startLiveTrackingIfNeeded(ride)
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPermission = fineGranted || coarseGranted
        if (!hasPermission) {
            requestLocationPermissions.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // -- Helper Functions --
    fun dialPassenger(phone: String?) {
        val number = phone?.trim().orEmpty()
        if (number.isNotBlank()) {
            context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$number") })
        }
    }

    // -- Timer Logic --
    LaunchedEffect(uiState.activeRide?.status) {
        if (uiState.activeRide?.status == "on_location") {
            arrivedSeconds = 0
            while (uiState.activeRide?.status == "on_location") {
                delay(1000)
                arrivedSeconds += 1
            }
        } else {
            arrivedSeconds = 0
        }
    }

    // -- Lifecycle --
    DisposableEffect(lifecycleOwner, uiState.isInChatMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.isInChatMode) viewModel.exitChatMode()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.activeRide == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val ride = uiState.activeRide!!
        val isRideStarted = uiState.isRideStarted
        val hasArrivedAtPickup = uiState.hasArrivedAtPickup
        val inPickupGeofence = uiState.pickupArrivalDetected || debugForcedArrive

        // Don't leak debug overrides across rides.
        LaunchedEffect(ride.bookingId) {
            debugForcedArrive = false
        }

        LaunchedEffect(ride.bookingId, ride.status) {
            viewModel.startLiveTrackingIfNeeded(ride)
        }

        val eta = uiState.etaText ?: "—"
        val dist = uiState.distanceText ?: "—"
        val passengerName = ride.customerName ?: "Passenger"

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContainerColor = Color.White,
            sheetShape = RideInProgressUiTokens.SheetShape,
            sheetShadowElevation = 16.dp,
            sheetPeekHeight = 230.dp,
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    // IMPORTANT: determine UI phase by ride status progression.
                    // If the ride is started (`en_route_do`), always show "Ride In Progress" even though
                    // `hasArrivedAtPickup` (status == on_location) will be false.
                    if (isRideStarted) {
                        // 4: Ride in progress (5 is the complete confirmation dialog)
                        StatusHeaderBanner("Ride In Progress")

                        Column(modifier = Modifier.padding(24.dp)) {
                            MetricHeader(eta = eta, distance = dist, subTitle = "Dropping Off $passengerName")
                            Spacer(Modifier.height(24.dp))

                            ConnectWithPassengerRow(
                                passengerName = passengerName,
                                onCall = { dialPassenger(ride.customerPhone) },
                                onChat = { viewModel.enterChatMode(); onNavigateToChat(ride.bookingId, ride.customerId, passengerName) }
                            )
                            Spacer(Modifier.height(24.dp))

                            TripTimelineView(pickup = ride.pickupAddress, dropoff = ride.dropoffAddress)
                            Spacer(Modifier.height(24.dp))

                            ShareTripButton(onClick = { /* Share Logic */ })
                            Spacer(Modifier.height(16.dp))

                            if (uiState.dropoffArrivalDetected) {
                                FullWidthActionButton(
                                    text = "Complete Ride",
                                    color = RideInProgressUiTokens.GreenButton,
                                    onClick = { showCompleteConfirm = true }
                                )
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    else if (!hasArrivedAtPickup) {
                        if (inPickupGeofence) {
                            // 2: Inside geofence - "Arrived" button activated, different header to indicate proximity
                            StatusHeaderBanner("You Are Near the Pickup Location")

                            Column(modifier = Modifier.padding(24.dp)) {
                                MetricHeader(eta = eta, distance = dist, subTitle = "Picking Up $passengerName")
                                Spacer(Modifier.height(24.dp))

                                ConnectWithPassengerRow(
                                    passengerName = passengerName,
                                    onCall = { dialPassenger(ride.customerPhone) },
                                    onChat = { viewModel.enterChatMode(); onNavigateToChat(ride.bookingId, ride.customerId, passengerName) }
                                )

                                Spacer(Modifier.height(32.dp)) // Extra space to make the Arrived button more prominent

                                FullWidthActionButton(
                                    text = "Arrived at Pickup Point",
                                    color = RideInProgressUiTokens.Orange,
                                    onClick = { viewModel.emitOnLocation(ride) }
                                )
                                Spacer(Modifier.height(16.dp))

                                TripTimelineView(pickup = ride.pickupAddress, dropoff = ride.dropoffAddress)
                                Spacer(Modifier.height(24.dp))

                                ShareTripButton(onClick = { /* Share Logic */ })
                                Spacer(Modifier.height(16.dp))

                                FullWidthActionButton(
                                    text = "Cancel Ride",
                                    color = Color(0xFFE53935),
                                    isOutline = true,
                                    onClick = { showCancelConfirm = true }
                                )
                                Spacer(Modifier.height(32.dp))
                            }
                        }
                        else {
                            // 1: Not in geofence - standard en route screen, no Arrived button
                            StatusHeaderBanner("On My Way To Pickup Location")

                            Column(modifier = Modifier.padding(24.dp)) {
                                MetricHeader(eta = eta, distance = dist, subTitle = "Picking Up $passengerName")
                                Spacer(Modifier.height(24.dp))

                                ConnectWithPassengerRow(
                                    passengerName = passengerName,
                                    onCall = { dialPassenger(ride.customerPhone) },
                                    onChat = { viewModel.enterChatMode(); onNavigateToChat(ride.bookingId, ride.customerId, passengerName) }
                                )

                                Spacer(Modifier.height(24.dp))

                                TripTimelineView(pickup = ride.pickupAddress, dropoff = ride.dropoffAddress)
                                Spacer(Modifier.height(24.dp))

                                ShareTripButton(onClick = { /* Share Logic */ })
                                Spacer(Modifier.height(16.dp))

                                if (BuildConfig.DEBUG) {
                                    TextButton(
                                        // "Geofence" here is a proximity gate (<=100m) driven by location updates.
                                        // In debug, allow forcing it open so the normal "Arrived at Pickup Point"
                                        // flow can be tested without physically driving to the pickup.
                                        onClick = { debugForcedArrive = !debugForcedArrive },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (debugForcedArrive) "Debug: Disable Pickup Geofence Override"
                                            else "Debug: Enable Pickup Geofence Override"
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }

                                FullWidthActionButton(
                                    text = "Cancel Ride",
                                    color = Color(0xFFE53935),
                                    isOutline = true,
                                    onClick = { showCancelConfirm = true }
                                )
                                Spacer(Modifier.height(32.dp))
                            }
                        }
                    }
                    else if (!isRideStarted) {
                        // 3: Arrived at pickup, waiting for passenger + PIN
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                                .imePadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val mm = arrivedSeconds / 60
                            val ss = arrivedSeconds % 60
                            BlackTimerPill(String.format("%02d : %02d", mm, ss))
                            Spacer(Modifier.height(12.dp))

                            WaitingForPassengerRow(
                                passengerName = passengerName,
                                onCall = { dialPassenger(ride.customerPhone) },
                                onChat = { viewModel.enterChatMode(); onNavigateToChat(ride.bookingId, ride.customerId, passengerName) }
                            )
                            Spacer(Modifier.height(24.dp))

                            Text(
                                "Ask the passenger for their 4-digit ride PIN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))

                            PinInputRow(
                                value = otp,
                                onValueChange = { if (it.length <= 4) otp = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (otpError != null) {
                                Text(otpError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                            Spacer(Modifier.height(24.dp))

                            FullWidthActionButton(
                                text = "Start The Ride",
                                color = RideInProgressUiTokens.Orange,
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                enabled = otp.length == 4,
                                onClick = {
                                    if (otp.length != 4) {
                                        otpError = "Please enter PIN"
                                        return@FullWidthActionButton
                                    }
                                    if (!viewModel.verifyAndStartRide(ride, otp)) {
                                        otpError = "Incorrect PIN"
                                    } else {
                                        otp = ""
                                        otpError = null
                                    }
                                }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    else {
                        // 4: Ride in progress handled above by `isRideStarted`.
                        // This branch should not be reachable unless new status values are added.
                        StatusHeaderBanner("Ride In Progress")
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) {
                    RideInProgressMap(
                        ride = uiState.activeRide,
                        driverLocation = uiState.displayLocation,
                        activeRoutePolyline = uiState.activeRoutePolyline,
                        previewRoutePolyline = uiState.previewRoutePolyline,
                        autoFollowEnabled = !uiState.isInChatMode,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.statusBarsPadding())
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Ride") },
            text = { Text("Are you sure you want to cancel this ride?") },
            confirmButton = { Button(onClick = { showCancelConfirm = false; viewModel.stopLiveTracking(); onBack() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Yes, Cancel") } },
            dismissButton = { TextButton(onClick = { showCancelConfirm = false }) { Text("No") } }
        )
    }

    if (showCompleteConfirm) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirm = false },
            title = { Text("Complete Ride") },
            text = { Text("Are you sure you want to complete this ride?") },
            confirmButton = { Button(onClick = { showCompleteConfirm = false; val r = uiState.activeRide; if (r != null) { viewModel.completeRide(r); onNavigateToFinalizeRates(r.bookingId, "finalizeOnly", "ride") } }, colors = ButtonDefaults.buttonColors(containerColor = RideInProgressUiTokens.GreenButton)) { Text("Complete") } },
            dismissButton = { TextButton(onClick = { showCompleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

