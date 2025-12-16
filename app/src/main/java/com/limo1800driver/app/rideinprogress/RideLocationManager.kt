package com.limo1800driver.app.rideinprogress

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns FusedLocationProvider updates for the active ride.
 *
 * Why this exists:
 * - ViewModels shouldn't directly manage platform callbacks.
 * - This makes location logic testable (throttling/emission decisions can be done outside).
 * - In a later step, this manager will be driven by a foreground service for background safety.
 */
@Singleton
class RideLocationManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

    private val _lastLocation = MutableStateFlow<RideLocationSample?>(null)
    val lastLocation: StateFlow<RideLocationSample?> = _lastLocation.asStateFlow()

    private var locationCallback: LocationCallback? = null

    fun hasFineOrCoarseLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start high-accuracy tracking suitable for navigation.
     *
     * Note: interval/distance defaults intentionally mirror the current working implementation
     * in the old ViewModel, so behavior doesn't regress during refactor.
     */
    fun start(
        updateIntervalMs: Long = 3000L,
        minUpdateIntervalMs: Long = 2000L,
        minUpdateDistanceMeters: Float = 5f
    ) {
        if (locationCallback != null) return

        if (!hasFineOrCoarseLocationPermission()) {
            Timber.tag("RideLocationManager").w("Location permission missing; not starting")
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, updateIntervalMs)
            .setMinUpdateIntervalMillis(minUpdateIntervalMs)
            .setMinUpdateDistanceMeters(minUpdateDistanceMeters)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                _lastLocation.value = loc.toSample()
            }
        }

        locationCallback = cb

        runCatching {
            fusedClient.requestLocationUpdates(request, cb, appContext.mainLooper)
            Timber.tag("RideLocationManager").d("Started fused location updates")
        }.onFailure {
            Timber.tag("RideLocationManager").e(it, "Failed to start location updates")
            locationCallback = null
        }
    }

    fun stop() {
        val cb = locationCallback ?: return
        locationCallback = null
        runCatching { fusedClient.removeLocationUpdates(cb) }
            .onFailure { Timber.tag("RideLocationManager").e(it, "Failed to stop location updates") }
    }
}

private fun Location.toSample(): RideLocationSample {
    val bearing = if (hasBearing()) bearing else 0f
    val speed = if (hasSpeed()) speed else 0f
    val accuracy = if (hasAccuracy()) accuracy else null
    return RideLocationSample(
        latitude = latitude,
        longitude = longitude,
        bearingDegrees = bearing,
        speedMps = speed,
        accuracyMeters = accuracy,
        timestampMs = time
    )
}


