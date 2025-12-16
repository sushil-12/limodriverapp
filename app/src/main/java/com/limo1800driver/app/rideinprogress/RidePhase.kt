package com.limo1800driver.app.rideinprogress

/**
 * High-level ride phase derived from backend status strings.
 *
 * We keep this in the Ride In Progress feature module (not the socket layer) because:
 * - backend statuses can change/expand
 * - UI/business rules want stable semantic phases
 */
enum class RidePhase {
    ToPickup,
    AtPickup,
    ToDropoff,
    Ended,
    Unknown
}

fun ridePhaseFromStatus(status: String?): RidePhase = when (status) {
    "en_route_pu" -> RidePhase.ToPickup
    "on_location" -> RidePhase.AtPickup
    "en_route_do" -> RidePhase.ToDropoff
    "ended" -> RidePhase.Ended
    else -> RidePhase.Unknown
}


