package com.limo1800driver.app.rideinprogress

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.limo1800driver.app.data.network.NetworkConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class RideRoute(
    val polyline: List<LatLng>,
    val distanceMeters: Long,
    val durationSeconds: Long
)

/**
 * Fetches driving routes via Google Directions API.
 *
 * Key design choices:
 * - Keep a small in-memory cache keyed by origin+destination to avoid over-updates (iOS uses 120s cache).
 * - Return a decoded polyline for drawing + snapping.
 */
@Singleton
class RideRouteRepository @Inject constructor(
    private val directionsApi: GoogleDirectionsApi
) {
    private data class CacheKey(val origin: String, val destination: String)

    private var cacheKey: CacheKey? = null
    private var cacheValue: RideRoute? = null
    private var cacheTimestampMs: Long = 0L

    /**
     * Cache TTL for repeated requests with the same origin/destination.
     * iOS uses 120s for "Google trip metrics"; we keep the same default.
     */
    private val cacheTtlMs: Long = 120_000L

    suspend fun getRoute(origin: LatLng, destination: LatLng, force: Boolean = false): RideRoute? {
        val o = "${origin.latitude},${origin.longitude}"
        val d = "${destination.latitude},${destination.longitude}"
        val key = CacheKey(o, d)

        val now = System.currentTimeMillis()
        if (!force && cacheKey == key && cacheValue != null && (now - cacheTimestampMs) < cacheTtlMs) {
            return cacheValue
        }

        return runCatching {
            val response = directionsApi.directions(
                origin = o,
                destination = d,
                apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            if (response.routes.isEmpty()) {
                Timber.tag("RideRouteRepo").w("Directions returned no routes status=${response.status}")
                return@runCatching null
            }

            val route = response.routes.first()
            val points = route.overviewPolyline?.points.orEmpty()
            if (points.isBlank()) {
                Timber.tag("RideRouteRepo").w("Directions missing overview_polyline")
                return@runCatching null
            }

            val decoded = PolyUtil.decode(points)
            val leg = route.legs.firstOrNull()
            val distance = leg?.distance?.value ?: 0L
            val duration = leg?.duration?.value ?: 0L

            RideRoute(polyline = decoded, distanceMeters = distance, durationSeconds = duration)
        }.onSuccess { rr ->
            if (rr != null) {
                cacheKey = key
                cacheValue = rr
                cacheTimestampMs = now
            }
        }.onFailure {
            Timber.tag("RideRouteRepo").e(it, "Directions request failed")
        }.getOrNull()
    }
}


