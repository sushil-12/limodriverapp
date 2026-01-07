package com.limo1800driver.app.ui.screens.map
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import com.limo1800driver.app.data.model.dashboard.AffiliateBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.AffiliateExtraStop
import com.limo1800driver.app.data.network.NetworkConfig
import com.limo1800driver.app.data.model.UiState
import com.limo1800driver.app.ui.components.PassengerInfoCard
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerText
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.viewmodel.BookingMapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
// -----------------------------------------------------------------------------
// 1. DATA MODELS & STYLES
// -----------------------------------------------------------------------------
/**
 * State model for the booking map bottom sheet UI
 */
data class BookingMapState(
    val bookingId: String,
    val routeDescription: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val durationText: String,
    val passengerName: String,
    val passengerPhone: String,
    val hasExtraStops: Boolean
)
private data class RouteResult(
    val points: List<LatLng>,
    val distanceText: String = "",
    val durationText: String = "",
    val totalDurationSeconds: Long = 0L,
    val legDurations: List<Long> = emptyList(),
    val hasTolls: Boolean = false
)
object MapStyles {
    // Ultra-minimalist style: Silver/Grayscale, high contrast roads, NO POIs/Businesses
    const val ULTRA_CLEAN_STYLE = """
    [
      {
        "elementType": "geometry",
        "stylers": [{ "color": "#f5f5f5" }]
      },
      {
        "elementType": "labels.icon",
        "stylers": [{ "visibility": "off" }]
      },
      {
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#616161" }]
      },
      {
        "elementType": "labels.text.stroke",
        "stylers": [{ "color": "#f5f5f5" }]
      },
      {
        "featureType": "administrative.land_parcel",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#bdbdbd" }]
      },
      {
        "featureType": "poi",
        "elementType": "geometry",
        "stylers": [{ "color": "#eeeeee" }]
      },
      {
        "featureType": "poi",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#757575" }]
      },
      {
        "featureType": "poi.park",
        "elementType": "geometry",
        "stylers": [{ "color": "#e5e5e5" }]
      },
      {
        "featureType": "poi.park",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#9e9e9e" }]
      },
      {
        "featureType": "road",
        "elementType": "geometry",
        "stylers": [{ "color": "#ffffff" }]
      },
      {
        "featureType": "road.arterial",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#757575" }]
      },
      {
        "featureType": "road.highway",
        "elementType": "geometry",
        "stylers": [{ "color": "#dadada" }]
      },
      {
        "featureType": "road.highway",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#616161" }]
      },
      {
        "featureType": "road.local",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#9e9e9e" }]
      },
      {
        "featureType": "transit.line",
        "elementType": "geometry",
        "stylers": [{ "color": "#e5e5e5" }]
      },
      {
        "featureType": "transit.station",
        "elementType": "geometry",
        "stylers": [{ "color": "#eeeeee" }]
      },
      {
        "featureType": "water",
        "elementType": "geometry",
        "stylers": [{ "color": "#c9c9c9" }]
      },
      {
        "featureType": "water",
        "elementType": "labels.text.fill",
        "stylers": [{ "color": "#9e9e9e" }]
      }
    ]
    """
}
// -----------------------------------------------------------------------------
// 2. MAIN SCREEN COMPOSABLE
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingMapView(
    bookingId: Int,
    onBack: () -> Unit,
    viewModel: BookingMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // -- Data State --
    val bookingPreviewState by viewModel.bookingPreviewState.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val booking = when (bookingPreviewState) {
        is UiState.Success -> (bookingPreviewState as UiState.Success<AffiliateBookingPreviewData>).data
        else -> null
    }
    // -- Map State --
    var routeResult by remember { mutableStateOf(RouteResult(emptyList())) }
    var toPickupRoute by remember { mutableStateOf(RouteResult(emptyList())) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.8781, -87.6298), 10f)
    }
    // Logic: Is camera user-moved?
    var isCameraCentered by remember { mutableStateOf(true) }
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isCameraCentered = false
        }
    }
    // Current location
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation(context) { loc ->
                currentLocation = loc
            }
        }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation(context) { loc ->
                currentLocation = loc
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    // Logic: Fetch Booking Preview
    LaunchedEffect(bookingId) {
        viewModel.fetchBookingPreview(bookingId)
    }
    // Logic: Route & Animate
    val pickupCoord = remember(booking) {
        booking?.let {
            parseCoordinate(it.pickupLatitude, it.pickupLongitude)
        }
    }
    val dropoffCoord = remember(booking) {
        booking?.let {
            // Use regular coordinates if valid, otherwise fall back to airport coordinates
            val regularCoord = parseCoordinate(it.dropoffLatitude, it.dropoffLongitude)
            regularCoord ?: parseCoordinate(it.dropoffAirportLatitude, it.dropoffAirportLongitude)
        }
    }
    val extraStopCoords = remember(booking) {
        booking?.extraStops?.mapNotNull { stop: AffiliateExtraStop ->
            if (stop.latitude == "0.00000000" && stop.longitude == "0.00000000") null
            else parseCoordinate(stop.latitude, stop.longitude)
        } ?: emptyList()
    }
    LaunchedEffect(pickupCoord, dropoffCoord, extraStopCoords) {
        if (pickupCoord != null && dropoffCoord != null) {
            // 1. Fetch Route (including extra stops as waypoints)
            val result = fetchRouteExtended(pickupCoord, dropoffCoord, extraStopCoords, NetworkConfig.GOOGLE_PLACES_API_KEY)
            routeResult = result
            // 2. Animate Camera (with bottom padding for sheet)
            if (result.points.isNotEmpty()) {
                val bounds = LatLngBounds.builder().apply {
                    result.points.forEach { include(it) }
                    include(pickupCoord)
                    include(dropoffCoord)
                    extraStopCoords.forEach { include(it) }
                }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                isCameraCentered = true
            }
        } else if (pickupCoord != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupCoord, 15f))
            isCameraCentered = true
        }
    }
    LaunchedEffect(currentLocation, pickupCoord) {
        val currentLoc = currentLocation
        if (currentLoc != null && pickupCoord != null) {
            toPickupRoute = fetchRouteExtended(currentLoc, pickupCoord, emptyList(), NetworkConfig.GOOGLE_PLACES_API_KEY)
        }
    }
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 280.dp,
        sheetContent = {
            when (bookingPreviewState) {
                is UiState.Loading -> {
                    BookingMapBottomSheetShimmerContent()
                }
                is UiState.Error -> {
                    BookingMapBottomSheetErrorContent()
                }
                is UiState.Success -> {
                    val bookingData = (bookingPreviewState as UiState.Success<AffiliateBookingPreviewData>).data
                    val hasExtraStops = !bookingData.extraStops.isNullOrEmpty()
                    val routeDescription = remember(bookingData.pickupAddress, bookingData.dropoffAddress, hasExtraStops) {
                        if (hasExtraStops) {
                            "Multi-stop route: ${bookingData.extraStops?.size ?: 0} stops"
                        } else {
                            "Pickup → Dropoff"
                        }
                    }
                    val state = remember(bookingData, routeResult, routeDescription) {
                        BookingMapState(
                            bookingId = "#${bookingData.id}",
                            routeDescription = routeDescription,
                            pickupAddress = bookingData.pickupAddress,
                            dropoffAddress = bookingData.dropoffAddress,
                            durationText = routeResult.durationText.ifEmpty { "Calculating..." },
                            passengerName = bookingData.passengerName,
                            passengerPhone = bookingData.passengerCell ?: "",
                            hasExtraStops = hasExtraStops
                        )
                    }
                    BookingMapDetailSheet(
                        state = state,
                        routeData = routeResult,
                        toPickupData = toPickupRoute,
                        bookingData = bookingData,
                        onNavigateToPickup = {
                            if (pickupCoord != null) openNavigationToPickup(context, pickupCoord)
                        },
                        onPreviewFullRoute = {
                            if (pickupCoord != null && dropoffCoord != null) {
                                openRouteOverview(context, pickupCoord, dropoffCoord, extraStopCoords)
                            }
                        },
                        onCallPassenger = {
                            bookingData.passengerCell?.let { phone -> makePhoneCall(context, phone) }
                        }
                    )
                }
            }
        },
        sheetContainerColor = Color.White,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = null
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            // ---------------- LAYER 1: GOOGLE MAP ----------------
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = MapStyleOptions(MapStyles.ULTRA_CLEAN_STYLE),
                    isMyLocationEnabled = currentLocation != null,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                    rotationGesturesEnabled = true
                )
            ) {
                // Route Line (Shadow + Main)
                if (routeResult.points.isNotEmpty()) {
                    Polyline(
                        points = routeResult.points,
                        color = Color.Black.copy(alpha = 0.2f),
                        width = 22f,
                        zIndex = 0f
                    )
                    Polyline(
                        points = routeResult.points,
                        color = LimoOrange,
                        width = 14f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        zIndex = 1f
                    )
                }
                // Markers
                pickupCoord?.let {
                    Marker(
                        state = MarkerState(it),
                        title = "Pickup",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                        zIndex = 3f
                    )
                }
                // Extra stop markers
                extraStopCoords.forEachIndexed { index, coord ->
                    Marker(
                        state = MarkerState(coord),
                        title = "Stop ${index + 1}",
                        snippet = booking?.extraStops?.getOrNull(index)?.address ?: "Extra Stop",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        zIndex = 2f
                    )
                }
                dropoffCoord?.let {
                    Marker(
                        state = MarkerState(it),
                        title = "Dropoff",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        zIndex = 3f
                    )
                }
            }
            // ---------------- LAYER 2: TOP BAR (Improved design) ----------------
            GlassTopBar(
                title = "Booking #${booking?.id ?: bookingId}",
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            // ---------------- LAYER 3: RE-CENTER BUTTON ----------------
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                // Re-center Button
                AnimatedVisibility(
                    visible = !isCameraCentered,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                if (routeResult.points.isNotEmpty()) {
                                    val bounds = LatLngBounds.builder().apply {
                                        routeResult.points.forEach { include(it) }
                                        pickupCoord?.let { include(it) }
                                        dropoffCoord?.let { include(it) }
                                        extraStopCoords.forEach { include(it) }
                                    }.build()
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                                } else if (pickupCoord != null) {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupCoord, 15f))
                                }
                                isCameraCentered = true
                            }
                        },
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, "Re-center")
                    }
                }
            }
        }
    }
}
// -----------------------------------------------------------------------------
// 3. UI COMPONENTS
// -----------------------------------------------------------------------------
/**
 * The actual content of the booking map bottom sheet
 */
