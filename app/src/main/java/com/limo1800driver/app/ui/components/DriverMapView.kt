package com.limo1800driver.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.theme.LimoOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Define major global hubs for connectivity visualization
private val GLOBAL_HUBS = listOf(
    LatLng(40.7128, -74.0060), // NYC
    LatLng(51.5074, -0.1278),  // London
    LatLng(25.2048, 55.2708),  // Dubai
    LatLng(35.6762, 139.6503), // Tokyo
    LatLng(34.0522, -118.2437), // LA
    LatLng(48.8566, 2.3522),   // Paris
    LatLng(1.3521, 103.8198)   // Singapore
)

@Composable
fun DriverMapView(
    bookings: List<DriverBooking>,
    selectedBooking: DriverBooking? = null,
    showRoute: Boolean = false,
    isSatelliteView: Boolean = false,
    onBookingMarkerClick: (DriverBooking) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Optimized Camera State
    val defaultLocation = LatLng(40.7128, -74.0060)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    // 2. Map Style & Settings (Minimalist & Dark Mode Ready)
    val mapProperties = remember(isSatelliteView) {
        MapProperties(
            mapType = if (isSatelliteView) MapType.HYBRID else MapType.NORMAL,
            isTrafficEnabled = false,
            isIndoorEnabled = false,
            mapStyleOptions = try {
                // Load minimalist dark/light style based on system theme or preference
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
            } catch (e: Exception) { null }
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false, // Disable Google Maps toolbar for cleaner look
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true
        )
    }

    // 3. Effect: Smart Camera Updates
    LaunchedEffect(selectedBooking, bookings) {
        if (selectedBooking != null) {
            // Focus on selected booking
            val bounds = LatLngBounds.builder().apply {
                selectedBooking.pickupLatitude?.toDoubleOrNull()?.let { lat ->
                    selectedBooking.pickupLongitude?.toDoubleOrNull()?.let { lng -> include(LatLng(lat, lng)) }
                }
                selectedBooking.dropoffLatitude?.toDoubleOrNull()?.let { lat ->
                    selectedBooking.dropoffLongitude?.toDoubleOrNull()?.let { lng -> include(LatLng(lat, lng)) }
                }
            }.build()

            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150), 800)
            } catch (e: Exception) {
                // Fallback if bounds are too small (single point)
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(bounds.center, 14f), 800)
            }
        } else if (bookings.isNotEmpty()) {
            // If no selection, fit all bookings (Dashboard view)
            val boundsBuilder = LatLngBounds.builder()
            var hasValidPoints = false
            bookings.forEach {
                it.pickupLatitude?.toDoubleOrNull()?.let { lat ->
                    it.pickupLongitude?.toDoubleOrNull()?.let { lng ->
                        boundsBuilder.include(LatLng(lat, lng))
                        hasValidPoints = true
                    }
                }
            }
            if (hasValidPoints) {
                try {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150), 1000)
                } catch (e: Exception) { /* Ignore invalid bounds */ }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            // 4. Global Connectivity Layer
            // Only visible when zoomed out to show "World View"
            if (cameraPositionState.position.zoom < 6f) {
                GlobalConnectivityLayer()
            }

            // 5. Booking Markers & Routes
            bookings.forEach { booking ->
                val pickup = booking.getPickupLatLng()
                val dropoff = booking.getDropoffLatLng()
                val isSelected = booking.bookingId == selectedBooking?.bookingId

                // Pickup Marker
                if (pickup != null) {
                    MarkerComposable(
                        state = MarkerState(position = pickup),
                        zIndex = if (isSelected) 1f else 0f,
                        onClick = { onBookingMarkerClick(booking); true }
                    ) {
                        PremiumMapMarker(
                            type = "Pickup",
                            isSelected = isSelected,
                            color = LimoOrange
                        )
                    }
                }

                // Dropoff Marker
                if (dropoff != null) {
                    MarkerComposable(
                        state = MarkerState(position = dropoff),
                        zIndex = if (isSelected) 1f else 0f,
                        onClick = { onBookingMarkerClick(booking); true }
                    ) {
                        PremiumMapMarker(
                            type = "Drop",
                            isSelected = isSelected,
                            color = Color.Black
                        )
                    }
                }

                // Active Route Line
                if (showRoute && isSelected && pickup != null && dropoff != null) {
                    Polyline(
                        points = listOf(pickup, dropoff),
                        color = LimoOrange,
                        width = 10f, // Thicker line for better visibility
                        geodesic = true,
                        pattern = listOf(Dash(30f), Gap(10f)) // Dashed line looks more technical/premium
                    )
                }
            }
        }

        // 6. Connectivity Status Overlay (Glassmorphism Style)
        ConnectivityStatusOverlay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )
    }
}

// --- Helper Extensions for Robustness ---
private fun DriverBooking.getPickupLatLng(): LatLng? {
    val lat = pickupLatitude?.toDoubleOrNull()
    val lng = pickupLongitude?.toDoubleOrNull()
    return if (lat != null && lng != null) LatLng(lat, lng) else null
}

private fun DriverBooking.getDropoffLatLng(): LatLng? {
    val lat = dropoffLatitude?.toDoubleOrNull()
    val lng = dropoffLongitude?.toDoubleOrNull()
    return if (lat != null && lng != null) LatLng(lat, lng) else null
}

// --- Visual Components ---

@Composable
private fun PremiumMapMarker(
    type: String,
    isSelected: Boolean,
    color: Color
) {
    // Smooth scale animation for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        // Label Pill
        if (isSelected) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(50),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = type.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Dot Design
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.White, CircleShape) // White border
                .padding(3.dp) // Stroke width
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, CircleShape)
            )
        }

        // Pin Stem (Triangle)
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(8.dp)
                .background(color.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun GlobalConnectivityLayer() {
    // 1. Draw Network Lines (Geodesic Arcs)
    val pathEffect = remember { listOf(Dash(20f), Gap(20f)) } // Dashed flight paths

    // Connect hubs in a loop or mesh
    for (i in 0 until GLOBAL_HUBS.size - 1) {
        Polyline(
            points = listOf(GLOBAL_HUBS[i], GLOBAL_HUBS[i+1]),
            color = Color.Gray.copy(alpha = 0.2f), // Very subtle
            width = 2f,
            geodesic = true,
            pattern = pathEffect
        )
    }
    // Close loop
    Polyline(
        points = listOf(GLOBAL_HUBS.last(), GLOBAL_HUBS.first()),
        color = Color.Gray.copy(alpha = 0.2f),
        width = 2f,
        geodesic = true,
        pattern = pathEffect
    )

    // 2. Animated Pulse Markers at Hubs
    GLOBAL_HUBS.forEach { hub ->
        MarkerComposable(state = MarkerState(position = hub)) {
            NetworkPulseDot()
        }
    }
}

@Composable
private fun NetworkPulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Ripple Effect
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .alpha(alpha)
                .background(LimoOrange, CircleShape)
        )
        // Center Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(LimoOrange, CircleShape)
        )
    }
}

@Composable
private fun ConnectivityStatusOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.75f), // Semi-transparent dark
        shadowElevation = 0.dp // Flat look
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF4CAF50), CircleShape) // Green dot
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "1-800-Limo Global Network",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}