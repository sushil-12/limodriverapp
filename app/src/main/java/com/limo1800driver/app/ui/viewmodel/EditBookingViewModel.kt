package com.limo1800driver.app.ui.viewmodel

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.AdminReservationRatesData
import com.limo1800driver.app.data.model.dashboard.AdminReservationRateArray
import com.limo1800driver.app.data.model.dashboard.AdminReservationRateItem
import com.limo1800driver.app.data.model.dashboard.MobileDataAirline
import com.limo1800driver.app.data.model.dashboard.MobileDataAirport
import com.limo1800driver.app.data.model.dashboard.EditReservationExtraStopRequest
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import com.limo1800driver.app.rideinprogress.GoogleDirectionsApi
import com.limo1800driver.app.data.network.NetworkConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.math.*

// --- Added Data Class for Shares ---
data class ReservationShareData(
    val baseRate: Double,
    val grandTotal: Double,
    val stripeFee: Double,
    val adminShare: Double,
    val deductedAdminShare: Double,
    val affiliateShare: Double,
    val farmoutShare: Double? = null,
    val travelAgentShare: Double? = null
)

data class EditBookingUiState(
    val isLoading: Boolean = false,
    val preview: AdminBookingPreviewData? = null,
    val rates: AdminReservationRatesData? = null,
    val airlines: List<MobileDataAirline> = emptyList(),
    val airports: List<MobileDataAirport> = emptyList(),
    val isLoadingAirlines: Boolean = false,
    val isLoadingAirports: Boolean = false,
    val airlinesError: String? = null,
    val airportsError: String? = null,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null, // Critical errors that should replace the screen
    val validationErrors: List<String> = emptyList(), // Validation errors that should show inline on fields
    // Location validation state
    val locationValidationError: String? = null,
    val showLocationErrorBanner: Boolean = false,
    // Rates update state
    val isRatesLoading: Boolean = false,
    val ratesErrorMessage: String? = null,
    val latestRatesUpdate: Map<String, String>? = null,
    val isManuallyUpdatingRates: Boolean = false,
    // State tracking for rates recalculation
    val lastPickupAddress: String? = null,
    val lastDropoffAddress: String? = null,
    val lastPickupCoordinates: Pair<Double, Double>? = null,
    val lastDropoffCoordinates: Pair<Double, Double>? = null,
    val lastServiceType: String? = null,
    val lastNumberOfVehicles: String? = null,
    val lastNumberOfHours: String? = null,
    // Extra stops tracking
    val lastExtraStopCoordinates: List<Pair<Double, Double>> = emptyList(),
    val lastExtraStopAddresses: List<String> = emptyList(),
    val extraStopsChangeTrigger: Long = 0, // Increments when extra stops are added/removed to force recalculation
    val isInitialLoad: Boolean = true,
    // Distance/time tracking
    val originalApiDistance: String = "",
    val originalApiDuration: String = "",
    val hasLocationChanged: Boolean = false,
    // Calculated distance/duration from Directions API
    val calculatedDistance: Int? = null,
    val calculatedDuration: Int? = null,
    // --- New State Fields ---
    val autoBookingInstructions: String = "",
    val autoMeetGreetChoice: Int = 2, // Default to Airport (2)
    val autoMeetGreetName: String = "Driver - Airport - Text/call after plane lands with curbside meet location",
    val calculatedShares: ReservationShareData? = null
)

