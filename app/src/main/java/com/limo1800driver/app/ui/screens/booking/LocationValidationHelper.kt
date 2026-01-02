package com.limo1800driver.app.ui.screens.booking

import com.limo1800driver.app.data.network.NetworkConfig
import com.limo1800driver.app.rideinprogress.GoogleDirectionsApi
import timber.log.Timber

/**
 * Helper class for location validation matching iOS behavior
 * Validates that pickup and dropoff are in the same country and not the same location
 */
object LocationValidationHelper {
    
    /**
     * Normalize country name to match iOS logic
     * Handles US states, Canadian provinces, and common country name variations
     */
    fun normalizeCountry(country: String?): String? {
        if (country.isNullOrBlank()) return null
        
        val cleaned = country.replace(".", "")
            .uppercase()
            .replace(" ", "")
        
        // Check if contains digits (e.g., "FRANCE75001" -> "FRANCE")
        val containsDigit = cleaned.any { it.isDigit() }
        val compact = if (containsDigit) {
            cleaned.replace(Regex("[0-9]"), "").takeIf { it.isNotBlank() } ?: return null
        } else {
            cleaned
        }
        
        // US state abbreviations
        val usStates = setOf(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA",
            "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT",
            "VA", "WA", "WV", "WI", "WY"
        )
        if (usStates.contains(compact)) return "UNITEDSTATES"
        
        // Canadian province codes
        val caProvinces = setOf("AB", "BC", "MB", "NB", "NL", "NT", "NS", "NU", "ON", "PE", "QC", "SK", "YT")
        if (caProvinces.contains(compact)) return "CANADA"
        
        // Common country name variations
        when (compact) {
            "UNITEDSTATESOFAMERICA", "UNITEDSTATES", "USA", "US" -> return "UNITEDSTATES"
            "UNITEDKINGDOM", "GREATBRITAIN", "BRITAIN", "ENGLAND", "UK" -> return "UNITEDKINGDOM"
            "UNITEDARABEMIRATES", "UAE" -> return "UNITEDARABEMIRATES"
        }
        
        // If 3 characters or less, return as is (likely country code)
        if (compact.length <= 3) return compact
        
        // Otherwise return uppercased version
        return country.uppercase()
    }
    
    /**
     * Extract country from address string
     * Looks for country in the last component of comma-separated address
     */
    fun extractCountryFromAddress(address: String): String? {
        val components = address.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        // Try the last component first (often country)
        for (component in components.reversed()) {
            if (component.any { it.isLetter() }) {
                return component
            }
        }
        
        return components.lastOrNull()
    }
    
    /**
     * Extract city from address string
     */
    fun extractCityFromAddress(address: String): String? {
        val components = address.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        // Try penultimate component (often city)
        if (components.size >= 2) {
            val withoutCountry = components.dropLast(1)
            for (component in withoutCountry.reversed()) {
                if (component.any { it.isLetter() }) {
                    return component
                }
            }
        }
        
        return components.firstOrNull()
    }
    
    /**
     * Normalize city name
     */
    fun normalizeCity(city: String?): String? {
        if (city.isNullOrBlank()) return null
        return city.replace(Regex("\\s+"), " ")
            .replace(Regex("[,;]"), " ")
            .trim()
            .uppercase()
    }
    
