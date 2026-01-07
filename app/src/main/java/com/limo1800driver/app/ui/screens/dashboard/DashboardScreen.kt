package com.limo1800driver.app.ui.screens.dashboard

import android.Manifest
import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.components.*
import com.limo1800driver.app.ui.viewmodel.*
import com.limo1800driver.app.ui.viewmodel.ScheduledPickupsViewModel
import com.limo1800driver.app.ui.navigation.NavRoutes
import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import android.content.Intent
import timber.log.Timber

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
    scheduledPickupsViewModel: ScheduledPickupsViewModel = hiltViewModel(),
    profileViewModel: DriverProfileViewModel = hiltViewModel(),
    statsViewModel: DashboardStatsViewModel = hiltViewModel(),
    updatesViewModel: DashboardUpdatesViewModel = hiltViewModel(),
    activeRideViewModel: ActiveRideViewModel = hiltViewModel(),
    reminderViewModel: DriverBookingReminderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // --- Permissions Logic ---
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

    // --- ViewModels & State ---
    val bookingsState by bookingsViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()
    val updatesState by updatesViewModel.uiState.collectAsStateWithLifecycle()
    val activeRide by activeRideViewModel.activeRide.collectAsStateWithLifecycle()
    val reminder by reminderViewModel.reminder.collectAsStateWithLifecycle()
    val passengerState by reminderViewModel.passenger.collectAsStateWithLifecycle()
    var isReminderProcessing by remember { mutableStateOf(false) }

    // --- Layout Constants ---
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val initialDrawerState = remember(navBackStackEntry) {
        val shouldOpen = navBackStackEntry.savedStateHandle.get<Boolean>("openDrawer") ?: false
        if (shouldOpen) navBackStackEntry.savedStateHandle.set("openDrawer", false)
        shouldOpen
    }
    var showDrawerMenu by remember { mutableStateOf(initialDrawerState) }
    var selectedTab by remember { mutableStateOf(DashboardTab.DRIVE) }
    var selectedBooking by remember { mutableStateOf<DriverBooking?>(null) }
    var showRoute by remember { mutableStateOf(false) }
    var isSatelliteView by remember { mutableStateOf(false) }

    // --- Bottom Sheet Logic ---
    var sheetHeight by remember { mutableStateOf(0.40f) }
    val minHeight = 0.38f
    val maxHeight = 0.68f

    LaunchedEffect(Unit) {
        profileViewModel.fetchDriverProfile()
        statsViewModel.fetchDashboardStats()
        updatesViewModel.fetchDriverUpdates()
        bookingsViewModel.fetchBookings(resetData = true)
        activeRideViewModel.ensureConnected()
        if (!hasLocationPermission && !requestedLocationOnce) {
            requestLocationPermissions.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // --- Side Effects (Navigation/Popups) ---
    if (reminder != null && reminder!!.requiresDriverEnRouteAction()) {
        com.limo1800driver.app.ui.components.DriverEnRouteReminderDialog(
            reminder = reminder!!,
            isProcessing = isReminderProcessing,
            onDriverEnRoute = {
                isReminderProcessing = true
                reminderViewModel.sendDriverEnRoute(reminder!!)
                onNavigateToRideInProgress(reminder!!.bookingId)
                isReminderProcessing = false
            },
            onNotNow = { reminderViewModel.dismiss() },
            passengerName = passengerState.name,
            passengerPhone = passengerState.phone
        )
    }

    var lastNavigatedRideId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeRide?.bookingId, activeRide?.status) {
        val ride = activeRide ?: return@LaunchedEffect
        if (lastNavigatedRideId == ride.bookingId || ride.status == "ended") return@LaunchedEffect
        lastNavigatedRideId = ride.bookingId
        onNavigateToRideInProgress(ride.bookingId)
    }

    LaunchedEffect(openDrawerRequest) {
        if (openDrawerRequest && !showDrawerMenu) {
            showDrawerMenu = true
            onDrawerRequestConsumed()
        }
    }

    // --- Scroll Connection for Sheet ---
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
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
                if (delta > 0) {
                    val newHeight = (sheetHeight - (delta / screenHeightPx)).coerceIn(minHeight, maxHeight)
                    val consumedDelta = if (newHeight != sheetHeight) available.y else 0f
                    sheetHeight = newHeight
                    return Offset(0f, consumedDelta)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val midpoint = (minHeight + maxHeight) / 2f
                sheetHeight = if (sheetHeight >= midpoint) maxHeight else minHeight
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map Layer
        DriverMapView(
            bookings = bookingsState.bookings,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = with(density) { (screenHeightPx * minHeight).toDp() - 20.dp }), // Adjust map padding so logo isn't hidden
            hasLocationPermission = hasLocationPermission // Pass permission status to prevent crash
        )

        // 2. Menu Button
        Card(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 16.dp)
                .size(48.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Higher shadow for better visibility
        ) {
            IconButton(onClick = { showDrawerMenu = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
            }
        }

        // 3. Location Permission Banner
        if (!hasLocationPermission) {
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true
            val isPermanentlyDenied = requestedLocationOnce && !shouldShowRationale

            Card(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp) // Below menu button
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)), // Dark mode banner looks premium
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Enable location for navigation", color = Color.LightGray, fontSize = 12.sp)
                    }
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (isPermanentlyDenied) "Settings" else "Enable", fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Draggable Bottom Sheet Container
        val sheetHeightDp = with(density) { (screenHeightPx * sheetHeight).toDp() }

        // Main Sheet Surface
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeightDp)
                .nestedScroll(connection), // Nested scroll attached here
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), // More pronounced curve
            shadowElevation = 20.dp, // Deep shadow
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- STICKY HEADER (Drag Handle + Tabs) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
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
                    // Refined Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp) // Wider handle
                                .height(5.dp) // Slightly thicker
                                .background(Color(0xFFE0E0E0), RoundedCornerShape(50)) // Pill shape
                        )
                    }

                    // Tab Control
                    DashboardSegmentedControl(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp) // More padding from edges
                            .padding(bottom = 16.dp)
                    )

                    // Subtle Divider
                    Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                }

                // --- SCROLLABLE CONTENT ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFFAFAFA)) // Slight off-white to separate content from header
                ) {
                    when (selectedTab) {
                        DashboardTab.DRIVE -> {
                            DriveTabContent(
                                alerts = updatesState.alerts,
                                alertsLoading = updatesState.isLoading,
                                monthlyEarnings = statsState.stats?.monthly?.earnings?.let { String.format("%.2f", it) } ?: "0.00",
                                upcomingRides = statsState.stats?.today?.rides?.toString() ?: "0",
                                currencySymbol = statsState.stats?.currencySymbol,
                                statsLoading = statsState.isLoading,
                                onBookingSelected = { selectedBooking = it },
                                onEditClick = { booking -> onNavigateToEditBooking(booking.bookingId, "dashboard") },
                                onViewOnMapClick = {
                                    selectedBooking = it
                                    showRoute = true
                                },
                                onDriverEnRouteClick = { /* TODO */ },
                                onFinalizeClick = { booking ->
                                    val isPending = booking.bookingStatus.equals("pending", ignoreCase = true)
                                    if (isPending) onNavigateToBookingPreview(booking.bookingId)
                                    else onNavigateToFinalizeRates(booking.bookingId, "finalizeOnly", "dashboard")
                                },
                                onAlertClick = { /* TODO */ },
                                onActionLinkClick = { route ->
                                    handleAlertActionLink(context, route, navBackStackEntry)
                                },
                                bookingsViewModel = bookingsViewModel,
                                scheduledPickupsViewModel = scheduledPickupsViewModel,
                            )
                        }
                        DashboardTab.EARNINGS -> {
                            EarningsView(
                                onNavigateToWallet = onNavigateToWallet,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }

        // Drawer Menu
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

// ... (Rest of the helper components like DriveTabContent, etc. remain the same) ...
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
    onActionLinkClick: ((String) -> Unit)? = null,
    bookingsViewModel: DriverBookingsViewModel,
    scheduledPickupsViewModel: ScheduledPickupsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp) // Consistent padding
            .padding(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp) // More spacing between sections
    ) {
        // Dashboard Status Alerts
        DashboardStatusAlertCarousel(
            alerts = alerts,
            isLoading = alertsLoading,
            onAlertClick = onAlertClick,
            onActionLinkClick = onActionLinkClick
        )

        // Scheduled Pickups Section
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scheduled Pickups", // Cleaned up text
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            ScheduledPickupsPager(
                onBookingSelected = onBookingSelected,
                onEditClick = onEditClick,
                onViewOnMapClick = onViewOnMapClick,
                onDriverEnRouteClick = onDriverEnRouteClick,
                onFinalizeClick = onFinalizeClick,
                scheduledPickupsViewModel = scheduledPickupsViewModel
            )
        }

        // Help and Support Section
        //HelpAndSupportCard()
    }
}