@HiltViewModel
class EditBookingViewModel @Inject constructor(
    private val repository: DriverDashboardRepository,
    private val directionsApi: GoogleDirectionsApi,
    @ApplicationContext private val context: Context // Injected for Geocoder
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditBookingUiState())
    val uiState: StateFlow<EditBookingUiState> = _uiState.asStateFlow()

    // Geocoder for "In-Town" vs "Out-Town" logic
    private val geocoder = Geocoder(context, Locale.getDefault())

    fun load(bookingId: Int) {
        viewModelScope.launch {
            Timber.tag("BookingFlow").d("EditBookingViewModel.load() - Loading booking ID: $bookingId")
            Timber.tag("BookingFlow").d("Using API: api/affiliate/get-reservation?booking_id=$bookingId")
            
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null,
                isInitialLoad = true
            )

            val previewResult = repository.getReservation(bookingId)
            val ratesResult = repository.getReservationRates(bookingId)

            var preview: AdminBookingPreviewData? = null
            var rates: AdminReservationRatesData? = null
            var error: String? = null

            previewResult.onSuccess { resp ->
                if (resp.success) {
                    // get-reservation API returns ReservationData which is now a type alias for AdminBookingPreviewData
                    preview = resp.data
                    Timber.tag("BookingFlow").d("EditBookingViewModel - get-reservation API success")
                    Timber.tag("BookingFlow").d("Reservation ID: ${preview?.reservationId}, Transfer Type: ${preview?.transferType}")
                } else {
                    error = resp.message
                    Timber.tag("BookingFlow").e("EditBookingViewModel - get-reservation API error: ${resp.message}")
                }
            }.onFailure { e ->
                error = e.message
                Timber.tag("BookingFlow").e(e, "EditBookingViewModel - get-reservation API failure")
            }

            ratesResult.onSuccess { resp ->
                if (resp.success) {
                    rates = resp.data
                    Timber.tag("BookingFlow").d("EditBookingViewModel - get-reservation-rates API success")
                } else {
                    error = error ?: resp.message
                    Timber.tag("BookingFlow").e("EditBookingViewModel - get-reservation-rates API error: ${resp.message}")
                }
            }.onFailure { e ->
                error = error ?: e.message
                Timber.tag("BookingFlow").e(e, "EditBookingViewModel - get-reservation-rates API failure")
            }

            // Store original API distance and duration
            val originalDistance = preview?.distance ?: ""
            val originalDuration = preview?.duration ?: ""

            val originalPickupCoord = preview?.pickupLatitude?.toDoubleOrNull()?.let { lat ->
                preview.pickupLongitude?.toDoubleOrNull()?.let { lon -> Pair(lat, lon) }
            }
            val originalDropoffCoord = preview?.dropoffLatitude?.toDoubleOrNull()?.let { lat ->
                preview.dropoffLongitude?.toDoubleOrNull()?.let { lon -> Pair(lat, lon) }
            }

            Timber.tag("BookingFlow").d("=== Load Complete ===")
            Timber.tag("BookingFlow").d("preview loaded: ${preview != null}, rates loaded: ${rates != null}")
            Timber.tag("BookingFlow").d("error: $error")
            Timber.tag("BookingFlow").d("originalDistance: $originalDistance, originalDuration: $originalDuration")

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                preview = preview,
                rates = rates,
                error = error,
                originalApiDistance = originalDistance,
                originalApiDuration = originalDuration,
                lastPickupCoordinates = originalPickupCoord,
                lastDropoffCoordinates = originalDropoffCoord,
                hasLocationChanged = false
            )

            // Initialize defaults based on loaded data
            preview?.transferType?.let {
                Timber.tag("BookingFlow").d("Initializing defaults for transferType: $it")
                updateTransferTypeDefaults(it)
            }
        }
    }

    // --- New: Logic to update Instructions & Meet/Greet based on Transfer Type ---
    fun updateTransferTypeDefaults(transferType: String) {
        val lowerType = transferType.lowercase(Locale.ROOT)
        var instructions = "1. Driver - Text on location. Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route"
        var meetGreetId = 2 // Default Airport
        var meetGreetName = "Driver - Airport - Text/call after plane lands with curbside meet location"

        if (lowerType.contains("city_")) {
            meetGreetId = 1
            meetGreetName = "Driver - Text/call when on location"
        }

        if (lowerType.contains("cruise")) {
            instructions = if (lowerType.startsWith("cruise_")) {
                "1. Pax - Text driver when docked. 2. Driver - Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route. Text pax with pickup instructions when ship has arrived."
            } else {
                instructions // Keep default
            }
        } else if (lowerType.contains("airport_")) {
            instructions = "1. Pax - Text driver when landing. 2. Driver - Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route. Text pax with pickup instructions when plane has arrived."
        } else if (lowerType.contains("city_")) {
            // City to City default
        }

        _uiState.value = _uiState.value.copy(
            autoBookingInstructions = instructions,
            autoMeetGreetChoice = meetGreetId,
            autoMeetGreetName = meetGreetName
        )
    }

    fun setLocationValidationError(error: String?) {
        _uiState.value = _uiState.value.copy(
            locationValidationError = error,
            showLocationErrorBanner = error != null
        )
    }
    
    /**
     * Validate addresses using Google Directions API
     * Returns error message if route cannot be calculated, null if valid
     */
    suspend fun validateAddressesWithDirectionsApi(
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        extraStopCoords: List<Pair<Double, Double>> = emptyList()
    ): String? {
        return com.limo1800driver.app.ui.screens.booking.LocationValidationHelper.validateAddressesWithDirectionsApi(
            directionsApi = directionsApi,
            pickupCoord = pickupCoord,
            dropoffCoord = dropoffCoord,
            extraStopCoords = extraStopCoords
        )
    }

    fun hideLocationErrorBanner() {
        _uiState.value = _uiState.value.copy(showLocationErrorBanner = false)
    }

    fun markInitialLoadComplete() {
        _uiState.value = _uiState.value.copy(isInitialLoad = false)
    }

    fun updateLastKnownValues(
        pickupAddress: String?,
        dropoffAddress: String?,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        serviceType: String?,
        numberOfVehicles: String?,
        numberOfHours: String?,
        extraStopCoordinates: List<Pair<Double, Double>> = emptyList(),
        extraStopAddresses: List<String> = emptyList()
    ) {
        _uiState.value = _uiState.value.copy(
            lastPickupAddress = pickupAddress,
            lastDropoffAddress = dropoffAddress,
            lastPickupCoordinates = pickupCoord,
            lastDropoffCoordinates = dropoffCoord,
            lastServiceType = serviceType,
            lastNumberOfVehicles = numberOfVehicles,
            lastNumberOfHours = numberOfHours,
            lastExtraStopCoordinates = extraStopCoordinates,
            lastExtraStopAddresses = extraStopAddresses
        )
    }

    fun resetLastKnownExtraStops() {
        _uiState.value = _uiState.value.copy(
            lastExtraStopCoordinates = emptyList(),
            lastExtraStopAddresses = emptyList()
        )
        Timber.tag("BookingFlow").d("Reset last known extra stops to force change detection")
    }

    fun triggerExtraStopsChange() {
        _uiState.value = _uiState.value.copy(
            extraStopsChangeTrigger = System.currentTimeMillis()
        )
        Timber.tag("BookingFlow").d("Triggered extra stops change - timestamp: ${_uiState.value.extraStopsChangeTrigger}")
    }

    fun markLocationChanged(hasChanged: Boolean) {
        _uiState.value = _uiState.value.copy(hasLocationChanged = hasChanged)
    }

    fun setManuallyUpdatingRates(isManual: Boolean) {
        _uiState.value = _uiState.value.copy(isManuallyUpdatingRates = isManual)
    }

    fun resetState() {
        _uiState.value = EditBookingUiState()
    }

    // --- Validation Logic ---
    private fun validateBookingRequirements(
        userInput: EditReservationUserInput
    ): Boolean {
        Timber.tag("BookingFlow").d("=== validateBookingRequirements START ===")
        Timber.tag("BookingFlow").d("transferType: ${userInput.transferType}")
        Timber.tag("BookingFlow").d("pickupAirlineName: '${userInput.pickupAirlineName}', pickupFlight: '${userInput.pickupFlight}'")
        Timber.tag("BookingFlow").d("dropoffAirlineName: '${userInput.dropoffAirlineName}'")
        Timber.tag("BookingFlow").d("cruiseName: '${userInput.cruiseName}', cruisePort: '${userInput.cruisePort}'")
        
        val transferType = userInput.transferType?.lowercase() ?: ""
        Timber.tag("BookingFlow").d("Normalized transferType: '$transferType'")

        val errors = mutableListOf<String>()

        // 1. Airport Validation
        if (transferType.contains("airport")) {
            val isPickupAirport = transferType.startsWith("airport_")
            val isDropoffAirport = transferType.endsWith("_airport")
            Timber.tag("BookingFlow").d("Airport transfer detected - isPickupAirport: $isPickupAirport, isDropoffAirport: $isDropoffAirport")

            if (isPickupAirport) {
                val hasAirline = !userInput.pickupAirlineName.isNullOrBlank()
                val hasFlight = !userInput.pickupFlight.isNullOrBlank()
                Timber.tag("BookingFlow").d("Pickup airport validation - hasAirline: $hasAirline, hasFlight: $hasFlight")
                
                if (!hasAirline) {
                    errors.add("pickup_airline")
                    Timber.tag("BookingFlow").e("❌ Validation FAILED: pickup_airline is required")
                }
                if (!hasFlight) {
                    errors.add("pickup_flight_number")
                    Timber.tag("BookingFlow").e("❌ Validation FAILED: pickup_flight_number is required")
                }
            }
            if (isDropoffAirport) {
                val hasDropoffAirline = !userInput.dropoffAirlineName.isNullOrBlank()
                Timber.tag("BookingFlow").d("Dropoff airport validation - hasDropoffAirline: $hasDropoffAirline")
                
                if (!hasDropoffAirline) {
                    errors.add("dropoff_airline")
                    Timber.tag("BookingFlow").e("❌ Validation FAILED: dropoff_airline is required")
                }
            }
        }

        // 2. Cruise Validation
        if (transferType.contains("cruise")) {
            val hasCruiseName = !userInput.cruiseName.isNullOrBlank()
            val hasCruisePort = !userInput.cruisePort.isNullOrBlank()
            Timber.tag("BookingFlow").d("Cruise validation - hasCruiseName: $hasCruiseName, hasCruisePort: $hasCruisePort")
            
            if (!hasCruiseName) {
                errors.add("cruise_ship_name")
                Timber.tag("BookingFlow").e("❌ Validation FAILED: cruise_ship_name is required")
            }
            if (!hasCruisePort) {
                errors.add("cruise_port")
                Timber.tag("BookingFlow").e("❌ Validation FAILED: cruise_port is required")
            }
        }

        // 3. Loose Customer Validation (if applicable logic exists in user input)
        // Add here if userInput has loose customer fields

        if (errors.isNotEmpty()) {
            Timber.tag("BookingFlow").e("❌ Validation FAILED with ${errors.size} errors: $errors")
            _uiState.value = _uiState.value.copy(
                validationErrors = errors,
                error = null // Don't replace the whole screen
            )
            return false
        }

        Timber.tag("BookingFlow").d("✅ Validation PASSED")
        _uiState.value = _uiState.value.copy(validationErrors = emptyList()) // Clear any previous validation errors
        return true
    }

    // --- Share Calculation Logic (Ported from TS) ---
    private fun calculateShares(
        ratesData: AdminReservationRatesData,
        numberOfVehicles: Int,
        numberOfHours: Int,
        serviceType: String,
        accountType: String?,
        isFarmout: Boolean,
        createdByAdmin: Boolean
    ): ReservationShareData {
        var baseRate = 0.0

        // Calculate Base Rate
        val rateArray = ratesData.rateArray
        val baseItem = rateArray?.allInclusiveRates?.get("Base_Rate")

        if (serviceType == "charter_tour" && ratesData.minRateInvolved != true) {
            baseRate += (baseItem?.baserate?.toDouble() ?: 0.0) * numberOfHours
        } else {
            baseRate += (baseItem?.baserate?.toDouble() ?: 0.0)
        }

        // Add ELH, Stops, Wait
        listOf("ELH_Charges", "Stops", "Wait").forEach { key ->
            baseRate += rateArray?.allInclusiveRates?.get(key)?.baserate?.toDouble() ?: 0.0
        }

        // Add Amenities
        rateArray?.amenities?.values?.forEach { item ->
            baseRate += item.baserate?.toDouble() ?: 0.0
        }

        // Multiply by vehicles
        if (numberOfVehicles > 0) {
            baseRate *= numberOfVehicles
        }

        val grandTotal = ratesData.grandTotal ?: 0.0
        val stripeFee = (grandTotal * 0.05) + 0.30

        // Admin Share Calculation
        var adminSharePercent = 25.0
        var travelAgentShare: Double? = null
        var farmoutShare: Double? = null
        var affiliateShare = 0.0

        // Check Logic from TS (Line 1205)
        if (accountType == "travel_planner" && !createdByAdmin) {
            adminSharePercent = 15.0
            val adminShare = (baseRate * adminSharePercent) / 100
            val deducted = adminShare - stripeFee
            travelAgentShare = baseRate * 0.10

            affiliateShare = grandTotal - (baseRate * 0.25) // This seems consistent with TS logic

            return ReservationShareData(baseRate, grandTotal, stripeFee, adminShare, deducted, affiliateShare, null, travelAgentShare)
        }
        else if (isFarmout) {
            adminSharePercent = 15.0
            val adminShare = (baseRate * adminSharePercent) / 100
            val deducted = adminShare - stripeFee
            farmoutShare = baseRate * 0.10
            affiliateShare = grandTotal - (baseRate * 0.25)

            return ReservationShareData(baseRate, grandTotal, stripeFee, adminShare, deducted, affiliateShare, farmoutShare, null)
        }

        // Default Case
        val extraGratuity = rateArray?.misc?.get("Extra_Gratuity")?.amount?.toDouble() ?: 0.0
        var adminShare = (baseRate * adminSharePercent) / 100
        adminShare += (extraGratuity * 0.25)

        val deductedAdminShare = adminShare - stripeFee
        affiliateShare = grandTotal - (baseRate * 0.25)

        return ReservationShareData(baseRate, grandTotal, stripeFee, adminShare, deductedAdminShare, affiliateShare)
    }

    // --- Distance Calculation with Google Directions API ---
    suspend fun calculateDistanceWithDirections(
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        extraStopCoordinates: List<Pair<Double, Double>>
    ): Pair<Int, Int>? {
        if (pickupCoord == null || dropoffCoord == null) return null
        
        return try {
            val origin = "${pickupCoord.first},${pickupCoord.second}"
            val destination = "${dropoffCoord.first},${dropoffCoord.second}"
            val waypoints = if (extraStopCoordinates.isNotEmpty()) {
                extraStopCoordinates.joinToString("|") { "${it.first},${it.second}" }
            } else null
            
            val response = if (waypoints != null) {
                directionsApi.directionsWithWaypoints(
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    mode = "driving",
                    apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY
                )
            } else {
                directionsApi.directions(
                    origin = origin,
                    destination = destination,
                    mode = "driving",
                    apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY
                )
            }
            
            // Check response status first
            val status = response.status?.uppercase() ?: "UNKNOWN"
            if (status != "OK") {
                Timber.tag("EditBookingViewModel").w("Directions API returned status: $status")
                return null
            }
            
            if (response.routes.isNotEmpty()) {
                val route = response.routes.first()
                var totalDistance = 0L
                var totalDuration = 0L
                
                route.legs.forEach { leg ->
                    totalDistance += leg.distance?.value ?: 0L
                    totalDuration += leg.duration?.value ?: 0L
                }
                
                Pair(totalDistance.toInt(), totalDuration.toInt())
            } else {
                Timber.tag("EditBookingViewModel").w("Directions API returned OK but no routes")
                null
            }
        } catch (e: Exception) {
            Timber.tag("EditBookingViewModel").e(e, "Failed to calculate distance with Directions API")
            null
        }
    }
    
    fun calculateTotalDistance(
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        extraStopCoordinates: List<Pair<Double, Double>>
    ): Int {
        if (pickupCoord == null || dropoffCoord == null) return 0
        
        // Use calculated distance if available, otherwise fallback to Haversine
        _uiState.value.calculatedDistance?.let { return it }
        
        // Fallback to Haversine calculation
        val earthRadius = 6371000.0
        var totalDistance = 0.0
        var previousCoord: Pair<Double, Double> = pickupCoord
        for (stopCoord in extraStopCoordinates) {
            totalDistance += calculateHaversineDistance(previousCoord, stopCoord, earthRadius)
            previousCoord = stopCoord
        }
        totalDistance += calculateHaversineDistance(previousCoord, dropoffCoord, earthRadius)
        val straightLineDistanceKm = totalDistance / 1000.0
        val drivingFactor = calculateDrivingFactor(straightLineDistanceKm, pickupCoord, dropoffCoord)
        return (totalDistance * drivingFactor).toInt()
    }

    private fun calculateHaversineDistance(coord1: Pair<Double, Double>, coord2: Pair<Double, Double>, earthRadius: Double): Double {
        val lat1Rad = Math.toRadians(coord1.first)
        val lat2Rad = Math.toRadians(coord2.first)
        val deltaLatRad = Math.toRadians(coord2.first - coord1.first)
        val deltaLonRad = Math.toRadians(coord2.second - coord1.second)
        val a = sin(deltaLatRad / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun calculateDrivingFactor(straightLineDistanceKm: Double, pickupCoord: Pair<Double, Double>, dropoffCoord: Pair<Double, Double>): Double {
        return when {
            straightLineDistanceKm < 5 -> 1.3
            straightLineDistanceKm < 50 -> 1.4
            else -> 1.5
        }
    }

    fun calculateTotalTime(distanceInMeters: Int): Int {
        // Use calculated duration if available
        _uiState.value.calculatedDuration?.let { return it }
        
        // Fallback to estimated time based on distance
        val distanceInMiles = distanceInMeters / 1609.0
        val averageSpeedMph = 45.0
        val timeInHours = distanceInMiles / averageSpeedMph
        return (timeInHours * 3600).toInt()
    }
    
    fun updateCalculatedDistanceAndDuration(distance: Int?, duration: Int?) {
        // Preserve hasLocationChanged if already true, or set it if we have calculated values and original API data
        val shouldMarkChanged = _uiState.value.hasLocationChanged || 
            (distance != null && _uiState.value.originalApiDistance.isNotBlank())
        
        _uiState.value = _uiState.value.copy(
            calculatedDistance = distance,
            calculatedDuration = duration,
            hasLocationChanged = shouldMarkChanged
        )
    }

    fun formatDistance(distanceInMeters: Int): String {
        val distanceInMiles = distanceInMeters / 1609.0
        return String.format(Locale.getDefault(), "%.2f miles", distanceInMiles)
    }

    fun formatDuration(timeInSeconds: Int): String = convertToMinutes(timeInSeconds)

    private fun convertToMinutes(value: Int): String {
        val days = value / (24 * 60 * 60)
        val remainingSeconds = value % (24 * 60 * 60)
        val hours = remainingSeconds / (60 * 60)
        val remainingMinutes = (remainingSeconds % (60 * 60)) / 60
        val result = StringBuilder()
        if (days > 0) result.append("$days days, ")
        if (hours > 0 || (days == 0 && hours == 0)) result.append("$hours hours, ")
        result.append("$remainingMinutes minutes")
        return result.toString()
    }

    fun getDisplayDistance(pickupCoord: Pair<Double, Double>?, dropoffCoord: Pair<Double, Double>?, extraStopCoordinates: List<Pair<Double, Double>>): String {
        // If location hasn't changed, use original API distance
        if (!_uiState.value.hasLocationChanged) {
            if (_uiState.value.originalApiDistance.isNotBlank()) {
                return formatApiDistance(_uiState.value.originalApiDistance)
            }
        }
        
        // Location changed - use calculated distance if available
        _uiState.value.calculatedDistance?.let {
            return formatDistance(it)
        }
        
        // Fallback: calculate distance if coordinates are available
        if (pickupCoord != null && dropoffCoord != null) {
            return formatDistance(calculateTotalDistance(pickupCoord, dropoffCoord, extraStopCoordinates))
        }
        
        // Last resort: use original API distance even if blank
        return if (_uiState.value.originalApiDistance.isNotBlank()) {
            formatApiDistance(_uiState.value.originalApiDistance)
        } else {
            "0.00 miles"
        }
    }

    fun getDisplayDuration(pickupCoord: Pair<Double, Double>?, dropoffCoord: Pair<Double, Double>?, extraStopCoordinates: List<Pair<Double, Double>>): String {
        // If location hasn't changed, use original API duration
        if (!_uiState.value.hasLocationChanged) {
            if (_uiState.value.originalApiDuration.isNotBlank()) {
                return formatApiDuration(_uiState.value.originalApiDuration)
            }
        }
        
        // Location changed - use calculated duration if available
        _uiState.value.calculatedDuration?.let {
            return formatDuration(it)
        }
        
        // Fallback: calculate duration if coordinates are available
        if (pickupCoord != null && dropoffCoord != null) {
            val distance = calculateTotalDistance(pickupCoord, dropoffCoord, extraStopCoordinates)
            return formatDuration(calculateTotalTime(distance))
        }
        
        // Last resort: use original API duration even if blank
        return if (_uiState.value.originalApiDuration.isNotBlank()) {
            formatApiDuration(_uiState.value.originalApiDuration)
        } else {
            "0 hours, 0 minutes"
        }
    }

    private fun formatApiDistance(distance: String): String {
        val clean = distance.replace(" miles", "", ignoreCase = true).replace(" mile", "", ignoreCase = true).replace(" mi", "", ignoreCase = true).replace(" ", "").trim()
        val value = clean.toDoubleOrNull() ?: return distance
        return if (value > 1000) formatDistance(value.toInt()) else String.format(Locale.getDefault(), "%.2f miles", value)
    }

    private fun formatApiDuration(duration: String): String {
        val seconds = duration.toIntOrNull()
        return if (seconds != null) formatDuration(seconds) else duration
    }

    private fun convertTimeToAPIFormat(timeString: String): String {
        if (timeString.isBlank()) return "12:00:00"
        val formats = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm", "HH:mm:ss")
        for (formatStr in formats) {
            try {
                val format = java.text.SimpleDateFormat(formatStr, Locale.getDefault())
                val date = format.parse(timeString)
                if (date != null) {
                    val outputFormat = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    return outputFormat.format(date)
                }
            } catch (e: Exception) { }
        }
        if (timeString.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) return timeString
        return "12:00:00"
    }

    fun loadMobileDataIfNeeded() {
        viewModelScope.launch {
            if (_uiState.value.airports.isNotEmpty() && _uiState.value.airlines.isNotEmpty()) return@launch
            if (_uiState.value.airlines.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoadingAirlines = true, airlinesError = null)
                repository.getMobileDataAirlines().onSuccess { resp ->
                    _uiState.value = if (resp.success && resp.data != null) _uiState.value.copy(airlines = resp.data.airlinesData, isLoadingAirlines = false)
                    else _uiState.value.copy(isLoadingAirlines = false, airlinesError = resp.message)
                }.onFailure { e -> _uiState.value = _uiState.value.copy(isLoadingAirlines = false, airlinesError = e.message) }
            }
            if (_uiState.value.airports.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoadingAirports = true, airportsError = null)
                repository.getMobileDataAirports().onSuccess { resp ->
                    _uiState.value = if (resp.success && resp.data != null) _uiState.value.copy(airports = resp.data.airportsData, isLoadingAirports = false)
                    else _uiState.value.copy(isLoadingAirports = false, airportsError = resp.message)
                }.onFailure { e -> _uiState.value = _uiState.value.copy(isLoadingAirports = false, airportsError = e.message) }
            }
        }
    }

    /**
     * Updates AdminReservationRateArray baserate values from dynamicRates (user edits)
     * and recalculates sub_total and grand_total
     * This ensures the payload reflects the current UI state
     */
    private fun updateRateArrayFromDynamicRates(
        ratesData: AdminReservationRatesData,
        dynamicRates: Map<String, Any>,
        preview: AdminBookingPreviewData,
        serviceType: String?,
        numberOfHours: Int?,
        numberOfVehicles: Int?
    ): AdminReservationRatesData {
        Timber.tag("BookingFlow").d("=== updateRateArrayFromDynamicRates START ===")
        Timber.tag("BookingFlow").d("dynamicRates keys: ${dynamicRates.keys.joinToString()}")
        dynamicRates.forEach { (key, value) ->
            Timber.tag("BookingFlow").d("  dynamicRates[$key] = $value (${value.javaClass.simpleName})")
        }

        fun updateMap(
            map: Map<String, AdminReservationRateItem>,
            sectionName: String
        ): Map<String, AdminReservationRateItem> {
            return map.mapValues { (key, item) ->
                val originalBaserate = item.baserate
                val updatedBaserate = when (val value = dynamicRates[key]) {
                    is String -> value.toDoubleOrNull() ?: item.baserate
                    is Number -> value.toDouble()
                    else -> item.baserate
                }
                if (originalBaserate != updatedBaserate) {
                    Timber.tag("BookingFlow").d("  [$sectionName] $key: baserate ${originalBaserate} -> ${updatedBaserate}")
                }
                item.copy(baserate = updatedBaserate)
            }
        }

        val updatedRateArray = AdminReservationRateArray(
            allInclusiveRates = updateMap(ratesData.rateArray.allInclusiveRates, "allInclusiveRates"),
            taxes = updateMap(ratesData.rateArray.taxes, "taxes"),
            amenities = updateMap(ratesData.rateArray.amenities, "amenities"),
            misc = updateMap(ratesData.rateArray.misc, "misc")
        )

        // Log key rate values after update
        updatedRateArray.allInclusiveRates["Base_Rate"]?.let {
            Timber.tag("BookingFlow").d("Final Base_Rate: baserate=${it.baserate}, amount=${it.amount}")
        }
        updatedRateArray.allInclusiveRates["Stops"]?.let {
            Timber.tag("BookingFlow").d("Final Stops: baserate=${it.baserate}, amount=${it.amount}")
        }

        // Recalculate totals using RateCalculator
        val dynamicRatesString = dynamicRates.mapValues { (_, v) -> 
            when (v) {
                is String -> v
                is Number -> v.toString()
                else -> v.toString()
            }
        }
        val taxIsPercent = updatedRateArray.taxes.mapValues { (_, item) -> 
            item.type == "percent"
        }
        
        val totals = com.limo1800driver.app.ui.utils.RateCalculator.calculate(
            rateArray = updatedRateArray,
            dynamicRates = dynamicRatesString,
            taxIsPercent = taxIsPercent,
            serviceType = serviceType,
            numberOfHours = numberOfHours ?: 0,
            numberOfVehicles = numberOfVehicles ?: 1,
            accountType = preview.accountType,
            createdBy = preview.createdBy,
            reservationType = preview.reservationType
        )
        
        Timber.tag("BookingFlow").d("Recalculated totals: subTotal=${totals.subTotal}, grandTotal=${totals.grandTotal}")
        Timber.tag("BookingFlow").d("Original totals: subTotal=${ratesData.subTotal}, grandTotal=${ratesData.grandTotal}")
        
        Timber.tag("BookingFlow").d("=== updateRateArrayFromDynamicRates END ===")
        return ratesData.copy(
            rateArray = updatedRateArray,
            subTotal = totals.subTotal,
            grandTotal = totals.grandTotal
        )
    }

    // --- Save Edit Reservation (Updated) ---
    fun saveEditReservation(
        bookingId: Int,
        pickupAddress: String?,
        dropoffAddress: String?,
        pickupDate: String?,
        pickupTime: String?,
        vehicleId: Int?,
        rates: Map<String, Any>?,
        serviceType: String? = null,
        transferType: String? = null,
        numberOfHours: Int? = null,
        numberOfVehicles: Int? = null,
        meetGreetChoiceName: String? = null,
        bookingInstructions: String? = null,
        passengerName: String? = null,
        passengerEmail: String? = null,
        passengerCellIsd: String? = null,
        passengerCell: String? = null,
        pickupAirportName: String? = null,
        pickupAirlineName: String? = null,
        pickupFlight: String? = null,
        originAirportCity: String? = null,
        cruisePort: String? = null,
        cruiseName: String? = null,
        cruiseTime: String? = null,
        dropoffAirportName: String? = null,
        dropoffAirlineName: String? = null,
        dropoffFlight: String? = null,
        departingAirportCity: String? = null,
        extraStops: List<EditReservationExtraStopRequest>? = null,
        pickupLatitude: String? = null,
        pickupLongitude: String? = null,
        dropoffLatitude: String? = null,
        dropoffLongitude: String? = null,
        pickupAirportId: Int? = null,
        dropoffAirportId: Int? = null,
        pickupAirlineId: Int? = null,
        dropoffAirlineId: Int? = null
    ) {
        Timber.tag("BookingFlow").d("=== saveEditReservation START ===")
        Timber.tag("BookingFlow").d("bookingId: $bookingId")
        Timber.tag("BookingFlow").d("pickupAddress: '$pickupAddress'")
        Timber.tag("BookingFlow").d("dropoffAddress: '$dropoffAddress'")
        Timber.tag("BookingFlow").d("pickupAirportName: '$pickupAirportName'")
        Timber.tag("BookingFlow").d("pickupAirlineName: '$pickupAirlineName'")
        Timber.tag("BookingFlow").d("pickupFlight: '$pickupFlight'")
        Timber.tag("BookingFlow").d("dropoffAirportName: '$dropoffAirportName'")
        Timber.tag("BookingFlow").d("dropoffAirlineName: '$dropoffAirlineName'")
        Timber.tag("BookingFlow").d("transferType: '$transferType'")
        Timber.tag("BookingFlow").d("serviceType: '$serviceType'")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true, 
                error = null, 
                validationErrors = emptyList(),
                successMessage = null
            )

            val preview = _uiState.value.preview
            var ratesData = _uiState.value.rates

            Timber.tag("BookingFlow").d("preview: ${preview != null}, ratesData: ${ratesData != null}")

            if (preview == null || ratesData == null) {
                val errorMsg = "Booking data not loaded"
                Timber.tag("BookingFlow").e("❌ $errorMsg")
                _uiState.value = _uiState.value.copy(isSaving = false, error = errorMsg)
                return@launch
            }

            // Update rateArray from dynamicRates (user edits) before building payload
            if (rates != null && rates.isNotEmpty()) {
                Timber.tag("BookingFlow").d("=== UI dynamicRates received in saveEditReservation ===")
                Timber.tag("BookingFlow").d("dynamicRates count: ${rates.size}")
                rates.forEach { (key, value) ->
                    Timber.tag("BookingFlow").d("  UI[$key] = $value")
                }
                ratesData = updateRateArrayFromDynamicRates(
                    ratesData = ratesData,
                    dynamicRates = rates,
                    preview = preview,
                    serviceType = serviceType,
                    numberOfHours = numberOfHours,
                    numberOfVehicles = numberOfVehicles
                )
            } else {
                Timber.tag("BookingFlow").w("⚠️ No dynamicRates provided - using original ratesData")
            }

            // Build User Input
            val userInput = EditReservationUserInput(
                pickupAddress = pickupAddress,
                dropoffAddress = dropoffAddress,
                pickupDate = pickupDate,
                pickupTime = pickupTime,
                pickupLatitude = pickupLatitude,
                pickupLongitude = pickupLongitude,
                dropoffLatitude = dropoffLatitude,
                dropoffLongitude = dropoffLongitude,
                vehicleId = vehicleId,
                serviceType = serviceType,
                transferType = transferType,
                numberOfHours = numberOfHours,
                numberOfVehicles = numberOfVehicles,
                meetGreetChoiceName = meetGreetChoiceName,
                bookingInstructions = bookingInstructions,
                passengerName = passengerName,
                passengerEmail = passengerEmail,
                passengerCellIsd = passengerCellIsd,
                passengerCell = passengerCell,
                pickupAirportId = pickupAirportId,
                pickupAirportName = pickupAirportName,
                pickupAirlineId = pickupAirlineId,
                pickupAirlineName = pickupAirlineName,
                pickupFlight = pickupFlight,
                originAirportCity = originAirportCity,
                cruisePort = cruisePort,
                cruiseName = cruiseName,
                cruiseTime = cruiseTime,
                dropoffAirportId = dropoffAirportId,
                dropoffAirportName = dropoffAirportName,
                dropoffAirlineId = dropoffAirlineId,
                dropoffAirlineName = dropoffAirlineName,
                dropoffFlight = dropoffFlight,
                departingAirportCity = departingAirportCity,
                extraStops = emptyList(), // Processed below
                hasLocationChanged = _uiState.value.hasLocationChanged
            )

            // 1. Validate Mandatory Fields
            Timber.tag("BookingFlow").d("Starting validation...")
            if (!validateBookingRequirements(userInput)) {
                Timber.tag("BookingFlow").e("❌ Validation failed, stopping save")
                _uiState.value = _uiState.value.copy(isSaving = false)
                return@launch
            }
            Timber.tag("BookingFlow").d("✅ Validation passed, continuing with save")

            // 2. Calculate Shares
            val shareData = calculateShares(
                ratesData = ratesData,
                numberOfVehicles = numberOfVehicles ?: 1,
                numberOfHours = numberOfHours ?: 0,
                serviceType = serviceType ?: "one_way",
                accountType = preview.accountType,
                isFarmout = preview.reservationType == "farmout",
                createdByAdmin = preview.createdBy == 1
            )

            // 3. Process Extra Stops (Add 'rate' logic)
            val extraStopsInput = extraStops?.mapNotNull { stop ->
                val address = stop.address
                if (address.isNullOrBlank()) return@mapNotNull null

                // Keep existing rate if available, or just pass input
                com.limo1800driver.app.ui.viewmodel.ExtraStopInput(
                    address = address,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    rate = stop.rate ?: "out_town", // Ideally calculated earlier
                    bookingInstructions = stop.bookingInstructions
                )
            }

            // 4. Update Input with processed stops
            val finalUserInput = userInput.copy(extraStops = extraStopsInput)

            // 5. Build Request
            Timber.tag("BookingFlow").d("Building request payload...")
            Timber.tag("BookingFlow").d("Calculated distance: ${_uiState.value.calculatedDistance}, duration: ${_uiState.value.calculatedDuration}")
            Timber.tag("BookingFlow").d("Has location changed: ${_uiState.value.hasLocationChanged}")
            val request = try {
                EditReservationPayloadBuilder.buildEditReservationRequest(
                    preview = preview,
                    rates = ratesData,
                    userInput = finalUserInput,
                    airports = _uiState.value.airports,
                    airlines = _uiState.value.airlines,
                    calculatedShares = shareData, // Make sure your Builder accepts this!
                    calculatedDistance = _uiState.value.calculatedDistance,
                    calculatedDuration = _uiState.value.calculatedDuration
                )
            } catch (e: Exception) {
                val errorMsg = "Failed to build request: ${e.message}"
                Timber.tag("BookingFlow").e(e, "❌ $errorMsg")
                _uiState.value = _uiState.value.copy(isSaving = false, error = errorMsg)
                return@launch
            }
            Timber.tag("BookingFlow").d("✅ Request built successfully")

            // 6. Send Request
            Timber.tag("BookingFlow").d("Sending edit reservation request to API...")
            repository.editReservation(request)
                .onSuccess { resp ->
                    Timber.tag("BookingFlow").d("API Response received - success: ${resp.success}, message: ${resp.message}")
                    if (resp.success) {
                        Timber.tag("BookingFlow").d("✅ Save successful!")
                        _uiState.value = _uiState.value.copy(isSaving = false, successMessage = resp.message)
                    } else {
                        val errorMsg = parseApiError(resp.message, resp.data)
                        Timber.tag("BookingFlow").e("❌ Save failed: $errorMsg")
                        _uiState.value = _uiState.value.copy(isSaving = false, error = errorMsg)
                    }
                }
                .onFailure { e ->
                    val errorMsg = parseNetworkError(e)
                    Timber.tag("BookingFlow").e(e, "❌ Network error: $errorMsg")
                    _uiState.value = _uiState.value.copy(isSaving = false, error = errorMsg)
                }
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearValidationError(errorKey: String? = null) {
        Timber.tag("BookingFlow").d("Clearing validation error${if (errorKey != null) ": $errorKey" else "s"}")
        if (errorKey != null) {
            val currentErrors = _uiState.value.validationErrors.toMutableList()
            currentErrors.remove(errorKey)
            _uiState.value = _uiState.value.copy(validationErrors = currentErrors)
        } else {
            _uiState.value = _uiState.value.copy(validationErrors = emptyList())
        }
    }

    private fun parseApiError(message: String?, data: Any?): String = message ?: "An error occurred"

    private fun parseNetworkError(error: Throwable): String {
        return when {
            error.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Please check your connection and try again."
            error.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection and try again."
            else -> error.message ?: "An unexpected error occurred"
        }
    }

    // --- Update Rates Logic (Updated for Round Trip) ---
    fun updateRatesForLocationChange(
        bookingId: Int,
        vehicleId: Int,
        transferType: String,
        serviceType: String,
        numberOfVehicles: Int,
        pickupTime: String,
        numberOfHours: String,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        extraStopCoordinates: List<Pair<Double, Double>>,
        extraStopAddresses: List<String>
    ) {
        Timber.tag("BookingFlow").d("=== updateRatesForLocationChange called ===")
        Timber.tag("BookingFlow").d("bookingId: $bookingId, vehicleId: $vehicleId")
        Timber.tag("BookingFlow").d("transferType: $transferType, serviceType: $serviceType")
        Timber.tag("BookingFlow").d("pickupCoord: $pickupCoord, dropoffCoord: $dropoffCoord")
        Timber.tag("BookingFlow").d("extraStops: ${extraStopAddresses.size}")

        if (_uiState.value.isManuallyUpdatingRates) {
            Timber.tag("BookingFlow").d("Skipping - manual update in progress")
            return
        }

        viewModelScope.launch {
            markLocationChanged(true)
            _uiState.value = _uiState.value.copy(isRatesLoading = true, ratesErrorMessage = null)

            // Use calculated distance from Directions API if available, otherwise calculate
            val distance = if (pickupCoord != null && dropoffCoord != null) {
                _uiState.value.calculatedDistance ?: calculateTotalDistance(pickupCoord, dropoffCoord, extraStopCoordinates)
            } else {
                0
            }

            // Normalize Types
            val normalizedServiceType = if (serviceType.lowercase().contains("charter")) "charter_tour" else if (serviceType.lowercase().contains("round")) "round_trip" else "one_way"

            val normalizedTransferType = transferType.lowercase().replace(" ", "_").replace("?", "") // Simplified mapping

            val noOfHours = if (normalizedServiceType == "charter_tour") numberOfHours.toIntOrNull() ?: 0 else 0

            // --- Geocoding for In-Town/Out-Town logic ---
            val extraStopRequests = extraStopAddresses.mapIndexedNotNull { index, address ->
                if (address.isBlank() || index >= extraStopCoordinates.size) return@mapIndexedNotNull null
                val coord = extraStopCoordinates[index]

                // Determine rate type via Geocoder
                val rateType = checkStopRateType(pickupCoord, coord)

                com.limo1800driver.app.data.model.dashboard.BookingRatesExtraStopRequest(
                    address = address,
                    latitude = coord.first.toString(),
                    longitude = coord.second.toString(),
                    bookingInstructions = "",
                    rate = rateType
                )
            }

            // Handle Round Trip Logic
            var returnDistance = 0
            var returnPickupTime = "12:00 pm"

            if (normalizedServiceType == "round_trip") {
                returnDistance = distance // Assume roughly same distance back
                // In a real app, you might want separate Return Extra Stops logic here if UI supports it
            }

            val request = com.limo1800driver.app.data.model.dashboard.BookingRatesVehicleRequest(
                vehicleId = vehicleId,
                returnVehicleId = "",
                transferType = normalizedTransferType,
                serviceType = normalizedServiceType,
                numberOfVehicles = numberOfVehicles,
                distance = distance,
                returnDistance = returnDistance,
                noOfHours = noOfHours,
                isMasterVehicle = false,
                extraStops = extraStopRequests,
                returnExtraStops = emptyList(), // Can populate if needed
                manualChangeAffVeh = false,
                pickupTime = convertTimeToAPIFormat(pickupTime),
                returnPickupTime = returnPickupTime,
                affiliateType = "affiliate",
                returnAffiliateType = "affiliate"
            )

            repository.getBookingRatesVehicle(request)
                .onSuccess { resp ->
                    if (resp.success && resp.data != null) {
                        val responseData = resp.data
                        val updatedRates = mutableMapOf<String, String>()

                        // Map all_inclusive_rates
                        responseData.rateArray?.allInclusiveRates?.forEach { (k, v) -> 
                            v?.baserate?.let { updatedRates[k] = String.format("%.2f", it) }
                        }
                        
                        // Map taxes
                        responseData.rateArray?.taxes?.forEach { (k, v) -> 
                            v?.baserate?.let { updatedRates[k] = String.format("%.2f", it) }
                        }
                        
                        // Map amenities
                        responseData.rateArray?.amenities?.forEach { (k, v) -> 
                            v?.baserate?.let { updatedRates[k] = String.format("%.2f", it) }
                        }
                        
                        // Map misc
                        responseData.rateArray?.misc?.forEach { (k, v) -> 
                            v?.baserate?.let { updatedRates[k] = String.format("%.2f", it) }
                        }

                        // Update UI State with new rates
                        val rateArray = responseData.rateArray
                        val mappedRateArray = AdminReservationRateArray(
                            allInclusiveRates = rateArray?.allInclusiveRates?.mapNotNull { (key, value) ->
                                value?.takeIf { it.rateLabel != null }?.let { item ->
                                    key to AdminReservationRateItem(
                                        rateLabel = item.rateLabel ?: "",
                                        baserate = item.baserate,
                                        multiple = item.multiple,
                                        percentage = item.percentage,
                                        amount = item.amount,
                                        type = item.type,
                                        flatBaserate = item.flatBaserate
                                    )
                                }
                            }?.toMap() ?: emptyMap(),
                            taxes = rateArray?.taxes?.mapNotNull { (key, value) ->
                                value?.takeIf { it.rateLabel != null }?.let { item ->
                                    key to AdminReservationRateItem(
                                        rateLabel = item.rateLabel ?: "",
                                        baserate = item.baserate,
                                        multiple = item.multiple,
                                        percentage = item.percentage,
                                        amount = item.amount,
                                        type = item.type,
                                        flatBaserate = item.flatBaserate
                                    )
                                }
                            }?.toMap() ?: emptyMap(),
                            amenities = rateArray?.amenities?.mapNotNull { (key, value) ->
                                value?.takeIf { it.rateLabel != null }?.let { item ->
                                    key to AdminReservationRateItem(
                                        rateLabel = item.rateLabel ?: "",
                                        baserate = item.baserate,
                                        multiple = item.multiple,
                                        percentage = item.percentage,
                                        amount = item.amount,
                                        type = item.type,
                                        flatBaserate = item.flatBaserate
                                    )
                                }
                            }?.toMap() ?: emptyMap(),
                            misc = rateArray?.misc?.mapNotNull { (key, value) ->
                                value?.takeIf { it.rateLabel != null }?.let { item ->
                                    key to AdminReservationRateItem(
                                        rateLabel = item.rateLabel ?: "",
                                        baserate = item.baserate,
                                        multiple = item.multiple,
                                        percentage = item.percentage,
                                        amount = item.amount,
                                        type = item.type,
                                        flatBaserate = item.flatBaserate
                                    )
                                }
                            }?.toMap() ?: emptyMap()
                        )

                        _uiState.value = _uiState.value.copy(
                            isRatesLoading = false, 
                            latestRatesUpdate = updatedRates,
                            rates = AdminReservationRatesData(
                                subTotal = responseData.subTotal,
                                grandTotal = responseData.grandTotal,
                                minRateInvolved = responseData.minRateInvolved,
                                rateArray = mappedRateArray
                            )
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isRatesLoading = false, ratesErrorMessage = resp.message)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isRatesLoading = false, ratesErrorMessage = e.message)
                }
        }
    }

    // --- Geocoder Helper for Rate Logic ---
    private suspend fun checkStopRateType(pickupCoord: Pair<Double, Double>?, stopCoord: Pair<Double, Double>): String {
        if (pickupCoord == null) return "out_town"

        return withContext(Dispatchers.IO) {
            try {
                // Get Pickup City
                val pickupList = geocoder.getFromLocation(pickupCoord.first, pickupCoord.second, 1)
                val pickupCity = pickupList?.firstOrNull()?.locality ?: ""

                // Get Stop City
                val stopList = geocoder.getFromLocation(stopCoord.first, stopCoord.second, 1)
                val stopCity = stopList?.firstOrNull()?.locality ?: ""

                if (pickupCity.isNotBlank() && stopCity.isNotBlank() && pickupCity.equals(stopCity, ignoreCase = true)) {
                    "in_town"
                } else {
                    "out_town"
                }
            } catch (e: Exception) {
                Timber.e(e, "Geocoding failed")
                "out_town" // Fallback
            }
        }
    }

    // --- Check Changes Logic (Unchanged) ---
    fun checkLocationChangesAndUpdateRates(
        bookingId: Int,
        vehicleId: Int?,
        pickupAddress: String?,
        dropoffAddress: String?,
        pickupCoord: Pair<Double, Double>?,
        dropoffCoord: Pair<Double, Double>?,
        serviceType: String?,
        numberOfVehicles: String?,
        numberOfHours: String?,
        transferType: String?,
        pickupTime: String?,
        extraStopCoordinates: List<Pair<Double, Double>>,
        extraStopAddresses: List<String>
    ) {
        val currentState = _uiState.value
        if (currentState.isInitialLoad) return

        // Helper to check lists
        fun extraStopsEqual(l1: List<Pair<Double, Double>>, l2: List<Pair<Double, Double>>): Boolean {
            if (l1.size != l2.size) return false
            return l1.zip(l2).all { (c1, c2) -> coordinatesEqual(c1, c2) }
        }

        val hasChanged =
            pickupAddress != currentState.lastPickupAddress ||
                    dropoffAddress != currentState.lastDropoffAddress ||
                    !coordinatesEqual(pickupCoord, currentState.lastPickupCoordinates) ||
                    !coordinatesEqual(dropoffCoord, currentState.lastDropoffCoordinates) ||
                    serviceType != currentState.lastServiceType ||
                    numberOfVehicles != currentState.lastNumberOfVehicles ||
                    numberOfHours != currentState.lastNumberOfHours ||
                    !extraStopsEqual(extraStopCoordinates, currentState.lastExtraStopCoordinates)

        if (hasChanged && vehicleId != null && transferType != null && pickupTime != null) {
            updateRatesForLocationChange(
                bookingId, vehicleId, transferType, serviceType ?: "",
                numberOfVehicles?.toIntOrNull() ?: 1, pickupTime, numberOfHours ?: "",
                pickupCoord, dropoffCoord, extraStopCoordinates, extraStopAddresses
            )
            updateLastKnownValues(pickupAddress, dropoffAddress, pickupCoord, dropoffCoord, serviceType, numberOfVehicles, numberOfHours, extraStopCoordinates, extraStopAddresses)
        }
    }

    private fun coordinatesEqual(coord1: Pair<Double, Double>?, coord2: Pair<Double, Double>?): Boolean {
        if (coord1 == null && coord2 == null) return true
        if (coord1 == null || coord2 == null) return false
        return abs(coord1.first - coord2.first) < 0.00001 && abs(coord1.second - coord2.second) < 0.00001
    }
}