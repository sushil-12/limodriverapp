package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.components.*
import com.limo1800driver.app.ui.viewmodel.*

/**
 * Main Dashboard Screen for Driver App
 * Matches iOS DashboardView with map, tabs, and content sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToBooking: (Int) -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToMyActivity: () -> Unit = {},
    onNavigateToPreArrangedRides: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAccountSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
    bookingsViewModel: DriverBookingsViewModel = hiltViewModel(),
    profileViewModel: DriverProfileViewModel = hiltViewModel(),
    statsViewModel: DashboardStatsViewModel = hiltViewModel(),
    updatesViewModel: DashboardUpdatesViewModel = hiltViewModel()
) {
    val bookingsState by bookingsViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()
    val updatesState by updatesViewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    var showDrawerMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DashboardTab.DRIVE) }
    var selectedBooking by remember { mutableStateOf<DriverBooking?>(null) }
    var showRoute by remember { mutableStateOf(false) }
    var isSatelliteView by remember { mutableStateOf(false) }
    var sheetHeight by remember { mutableStateOf(0.35f) }
    val coroutineScope = rememberCoroutineScope()
    
    // Fetch data on first load
    LaunchedEffect(Unit) {
        profileViewModel.fetchDriverProfile()
        statsViewModel.fetchDashboardStats()
        updatesViewModel.fetchDriverUpdates()
        bookingsViewModel.fetchBookings(resetData = true)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full Screen Map Background
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
        
        // 2. Top UI Layers (Menu & Map Controls)
        // Menu Button - Match user app design
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
        
        // Map Controls (Satellite Toggle)
        MapControls(
            isSatelliteView = isSatelliteView,
            onSatelliteToggle = { isSatelliteView = !isSatelliteView },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp)
        )
        
        // 3. Draggable Bottom Sheet
        val minHeight = 0.35f
        val maxHeight = 0.85f
        val sheetHeightDp = with(density) { (screenHeightPx * sheetHeight).toDp() }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeightDp)
                .shadow(16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val newHeight = (sheetHeight - (delta / screenHeightPx)).coerceIn(minHeight, maxHeight)
                        sheetHeight = newHeight
                    },
                    onDragStopped = {
                        val midpoint = (minHeight + maxHeight) / 2f
                        sheetHeight = if (sheetHeight >= midpoint) maxHeight else minHeight
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .padding(top = 12.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tab Section
                DashboardTabSection(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                // Content Section (Scrollable inside bottom sheet)
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
                            onEditClick = { onNavigateToBooking(it.bookingId) },
                            onViewOnMapClick = {
                                selectedBooking = it
                                showRoute = true
                            },
                            onDriverEnRouteClick = {
                                // TODO: Implement driver en route action
                            },
                            onFinalizeClick = { onNavigateToBooking(it.bookingId) },
                            onAlertClick = { alert ->
                                // TODO: Handle alert click (e.g., navigate to settings)
                            },
                            bookingsViewModel = bookingsViewModel
                        )
                    }
                    DashboardTab.EARNINGS -> {
                        EarningsView()
                    }
                }
            }
        }
        
        // Drawer Menu
        DashboardDrawerMenu(
            isPresented = showDrawerMenu,
            driverProfile = profileState.profile,
            isLoading = profileState.isLoading,
            vehicleName = null,
            vehicleMakeModel = null,
            vehicleYear = null,
            vehicleColor = null,
            vehicleImageUrl = null,
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

/**
 * Drive Tab Content
 * Contains alerts, statistics, scheduled pickups, and help & support
 */
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
    bookingsViewModel: DriverBookingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Status Alerts
        DashboardStatusAlertCarousel(
            alerts = alerts,
            isLoading = alertsLoading,
            onAlertClick = onAlertClick
        )
        
        // Statistics Panel
        DashboardStatisticsPanel(
            monthlyEarnings = monthlyEarnings,
            upcomingRides = upcomingRides,
            currencySymbol = currencySymbol,
            isLoading = statsLoading
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

/**
 * Map Controls Component
 * Matches user app design with Card instead of FAB
 */
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
