package com.limo1800driver.app.rideinprogress

/**
 * Immutable location snapshot used across the Ride In Progress stack.
 *
 * Keep this UI-agnostic and platform-agnostic (no Android Location dependency) so it can be
 * unit-tested and reused by ViewModel/Map/Socket layers.
 */
data class RideLocationSample(
    val latitude: Double,
    val longitude: Double,
    /** Degrees in [0, 360). */
    val bearingDegrees: Float,
    /** Meters/second. */
    val speedMps: Float,
    /** Horizontal accuracy in meters (best-effort). */
    val accuracyMeters: Float?,
    /** Wall-clock time in millis when this sample was produced. */
    val timestampMs: Long
)


