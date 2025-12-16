package com.limo1800driver.app.ui.viewmodel

import com.google.android.gms.maps.model.LatLng
import com.limo1800driver.app.data.socket.DriverActiveRide
import com.limo1800driver.app.rideinprogress.RideLocationSample

/**
 * Single source of truth for the Ride In Progress screen.
 *
 * Why:
 * - Compose screens should ideally collect ONE state object to reduce wiring noise,
 *   reduce recomposition surfaces, and improve testability.
 */
data class RideInProgressUiState(
    val activeRide: DriverActiveRide? = null,
    val lastLocation: RideLocationSample? = null,
    val snappedLocation: RideLocationSample? = null,
    val activeRoutePolyline: List<LatLng> = emptyList(),
    val previewRoutePolyline: List<LatLng> = emptyList(),
    val etaText: String? = null,
    val distanceText: String? = null,
    val pickupArrivalDetected: Boolean = false,
    val dropoffArrivalDetected: Boolean = false,
    val isInChatMode: Boolean = false
) {
    val displayLocation: RideLocationSample? get() = snappedLocation ?: lastLocation
    val hasArrivedAtPickup: Boolean get() = activeRide?.status == "on_location"
    val isRideStarted: Boolean get() = activeRide?.status == "en_route_do"
}


