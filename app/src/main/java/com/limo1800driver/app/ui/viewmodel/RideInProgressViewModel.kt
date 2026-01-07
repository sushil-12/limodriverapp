package com.limo1800driver.app.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.socket.DriverActiveRide
import dagger.hilt.android.lifecycle.HiltViewModel
import com.limo1800driver.app.rideinprogress.RideLocationManager
import com.limo1800driver.app.rideinprogress.RideLocationSample
import com.limo1800driver.app.rideinprogress.RideSocketManager
import com.limo1800driver.app.rideinprogress.RideRouteRepository
import com.limo1800driver.app.rideinprogress.RideRouteSnapper
import com.limo1800driver.app.rideinprogress.RidePhase
import com.limo1800driver.app.rideinprogress.ridePhaseFromStatus
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@HiltViewModel
class RideInProgressViewModel @Inject constructor(
    private val socketManager: RideSocketManager,
    private val locationManager: RideLocationManager,
    private val routeRepository: RideRouteRepository
) : ViewModel() {

    val activeRide: StateFlow<DriverActiveRide?> = socketManager.activeRide
    val lastLocation: StateFlow<RideLocationSample?> = locationManager.lastLocation
    private val _snappedLocation = MutableStateFlow<RideLocationSample?>(null)
    val snappedLocation: StateFlow<RideLocationSample?> = _snappedLocation.asStateFlow()

    private val _activeRoutePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val activeRoutePolyline: StateFlow<List<LatLng>> = _activeRoutePolyline.asStateFlow()

    private val _previewRoutePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val previewRoutePolyline: StateFlow<List<LatLng>> = _previewRoutePolyline.asStateFlow()

    private val _etaText = MutableStateFlow<String?>(null)
    val etaText: StateFlow<String?> = _etaText.asStateFlow()

    private val _distanceText = MutableStateFlow<String?>(null)
    val distanceText: StateFlow<String?> = _distanceText.asStateFlow()

    private val _pickupArrivalDetected = MutableStateFlow(false)
    val pickupArrivalDetected: StateFlow<Boolean> = _pickupArrivalDetected.asStateFlow()

    private val _dropoffArrivalDetected = MutableStateFlow(false)
    val dropoffArrivalDetected: StateFlow<Boolean> = _dropoffArrivalDetected.asStateFlow()

    private var trackingBookingId: Int? = null
    private var trackingJob: Job? = null
    private var emissionJob: Job? = null
    private var previewRouteJob: Job? = null
    private var activeRouteJob: Job? = null
    private var lastRouteRequestAtMs: Long = 0L
    private var lastRouteOrigin: LatLng? = null
    private val snapper = RideRouteSnapper(maxSnapDistanceMeters = 100.0)

    private var lastSocketEmissionAtMs: Long = 0L
    private var lastSocketEmissionLocation: LatLng? = null

    // Chat-mode safety (mirrors iOS behavior: pause tracking during chat, add cooldown after returning)
    private val _isInChatMode = MutableStateFlow(false)
    val isInChatMode: StateFlow<Boolean> = _isInChatMode.asStateFlow()
    private var chatCooldownUntilMs: Long = 0L

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<RideInProgressUiState> = combine(
        activeRide,
        lastLocation,
        snappedLocation,
        activeRoutePolyline,
        previewRoutePolyline,
        etaText,
        distanceText,
        pickupArrivalDetected,
        dropoffArrivalDetected,
        isInChatMode
    ) { values: Array<Any?> ->
        RideInProgressUiState(
            activeRide = values[0] as DriverActiveRide?,
            lastLocation = values[1] as RideLocationSample?,
            snappedLocation = values[2] as RideLocationSample?,
            activeRoutePolyline = values[3] as List<LatLng>,
            previewRoutePolyline = values[4] as List<LatLng>,
            etaText = values[5] as String?,
            distanceText = values[6] as String?,
            pickupArrivalDetected = values[7] as Boolean,
            dropoffArrivalDetected = values[8] as Boolean,
            isInChatMode = values[9] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RideInProgressUiState()
    )

    init {
        // Ensure preview route (pickup -> dropoff) is available even if live tracking isn't running yet
        // (e.g., when the ride is already in "on_location").
        viewModelScope.launch {
            activeRide.collectLatest { ride ->
                if (ride != null) warmPreviewRouteIfPossible(ride)
            }
        }
    }

    fun ensureConnected() {
        socketManager.ensureConnected()
    }

    /**
     * Call right before navigating out to chat (or launching an external messaging app).
     * We pause location updates + emissions to avoid fighting UI/lifecycle.
     */
    fun enterChatMode() {
        if (_isInChatMode.value) return
        _isInChatMode.value = true
        stopLiveTracking()
    }

    /**
     * Call when the app/screen resumes after chat.
     * We intentionally apply a cooldown to prevent over-updates right after returning (iOS parity).
     */
    fun exitChatMode(cooldownMs: Long = 60_000L) {
        if (!_isInChatMode.value) return
        _isInChatMode.value = false
        chatCooldownUntilMs = System.currentTimeMillis() + cooldownMs

        // Restart tracking after cooldown if ride is still active.
        viewModelScope.launch {
            delay(cooldownMs)
            if (_isInChatMode.value) return@launch
            val ride = activeRide.value ?: return@launch
            startLiveTrackingIfNeeded(ride)
        }
    }

    fun startLiveTrackingIfNeeded(ride: DriverActiveRide) {
        // iOS keeps tracking while this view is active (unless in chat mode).
        // We track for all "active" phases so live marker + socket emission works even if the app
        // is opened directly into `on_location`.
        if (_isInChatMode.value) return
        if (System.currentTimeMillis() < chatCooldownUntilMs) return

        if (ride.status == "ended") {
            stopLiveTracking()
            return
        }
        // Backend statuses can vary; as long as it's not ended, we can track + compute metrics.
        // This prevents ETA/distance from being stuck on "—" when status strings differ.
        if (ridePhaseFromStatus(ride.status) == RidePhase.Ended) return
        if (trackingBookingId == ride.bookingId && trackingJob != null) return

        stopLiveTracking()
        trackingBookingId = ride.bookingId

        if (!locationManager.hasFineOrCoarseLocationPermission()) {
            Timber.tag("RideInProgressVM").w("Location permission missing; live tracking not started")
            return
        }

        // Start the fused location stream (manager owns platform callbacks).
        locationManager.start()

        trackingJob = viewModelScope.launch {
            locationManager.lastLocation.collectLatest { sample ->
                if (sample != null) handleLocationUpdate(ride, sample)
            }
        }

        // iOS has a timer that ensures driver.location.update is emitted regularly even if location callbacks are sparse.
        emissionJob?.cancel()
        emissionJob = viewModelScope.launch {
            while (trackingBookingId == ride.bookingId && !_isInChatMode.value) {
                delay(5_000L)
                if (_isInChatMode.value) continue
                if (System.currentTimeMillis() < chatCooldownUntilMs) continue
                val sample = _snappedLocation.value ?: lastLocation.value ?: continue
                val snapped = LatLng(sample.latitude, sample.longitude)
                maybeEmitToSocket(ride, sample, snapped)
            }
        }

        Timber.tag("RideInProgressVM").d("Started live tracking for bookingId=${ride.bookingId}")

        // Warm preview route (pickup -> dropoff) once per ride.
        warmPreviewRouteIfPossible(ride)
    }

    fun stopLiveTracking() {
        trackingBookingId = null
        trackingJob?.cancel()
        trackingJob = null
        emissionJob?.cancel()
        emissionJob = null
        previewRouteJob?.cancel()
        previewRouteJob = null
        activeRouteJob?.cancel()
        activeRouteJob = null
        locationManager.stop()
    }

    private fun handleLocationUpdate(ride: DriverActiveRide, sample: RideLocationSample) {
        if (_isInChatMode.value) return
        if (System.currentTimeMillis() < chatCooldownUntilMs) return

        val raw = LatLng(sample.latitude, sample.longitude)
        val snapped = if (_activeRoutePolyline.value.size >= 2) {
            snapper.snap(raw, _activeRoutePolyline.value)
        } else {
            raw
        }

        // Keep a snapped version for UI (car marker snaps to route like iOS).
        _snappedLocation.value = sample.copy(latitude = snapped.latitude, longitude = snapped.longitude)

        // Throttle socket emissions (time + distance), but don't throttle UI updates.
        maybeEmitToSocket(ride, sample, snapped)

        // Update route + ETA/distance (throttled to avoid over-updates).
        maybeRefreshActiveRoute(ride, sample)

        // Arrival detection (<=100m) - used to enable "Arrived at pickup" UI gate
        updateArrivalDetection(ride, snapped)
    }

    private fun updateArrivalDetection(ride: DriverActiveRide, snapped: LatLng) {
        // Pickup proximity (used to help the driver; we do NOT auto-emit status to avoid accidental transitions)
        if (ride.status == "en_route_pu") {
            val lat = ride.pickupLatitude
            val lng = ride.pickupLongitude
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                val d = FloatArray(1)
                Location.distanceBetween(snapped.latitude, snapped.longitude, lat, lng, d)
                _pickupArrivalDetected.value = (d.firstOrNull() ?: Float.MAX_VALUE) <= 100f
            }
        } else {
            _pickupArrivalDetected.value = false
        }

        // Dropoff proximity (for UI; status transition still handled by auto-ended logic above)
        if (ride.status == "en_route_do") {
            val lat = ride.dropoffLatitude
            val lng = ride.dropoffLongitude
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                val d = FloatArray(1)
                Location.distanceBetween(snapped.latitude, snapped.longitude, lat, lng, d)
                _dropoffArrivalDetected.value = (d.firstOrNull() ?: Float.MAX_VALUE) <= 50f
            }
        } else {
            _dropoffArrivalDetected.value = false
        }
    }

    private fun maybeEmitToSocket(ride: DriverActiveRide, sample: RideLocationSample, snapped: LatLng) {
        if (!shouldEmitToSocket(snapped)) {
            Timber.tag("RideInProgressVM").d("maybeEmitToSocket: throttled, skipping emission")
            return
        }
        Timber.tag("RideInProgressVM").d("maybeEmitToSocket: emitting location update bookingId=${ride.bookingId} lat=${snapped.latitude} lng=${snapped.longitude}")
        socketManager.emitDriverLocationUpdate(
            ride,
            sample.copy(latitude = snapped.latitude, longitude = snapped.longitude)
        )
        lastSocketEmissionAtMs = System.currentTimeMillis()
        lastSocketEmissionLocation = snapped
    }

    private fun shouldEmitToSocket(snapped: LatLng): Boolean {
        val now = System.currentTimeMillis()
        val dt = now - lastSocketEmissionAtMs
        if (lastSocketEmissionLocation == null) {
            Timber.tag("RideInProgressVM").d("shouldEmitToSocket: first emission, allowing")
            return true
        }
        if (dt >= 5_000L) {
            Timber.tag("RideInProgressVM").d("shouldEmitToSocket: time threshold met (${dt}ms >= 5000ms), allowing")
            return true
        }

        val prev = lastSocketEmissionLocation ?: return true
        val d = FloatArray(1)
        Location.distanceBetween(prev.latitude, prev.longitude, snapped.latitude, snapped.longitude, d)
        val meters = d.firstOrNull() ?: 0f
        val shouldEmit = meters >= 10f
        if (!shouldEmit) {
            Timber.tag("RideInProgressVM").d("shouldEmitToSocket: distance threshold not met (${meters}m < 10m, dt=${dt}ms), throttling")
        } else {
            Timber.tag("RideInProgressVM").d("shouldEmitToSocket: distance threshold met (${meters}m >= 10m), allowing")
        }
        return shouldEmit
    }

    private fun warmPreviewRouteIfPossible(ride: DriverActiveRide) {
        val pickup = ride.pickupLatitude?.takeIf { it != 0.0 }?.let { lat ->
            ride.pickupLongitude?.takeIf { it != 0.0 }?.let { lng -> LatLng(lat, lng) }
        } ?: return
        val dropoff = ride.dropoffLatitude?.takeIf { it != 0.0 }?.let { lat ->
            ride.dropoffLongitude?.takeIf { it != 0.0 }?.let { lng -> LatLng(lat, lng) }
        } ?: return

        // Avoid refetching if already present.
        if (_previewRoutePolyline.value.isNotEmpty()) return

        previewRouteJob?.cancel()
        previewRouteJob = viewModelScope.launch {
            val route = routeRepository.getRoute(pickup, dropoff, force = false) ?: return@launch
            _previewRoutePolyline.value = route.polyline
        }
    }

    private fun maybeRefreshActiveRoute(ride: DriverActiveRide, sample: RideLocationSample) {
        val destination = when (ride.status) {
            "en_route_do" -> ride.dropoffLatitude?.takeIf { it != 0.0 }?.let { lat ->
                ride.dropoffLongitude?.takeIf { it != 0.0 }?.let { lng -> LatLng(lat, lng) }
            }
            else -> ride.pickupLatitude?.takeIf { it != 0.0 }?.let { lat ->
                ride.pickupLongitude?.takeIf { it != 0.0 }?.let { lng -> LatLng(lat, lng) }
            }
        } ?: return

        val origin = LatLng(sample.latitude, sample.longitude)
        val now = System.currentTimeMillis()

        // Throttle route recalculation:
        // - no more than every 15s
        // - unless the driver moved significantly since last request (>= 75m)
        val movedMeters = lastRouteOrigin?.let { prev ->
            val d = FloatArray(1)
            Location.distanceBetween(prev.latitude, prev.longitude, origin.latitude, origin.longitude, d)
            d.firstOrNull() ?: 0f
        } ?: Float.MAX_VALUE

        if ((now - lastRouteRequestAtMs) < 15_000L && movedMeters < 75f) return

        lastRouteRequestAtMs = now
        lastRouteOrigin = origin

        activeRouteJob?.cancel()
        activeRouteJob = viewModelScope.launch {
            val route = routeRepository.getRoute(origin, destination, force = false)
            if (route == null) {
                // Fallback: still show *something* if Directions fails (key not enabled / quota / network).
                setFallbackTripMetrics(origin, destination)
                return@launch
            }
            _activeRoutePolyline.value = route.polyline
            _distanceText.value = formatDistance(route.distanceMeters)
            _etaText.value = formatDuration(route.durationSeconds)
        }
    }

    private fun setFallbackTripMetrics(origin: LatLng, destination: LatLng) {
        val d = FloatArray(1)
        android.location.Location.distanceBetween(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude,
            d
        )
        val meters = (d.firstOrNull() ?: 0f).toLong().coerceAtLeast(0L)

        // Conservative driving speed assumption to avoid wildly optimistic ETAs.
        val assumedMps = 11.11 // ~40 km/h
        val seconds = (meters / assumedMps).toLong().coerceAtLeast(0L)

        if (_distanceText.value == null) _distanceText.value = formatDistance(meters)
        if (_etaText.value == null) _etaText.value = formatDuration(seconds)
    }

    private fun formatDistance(distanceMeters: Long): String {
        return if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000.0)
        } else {
            "$distanceMeters m"
        }
    }

    private fun formatDuration(durationSeconds: Long): String {
        val minutes = (durationSeconds / 60).coerceAtLeast(0)
        return "${minutes} Min"
    }

    fun emitOnLocation(ride: DriverActiveRide) {
        socketManager.emitBookingStatusUpdate(ride, status = "on_location", currentLocation = null)
    }

    /**
     * iOS currently uses a hard-coded "1234" verification check in `RideInProgressView`.
     * We replicate that while we confirm the real backend verification endpoint.
     */
    fun verifyAndStartRide(ride: DriverActiveRide, otp: String): Boolean {
        if (otp != "1234") return false
        socketManager.emitBookingStatusUpdate(ride, status = "en_route_do", currentLocation = null)
        return true
    }

    fun completeRide(ride: DriverActiveRide) {
        // iOS uses "ended" when dropoff arrival is detected.
        socketManager.emitBookingStatusUpdate(ride, status = "ended", currentLocation = null)
        socketManager.clearActiveRide()
        stopLiveTracking()
    }

    override fun onCleared() {
        stopLiveTracking()
        super.onCleared()
    }
}


