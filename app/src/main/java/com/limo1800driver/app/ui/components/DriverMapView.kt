package com.limo1800driver.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import kotlinx.coroutines.launch

@SuppressLint("PotentialBehaviorOverride")
@Composable
fun DriverMapView(
    bookings: List<DriverBooking>,
    // Pass padding values to respect BottomSheets/TopBars (Uber-like centering)
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
    modifier: Modifier = Modifier,
    // Only enable my location if permissions are granted
    hasLocationPermission: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Map Styling: Load the minimal JSON style for that "clean" look
    // Only enable my location if permissions are granted to prevent SecurityException
    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission, // Only enable if permissions are granted
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
        )
    }

    // 2. UI Settings: Keep gestures enabled but hide controls
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true
        )
    }

    // 3. Camera State
    val cameraPositionState = rememberCameraPositionState {
        // Default startup location (e.g., last known or a default hub)
        position = CameraPosition.fromLatLngZoom(LatLng(40.7128, -74.0060), 12f)
    }

    // 4. Smooth Camera Updates (The "Uber" Feel)
    // We listen to the bookings list. If it changes, we smoothly animate the camera.
    LaunchedEffect(bookings) {
        if (bookings.isNotEmpty()) {
            val bounds = LatLngBounds.builder()
            var hasValidPoints = false

            bookings.forEach { booking ->
                booking.getLatLng()?.let {
                    bounds.include(it)
                    hasValidPoints = true
                }
            }

            if (hasValidPoints) {
                // If single booking, zoom to it. If multiple, fit bounds.
                val update = if (bookings.size == 1) {
                    val target = bookings.first().getLatLng()!!
                    CameraUpdateFactory.newLatLngZoom(target, 15f)
                } else {
                    // 100 padding is arbitrary; adjust based on design
                    CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                }

                // Animate over 1000ms for smoothness
                cameraPositionState.animate(update, 1000)
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        // Apply padding to the Google Map logo and legal text so they aren't hidden by UI
        contentPadding = contentPadding
    ) {
        bookings.forEach { booking ->
            val position = booking.getLatLng()
            if (position != null) {
                // Use a stable key if possible to prevent unnecessary marker recomposition
                key(booking.bookingId) {
                Marker(
                    state = MarkerState(position = position),
                        title = "Booking #${booking.bookingId}",
                        snippet = booking.pickupAddress,
                        // For true Uber polish, use a custom Drawable resource, not the default HUE
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        // Flat markers rotate with the map, creating a more integrated 2D feel
                        flat = true
                    )
                }
            }
        }
    }
}

private fun DriverBooking.getLatLng(): LatLng? {
    val lat = pickupLatitude?.toDoubleOrNull()
    val lng = pickupLongitude?.toDoubleOrNull()
    return if (lat != null && lng != null) LatLng(lat, lng) else null
}