    /**
     * Normalize location text for comparison
     */
    fun normalizeLocationText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[,;]"), " ")
            .trim()
            .uppercase()
    }
    
    /**
     * Check if two coordinates are approximately equal (within tolerance)
     */
    fun coordinatesApproximatelyEqual(
        coord1: Pair<Double, Double>?,
        coord2: Pair<Double, Double>?,
        tolerance: Double = 0.00001
    ): Boolean {
        if (coord1 == null || coord2 == null) return false
        return kotlin.math.abs(coord1.first - coord2.first) < tolerance &&
                kotlin.math.abs(coord1.second - coord2.second) < tolerance
    }
    
    /**
     * Check if two locations are the same (coordinate-based or text-based)
     */
    fun areLocationsSame(
        pickupText: String,
        dropoffText: String,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?
    ): Boolean {
        // Check coordinates first
        if (pickupCoord != null && dropoffCoord != null) {
            if (coordinatesApproximatelyEqual(pickupCoord, dropoffCoord)) {
                return true
            }
        }
        
        // Check text-based comparison
        val normalizedPickup = normalizeLocationText(pickupText)
        val normalizedDropoff = normalizeLocationText(dropoffText)
        
        if (normalizedPickup.isNotBlank() && normalizedDropoff.isNotBlank()) {
            if (normalizedPickup == normalizedDropoff) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Resolve countries for pickup and dropoff based on transfer type
     * Returns Pair<pickupCountry, dropoffCountry>
     */
    fun resolveCountries(
        pickupAddress: String,
        dropoffAddress: String,
        pickupAirport: String?,
        dropoffAirport: String?,
        transferType: String,
        pickupCountryFromLocation: String? = null,
        dropoffCountryFromLocation: String? = null,
        pickupAirportCountry: String? = null,
        dropoffAirportCountry: String? = null
    ): Pair<String?, String?> {
        val transferTypeKey = transferType.lowercase().replace("_", "").replace(" ", "")
        
        return when {
            // City To Airport: pickup from address, dropoff from airport
            transferTypeKey.contains("citytoairport") -> {
                Pair(
                    pickupCountryFromLocation ?: extractCountryFromAddress(pickupAddress),
                    dropoffAirportCountry
                )
            }
            // Airport To City: pickup from airport, dropoff from address
            transferTypeKey.contains("airporttocity") -> {
                Pair(
                    pickupAirportCountry,
                    dropoffCountryFromLocation ?: extractCountryFromAddress(dropoffAddress)
                )
            }
            // Airport To Airport: both from airports
            transferTypeKey.contains("airporttoairport") -> {
                Pair(pickupAirportCountry, dropoffAirportCountry)
            }
            // Airport To Cruise Port: pickup from airport, dropoff from address
            transferTypeKey.contains("airporttocruise") -> {
                Pair(
                    pickupAirportCountry,
                    dropoffCountryFromLocation ?: extractCountryFromAddress(dropoffAddress)
                )
            }
            // Cruise Port To Airport: pickup from address, dropoff from airport
            transferTypeKey.contains("cruisetoairport") -> {
                Pair(
                    pickupCountryFromLocation ?: extractCountryFromAddress(pickupAddress),
                    dropoffAirportCountry
                )
            }
            // Default: both from addresses
            else -> {
                Pair(
                    pickupCountryFromLocation ?: extractCountryFromAddress(pickupAddress),
                    dropoffCountryFromLocation ?: extractCountryFromAddress(dropoffAddress)
                )
            }
        }
    }
    
    /**
     * Check for location conflicts and return error message if any
     * Returns null if no conflicts, error message string if conflict exists
     */
    fun locationConflictReason(
        pickupAddress: String,
        dropoffAddress: String,
        pickupAirport: String?,
        dropoffAirport: String?,
        transferType: String,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        pickupCountryFromLocation: String? = null,
        dropoffCountryFromLocation: String? = null,
        pickupAirportCountry: String? = null,
        dropoffAirportCountry: String? = null
    ): String? {
        // Check if locations are the same
        val pickupText = when {
            transferType.lowercase().contains("airport") && !transferType.lowercase().contains("toairport") -> {
                pickupAirport ?: pickupAddress
            }
            else -> pickupAddress
        }
        
        val dropoffText = when {
            transferType.lowercase().contains("toairport") || transferType.lowercase().contains("to_airport") -> {
                dropoffAirport ?: dropoffAddress
            }
            else -> dropoffAddress
        }
        
        if (areLocationsSame(pickupText, dropoffText, pickupCoord, dropoffCoord)) {
            return "Pickup and drop-off cannot be the same. Please select different locations."
        }
        
        // Check country matching
        val countries = resolveCountries(
            pickupAddress = pickupAddress,
            dropoffAddress = dropoffAddress,
            pickupAirport = pickupAirport,
            dropoffAirport = dropoffAirport,
            transferType = transferType,
            pickupCountryFromLocation = pickupCountryFromLocation,
            dropoffCountryFromLocation = dropoffCountryFromLocation,
            pickupAirportCountry = pickupAirportCountry,
            dropoffAirportCountry = dropoffAirportCountry
        )
        
        val normalizedPickupCountry = normalizeCountry(countries.first)
        val normalizedDropoffCountry = normalizeCountry(countries.second)
        
        if (normalizedPickupCountry != null && normalizedDropoffCountry != null) {
            if (normalizedPickupCountry != normalizedDropoffCountry) {
                return "Pickup and drop-off must be in the same country. Please select valid locations."
            }
        }
        
        return null
    }
    
    /**
     * Validate extra stop against pickup and dropoff locations
     * Returns error message if validation fails, null if valid
     */
    fun validateExtraStop(
        stopAddress: String,
        stopCoord: Pair<Double, Double>?,
        stopCountry: String?,
        pickupAddress: String,
        dropoffAddress: String,
        pickupAirport: String?,
        dropoffAirport: String?,
        transferType: String,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        pickupCountryFromLocation: String? = null,
        dropoffCountryFromLocation: String? = null,
        pickupAirportCountry: String? = null,
        dropoffAirportCountry: String? = null,
        isPerformingSubmitValidation: Boolean = false
    ): String? {
        val trimmedAddress = stopAddress.trim()
        if (trimmedAddress.isBlank()) return null
        
        // Check if stop matches pickup or dropoff coordinates
        if (stopCoord != null) {
            if (pickupCoord != null && coordinatesApproximatelyEqual(stopCoord, pickupCoord, tolerance = 0.002)) {
                return "Extra stop cannot be the same as pickup location. Please select a different location."
            }
            if (dropoffCoord != null && coordinatesApproximatelyEqual(stopCoord, dropoffCoord, tolerance = 0.002)) {
                return "Extra stop cannot be the same as drop-off location. Please select a different location."
            }
        } else if (!isPerformingSubmitValidation) {
            // User is typing; only block at submit time
            return null
        } else {
            // At submit time, require selection from autocomplete
            return "Please select the extra stop from suggestions so we can validate it."
        }
        
        // Check text-based comparison
        val normalizedStopAddress = normalizeLocationText(trimmedAddress)
        val pickupText = when {
            transferType.lowercase().contains("airport") && !transferType.lowercase().contains("toairport") -> {
                pickupAirport ?: pickupAddress
            }
            else -> pickupAddress
        }
        val dropoffText = when {
            transferType.lowercase().contains("toairport") || transferType.lowercase().contains("to_airport") -> {
                dropoffAirport ?: dropoffAddress
            }
            else -> dropoffAddress
        }
        
        val normalizedPickupAddress = normalizeLocationText(pickupText)
        val normalizedDropoffAddress = normalizeLocationText(dropoffText)
        
        if (normalizedStopAddress.isNotBlank()) {
            if (normalizedPickupAddress.isNotBlank() && normalizedStopAddress == normalizedPickupAddress) {
                return "Extra stop cannot be the same as pickup location. Please select a different location."
            }
            if (normalizedDropoffAddress.isNotBlank() && normalizedStopAddress == normalizedDropoffAddress) {
                return "Extra stop cannot be the same as drop-off location. Please select a different location."
            }
        }
        
        // Country validation - strict: stop must match BOTH pickup and dropoff when known
        val stopCountryNormalized = normalizeCountry(stopCountry ?: extractCountryFromAddress(trimmedAddress))
        val countries = resolveCountries(
            pickupAddress = pickupAddress,
            dropoffAddress = dropoffAddress,
            pickupAirport = pickupAirport,
            dropoffAirport = dropoffAirport,
            transferType = transferType,
            pickupCountryFromLocation = pickupCountryFromLocation,
            dropoffCountryFromLocation = dropoffCountryFromLocation,
            pickupAirportCountry = pickupAirportCountry,
            dropoffAirportCountry = dropoffAirportCountry
        )
        
        val pickupCountryNormalized = normalizeCountry(countries.first)
        val dropoffCountryNormalized = normalizeCountry(countries.second)
        
        if (stopCountryNormalized == null) {
            return "Extra stop must include a valid country that matches pickup and drop-off."
        }
        
        if (pickupCountryNormalized != null && stopCountryNormalized != pickupCountryNormalized) {
            return "Extra stop must be in the same country as pickup and drop-off. Please select valid locations."
        }
        
        if (dropoffCountryNormalized != null && stopCountryNormalized != dropoffCountryNormalized) {
            return "Extra stop must be in the same country as pickup and drop-off. Please select valid locations."
        }
        
        if (pickupCountryNormalized == null && dropoffCountryNormalized == null) {
            return "Please enter pickup and drop-off before adding an extra stop so we can verify it is in the same country."
        }
        
        return null
    }
    
    /**
     * Validate that addresses are routable using Google Directions API
     * Returns error message if route cannot be calculated, null if valid
     */
    suspend fun validateRouteWithDirectionsApi(
        directionsApi: GoogleDirectionsApi,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        extraStopCoords: List<Pair<Double, Double>> = emptyList()
    ): String? {
        // If coordinates are missing, we can't validate with Directions API
        if (pickupCoord == null || dropoffCoord == null) {
            return null // Let other validation handle missing coordinates
        }
        
        return try {
            val origin = "${pickupCoord.first},${pickupCoord.second}"
            val destination = "${dropoffCoord.first},${dropoffCoord.second}"
            
            val waypoints = if (extraStopCoords.isNotEmpty()) {
                extraStopCoords.joinToString("|") { "${it.first},${it.second}" }
            } else {
                null
            }
            
            val response = if (waypoints != null) {
                directionsApi.directionsWithWaypoints(
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY
                )
            } else {
                directionsApi.directions(
                    origin = origin,
                    destination = destination,
                    apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY
                )
            }
            
            // Check if route was successfully calculated
            when (response.status?.uppercase()) {
                "OK" -> {
                    if (response.routes.isEmpty()) {
                        Timber.tag("LocationValidation").w("Directions API returned OK but no routes")
                        "Unable to calculate route between the selected locations. Please verify the addresses are correct."
                    } else {
                        null // Route is valid
                    }
                }
                "ZERO_RESULTS" -> {
                    "No route found between the selected locations. Please verify the addresses are correct and routable."
                }
                "NOT_FOUND" -> {
                    "One or more locations could not be found. Please select valid addresses from the suggestions."
                }
                "INVALID_REQUEST" -> {
                    "Invalid route request. Please verify the addresses are correct."
                }
                else -> {
                    val errorMsg = response.errorMessage ?: "Unknown error"
                    Timber.tag("LocationValidation").w("Directions API error: ${response.status} - $errorMsg")
                    "Unable to validate route: ${response.status}. Please verify the addresses are correct."
                }
            }
        } catch (e: Exception) {
            Timber.tag("LocationValidation").e(e, "Error validating route with Directions API")
            // Don't block user if API call fails - this is a validation enhancement
            null
        }
    }
    
    /**
     * Validate pickup and dropoff addresses using Directions API
     * This is a convenience wrapper that combines coordinate validation with route validation
     */
    suspend fun validateAddressesWithDirectionsApi(
        directionsApi: GoogleDirectionsApi,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        extraStopCoords: List<Pair<Double, Double>> = emptyList()
    ): String? {
        // First check if coordinates are present
        if (pickupCoord == null) {
            return "Pickup location must be selected from suggestions to validate the route."
        }
        if (dropoffCoord == null) {
            return "Drop-off location must be selected from suggestions to validate the route."
        }
        
        // Then validate route with Directions API
        return validateRouteWithDirectionsApi(
            directionsApi = directionsApi,
            pickupCoord = pickupCoord,
            dropoffCoord = dropoffCoord,
            extraStopCoords = extraStopCoords
        )
    }
}