@Composable
private fun BookingMapDetailSheet(
    state: BookingMapState,
    routeData: RouteResult,
    toPickupData: RouteResult,
    bookingData: AffiliateBookingPreviewData,
    onNavigateToPickup: () -> Unit,
    onPreviewFullRoute: () -> Unit,
    onCallPassenger: () -> Unit
) {
    val now = LocalDateTime.now()
    val pickupDT = try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        LocalDateTime.parse("${bookingData.pickupDate} ${bookingData.pickupTime.take(5)}", formatter)
    } catch (e: Exception) {
        now
    }
    val minBefore = if (toPickupData.totalDurationSeconds > 0) {
        (toPickupData.totalDurationSeconds / 60).toInt()
    } else 0
    val pickupFormatted = "Pickup on ${bookingData.pickupDate.replace("-", " ")} at ${formatTimeCompact(bookingData.pickupTime)}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 32.dp) // Increased bottom padding for better touch area
    ) {
        // Drag Handle (centered and more prominent for UX)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp), // Increased padding for easier drag
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp) // Slightly wider for better grip
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFBDBDBD)) // Softer gray for modern look
            )
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp)) { // Increased horizontal padding for breathing room
            // Warning Banner
            if (minBefore > 0) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Plan to start the ride about $minBefore min before pickup so you arrive comfortably on time.",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            // Pickup Info Combined
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pickupFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                // Booking ID Badge
                Surface(
                    color = Color(0xFF212121),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${state.bookingId} (TA)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Route Description
            Text(
                text = state.routeDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Show More >",
                color = Color.Blue,
                fontSize = 14.sp,
                modifier = Modifier.clickable { /* TODO: Expand description or sheet */ }
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val navigateText = "Navigate to Pickup"
                OutlinedButton(
                    onClick = onNavigateToPickup,
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        navigateText,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = onPreviewFullRoute,
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Text(
                        "See full Route",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            if (routeData.hasTolls) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Route may include toll roads",
                            color = Color(0xFFFF9800),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            // Timeline
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            BookingMapTimelineView(
                bookingData = bookingData,
                routeData = routeData
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Passenger Card
            if (bookingData.status?.lowercase() != "pending") {
                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                PassengerInfoCard(
                    name = state.passengerName,
                    phone = state.passengerPhone,
                    onMessage = { /* Not implemented for booking map */ },
                    onCall = onCallPassenger
                )
            }
        }
    }
}
/**
 * Timeline view for booking map showing pickup, extra stops, and dropoff
 */
@Composable
private fun BookingMapTimelineView(bookingData: AffiliateBookingPreviewData, routeData: RouteResult) {
    val extraStops = bookingData.extraStops ?: emptyList()
    val pickupDT = try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        LocalDateTime.parse("${bookingData.pickupDate} ${bookingData.pickupTime.take(5)}", formatter)
    } catch (e: Exception) {
        LocalDateTime.now()
    }
    // Create stops list: Pickup -> Extra Stops -> Dropoff
    val allStops = mutableListOf<TimelineStopItem>()
    allStops.add(TimelineStopItem(
        time = formatDateTimeCompact(pickupDT),
        address = bookingData.pickupAddress,
        type = TimelineStopType.PICKUP
    ))
    var cumSec = 0L
    if (routeData.legDurations.size == extraStops.size + 1) {
        extraStops.forEachIndexed { index, stop ->
            cumSec += routeData.legDurations[index]
            val estDT = pickupDT.plusSeconds(cumSec)
            allStops.add(TimelineStopItem(
                time = "Est. ${formatDateTimeCompact(estDT)}",
                address = stop.address,
                type = TimelineStopType.EXTRA_STOP
            ))
        }
        cumSec += routeData.legDurations.last()
        val estDropDT = pickupDT.plusSeconds(cumSec)
        allStops.add(TimelineStopItem(
            time = "Est. ${formatDateTimeCompact(estDropDT)}",
            address = bookingData.dropoffAddress,
            type = TimelineStopType.DROPOFF
        ))
    } else {
        // Fallback if no route data
        extraStops.forEachIndexed { index, stop ->
            allStops.add(TimelineStopItem(
                time = "Stop ${index + 1}",
                address = stop.address,
                type = TimelineStopType.EXTRA_STOP
            ))
        }
        allStops.add(TimelineStopItem(
            time = "Dropoff",
            address = bookingData.dropoffAddress,
            type = TimelineStopType.DROPOFF
        ))
    }
    Column {
        allStops.forEachIndexed { index, stop ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Visual indicator column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    when (stop.type) {
                        TimelineStopType.PICKUP -> Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(LimoOrange, CircleShape)
                        )
                        TimelineStopType.EXTRA_STOP -> Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFFF9800), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                        TimelineStopType.DROPOFF -> Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(LimoOrange, RoundedCornerShape(1.dp))
                        )
                    }
                    // Connecting line (except for last stop)
                    if (index < allStops.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(32.dp)
                                .background(Color.Black)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                // Address content
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stop.time,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.width(100.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stop.address,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // Spacer between stops (except last one)
            if (index < allStops.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
/**
 * Shimmer loading state for booking map bottom sheet
 */
@Composable
private fun BookingMapBottomSheetShimmerContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Drag handle
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
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        }
        // Header shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                ShimmerText(modifier = Modifier.width(100.dp), height = 28.dp)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerText(modifier = Modifier.width(150.dp), height = 16.dp)
            }
            ShimmerBox(modifier = Modifier.size(50.dp, 24.dp), shape = RoundedCornerShape(4.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Buttons shimmer
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Timeline shimmer
        Column {
            repeat(3) { index ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    ShimmerBox(modifier = Modifier.size(12.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ShimmerText(modifier = Modifier.width(50.dp), height = 14.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 18.dp)
                        }
                    }
                }
                if (index < 2) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Passenger card shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(12.dp))
    }
}
/**
 * Error state for booking map bottom sheet
 */
@Composable
private fun BookingMapBottomSheetErrorContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag handle
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
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Failed to load booking details",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please try again later",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
// Data classes for timeline
private data class TimelineStopItem(
    val time: String,
    val address: String,
    val type: TimelineStopType
)
private enum class TimelineStopType {
    PICKUP,
    EXTRA_STOP,
    DROPOFF
}
@Composable
fun GlassTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handling Status Bar padding manually to ensure floating effect
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 8.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Title Pill - Improved: White background with black text for better readability
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.85f),
                shadowElevation = 8.dp,
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = LimoOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
// -----------------------------------------------------------------------------
// 4. HELPER FUNCTIONS
// -----------------------------------------------------------------------------
private val okHttpClient by lazy { OkHttpClient() }
private fun parseCoordinate(latStr: String, lonStr: String): LatLng? {
    val lat = latStr?.trim()?.toDoubleOrNull()
    val lon = lonStr?.trim()?.toDoubleOrNull()
    return if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) LatLng(lat, lon) else null
}
private fun parseCoordinate(lat: Double?, lon: Double?): LatLng? {
    return if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) LatLng(lat, lon) else null
}
private suspend fun fetchRouteExtended(origin: LatLng, destination: LatLng, extraStops: List<LatLng> = emptyList(), apiKey: String): RouteResult = withContext(Dispatchers.IO) {
    try {
        val urlBuilder = StringBuilder("https://maps.googleapis.com/maps/api/directions/json?")
            .append("origin=${origin.latitude},${origin.longitude}")
            .append("&destination=${destination.latitude},${destination.longitude}")
            .append("&mode=driving&key=$apiKey")
        // Add waypoints if extra stops exist
        if (extraStops.isNotEmpty()) {
            val waypoints = extraStops.joinToString("|") { "${it.latitude},${it.longitude}" }
            urlBuilder.append("&waypoints=$waypoints")
        }
        val response = okHttpClient.newCall(Request.Builder().url(urlBuilder.toString()).build()).execute()
        val bodyStr = response.body?.string() ?: return@withContext RouteResult(emptyList())
        val json = JSONObject(bodyStr)
        val routes = json.getJSONArray("routes")
        if (routes.length() > 0) {
            val routeObj = routes.getJSONObject(0)
            val legs = routeObj.getJSONArray("legs")
            var totalDistance = 0.0
            var totalDuration = 0L
            val legDurs = mutableListOf<Long>()
            var hasTolls = false
            if (legs.length() > 0) {
                for (i in 0 until legs.length()) {
                    val leg = legs.getJSONObject(i)
                    val distanceObj = leg.optJSONObject("distance") ?: continue
                    val durationObj = leg.optJSONObject("duration") ?: continue
                    totalDistance += distanceObj.optDouble("value")
                    val legDur = durationObj.optLong("value")
                    totalDuration += legDur
                    legDurs.add(legDur)
                    // Check for tolls
                    val steps = leg.optJSONArray("steps") ?: continue
                    for (j in 0 until steps.length()) {
                        val step = steps.getJSONObject(j)
                        if (step.has("html_instructions") && step.getString("html_instructions").lowercase().contains("toll")) {
                            hasTolls = true
                        }
                    }
                }
            }
            val dist = when {
                totalDistance >= 1000 -> "${String.format("%.1f", totalDistance / 1000)} km"
                else -> "${totalDistance.toInt()} m"
            }
            val dur = when {
                totalDuration >= 3600 -> "${totalDuration / 3600}h ${((totalDuration % 3600) / 60)}min"
                else -> "${totalDuration / 60} min"
            }
            val points = PolyUtil.decode(routeObj.getJSONObject("overview_polyline").getString("points"))
            RouteResult(points, dist, dur, totalDuration, legDurs, hasTolls)
        } else {
            RouteResult(emptyList())
        }
    } catch (e: Exception) {
        e.printStackTrace()
        RouteResult(emptyList())
    }
}
// BUTTON 1: TURN-BY-TURN NAVIGATION TO PICKUP
private fun openNavigationToPickup(context: Context, pickup: LatLng) {
    val uriStr = "google.navigation:q=${pickup.latitude},${pickup.longitude}&mode=d"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
    intent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback if app not installed
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)))
    }
}
// BUTTON 2: ROUTE OVERVIEW (PICKUP -> EXTRA STOPS -> DROPOFF)
private fun openRouteOverview(context: Context, pickup: LatLng, dropoff: LatLng, extraStops: List<LatLng> = emptyList()) {
    val uriBuilder = StringBuilder("http://maps.google.com/maps?saddr=${pickup.latitude},${pickup.longitude}")
    if (extraStops.isNotEmpty()) {
        // Add waypoints
        val waypoints = extraStops.joinToString("|") { "${it.latitude},${it.longitude}" }
        uriBuilder.append("&waypoints=$waypoints")
    }
    uriBuilder.append("&daddr=${dropoff.latitude},${dropoff.longitude}")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString()))
    intent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())))
    }
}
private fun makePhoneCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
    }
    context.startActivity(intent)
}
private fun formatTimeCompact(timeString: String?): String {
    if (timeString.isNullOrEmpty()) return "--"
    return try {
        val clean = if (timeString.split(":").size == 2) "$timeString:00" else timeString
        val date = SimpleDateFormat("HH:mm:ss", Locale.US).parse(clean)
        SimpleDateFormat("h:mma", Locale.US).format(date!!).lowercase().replace("am", "am").replace("pm", "pm")
    } catch (e: Exception) { "" }
}
private fun formatDateTimeCompact(ldt: LocalDateTime): String {
    val now = LocalDateTime.now()
    val days = ChronoUnit.DAYS.between(now.toLocalDate(), ldt.toLocalDate())
    val formatter = DateTimeFormatter.ofPattern("h:mma")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d h:mma")
    return when {
        days == 0L -> "Today ${ldt.format(formatter)}"
        days == 1L -> "Tomorrow ${ldt.format(formatter)}"
        else -> ldt.format(dateFormatter)
    }
}
private fun getCurrentLocation(context: Context, onLocation: (LatLng) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
        location?.let {
            onLocation(LatLng(it.latitude, it.longitude))
        }
    }
}
private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "confirmed", "accepted" -> Color(0xFF4CAF50) // Green
        "pending" -> Color(0xFFFF9800) // Orange
        "cancelled", "rejected" -> Color(0xFFF44336) // Red
        "completed" -> Color(0xFF2196F3) // Blue
        else -> Color.Gray
    }
}