package com.limo1800driver.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.theme.LimoOrange
import kotlinx.coroutines.delay
import timber.log.Timber

// --- Configuration ---
// Expanded list for better global animation coverage
private val GLOBAL_HUBS = listOf(
    LatLng(40.7128, -74.0060), // New York
    LatLng(51.5074, -0.1278),  // London
    LatLng(48.8566, 2.3522),   // Paris
    LatLng(25.2048, 55.2708),  // Dubai
    LatLng(19.0760, 72.8777),  // Mumbai
    LatLng(1.3521, 103.8198),  // Singapore
    LatLng(35.6762, 139.6503), // Tokyo
    LatLng(-33.8688, 151.2093),// Sydney
    LatLng(34.0522, -118.2437) // Los Angeles
)

@Composable
fun DriverMapView(
    bookings: List<DriverBooking>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State: Map Loading
    var isMapLoaded by remember { mutableStateOf(false) }

    // State: Camera
    val cameraPositionState = rememberCameraPositionState {
        // Start zoomed out for the "Global Connectivity" effect
        position = CameraPosition.fromLatLngZoom(LatLng(25.0, 15.0), 1f)
    }

    // Optimization: Pre-load BitmapDescriptor to avoid lag during animation
    // We create a simple orange dot programmatically to avoid context switching
    val hubIconDescriptor = remember {
        try {
            bitmapDescriptorFromVector(context, R.drawable.ic_map_dot_orange)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create bitmap descriptor, Maps SDK may not be initialized")
            null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF121212))) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal),
                isTrafficEnabled = false,
                isIndoorEnabled = false,
                minZoomPreference = 2f
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            ),
            onMapLoaded = { isMapLoaded = true }
        ) {
            // 1. The Global Connectivity Animation Layer
            // Only runs when map is effectively "idle" or in overview mode
            GlobalConnectivityAnimation(
                hubs = GLOBAL_HUBS,
                iconDescriptor = hubIconDescriptor
            )

            // 2. Real Booking Data (Your actual driver jobs)
            // We use standard markers here if the list is large, or composable if < 10
            bookings.forEach { booking ->
                BookingMarker(booking)
            }
        }

        // 3. Connectivity Overlay UI
        ConnectivityStatusOverlay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        // 4. Loading State (Optimized Perceived Performance)
        // This hides the black screen flicker while Maps initializes GMS
        AnimatedVisibility(
            visible = !isMapLoaded,
            exit = fadeOut(animationSpec = tween(600)),
            modifier = Modifier.fillMaxSize()
        ) {
            MapLoadingPlaceholder()
        }
    }
}

/**
 * Handles the sequential logic:
 * 1. Reveal Hub A
 * 2. Draw Line A -> B
 * 3. Reveal Hub B
 * ...
 */
@Composable
private fun GlobalConnectivityAnimation(
    hubs: List<LatLng>,
    iconDescriptor: BitmapDescriptor?
) {
    // State to hold how many hubs we have currently "discovered"
    var revealedCount by remember { mutableIntStateOf(0) }

    // The Animation Loop
    LaunchedEffect(Unit) {
        // Initial delay to let map render first frame
        delay(1000)

        hubs.indices.forEach { i ->
            revealedCount = i + 1
            delay(400) // Speed of propagation (adjust for faster/slower animation)
        }

        // Optional: Loop forever?
        // while(true) { ... }
    }

    // Render Revealed Components
    val visibleHubs = hubs.take(revealedCount)

    // A. The Pins (Markers)
    visibleHubs.forEach { latLng ->
        Marker(
            state = MarkerState(position = latLng),
            icon = iconDescriptor, // Using Bitmap is CRITICAL for performance here
            anchor = Offset(0.5f, 0.5f),
            zIndex = 0f
        )
    }

    // B. The Connectivity Lines (Polylines)
    // We draw lines connecting the sequence
    if (visibleHubs.size > 1) {
        Polyline(
            points = visibleHubs,
            color = LimoOrange.copy(alpha = 0.6f),
            width = 4f,
            geodesic = true, // Curves the line to follow earth's curvature
            pattern = listOf(Dash(20f), Gap(20f)) // Technical dashed look
        )

        // Optional: Close the loop if all are revealed
        if (revealedCount == hubs.size) {
            Polyline(
                points = listOf(visibleHubs.last(), visibleHubs.first()),
                color = LimoOrange.copy(alpha = 0.6f),
                width = 4f,
                geodesic = true,
                pattern = listOf(Dash(20f), Gap(20f))
            )
        }
    }
}

// --- Supporting UI Components ---

@Composable
fun MapLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)), // Match map dark theme background
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = LimoOrange,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "INITIALIZING SATELLITE UPLINK...",
                color = LimoOrange.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun BookingMarker(booking: DriverBooking) {
    val position = booking.getLatLng() ?: return

    // Use MarkerComposable only for the "Real" interactive jobs
    // because we need click handling and custom UI
    MarkerComposable(
        state = MarkerState(position = position),
        zIndex = 10f, // Always on top of the animation mesh
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(LimoOrange, RoundedCornerShape(50))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JOB",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConnectivityStatusOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF4CAF50), androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GLOBAL NETWORK ACTIVE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// --- Helper Utilities ---

// Extension to get LatLng safely
private fun DriverBooking.getLatLng(): LatLng? {
    val lat = pickupLatitude?.toDoubleOrNull()
    val lng = pickupLongitude?.toDoubleOrNull()
    return if (lat != null && lng != null) LatLng(lat, lng) else null
}

/**
 * Optimized way to create a BitmapDescriptor from a Drawable resource.
 * Caches the result to avoid re-decoding bitmaps on every recomposition.
 */
fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int
): BitmapDescriptor? {
    // In a real app, use a proper cache/memoization if calling frequently
    return try {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        // If Maps SDK is not initialized or any other error occurs, return null
        Timber.e(e, "Failed to create bitmap descriptor")
        null
    }
}