package com.limo1800driver.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.dashboard.DriverBooking

/**
 * Driver Map View Component
 * Displays map with booking markers and routes
 */
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
    
    // Default camera position (can be updated based on bookings)
    val defaultLocation = LatLng(41.8781, -87.6298) // Chicago default
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }
    
    // Map properties
    val mapProperties = remember(isSatelliteView) {
        MapProperties(
            mapType = if (isSatelliteView) MapType.HYBRID else MapType.NORMAL,
            isTrafficEnabled = false,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_themed)
            } catch (e: Exception) {
                null
            }
        )
    }
    
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = true,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true,
            scrollGesturesEnabled = true
        )
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            // Add markers for each booking
            bookings.forEach { booking ->
                booking.pickupLatitude?.toDoubleOrNull()?.let { lat ->
                    booking.pickupLongitude?.toDoubleOrNull()?.let { lng ->
                        Marker(
                            state = MarkerState(position = LatLng(lat, lng)),
                            title = "Pickup - Booking #${booking.bookingId}",
                            snippet = booking.pickupAddress,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                            onClick = {
                                onBookingMarkerClick(booking)
                                true
                            }
                        )
                    }
                }
                
                booking.dropoffLatitude?.toDoubleOrNull()?.let { lat ->
                    booking.dropoffLongitude?.toDoubleOrNull()?.let { lng ->
                        Marker(
                            state = MarkerState(position = LatLng(lat, lng)),
                            title = "Dropoff - Booking #${booking.bookingId}",
                            snippet = booking.dropoffAddress,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                            onClick = {
                                onBookingMarkerClick(booking)
                                true
                            }
                        )
                    }
                }
            }
            
            // Show route for selected booking
            if (showRoute && selectedBooking != null) {
                selectedBooking.pickupLatitude?.toDoubleOrNull()?.let { pickupLat ->
                    selectedBooking.pickupLongitude?.toDoubleOrNull()?.let { pickupLng ->
                        selectedBooking.dropoffLatitude?.toDoubleOrNull()?.let { dropoffLat ->
                            selectedBooking.dropoffLongitude?.toDoubleOrNull()?.let { dropoffLng ->
                                // Draw polyline between pickup and dropoff
                                Polyline(
                                    points = listOf(
                                        LatLng(pickupLat, pickupLng),
                                        LatLng(dropoffLat, dropoffLng)
                                    ),
                                    color = Color(0xFFFF9800), // Orange
                                    width = 8f
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

