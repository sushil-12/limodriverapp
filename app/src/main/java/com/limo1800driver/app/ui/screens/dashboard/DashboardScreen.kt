package com.limo1800driver.app.ui.screens.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.saveable.rememberSaveable
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.components.*
import com.limo1800driver.app.ui.viewmodel.*

/**
 * Main Dashboard Screen for Driver App
 * Replicates iOS SwiftUI Design: Map BG, Sticky Card Tabs, Scrollable Content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navBackStackEntry: androidx.navigation.NavBackStackEntry,
    openDrawerRequest: Boolean = false,
    onDrawerRequestConsumed: () -> Unit = {},
    onNavigateToBooking: (Int) -> Unit = {},
    onNavigateToBookingPreview: (Int) -> Unit = {},
    onNavigateToFinalizeRates: (bookingId: Int, mode: String, source: String) -> Unit = { _, _, _ -> },
    onNavigateToEditBooking: (bookingId: Int, source: String) -> Unit = { _, _ -> },
    onNavigateToWallet: () -> Unit = {},
    onNavigateToMyActivity: () -> Unit = {},
    onNavigateToPreArrangedRides: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAccountSettings: () -> Unit = {},
    onNavigateToRideInProgress: (Int) -> Unit = {},
    onLogout: () -> Unit = {},
    bookingsViewModel: DriverBookingsViewModel = hiltViewModel(),
    profileViewModel: DriverProfileViewModel = hiltViewModel(),
    statsViewModel: DashboardStatsViewModel = hiltViewModel(),
    updatesViewModel: DashboardUpdatesViewModel = hiltViewModel(),
    activeRideViewModel: ActiveRideViewModel = hiltViewModel(),
    reminderViewModel: DriverBookingReminderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    fun hasLocationPermissionNow(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    var hasLocationPermission by remember { mutableStateOf(hasLocationPermissionNow()) }
    var requestedLocationOnce by remember { mutableStateOf(false) }

    val requestLocationPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        requestedLocationOnce = true
        val fine = grantMap[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grantMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse || hasLocationPermissionNow()
    }

    val bookingsState by bookingsViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()
    val updatesState by updatesViewModel.uiState.collectAsStateWithLifecycle()
    val activeRide by activeRideViewModel.activeRide.collectAsStateWithLifecycle()
    val reminder by reminderViewModel.reminder.collectAsStateWithLifecycle()
    val passengerState by reminderViewModel.passenger.collectAsStateWithLifecycle()

    var isReminderProcessing by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Check savedStateHandle directly for initial drawer state
    val initialDrawerState = remember(navBackStackEntry) {
        val shouldOpen = navBackStackEntry.savedStateHandle.get<Boolean>("openDrawer") ?: false
        if (shouldOpen) {
            // Consume the request immediately
            navBackStackEntry.savedStateHandle.set("openDrawer", false)
        }
        shouldOpen
    }
    var showDrawerMenu by remember { mutableStateOf(initialDrawerState) }
    var selectedTab: DashboardTab by remember { mutableStateOf(DashboardTab.DRIVE) }
    var selectedBooking by remember { mutableStateOf<DriverBooking?>(null) }
    var showRoute by remember { mutableStateOf(false) }
    var isSatelliteView by remember { mutableStateOf(false) }

    // Bottom Sheet Logic
    var sheetHeight by remember { mutableStateOf(0.40f) }
    val minHeight = 0.35f
    val maxHeight = 0.88f

    LaunchedEffect(Unit) {
        // Load profile from cache first, then API if needed
        profileViewModel.fetchDriverProfile()
        statsViewModel.fetchDashboardStats()
        updatesViewModel.fetchDriverUpdates()
        bookingsViewModel.fetchBookings(resetData = true)
        activeRideViewModel.ensureConnected()
    }

    // Request location as soon as the driver lands on Dashboard (instead of waiting for Ride actions).
    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !requestedLocationOnce) {
            requestLocationPermissions.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // iOS: For 2h/1h/30m/15m reminder types, show a blocking popup with "Driver En Route".
    if (reminder != null && reminder!!.requiresDriverEnRouteAction()) {
        com.limo1800driver.app.ui.components.DriverEnRouteReminderDialog(
            reminder = reminder!!,
            isProcessing = isReminderProcessing,
            onDriverEnRoute = {
                isReminderProcessing = true
                reminderViewModel.sendDriverEnRoute(reminder!!)
                // Match iOS: immediately navigate to Ride In Progress after tapping "Start Ride".
                onNavigateToRideInProgress(reminder!!.bookingId)
                isReminderProcessing = false
            },
            onNotNow = {
                reminderViewModel.dismiss()
            },
            passengerName = passengerState.name,
            passengerPhone = passengerState.phone
        )
    }

    // Auto-navigate to RideInProgress when socket reports an active ride (mirrors iOS DashboardView behavior).
    var lastNavigatedRideId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeRide?.bookingId, activeRide?.status) {
        val ride = activeRide ?: return@LaunchedEffect
        if (lastNavigatedRideId == ride.bookingId) return@LaunchedEffect
        if (ride.status == "ended") return@LaunchedEffect
        lastNavigatedRideId = ride.bookingId
        onNavigateToRideInProgress(ride.bookingId)
    }

    // When returning from a menu screen, open the drawer menu automatically.
    // This handles cases where the drawer request comes in after initial composition
    LaunchedEffect(openDrawerRequest) {
        if (openDrawerRequest && !showDrawerMenu) {
            showDrawerMenu = true
            onDrawerRequestConsumed()
        }
    }

    // CRITICAL: NestedScrollConnection allows the sheet to drag UP naturally
    // when the user swipes up on the inner lists (Drive/Earnings).
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // If dragging UP (delta < 0) and sheet isn't full height, consume event to expand sheet
                if (delta < 0 && sheetHeight < maxHeight) {
                    val newHeight = (sheetHeight - (delta / screenHeightPx)).coerceIn(minHeight, maxHeight)
                    val consumed = if (newHeight != sheetHeight) available.y else 0f
                    sheetHeight = newHeight
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // If dragging DOWN (delta > 0) and list is at top, consume event to collapse sheet
                if (delta > 0) {
                    val newHeight = (sheetHeight - (delta / screenHeightPx)).coerceIn(minHeight, maxHeight)
                    val consumedDelta = if (newHeight != sheetHeight) available.y else 0f
                    sheetHeight = newHeight
                    return Offset(0f, consumedDelta)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Snap to nearest state
                val midpoint = (minHeight + maxHeight) / 2f
                sheetHeight = if (sheetHeight >= midpoint) maxHeight else minHeight
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map Background
        DriverMapView(
            bookings = bookingsState.bookings,
            selectedBooking = selectedBooking,
            showRoute = showRoute,
            isSatelliteView = isSatelliteView,
            onBookingMarkerClick = { booking ->
                selectedBooking = booking
                showRoute = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // Location permission banner (re-shown if denied; user can request again or open settings)
        if (!hasLocationPermission) {
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true ||
                activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) == true
            val isPermanentlyDenied = requestedLocationOnce && !shouldShowRationale

            Card(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Enable location for live ride tracking",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "We use your location to update the passenger and help navigation during the ride.",
                        fontSize = 13.sp,
                        color = Color(0xFF6D4C41)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (isPermanentlyDenied) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    requestLocationPermissions.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isPermanentlyDenied) "Open Settings" else "Enable Location")
                        }

                        TextButton(
                            onClick = { requestedLocationOnce = true }
                        ) {
                            Text("Not now")
                        }
                    }
                }
            }
        }

        // 2. Menu Button (Top Left)
        Card(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 16.dp)
                .align(Alignment.TopStart)
                .size(48.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            IconButton(
                onClick = { showDrawerMenu = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
            }
        }

        // 3. Map Controls (Top Right)
//        MapControls(
//            isSatelliteView = isSatelliteView,
//            onSatelliteToggle = { isSatelliteView = !isSatelliteView },
//            modifier = Modifier
//                .windowInsetsPadding(WindowInsets.statusBars)
//                .align(Alignment.TopEnd)
//                .padding(end = 16.dp, top = 16.dp)
//        )

        // 4. Draggable Bottom Sheet
        val sheetHeightDp = with(density) { (screenHeightPx * sheetHeight).toDp() }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeightDp)
                .shadow(16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .nestedScroll(connection) // Apply nested scroll logic here
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- STICKY HEADER SECTION ---
                // Matches iOS Tab Style: Two separate cards "Drive" and "Earnings"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White) // Sticky header background
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    val delta = dragAmount.y
                                    val newHeight = (sheetHeight - (delta / screenHeightPx)).coerceIn(minHeight, maxHeight)
                                    sheetHeight = newHeight
                                    change.consume()
                                },
                                onDragEnd = {
                                    val midpoint = (minHeight + maxHeight) / 2f
                                    sheetHeight = if (sheetHeight >= midpoint) maxHeight else minHeight
                                }
                            )
                        }
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        )
                    }

                    // NEW REDESIGNED TABS
                    DashboardSegmentedControl(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp) // Standard margin
                            .padding(bottom = 16.dp)     // Space before content
                    )
                }

                // --- SCROLLABLE CONTENT SECTION ---
                // Uses weight(1f) to take up remaining space.
                // The inner content MUST handle its own scrolling.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFFAFAFA)) // Light gray BG for content contrast
                ) {
                    when (selectedTab) {
                        DashboardTab.DRIVE -> {
                            DriveTabContent(
                                alerts = updatesState.alerts,
                                alertsLoading = updatesState.isLoading,
                                monthlyEarnings = statsState.stats?.monthly?.earnings?.let {
                                    String.format("%.2f", it)
                                } ?: "0.00",
                                upcomingRides = statsState.stats?.today?.rides?.toString() ?: "0",
                                currencySymbol = statsState.stats?.currencySymbol,
                                statsLoading = statsState.isLoading,
                                onBookingSelected = { selectedBooking = it },
                                onEditClick = { booking ->
                                    onNavigateToEditBooking(booking.bookingId, "dashboard")
                                },
                                onViewOnMapClick = {
                                    selectedBooking = it
                                    showRoute = true
                                },
                                onDriverEnRouteClick = { /* TODO */ },
                                onFinalizeClick = { booking ->
                                    val isPending = booking.bookingStatus.equals("pending", ignoreCase = true)
                                    if (isPending) {
                                        // "Accept/Reject" flow uses BookingPreview
                                        onNavigateToBookingPreview(booking.bookingId)
                                    } else {
                                        // Finalize routes directly to rates screen (single-step flow), same as PreArrangedRides.
                                        onNavigateToFinalizeRates(booking.bookingId, "finalizeOnly", "dashboard")
                                    }
                                },
                                onAlertClick = { /* TODO */ },
                                bookingsViewModel = bookingsViewModel,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()) // Important: Content scrolls itself
                            )
                        }
                    DashboardTab.EARNINGS -> {
                        EarningsView(
                            onNavigateToWallet = onNavigateToWallet,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()) // Important: Content scrolls itself
                        )
                    }
                    }
                }
            }
        }

        DashboardDrawerMenu(
            isPresented = showDrawerMenu,
            driverProfile = profileState.profile,
            isLoading = profileState.isLoading,
            vehicleName = profileState.profile?.vehicle?.vehicleCatName,
            vehicleMake = profileState.profile?.vehicle?.vehicleMakeName?.trim()?.takeIf { it.isNotEmpty() },
            vehicleModel = profileState.profile?.vehicle?.vehicleModelName?.trim()?.takeIf { it.isNotEmpty() },
            vehicleYear = profileState.profile?.vehicle?.vehicleYearName?.trim()?.takeIf { it.isNotEmpty() },
            vehicleColor = profileState.profile?.vehicle?.vehicleColorName?.trim()?.takeIf { it.isNotEmpty() },
            vehicleImageUrl = profileState.profile?.vehicle?.vehicleImage,
            notificationBadge = null,
            onClose = { showDrawerMenu = false },
            onNavigateToPreArrangedRides = onNavigateToPreArrangedRides,
            onNavigateToMyActivity = onNavigateToMyActivity,
            onNavigateToWallet = onNavigateToWallet,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToAccountSettings = onNavigateToAccountSettings,
            onLogout = onLogout
        )
    }
}

