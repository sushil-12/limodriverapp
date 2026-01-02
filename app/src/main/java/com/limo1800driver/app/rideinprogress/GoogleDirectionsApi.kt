package com.limo1800driver.app.rideinprogress

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Minimal Google Directions API client (REST).
 *
 * We only parse what we need:
 * - overview_polyline for drawing/snap-to-route
 * - distance + duration for metrics
 */
interface GoogleDirectionsApi {
    @GET("/maps/api/directions/json")
    suspend fun directions(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): GoogleDirectionsResponse
    
    /**
     * Get directions with waypoints for route validation.
     * Waypoints are passed as a pipe-separated string (e.g., "lat1,lng1|lat2,lng2").
     */
    @GET("/maps/api/directions/json")
    suspend fun directionsWithWaypoints(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): GoogleDirectionsResponse
}