/**
 * Handle action link clicks from dashboard alerts
 */
private fun handleAlertActionLink(
    context: android.content.Context,
    route: String,
    navBackStackEntry: androidx.navigation.NavBackStackEntry
) {
    when {
        route.startsWith("action://verify_email") -> {
            // Extract email from route: action://verify_email?email=user@example.com
            val uri = Uri.parse(route)
            val email = uri.getQueryParameter("email") ?: return
            openGmailForVerification(context, email)
        }
        route == NavRoutes.BasicInfoFromAccountSettings -> {
            navBackStackEntry.savedStateHandle["navigateToBasicInfo"] = true
        }
        route == NavRoutes.CompanyInfoFromAccountSettings -> {
            navBackStackEntry.savedStateHandle["navigateToCompanyInfo"] = true
        }
        else -> {
            // Handle other navigation routes if needed
            navBackStackEntry.savedStateHandle["navigateToRoute"] = route
        }
    }
}

/**
 * Opens Gmail app with search for verification emails.
 * Falls back to generic email client if Gmail is not available.
 */
private fun openGmailForVerification(context: android.content.Context, emailAddress: String) {
    try {
        // Search query for verification emails - common patterns
        val searchQuery = "subject:(verification OR verify) OR from:(1800limo OR noreply)"
        
        // Method 1: Try Gmail app with search deep link
        val gmailSearchUri = Uri.parse("googlegmail://co?q=${Uri.encode(searchQuery)}")
        val gmailSearchIntent = Intent(Intent.ACTION_VIEW, gmailSearchUri).apply {
            setPackage("com.google.android.gm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(gmailSearchIntent)
            Timber.d("✅ Opened Gmail app with search for verification: $emailAddress")
            return
        } catch (e: Exception) {
            Timber.d("Gmail app with search not available: ${e.message}")
        }
        
        // Method 2: Try Gmail web with search
        val gmailWebUri = Uri.parse("https://mail.google.com/mail/u/0/#search/${Uri.encode(searchQuery)}")
        val webIntent = Intent(Intent.ACTION_VIEW, gmailWebUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(webIntent)
            Timber.d("✅ Opened Gmail web with search")
            return
        } catch (e: Exception) {
            Timber.d("Gmail web not available: ${e.message}")
        }
        
        // Method 3: Open Gmail app directly (user can search manually)
        val gmailAppUri = Uri.parse("googlegmail://")
        val gmailAppIntent = Intent(Intent.ACTION_VIEW, gmailAppUri).apply {
            setPackage("com.google.android.gm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(gmailAppIntent)
            Timber.d("✅ Opened Gmail app (user can search manually)")
            return
        } catch (e: Exception) {
            Timber.d("Gmail app not installed: ${e.message}")
        }
        
        // Method 4: Fallback - Open Gmail web in browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(browserIntent)
            Timber.d("✅ Opened Gmail in browser")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open any email client or browser")
        }
        
    } catch (e: Exception) {
        Timber.e(e, "Error opening Gmail for verification")
    }
}