@Composable
private fun DriveTabContent(
    alerts: List<com.limo1800driver.app.ui.viewmodel.DashboardStatusAlert>,
    alertsLoading: Boolean,
    monthlyEarnings: String,
    upcomingRides: String,
    currencySymbol: String?,
    statsLoading: Boolean,
    onBookingSelected: (DriverBooking) -> Unit,
    onEditClick: (DriverBooking) -> Unit,
    onViewOnMapClick: (DriverBooking) -> Unit,
    onDriverEnRouteClick: (DriverBooking) -> Unit,
    onFinalizeClick: (DriverBooking) -> Unit,
    onAlertClick: (com.limo1800driver.app.ui.viewmodel.DashboardStatusAlert) -> Unit,
    bookingsViewModel: DriverBookingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Status Alerts
        DashboardStatusAlertCarousel(
            alerts = alerts,
            isLoading = alertsLoading,
            onAlertClick = onAlertClick
        )

        // Scheduled Pickups Section
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Scheduled Pickups",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            ScheduledPickupsPager(
                onBookingSelected = onBookingSelected,
                onEditClick = onEditClick,
                onViewOnMapClick = onViewOnMapClick,
                onDriverEnRouteClick = onDriverEnRouteClick,
                onFinalizeClick = onFinalizeClick,
                bookingsViewModel = bookingsViewModel
            )
        }

        // Help and Support Section
        HelpAndSupportCard()
    }
}

@Composable
private fun MapControls(
    isSatelliteView: Boolean,
    onSatelliteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        IconButton(
            onClick = onSatelliteToggle,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isSatelliteView) Icons.Default.Map else Icons.Default.Satellite,
                contentDescription = if (isSatelliteView) "Standard View" else "Satellite View",
                tint = if (isSatelliteView) Color(0xFFFF9800) else Color.Black
            )
        }
    }
}