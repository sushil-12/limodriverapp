package com.limo1800driver.app.rideinprogress

import com.limo1800driver.app.data.socket.DriverActiveRide
import com.limo1800driver.app.data.socket.DriverSocketService
import com.limo1800driver.app.data.storage.TokenManager
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A thin, testable wrapper over the existing Socket.IO service.
 *
 * Why this exists:
 * - Keeps RideInProgressViewModel focused on orchestration rather than payload construction.
 * - Centralizes JWT->driverId parsing (currently duplicated across ViewModels).
 * - Will later own throttling rules for driver.location.update (time + distance based).
 */
@Singleton
class RideSocketManager @Inject constructor(
    private val socketService: DriverSocketService,
    private val tokenManager: TokenManager
) {
    val activeRide: StateFlow<DriverActiveRide?> = socketService.activeRide

    fun ensureConnected() = socketService.connect()

    fun clearActiveRide() = socketService.clearActiveRide()

    fun emitDriverLocationUpdate(ride: DriverActiveRide, sample: RideLocationSample) {
        socketService.emitDriverLocationUpdate(
            bookingId = ride.bookingId,
            customerId = ride.customerId,
            latitude = sample.latitude,
            longitude = sample.longitude,
            heading = sample.bearingDegrees.toInt(),
            speed = sample.speedMps.toInt().coerceAtLeast(0)
        )
    }

    fun emitBookingStatusUpdate(
        ride: DriverActiveRide,
        status: String,
        currentLocation: Pair<Double, Double>? = null
    ) {
        socketService.emitBookingStatusUpdate(
            bookingId = ride.bookingId,
            customerId = ride.customerId,
            status = status,
            driverId = resolveDriverId(),
            currentLocation = currentLocation,
            pickupInfo = Triple(ride.pickupLatitude, ride.pickupLongitude, ride.pickupAddress),
            dropoffInfo = Triple(ride.dropoffLatitude, ride.dropoffLongitude, ride.dropoffAddress)
        )

        // Critical: update local activeRide so the UI transitions immediately (iOS parity).
        socketService.updateActiveRideStatus(ride.bookingId, status)
    }

    private fun resolveDriverId(): String {
        val token = tokenManager.getAccessToken()
        return extractUserIdFromJwt(token) ?: "unknown"
    }

    private fun extractUserIdFromJwt(token: String?): String? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadJson = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val obj = JSONObject(payloadJson)
            obj.optString("sub")
                .ifBlank { obj.optString("user_id") }
                .ifBlank { obj.optString("id") }
                .ifBlank { null }
        } catch (e: Exception) {
            Timber.tag("RideSocketManager").e(e, "Failed to parse JWT for driverId")
            null
        }
    }
}


