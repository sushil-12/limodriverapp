package com.limo1800driver.app.ui.screens.booking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.EditReservationExtraStopRequest
import com.limo1800driver.app.data.model.dashboard.MobileDataAirline
import com.limo1800driver.app.data.model.dashboard.MobileDataAirport
import com.limo1800driver.app.ui.booking.components.DatePickerDialog
import com.limo1800driver.app.ui.booking.components.TimePickerDialog
import com.limo1800driver.app.ui.components.*
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.viewmodel.EditBookingUiState
import com.limo1800driver.app.ui.viewmodel.EditBookingViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// CONSTANTS
// ============================================================================

private object Constants {
    const val MIN_CHARTER_HOURS = 2
    const val DEFAULT_VEHICLES = "1"
    const val DEFAULT_HOURS = "2"
    const val DEFAULT_CELL_ISD = "+1"
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val TIME_FORMAT = "HH:mm:ss"

    val BOTTOM_PADDING = 100.dp
    val BUTTON_HEIGHT = 50.dp
    val BUTTON_PADDING_HORIZONTAL = 20.dp
    val BUTTON_PADDING_VERTICAL = 16.dp
    val FIELD_SPACING = 12.dp
    val SECTION_SPACING = 16.dp
}

// ============================================================================
// DATA CLASSES
// ============================================================================

data class ExtraStopFormState(
    val address: String = "",
    val rate: String = "",
    val instructions: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class BookingFormState(
    val serviceType: String = "",
    val transferType: String = "",
    val passengerName: String = "",
    val passengerEmail: String = "",
    val passengerCellIsd: String = Constants.DEFAULT_CELL_ISD,
    val passengerCell: String = "",
    val numberOfVehicles: String = Constants.DEFAULT_VEHICLES,
    val numberOfHours: String = "",
    val pickupDate: String = "",
    val pickupTime: String = "",
    val meetGreetChoice: String = "",
    val pickupAddress: String = "",
    val dropoffAddress: String = "",
    val extraStops: List<ExtraStopFormState> = emptyList(),
    // Airport / Cruise Specifics
    val pickupAirport: String = "",
    val pickupAirline: String = "",
    val pickupFlightNumber: String = "",
    val pickupOriginCity: String = "",
    val dropoffAirport: String = "",
    val dropoffAirline: String = "",
    val dropoffFlightNumber: String = "",
    val dropoffDestinationCity: String = "",
    val cruisePort: String = "",
    val cruiseShipName: String = "",
    val shipArrivalTime: String = "",
    val specialInstructions: String = ""
)

// ============================================================================
// MAIN SCREEN
// ============================================================================

@Composable
fun EditBookingDetailsAndRatesScreen(
    bookingId: Int,
    source: String,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val viewModel: EditBookingViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Consolidated state management
    val screenState = rememberScreenState(
        bookingId = bookingId,
        viewModel = viewModel,
        state = state,
        onCompleted = onCompleted
    )

    // Derived states
    val isCharterTour = remember(screenState.formState.serviceType) {
        BookingFormUtils.isCharterTour(screenState.formState.serviceType)
    }

    val isValidForm = remember(
        screenState.formState.serviceType,
        screenState.formState.numberOfHours,
        screenState.locationValidationError
    ) {
        val hoursValid = BookingFormUtils.isCharterHoursValid(
            screenState.formState.serviceType,
            screenState.formState.numberOfHours
        )
        // Can't save if there is a location error
        hoursValid && screenState.locationValidationError == null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            CommonMenuHeader(
                title = "Edit a Booking (#$bookingId)",
                subtitle = "Booking Details",
                onBackClick = onBack
            )

            when {
                state.isLoading -> {
                    Timber.tag("BookingFlow").d("Showing loading state")
                    LoadingState()
                }
                // Only show ErrorState for critical errors, not validation errors
                state.error != null && state.preview == null -> {
                    Timber.tag("BookingFlow").e("Showing error state: ${state.error}")
                    ErrorState(error = state.error)
                }
                state.preview == null || state.rates == null -> {
                    Timber.tag("BookingFlow").w("Showing empty state - preview: ${state.preview != null}, rates: ${state.rates != null}")
                    EmptyState()
                }
                else -> {
                    Timber.tag("BookingFlow").d("Showing booking form content")
                    BookingFormContent(
                        screenState = screenState,
                        viewModel = viewModel,
                        rates = state.rates!!,
                        airports = state.airports,
                        airlines = state.airlines,
                        preview = state.preview!!,
                        isCharterTour = isCharterTour,
                        validationErrors = state.validationErrors
                    )
                }
            }
        }

        // Sticky Bottom Bar
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            SaveButtonBar(
                enabled = !state.isSaving && isValidForm,
                isLoading = state.isSaving,
                onSave = {
                    screenState.saveBooking(state.preview!!)
                }
            )
        }
    }

    // Date/Time Pickers Overlay
    DateTimePickers(
        showDatePicker = screenState.showDatePicker,
        showTimePicker = screenState.showTimePicker,
        showShipTimePicker = screenState.showShipTimePicker,
        pickupDate = screenState.formState.pickupDate,
        pickupTime = screenState.formState.pickupTime,
        shipArrivalTime = screenState.formState.shipArrivalTime,
        onDateSelected = { date ->
            screenState.updateForm(screenState.formState.copy(pickupDate = BookingFormUtils.formatDateYMD(date)))
        },
        onTimeSelected = { time ->
            screenState.updateForm(screenState.formState.copy(pickupTime = BookingFormUtils.formatTimeHHmmss(time)))
        },
        onShipTimeSelected = { time ->
            screenState.updateForm(screenState.formState.copy(shipArrivalTime = BookingFormUtils.formatTimeHHmmss(time)))
        },
        onDismissDatePicker = { screenState.onShowDatePickerChange(false) },
        onDismissTimePicker = { screenState.onShowTimePickerChange(false) },
        onDismissShipTimePicker = { screenState.onShowShipTimePickerChange(false) }
    )
}

// ============================================================================
// SCREEN STATE MANAGEMENT
// ============================================================================

@Composable
private fun rememberScreenState(
    bookingId: Int,
    viewModel: EditBookingViewModel,
    state: EditBookingUiState,
    onCompleted: () -> Unit
): ScreenStateHolder {
    val coroutineScope = rememberCoroutineScope()
    
    // UI State
    val dynamicRates = remember { mutableStateMapOf<String, String>() }
    val taxIsPercent = remember { mutableStateMapOf<String, Boolean>() }
    val numberOfHoursState = remember { mutableStateOf("") }
    val numberOfVehiclesState = remember { mutableStateOf(Constants.DEFAULT_VEHICLES) }

    var formState by remember { mutableStateOf(BookingFormState()) }

    // Dialog visibility
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showShipTimePicker by remember { mutableStateOf(false) }

    // Validation Logic State
    var locationValidationError by remember { mutableStateOf<String?>(null) }
    var pickupCountry by remember { mutableStateOf<String?>(null) }
    var dropoffCountry by remember { mutableStateOf<String?>(null) }
    // We store coordinates to potentially calculate distance/time later
    var pickupCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var dropoffCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Sync locationValidationError from ViewModel state
    LaunchedEffect(state.locationValidationError) {
        if (state.locationValidationError != locationValidationError) {
            locationValidationError = state.locationValidationError
            Timber.tag("BookingFlow").d("Synced locationValidationError from ViewModel: ${state.locationValidationError}")
        }
    }

    // 1. Initial Load
    LaunchedEffect(bookingId) {
        if (bookingId != 0) viewModel.load(bookingId)
    }
    LaunchedEffect(Unit) {
        viewModel.loadMobileDataIfNeeded()
    }

    // 2. Map Preview -> Form
    LaunchedEffect(state.preview, state.airports) {
        state.preview?.let { preview ->
            // Log API Response Data
            Timber.tag("BookingFlow").d("=== API Response Data ===")
            Timber.tag("BookingFlow").d("Service Type: ${preview.serviceType}")
            Timber.tag("BookingFlow").d("Transfer Type: ${preview.transferType}")
            Timber.tag("BookingFlow").d("Pickup Address: ${preview.pickupAddress}")
            Timber.tag("BookingFlow").d("Pickup Airport ID: ${preview.pickupAirport}, Name: ${preview.pickupAirportName}")
            Timber.tag("BookingFlow").d("Pickup Latitude: ${preview.pickupLatitude}, Longitude: ${preview.pickupLongitude}")
            Timber.tag("BookingFlow").d("Dropoff Address: ${preview.dropoffAddress}")
            Timber.tag("BookingFlow").d("Dropoff Airport ID: ${preview.dropoffAirport}, Name: ${preview.dropoffAirportName}")
            Timber.tag("BookingFlow").d("Dropoff Latitude: ${preview.dropoffLatitude}, Longitude: ${preview.dropoffLongitude}")
            Timber.tag("BookingFlow").d("Pickup Airline ID: ${preview.pickupAirline}, Name: ${preview.pickupAirlineName}")
            Timber.tag("BookingFlow").d("Dropoff Airline ID: ${preview.dropoffAirline}, Name: ${preview.dropoffAirlineName}")
            Timber.tag("BookingFlow").d("Cruise Port: ${preview.cruisePort}, Cruise Name: ${preview.cruiseName}")
            Timber.tag("BookingFlow").d("Pickup Date: ${preview.pickupDate}, Time: ${preview.pickupTime}")
            Timber.tag("BookingFlow").d("Number of Vehicles: ${preview.numberOfVehicles}, Hours: ${preview.numberOfHours}")
            Timber.tag("BookingFlow").d("Passenger: ${preview.passengerName}, Email: ${preview.passengerEmail}, Cell: ${preview.passengerCellIsd}${preview.passengerCell}")
            
            formState = BookingFormUtils.mapPreviewToFormState(preview)
            
            // Log Form State After Mapping
            Timber.tag("BookingFlow").d("=== Form State Set ===")
            Timber.tag("BookingFlow").d("Service Type: ${formState.serviceType}")
            Timber.tag("BookingFlow").d("Transfer Type: ${formState.transferType}")
            Timber.tag("BookingFlow").d("Pickup Address: ${formState.pickupAddress}")
            Timber.tag("BookingFlow").d("Pickup Airport: ${formState.pickupAirport}")
            Timber.tag("BookingFlow").d("Dropoff Address: ${formState.dropoffAddress}")
            Timber.tag("BookingFlow").d("Dropoff Airport: ${formState.dropoffAirport}")
            Timber.tag("BookingFlow").d("Pickup Airline: ${formState.pickupAirline}")
            Timber.tag("BookingFlow").d("Dropoff Airline: ${formState.dropoffAirline}")
            Timber.tag("BookingFlow").d("Cruise Port: ${formState.cruisePort}, Ship: ${formState.cruiseShipName}")
            Timber.tag("BookingFlow").d("Pickup Date: ${formState.pickupDate}, Time: ${formState.pickupTime}")
            Timber.tag("BookingFlow").d("Number of Vehicles: ${formState.numberOfVehicles}, Hours: ${formState.numberOfHours}")
            
            numberOfVehiclesState.value = formState.numberOfVehicles.ifBlank { Constants.DEFAULT_VEHICLES }
            numberOfHoursState.value = formState.numberOfHours

            val transferKey = BookingFormUtils.transferTypeKey(preview.transferType.orEmpty())
            Timber.tag("BookingFlow").d("Transfer Key: $transferKey")
            
            // Hydrate pickup coordinates
            if (transferKey in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port")) {
                // For airport pickup, try to get airport coordinates
                val airportName = preview.pickupAirportName ?: preview.pickupAirport
                Timber.tag("BookingFlow").d("Pickup is Airport: $airportName")
                if (!airportName.isNullOrBlank()) {
                    val apt = state.airports.find { it.displayName.equals(airportName, true) }
                    apt?.let {
                        val lat = it.lat
                        val lng = it.long
                        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                            pickupCoords = lat to lng
                            pickupCountry = it.country
                            Timber.tag("BookingFlow").d("Pickup Coords Set (Airport): lat=$lat, lng=$lng, country=${it.country}")
                        } else {
                            Timber.tag("BookingFlow").w("Pickup Airport found but invalid coordinates: lat=$lat, lng=$lng")
                        }
                    } ?: Timber.tag("BookingFlow").w("Pickup Airport not found in airports list: $airportName")
                }
            } else {
                // For regular pickup, use address coordinates
                val pLat = preview.pickupLatitude?.toDoubleOrNull()
                val pLng = preview.pickupLongitude?.toDoubleOrNull()
                Timber.tag("BookingFlow").d("Pickup is Address: ${preview.pickupAddress}")
                if (pLat != null && pLng != null && pLat != 0.0 && pLng != 0.0) {
                    pickupCoords = pLat to pLng
                    Timber.tag("BookingFlow").d("Pickup Coords Set (Address): lat=$pLat, lng=$pLng")
                } else {
                    Timber.tag("BookingFlow").w("Pickup Address has invalid coordinates: lat=$pLat, lng=$pLng")
                }
            }

            // Hydrate dropoff coordinates
            if (transferKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport")) {
                // For airport dropoff, try to get airport coordinates
                val airportName = preview.dropoffAirportName ?: preview.dropoffAirport
                Timber.tag("BookingFlow").d("Dropoff is Airport: $airportName")
                if (!airportName.isNullOrBlank()) {
                    val apt = state.airports.find { it.displayName.equals(airportName, true) }
                    apt?.let {
                        val lat = it.lat
                        val lng = it.long
                        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                            dropoffCoords = lat to lng
                            dropoffCountry = it.country
                            Timber.tag("BookingFlow").d("Dropoff Coords Set (Airport): lat=$lat, lng=$lng, country=${it.country}")
                        } else {
                            Timber.tag("BookingFlow").w("Dropoff Airport found but invalid coordinates: lat=$lat, lng=$lng")
                        }
                    } ?: Timber.tag("BookingFlow").w("Dropoff Airport not found in airports list: $airportName")
                }
            } else {
                // For regular dropoff, use address coordinates
                val dLat = preview.dropoffLatitude?.toDoubleOrNull()
                val dLng = preview.dropoffLongitude?.toDoubleOrNull()
                Timber.tag("BookingFlow").d("Dropoff is Address: ${preview.dropoffAddress}")
                if (dLat != null && dLng != null && dLat != 0.0 && dLng != 0.0) {
                    dropoffCoords = dLat to dLng
                    Timber.tag("BookingFlow").d("Dropoff Coords Set (Address): lat=$dLat, lng=$dLng")
                } else {
                    Timber.tag("BookingFlow").w("Dropoff Address has invalid coordinates: lat=$dLat, lng=$dLng")
                }
            }
            
            Timber.tag("BookingFlow").d("=== Initial Load Complete ===")
            
            // Update last known values with initial data
            // This allows us to detect actual user changes later
            // Calculate actual addresses based on transfer type (reuse existing transferKey from line 306)
            val actualPickupAddress = when {
                transferKey in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port") -> formState.pickupAirport
                transferKey in setOf("cruise_to_airport", "cruise_port_to_city") -> formState.cruisePort
                else -> formState.pickupAddress
            }
            val actualDropoffAddress = when {
                transferKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> formState.dropoffAirport
                else -> formState.dropoffAddress
            }
            
            viewModel.updateLastKnownValues(
                pickupAddress = actualPickupAddress,
                dropoffAddress = actualDropoffAddress,
                pickupCoord = pickupCoords,
                dropoffCoord = dropoffCoords,
                serviceType = formState.serviceType,
                numberOfVehicles = formState.numberOfVehicles,
                numberOfHours = formState.numberOfHours,
                extraStopCoordinates = formState.extraStops.mapNotNull { stop ->
                    if (stop.latitude != null && stop.longitude != null) {
                        stop.latitude to stop.longitude
                    } else null
                },
                extraStopAddresses = formState.extraStops.mapNotNull { stop ->
                    if (stop.address.isNotBlank() && stop.latitude != null && stop.longitude != null) {
                        stop.address
                    } else null
                }
            )
            
            // CRITICAL: Mark initial load complete AFTER all data hydration is done
            // This prevents LaunchedEffects (especially distance calculation) from triggering rate updates during initial load
            viewModel.markInitialLoadComplete()
            Timber.tag("BookingFlow").d("Initial load marked as complete, last known values updated")
        }
    }

    // 3. Service Type Logic
    LaunchedEffect(formState.serviceType) {
        val normalized = BookingFormUtils.normalizeServiceType(formState.serviceType)
        if (BookingFormUtils.isCharterTour(normalized)) {
            if (formState.numberOfHours.isBlank()) {
                formState = formState.copy(numberOfHours = Constants.DEFAULT_HOURS)
                numberOfHoursState.value = Constants.DEFAULT_HOURS
            }
        } else {
            if (formState.numberOfHours.isNotBlank()) {
                formState = formState.copy(numberOfHours = "")
                numberOfHoursState.value = ""
            }
        }
    }
    
    // 3.5. Update coordinates when airports are selected
    LaunchedEffect(formState.pickupAirport, formState.dropoffAirport, state.airports) {
        val transferKey = BookingFormUtils.transferTypeKey(formState.transferType)
        
        // Update pickup coordinates if airport is selected
        if (transferKey in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port")) {
            if (formState.pickupAirport.isNotBlank()) {
                Timber.tag("BookingFlow").d("Updating pickup airport: ${formState.pickupAirport}")
                val apt = state.airports.find { it.displayName.equals(formState.pickupAirport, true) }
                apt?.let {
                    val lat = it.lat
                    val lng = it.long
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        pickupCoords = lat to lng
                        pickupCountry = it.country
                        Timber.tag("BookingFlow").d("Pickup Airport Coords Updated: lat=$lat, lng=$lng, country=${it.country}")
                    } else {
                        Timber.tag("BookingFlow").w("Pickup Airport has invalid coordinates: lat=$lat, lng=$lng")
                    }
                } ?: Timber.tag("BookingFlow").w("Pickup Airport not found: ${formState.pickupAirport}")
            }
        }
        
        // Update dropoff coordinates if airport is selected
        if (transferKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport")) {
            if (formState.dropoffAirport.isNotBlank()) {
                Timber.tag("BookingFlow").d("Updating dropoff airport: ${formState.dropoffAirport}")
                val apt = state.airports.find { it.displayName.equals(formState.dropoffAirport, true) }
                apt?.let {
                    val lat = it.lat
                    val lng = it.long
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        dropoffCoords = lat to lng
                        dropoffCountry = it.country
                        Timber.tag("BookingFlow").d("Dropoff Airport Coords Updated: lat=$lat, lng=$lng, country=${it.country}")
                    } else {
                        Timber.tag("BookingFlow").w("Dropoff Airport has invalid coordinates: lat=$lat, lng=$lng")
                    }
                } ?: Timber.tag("BookingFlow").w("Dropoff Airport not found: ${formState.dropoffAirport}")
            }
        }
    }

    // 3.6. Calculate distance with Directions API when coordinates change
    LaunchedEffect(
        pickupCoords,
        dropoffCoords,
        formState.extraStops
    ) {
        if (pickupCoords != null && dropoffCoords != null) {
            val extraStopCoords = formState.extraStops.mapNotNull { stop ->
                if (stop.latitude != null && stop.longitude != null) {
                    stop.latitude to stop.longitude
                } else null
            }
            
            // Helper functions for coordinate comparison
            fun coordinatesEqual(c1: Pair<Double, Double>?, c2: Pair<Double, Double>?): Boolean {
                if (c1 == null && c2 == null) return true
                if (c1 == null || c2 == null) return false
                return kotlin.math.abs(c1.first - c2.first) < 0.00001 && kotlin.math.abs(c1.second - c2.second) < 0.00001
            }
            
            fun extraStopsEqual(l1: List<Pair<Double, Double>>, l2: List<Pair<Double, Double>>): Boolean {
                if (l1.size != l2.size) return false
                return l1.zip(l2).all { (c1, c2) -> coordinatesEqual(c1, c2) }
            }
            
            // Check if we should skip distance calculation (initial load or no user change)
            val lastKnownCoordsAreNull = state.lastPickupCoordinates == null || state.lastDropoffCoordinates == null
            val coordinatesMatchLastKnown = !lastKnownCoordsAreNull && 
                                             coordinatesEqual(pickupCoords, state.lastPickupCoordinates) &&
                                             coordinatesEqual(dropoffCoords, state.lastDropoffCoordinates) &&
                                             extraStopsEqual(extraStopCoords, state.lastExtraStopCoordinates)
            
            // Skip Directions API call during initial load or if coordinates haven't changed
            if (state.isInitialLoad || lastKnownCoordsAreNull || coordinatesMatchLastKnown) {
                Timber.tag("BookingFlow").d("Skipping Directions API call - initial load: ${state.isInitialLoad}, coords null: $lastKnownCoordsAreNull, coords match: $coordinatesMatchLastKnown")
                return@LaunchedEffect
            }
            
            Timber.tag("BookingFlow").d("Calculating distance via Directions API - coordinates changed from last known values")
            coroutineScope.launch {
                val result = viewModel.calculateDistanceWithDirections(
                    pickupCoord = pickupCoords,
                    dropoffCoord = dropoffCoords,
                    extraStopCoordinates = extraStopCoords
                )
                
                if (result != null) {
                    val (distance, duration) = result
                    viewModel.updateCalculatedDistanceAndDuration(distance, duration)
                    Timber.tag("BookingFlow").d("Distance calculated via Directions API: ${distance}m, Duration: ${duration}s")
                    
                    // Trigger rate recalculation if distance changed significantly (>100m) and we have required data
                    val originalDistance = state.originalApiDistance.toDoubleOrNull()?.toInt() ?: 0
                    if (kotlin.math.abs(distance - originalDistance) > 100) {
                        Timber.tag("BookingFlow").d("Distance changed significantly, recalculating rates...")
                        
                        // Only update rates if we have all required data
                        // Note: We already checked for initial load and coordinate changes before calling Directions API
                        if (state.preview != null && 
                            state.preview.vehicleId != null &&
                            formState.serviceType.isNotBlank() &&
                            formState.transferType.isNotBlank()) {
                            
                            val extraStopAddresses = formState.extraStops.mapNotNull { stop ->
                                if (stop.address.isNotBlank() && stop.latitude != null && stop.longitude != null) {
                                    stop.address
                                } else null
                            }
                            
                            viewModel.updateRatesForLocationChange(
                                bookingId = bookingId,
                                vehicleId = state.preview.vehicleId!!,
                                transferType = formState.transferType,
                                serviceType = formState.serviceType,
                                numberOfVehicles = formState.numberOfVehicles.toIntOrNull() ?: 1,
                                pickupTime = formState.pickupTime.ifBlank { "12:00 PM" },
                                numberOfHours = formState.numberOfHours,
                                pickupCoord = pickupCoords,
                                dropoffCoord = dropoffCoords,
                                extraStopCoordinates = extraStopCoords,
                                extraStopAddresses = extraStopAddresses
                            )
                        } else {
                            Timber.tag("BookingFlow").d("Skipping rate update - initial load, coordinates unchanged, or missing required data")
                        }
                    }
                } else {
                    Timber.tag("BookingFlow").w("Directions API failed, using Haversine fallback")
                }
            }
        }
    }

    // 4. Location Validation Logic (Replaces the external helper)
    LaunchedEffect(
        formState.pickupAddress, 
        formState.dropoffAddress, 
        formState.pickupAirport,
        formState.dropoffAirport,
        formState.cruisePort,
        pickupCountry, 
        dropoffCountry, 
        formState.transferType
    ) {
        // Determine actual addresses based on transfer type
        val transferKey = BookingFormUtils.transferTypeKey(formState.transferType)
        val actualPickupAddress = when {
            transferKey in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port") -> formState.pickupAirport
            transferKey in setOf("cruise_to_airport", "cruise_port_to_city") -> formState.cruisePort
            else -> formState.pickupAddress
        }
        val actualDropoffAddress = when {
            transferKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> formState.dropoffAirport
            else -> formState.dropoffAddress
        }
        
        val validationError = BookingFormUtils.validateLocationConflict(
            pickupAddress = actualPickupAddress,
            dropoffAddress = actualDropoffAddress,
            pickupCountry = pickupCountry,
            dropoffCountry = dropoffCountry,
            transferType = formState.transferType
        )
        if (validationError != locationValidationError) {
            Timber.tag("BookingFlow").d("Location Validation: $validationError")
            Timber.tag("BookingFlow").d("  Actual Pickup: $actualPickupAddress (country: $pickupCountry)")
            Timber.tag("BookingFlow").d("  Actual Dropoff: $actualDropoffAddress (country: $dropoffCountry)")
        }
        locationValidationError = validationError
    }

    // 4.5. Validate addresses and update rates when pickup/dropoff/extra stops change
    LaunchedEffect(
        formState.pickupAddress,
        formState.dropoffAddress,
        formState.pickupAirport,
        formState.dropoffAirport,
        formState.extraStops,
        formState.serviceType,
        formState.transferType,
        formState.numberOfVehicles,
        formState.numberOfHours,
        formState.pickupTime,
        state.preview?.vehicleId,
        pickupCoords,
        dropoffCoords,
        state.extraStopsChangeTrigger // Include trigger to force recalculation when stops are added/removed
    ) {
        Timber.tag("BookingFlow").d("=== Validation Effect Triggered ===")
        Timber.tag("BookingFlow").d("isInitialLoad: ${state.isInitialLoad}, preview: ${state.preview != null}, vehicleId: ${state.preview?.vehicleId}")
        Timber.tag("BookingFlow").d("pickupAddress: '${formState.pickupAddress}', dropoffAddress: '${formState.dropoffAddress}'")
        Timber.tag("BookingFlow").d("pickupCoords: $pickupCoords, dropoffCoords: $dropoffCoords")

        // CRITICAL: Skip rate updates during initial load to prevent overriding reservation-rates API results
        if (state.isInitialLoad) {
            Timber.tag("BookingFlow").d("Skipping rate update - initial load in progress")
            return@LaunchedEffect
        }

        // Skip if missing required data
        if (state.preview == null || state.preview.vehicleId == null) {
            Timber.tag("BookingFlow").d("Skipping validation - missing preview data")
            return@LaunchedEffect
        }

        // Skip if service type or transfer type is not set
        if (formState.serviceType.isBlank() || formState.transferType.isBlank()) {
            return@LaunchedEffect
        }

        // Check if we have valid addresses to validate
        val transferKey = BookingFormUtils.transferTypeKey(formState.transferType)
        val hasPickupAddress = when {
            transferKey in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port") -> formState.pickupAirport.isNotBlank()
            transferKey in setOf("cruise_to_airport", "cruise_port_to_city") -> formState.cruisePort.isNotBlank()
            else -> formState.pickupAddress.isNotBlank()
        }
        val hasDropoffAddress = when {
            transferKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> formState.dropoffAirport.isNotBlank()
            else -> formState.dropoffAddress.isNotBlank()
        }

        // Skip if we don't have addresses to validate yet
        if (!hasPickupAddress || !hasDropoffAddress) {
            return@LaunchedEffect
        }

        val extraStopCoords = formState.extraStops.mapNotNull { stop ->
            if (stop.latitude != null && stop.longitude != null) {
                stop.latitude to stop.longitude
            } else null
        }
        val extraStopAddresses = formState.extraStops.mapNotNull { stop ->
            if (stop.address.isNotBlank() && stop.latitude != null && stop.longitude != null) {
                stop.address
            } else null
        }

        // CRITICAL: Check if coordinates actually changed from last known values
        // This prevents rate updates when coordinates are just being set during initial load
        fun coordinatesEqual(c1: Pair<Double, Double>?, c2: Pair<Double, Double>?): Boolean {
            if (c1 == null && c2 == null) return true
            if (c1 == null || c2 == null) return false
            return kotlin.math.abs(c1.first - c2.first) < 0.00001 && kotlin.math.abs(c1.second - c2.second) < 0.00001
        }
        
        fun extraStopsEqual(l1: List<Pair<Double, Double>>, l2: List<Pair<Double, Double>>): Boolean {
            if (l1.size != l2.size) return false
            return l1.zip(l2).all { (c1, c2) -> coordinatesEqual(c1, c2) }
        }
        
        val pickupCoordsChanged = !coordinatesEqual(pickupCoords, state.lastPickupCoordinates)
        val dropoffCoordsChanged = !coordinatesEqual(dropoffCoords, state.lastDropoffCoordinates)
        val extraStopsChanged = !extraStopsEqual(extraStopCoords, state.lastExtraStopCoordinates)
        
        // Calculate actual addresses for comparison
        val actualPickupAddress = when {
            transferKey in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port") -> formState.pickupAirport
            transferKey in setOf("cruise_to_airport", "cruise_port_to_city") -> formState.cruisePort
            else -> formState.pickupAddress
        }
        val actualDropoffAddress = when {
            transferKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> formState.dropoffAirport
            else -> formState.dropoffAddress
        }
        
        val pickupAddressChanged = actualPickupAddress != state.lastPickupAddress
        val dropoffAddressChanged = actualDropoffAddress != state.lastDropoffAddress
        val serviceTypeChanged = formState.serviceType != state.lastServiceType
        val numberOfVehiclesChanged = formState.numberOfVehicles != state.lastNumberOfVehicles
        val numberOfHoursChanged = formState.numberOfHours != state.lastNumberOfHours
        
        val hasLocationChanged = pickupCoordsChanged || dropoffCoordsChanged || extraStopsChanged || 
                                pickupAddressChanged || dropoffAddressChanged || 
                                serviceTypeChanged || numberOfVehiclesChanged || numberOfHoursChanged
        
        if (!hasLocationChanged) {
            Timber.tag("BookingFlow").d("Skipping rate update - no actual location/parameter changes detected")
            Timber.tag("BookingFlow").d("  pickupCoords changed: $pickupCoordsChanged, dropoffCoords changed: $dropoffCoordsChanged")
            Timber.tag("BookingFlow").d("  pickupAddress changed: $pickupAddressChanged, dropoffAddress changed: $dropoffAddressChanged")
            Timber.tag("BookingFlow").d("  extraStops changed: $extraStopsChanged (current: ${extraStopCoords.size}, last: ${state.lastExtraStopCoordinates.size})")
            return@LaunchedEffect
        }
        
        Timber.tag("BookingFlow").d("Location/parameters changed - proceeding with validation and rate update")
        Timber.tag("BookingFlow").d("  pickupCoords changed: $pickupCoordsChanged, dropoffCoords changed: $dropoffCoordsChanged")
        Timber.tag("BookingFlow").d("  pickupAddress changed: $pickupAddressChanged, dropoffAddress changed: $dropoffAddressChanged")
        Timber.tag("BookingFlow").d("  extraStops changed: $extraStopsChanged (current: ${extraStopCoords.size}, last: ${state.lastExtraStopCoordinates.size})")

        coroutineScope.launch {
            Timber.tag("BookingFlow").d("Starting validation process...")

            // Step 1: Check if we have coordinates to validate routes
            if (pickupCoords == null || dropoffCoords == null) {
                Timber.tag("BookingFlow").w("Cannot validate route - coordinates not available yet")
                viewModel.setLocationValidationError("Please select pickup and drop-off locations from the suggestions to validate the route.")
                return@launch
            }

            // Step 2: Validate addresses using LocationValidationFunction
            Timber.tag("BookingFlow").d("Validating addresses for car route...")
            Timber.tag("BookingFlow").d("  Pickup: $pickupCoords, Dropoff: $dropoffCoords")
            Timber.tag("BookingFlow").d("  Extra stops: ${extraStopCoords.size}")
            val validationError = viewModel.validateAddressesWithDirectionsApi(
                pickupCoord = pickupCoords,
                dropoffCoord = dropoffCoords,
                extraStopCoords = extraStopCoords
            )

            if (validationError != null) {
                Timber.tag("BookingFlow").e("Address validation failed: $validationError")
                Timber.tag("BookingFlow").e("Setting location validation error in ViewModel")
                viewModel.setLocationValidationError(validationError)
                Timber.tag("BookingFlow").d("Location validation error set, current state: ${state.locationValidationError}")
                return@launch
            } else {
                Timber.tag("BookingFlow").d("Address validation passed")
            }

            // Step 3: Validation passed, clear any previous errors
            Timber.tag("BookingFlow").d("Validation passed, clearing errors")
            viewModel.setLocationValidationError(null)

            // Step 4: Update rates via booking-rates-vehicle API
            // Only update rates when there's an actual user change (already checked above)
            Timber.tag("BookingFlow").d("Addresses validated successfully, updating rates...")
            viewModel.updateRatesForLocationChange(
                bookingId = bookingId,
                vehicleId = state.preview.vehicleId!!,
                transferType = formState.transferType,
                serviceType = formState.serviceType,
                numberOfVehicles = formState.numberOfVehicles.toIntOrNull() ?: 1,
                pickupTime = formState.pickupTime.ifBlank { "12:00 PM" },
                numberOfHours = formState.numberOfHours,
                pickupCoord = pickupCoords,
                dropoffCoord = dropoffCoords,
                extraStopCoordinates = extraStopCoords,
                extraStopAddresses = extraStopAddresses
            )
            
            // Update last known values after triggering rate update
            // This prevents duplicate rate updates if user changes and then changes back
            viewModel.updateLastKnownValues(
                pickupAddress = actualPickupAddress,
                dropoffAddress = actualDropoffAddress,
                pickupCoord = pickupCoords,
                dropoffCoord = dropoffCoords,
                serviceType = formState.serviceType,
                numberOfVehicles = formState.numberOfVehicles,
                numberOfHours = formState.numberOfHours,
                extraStopCoordinates = extraStopCoords,
                extraStopAddresses = extraStopAddresses
            )
            Timber.tag("BookingFlow").d("Last known values updated after rate update trigger")
        }
    }

    // 4.6. Update dynamicRates when latestRatesUpdate changes
    LaunchedEffect(state.latestRatesUpdate) {
        state.latestRatesUpdate?.let { updatedRates ->
            Timber.tag("BookingFlow").d("Updating dynamic rates from API response")
            updatedRates.forEach { (key, value) ->
                dynamicRates[key] = value
            }
        }
    }
    
    // Track UI changes to dynamicRates
    LaunchedEffect(dynamicRates) {
        if (dynamicRates.isNotEmpty()) {
            Timber.tag("BookingFlow").d("=== UI dynamicRates changed ===")
            dynamicRates.forEach { (key, value) ->
                Timber.tag("BookingFlow").d("  UI[$key] = $value")
            }
        }
    }

    // 5. Success Handling
    LaunchedEffect(state.successMessage) {
        state.successMessage?.takeIf { it.isNotBlank() }?.let {
            viewModel.consumeSuccess()
            onCompleted()
        }
    }

    // 6. Save Function
    fun performSave(preview: AdminBookingPreviewData) {
        Timber.tag("BookingFlow").d("=== performSave START ===")
        Timber.tag("BookingFlow").d("bookingId: $bookingId")
        Timber.tag("BookingFlow").d("Pickup Coords: ${pickupCoords?.first}, ${pickupCoords?.second}")
        Timber.tag("BookingFlow").d("Dropoff Coords: ${dropoffCoords?.first}, ${dropoffCoords?.second}")
        Timber.tag("BookingFlow").d("Pickup Country: $pickupCountry, Dropoff Country: $dropoffCountry")
        Timber.tag("BookingFlow").d("Pickup Airport: '${formState.pickupAirport}'")
        Timber.tag("BookingFlow").d("Dropoff Airport: '${formState.dropoffAirport}'")
        Timber.tag("BookingFlow").d("Pickup Address: '${formState.pickupAddress}'")
        Timber.tag("BookingFlow").d("Dropoff Address: '${formState.dropoffAddress}'")
        Timber.tag("BookingFlow").d("Pickup Airline: '${formState.pickupAirline}'")
        Timber.tag("BookingFlow").d("Pickup Flight: '${formState.pickupFlightNumber}'")
        Timber.tag("BookingFlow").d("Dropoff Airline: '${formState.dropoffAirline}'")
        Timber.tag("BookingFlow").d("Transfer Type: '${formState.transferType}'")
        Timber.tag("BookingFlow").d("Service Type: '${formState.serviceType}'")
        Timber.tag("BookingFlow").d("Location Validation Error: $locationValidationError")
        
        val payloadRates = dynamicRates.mapValues { (_, v) -> v.toDoubleOrNull() ?: v }
        Timber.tag("BookingFlow").d("=== performSave: Preparing payloadRates ===")
        Timber.tag("BookingFlow").d("Dynamic rates count: ${payloadRates.size}")
        payloadRates.forEach { (key, value) ->
            Timber.tag("BookingFlow").d("  payloadRates[$key] = $value")
        }

        viewModel.saveEditReservation(
            bookingId = bookingId,
            pickupAddress = formState.pickupAddress,
            dropoffAddress = formState.dropoffAddress,
            pickupDate = formState.pickupDate,
            pickupTime = formState.pickupTime,
            vehicleId = preview.vehicleId,
            rates = payloadRates,
            serviceType = BookingFormUtils.normalizeServiceType(formState.serviceType),
            transferType = formState.transferType,
            numberOfHours = BookingFormUtils.serviceHoursForPayload(formState.serviceType, formState.numberOfHours),
            numberOfVehicles = formState.numberOfVehicles.toIntOrNull(),
            meetGreetChoiceName = formState.meetGreetChoice,
            bookingInstructions = formState.specialInstructions,
            passengerName = formState.passengerName,
            passengerEmail = formState.passengerEmail,
            passengerCellIsd = formState.passengerCellIsd,
            passengerCell = formState.passengerCell,
            pickupAirportName = formState.pickupAirport,
            pickupAirlineName = formState.pickupAirline,
            pickupFlight = formState.pickupFlightNumber,
            originAirportCity = formState.pickupOriginCity,
            cruisePort = formState.cruisePort,
            cruiseName = formState.cruiseShipName,
            cruiseTime = formState.shipArrivalTime,
            dropoffAirportName = formState.dropoffAirport,
            dropoffAirlineName = formState.dropoffAirline,
            dropoffFlight = formState.dropoffFlightNumber,
            departingAirportCity = formState.dropoffDestinationCity,
            extraStops = formState.extraStops.map {
                EditReservationExtraStopRequest(
                    address = it.address.ifBlank { null },
                    rate = it.rate.ifBlank { null },
                    bookingInstructions = it.instructions.ifBlank { null },
                    latitude = it.latitude?.toString(),
                    longitude = it.longitude?.toString()
                )
            },
            pickupLatitude = pickupCoords?.first?.toString(),
            pickupLongitude = pickupCoords?.second?.toString(),
            dropoffLatitude = dropoffCoords?.first?.toString(),
            dropoffLongitude = dropoffCoords?.second?.toString()
        )
        Timber.tag("BookingFlow").d("Save request sent with coordinates: pickup(${pickupCoords?.first}, ${pickupCoords?.second}), dropoff(${dropoffCoords?.first}, ${dropoffCoords?.second})")
    }

    return ScreenStateHolder(
        formState = formState,
        dynamicRates = dynamicRates,
        taxIsPercent = taxIsPercent,
        numberOfHoursState = numberOfHoursState,
        numberOfVehiclesState = numberOfVehiclesState,
        showDatePicker = showDatePicker,
        showTimePicker = showTimePicker,
        showShipTimePicker = showShipTimePicker,
        locationValidationError = locationValidationError,
        pickupCoords = pickupCoords,
        dropoffCoords = dropoffCoords,

        updateForm = { newState ->
            Timber.tag("BookingFlow").d("Form State Updated:")
            Timber.tag("BookingFlow").d("  Service Type: ${formState.serviceType} -> ${newState.serviceType}")
            Timber.tag("BookingFlow").d("  Transfer Type: ${formState.transferType} -> ${newState.transferType}")
            Timber.tag("BookingFlow").d("  Pickup Address: ${formState.pickupAddress} -> ${newState.pickupAddress}")
            Timber.tag("BookingFlow").d("  Pickup Airport: ${formState.pickupAirport} -> ${newState.pickupAirport}")
            Timber.tag("BookingFlow").d("  Dropoff Address: ${formState.dropoffAddress} -> ${newState.dropoffAddress}")
            Timber.tag("BookingFlow").d("  Dropoff Airport: ${formState.dropoffAirport} -> ${newState.dropoffAirport}")
            Timber.tag("BookingFlow").d("  Pickup Date: ${formState.pickupDate} -> ${newState.pickupDate}")
            Timber.tag("BookingFlow").d("  Pickup Time: ${formState.pickupTime} -> ${newState.pickupTime}")
            formState = newState
        },
        onShowDatePickerChange = { showDatePicker = it },
        onShowTimePickerChange = { showTimePicker = it },
        onShowShipTimePickerChange = { showShipTimePicker = it },

        // Location Setters
        setPickupLocationData = { country, coords ->
            Timber.tag("BookingFlow").d("Pickup Location Data Set: country=$country, coords=$coords")
            pickupCountry = country
            pickupCoords = coords
        },
        setDropoffLocationData = { country, coords ->
            Timber.tag("BookingFlow").d("Dropoff Location Data Set: country=$country, coords=$coords")
            dropoffCountry = country
            dropoffCoords = coords
        },
        clearLocationError = { 
            locationValidationError = null
            viewModel.setLocationValidationError(null)
        },

        saveBooking = ::performSave
    )
}

private data class ScreenStateHolder(
    val formState: BookingFormState,
    val dynamicRates: SnapshotStateMap<String, String>,
    val taxIsPercent: SnapshotStateMap<String, Boolean>,
    val numberOfHoursState: MutableState<String>,
    val numberOfVehiclesState: MutableState<String>,
    val showDatePicker: Boolean,
    val showTimePicker: Boolean,
    val showShipTimePicker: Boolean,
    val locationValidationError: String?,
    val pickupCoords: Pair<Double, Double>?,
    val dropoffCoords: Pair<Double, Double>?,

    val updateForm: (BookingFormState) -> Unit,
    val onShowDatePickerChange: (Boolean) -> Unit,
    val onShowTimePickerChange: (Boolean) -> Unit,
    val onShowShipTimePickerChange: (Boolean) -> Unit,

    val setPickupLocationData: (String?, Pair<Double, Double>?) -> Unit,
    val setDropoffLocationData: (String?, Pair<Double, Double>?) -> Unit,
    val clearLocationError: () -> Unit,

    val saveBooking: (AdminBookingPreviewData) -> Unit
)

// ============================================================================
// UI COMPONENTS
// ============================================================================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun BookingFormContent(
    screenState: ScreenStateHolder,
    viewModel: EditBookingViewModel,
    rates: com.limo1800driver.app.data.model.dashboard.AdminReservationRatesData,
    airports: List<MobileDataAirport>,
    airlines: List<MobileDataAirline>,
    preview: AdminBookingPreviewData,
    isCharterTour: Boolean,
    validationErrors: List<String>
) {
    val formState = screenState.formState
    
    // Helper functions to check and get error messages (matches user app pattern)
    val hasError: (String) -> Boolean = { errorKey -> validationErrors.contains(errorKey) }
    
    val getErrorMessage: (String) -> String? = { errorKey ->
        if (hasError(errorKey)) {
            when (errorKey) {
                "pickup_airline" -> "Airline is required"
                "pickup_flight_number" -> "Flight number is required"
                "dropoff_airline" -> "Airline is required"
                "cruise_ship_name" -> "Cruise ship name is required"
                "cruise_port" -> "Cruise port is required"
                else -> null
            }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = Constants.BOTTOM_PADDING)
    ) {
        Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))

        // Service Type & Transfer Type
        ServiceTypeSection(
            formState = formState,
            onFormChange = screenState.updateForm
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Date & Time
        DateTimeSection(
            formState = formState,
            onDateClick = { screenState.onShowDatePickerChange(true) },
            onTimeClick = { screenState.onShowTimePickerChange(true) }
        )

        Spacer(modifier = Modifier.height(Constants.FIELD_SPACING))

        // Number of Vehicles
        CommonTextField(
            label = "NUMBER OF VEHICLES",
            placeholder = Constants.DEFAULT_VEHICLES,
            text = formState.numberOfVehicles,
            onValueChange = {
                screenState.updateForm(formState.copy(numberOfVehicles = it))
                screenState.numberOfVehiclesState.value = it
            },
            keyboardType = KeyboardType.Number,
            isRequired = true
        )

        // Number of Hours (Charter Tour only)
        if (isCharterTour) {
            Spacer(modifier = Modifier.height(Constants.FIELD_SPACING))
            CommonTextField(
                label = "NUMBER OF HOURS",
                placeholder = "Min ${Constants.MIN_CHARTER_HOURS} hours",
                text = formState.numberOfHours,
                onValueChange = {
                    screenState.updateForm(formState.copy(numberOfHours = it))
                    screenState.numberOfHoursState.value = it
                },
                keyboardType = KeyboardType.Number,
                isRequired = true
            )
        }

        Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))

        // Locations
        DynamicLocationSection(
            screenState = screenState,
            airports = airports,
            airlines = airlines,
            viewModel = viewModel,
            hasError = hasError,
            getErrorMessage = getErrorMessage
        )

        Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))

        // Extra Stops
        ExtraStopsSection(
            formState = formState,
            onFormChange = screenState.updateForm,
            viewModel = viewModel,
            locationValidationError = screenState.locationValidationError,
            clearLocationError = screenState.clearLocationError
        )

        Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))

        // Total Travel Time
        TravelTimeSection(
            screenState = screenState,
            viewModel = viewModel,
            preview = preview
        )

        Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))

        // Meet & Greet
        CommonDropdown(
            label = "MEET AND GREET CHOICES",
            placeholder = "Select meet and greet",
            selectedValue = formState.meetGreetChoice,
            options = BookingFormUtils.meetGreetOptions(
                current = formState.meetGreetChoice,
                transferType = formState.transferType
            ),
            onValueSelected = { screenState.updateForm(formState.copy(meetGreetChoice = it)) },
            isRequired = true
        )

        Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))

        // Special Instructions
        CommonTextArea(
            label = "SPECIAL INSTRUCTIONS / EXACT BUILDING NAME",
            placeholder = "Enter instructions",
            text = formState.specialInstructions,
            onValueChange = { screenState.updateForm(formState.copy(specialInstructions = it)) },
            minLines = 3,
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Rate Calculator
        CommonRateCalculatorComponent(
            rateArray = rates.rateArray,
            dynamicRates = screenState.dynamicRates,
            taxIsPercent = screenState.taxIsPercent,
            serviceType = BookingFormUtils.normalizeServiceType(formState.serviceType),
            numberOfHours = screenState.numberOfHoursState,
            numberOfVehicles = screenState.numberOfVehiclesState,
            accountType = preview.accountType,
            createdBy = preview.createdBy,
            reservationType = preview.reservationType,
            currencySymbol = preview.currencySymbol,
            isEditable = true,
            showSummary = true
        )

        Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))
    }
}

// ============================================================================
// SUB-SECTIONS
// ============================================================================

@Composable
private fun ServiceTypeSection(
    formState: BookingFormState,
    onFormChange: (BookingFormState) -> Unit
) {
    CommonDropdown(
        label = "SERVICE TYPE",
        placeholder = "Select Service Type",
        selectedValue = formState.serviceType,
        options = BookingFormUtils.serviceTypeOptions(formState.serviceType),
        onValueSelected = { selected ->
            onFormChange(formState.copy(serviceType = BookingFormUtils.normalizeServiceType(selected)))
        },
        isRequired = true
    )

    Spacer(modifier = Modifier.height(Constants.FIELD_SPACING))

    CommonDropdown(
        label = "TRANSFER TYPE",
        placeholder = "Select Transfer Type",
        selectedValue = BookingFormUtils.transferTypeLabelFromValueOrRaw(formState.transferType),
        options = BookingFormUtils.transferTypeOptions(formState.transferType),
        onValueSelected = { selectedLabel ->
            val value = BookingFormUtils.transferTypeValueFromLabelOrNull(selectedLabel) ?: selectedLabel
            onFormChange(formState.copy(transferType = value))
        },
        isRequired = true
    )
}

@Composable
private fun DateTimeSection(
    formState: BookingFormState,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Constants.FIELD_SPACING),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            DateField(
                label = "TRAVEL DATE",
                value = formState.pickupDate,
                onClick = onDateClick,
                isRequired = true
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            TimeField(
                label = "PICKUP TIME",
                value = formState.pickupTime,
                onClick = onTimeClick,
                isRequired = true
            )
        }
    }
}

/**
 * Section Header - matches user app design
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LimoOrange),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DynamicLocationSection(
    screenState: ScreenStateHolder,
    airports: List<MobileDataAirport>,
    airlines: List<MobileDataAirline>,
    viewModel: EditBookingViewModel,
    hasError: (String) -> Boolean,
    getErrorMessage: (String) -> String?
) {
    val formState = screenState.formState
    val transferType = BookingFormUtils.transferTypeKey(formState.transferType)

    // --- PICKUP ---
    SectionHeader("Pick-up")

    Column {
        when {
            transferType in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port") -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AirportFields(
                        airport = formState.pickupAirport,
                        airline = formState.pickupAirline,
                        flightNo = formState.pickupFlightNumber,
                        city = formState.pickupOriginCity,
                        airports = airports,
                        airlines = airlines,
                        onAirportChange = { 
                            screenState.updateForm(formState.copy(pickupAirport = it))
                            // Clear validation error when user starts selecting
                            viewModel.clearValidationError("pickup_airport")
                        },
                        onAirlineChange = { 
                            screenState.updateForm(formState.copy(pickupAirline = it))
                            // Clear validation error when user starts selecting
                            viewModel.clearValidationError("pickup_airline")
                        },
                        onFlightNoChange = { 
                            screenState.updateForm(formState.copy(pickupFlightNumber = it))
                            // Clear validation error when user starts typing
                            viewModel.clearValidationError("pickup_flight_number")
                        },
                        onCityChange = { screenState.updateForm(formState.copy(pickupOriginCity = it)) },
                        isOrigin = true,
                        onAirportSelected = { airportName, _ ->
                            // Look up country for airport validation
                            val apt = airports.find { it.displayName.equals(airportName, true) }
                            val lat = apt?.lat
                            val lng = apt?.long
                            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                                screenState.setPickupLocationData(apt?.country, lat to lng)
                                // Clear location error when valid airport is selected
                                screenState.clearLocationError()
                            }
                        },
                        hasAirlineError = hasError("pickup_airline"),
                        hasFlightError = hasError("pickup_flight_number"),
                        airlineErrorMessage = getErrorMessage("pickup_airline"),
                        flightErrorMessage = getErrorMessage("pickup_flight_number")
                    )
                }
            }
            transferType in setOf("cruise_to_airport", "cruise_port_to_city") -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CruiseFields(
                        portName = formState.cruisePort,
                        shipName = formState.cruiseShipName,
                        time = formState.shipArrivalTime,
                        onPortChange = { 
                            screenState.updateForm(formState.copy(cruisePort = it))
                            viewModel.clearValidationError("cruise_port")
                        },
                        onShipChange = { 
                            screenState.updateForm(formState.copy(cruiseShipName = it))
                            viewModel.clearValidationError("cruise_ship_name")
                        },
                        onTimeClick = { screenState.onShowShipTimePickerChange(true) },
                        portErrorMessage = getErrorMessage("cruise_port"),
                        shipErrorMessage = getErrorMessage("cruise_ship_name")
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocationAutocomplete(
                        label = "PICKUP ADDRESS",
                        value = formState.pickupAddress,
                        onValueChange = { screenState.updateForm(formState.copy(pickupAddress = it)) },
                        onLocationSelected = { fullAddress, _, _, _, displayText, lat, lng, countryCode, _ ->
                            screenState.updateForm(formState.copy(pickupAddress = displayText))
                            if (lat != null && lng != null) {
                                screenState.setPickupLocationData(countryCode, lat to lng)
                            }
                            // Clear location error when valid location is selected
                            screenState.clearLocationError()
                        },
                        placeholder = "Search pickup location",
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth(),
                        errorMessage = screenState.locationValidationError
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // --- DROPOFF ---
    SectionHeader("Drop-off")

    Column {
        when {
            transferType in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AirportFields(
                        airport = formState.dropoffAirport,
                        airline = formState.dropoffAirline,
                        flightNo = formState.dropoffFlightNumber,
                        city = formState.dropoffDestinationCity,
                        airports = airports,
                        airlines = airlines,
                        onAirportChange = { 
                            screenState.updateForm(formState.copy(dropoffAirport = it))
                            // Clear validation error when user starts selecting
                            viewModel.clearValidationError("dropoff_airport")
                        },
                        onAirlineChange = { 
                            screenState.updateForm(formState.copy(dropoffAirline = it))
                            // Clear validation error when user starts selecting
                            viewModel.clearValidationError("dropoff_airline")
                        },
                        onFlightNoChange = { screenState.updateForm(formState.copy(dropoffFlightNumber = it)) },
                        onCityChange = { screenState.updateForm(formState.copy(dropoffDestinationCity = it)) },
                        isOrigin = false,
                        onAirportSelected = { airportName, _ ->
                            val apt = airports.find { it.displayName.equals(airportName, true) }
                            val lat = apt?.lat
                            val lng = apt?.long
                            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                                screenState.setDropoffLocationData(apt?.country, lat to lng)
                                // Clear location error when valid airport is selected
                                screenState.clearLocationError()
                            }
                        },
                        hasAirlineError = hasError("dropoff_airline"),
                        hasFlightError = false, // Dropoff flight is not required
                        airlineErrorMessage = getErrorMessage("dropoff_airline"),
                        flightErrorMessage = null
                    )
                }
            }
            transferType in setOf("airport_to_cruise_port", "city_to_cruise_port") -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LocationAutocomplete(
                            label = "DROPOFF ADDRESS (PORT)",
                            value = formState.dropoffAddress,
                            onValueChange = { screenState.updateForm(formState.copy(dropoffAddress = it)) },
                            onLocationSelected = { fullAddress, _, _, _, displayText, lat, lng, countryCode, _ ->
                                screenState.updateForm(formState.copy(dropoffAddress = displayText))
                                if (lat != null && lng != null) {
                                    screenState.setDropoffLocationData(countryCode, lat to lng)
                                }
                                // Clear location error when valid location is selected
                                screenState.clearLocationError()
                            },
                            placeholder = "Search port location",
                            isRequired = true,
                            modifier = Modifier.fillMaxWidth(),
                            errorMessage = screenState.locationValidationError
                        )
                    }
                    CruiseFields(
                        portName = formState.cruisePort,
                        shipName = formState.cruiseShipName,
                        time = formState.shipArrivalTime,
                        onPortChange = { 
                            screenState.updateForm(formState.copy(cruisePort = it))
                            viewModel.clearValidationError("cruise_port")
                        },
                        onShipChange = { 
                            screenState.updateForm(formState.copy(cruiseShipName = it))
                            viewModel.clearValidationError("cruise_ship_name")
                        },
                        onTimeClick = { screenState.onShowShipTimePickerChange(true) },
                        portErrorMessage = getErrorMessage("cruise_port"),
                        shipErrorMessage = getErrorMessage("cruise_ship_name")
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocationAutocomplete(
                        label = "DROP-OFF ADDRESS",
                        value = formState.dropoffAddress,
                        onValueChange = { screenState.updateForm(formState.copy(dropoffAddress = it)) },
                        onLocationSelected = { fullAddress, _, _, _, displayText, lat, lng, countryCode, _ ->
                            screenState.updateForm(formState.copy(dropoffAddress = displayText))
                            if (lat != null && lng != null) {
                                screenState.setDropoffLocationData(countryCode, lat to lng)
                            }
                            // Clear location error when valid location is selected
                            screenState.clearLocationError()
                        },
                        placeholder = "Search drop-off location",
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth(),
                        errorMessage = screenState.locationValidationError
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraStopsSection(
    formState: BookingFormState,
    onFormChange: (BookingFormState) -> Unit,
    viewModel: EditBookingViewModel,
    locationValidationError: String?,
    clearLocationError: () -> Unit
) {
    // Logic to ensure main addresses are filled before adding stops
    val areMainLocationsFilled = formState.pickupAddress.isNotBlank() ||
            formState.pickupAirport.isNotBlank() ||
            formState.cruisePort.isNotBlank()

    // Render existing stops
    formState.extraStops.forEachIndexed { index, stop ->
        if (index > 0) Spacer(modifier = Modifier.height(Constants.FIELD_SPACING))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                LocationAutocomplete(
                    label = "STOP ${index + 1} ADDRESS",
                    value = stop.address,
                    onValueChange = { newText ->
                        val list = formState.extraStops.toMutableList()
                        list[index] = list[index].copy(address = newText)
                        onFormChange(formState.copy(extraStops = list))
                    },
                    onLocationSelected = { fullAddress, _, _, _, displayText, lat, lng, _, _ ->
                        val list = formState.extraStops.toMutableList()
                        list[index] = list[index].copy(
                            address = displayText,
                            latitude = lat,
                            longitude = lng
                        )
                        onFormChange(formState.copy(extraStops = list))
                        // Clear location error when valid location is selected
                        clearLocationError()
                    },
                    placeholder = "Search stop location",
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = locationValidationError
                )
            }
            IconButton(
                onClick = {
                    val list = formState.extraStops.toMutableList()
                    list.removeAt(index)
                    onFormChange(formState.copy(extraStops = list))
                    // Clear location validation error when extra stop is deleted
                    clearLocationError()
                    // Trigger extra stops change to force rate recalculation
                    viewModel.triggerExtraStopsChange()
                    Timber.tag("BookingFlow").d("Extra stop deleted at index $index, clearing validation error and triggering rate recalculation")
                },
                modifier = Modifier.padding(top = 18.dp, start = 4.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Stop", tint = Color.Red)
            }
        }
    }

    Spacer(modifier = Modifier.height(Constants.SECTION_SPACING))

    Button(
        onClick = {
            val list = formState.extraStops.toMutableList()
            list.add(ExtraStopFormState())
            onFormChange(formState.copy(extraStops = list))
            // Trigger extra stops change to ensure proper tracking
            viewModel.triggerExtraStopsChange()
        },
        enabled = areMainLocationsFilled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Text("Add Extra Stop", fontSize = 14.sp)
    }
}

@Composable
private fun TravelTimeSection(
    screenState: ScreenStateHolder,
    viewModel: EditBookingViewModel,
    preview: AdminBookingPreviewData
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val extraStopCoords = remember(screenState.formState.extraStops) {
        screenState.formState.extraStops.mapNotNull { stop ->
            if (stop.latitude != null && stop.longitude != null) {
                stop.latitude to stop.longitude
            } else null
        }
    }
    
    // Observe state changes to trigger recomposition when distance/duration updates
    val distance = remember(
        screenState.pickupCoords,
        screenState.dropoffCoords,
        extraStopCoords,
        uiState.hasLocationChanged,
        uiState.calculatedDistance,
        uiState.originalApiDistance
    ) {
        viewModel.getDisplayDistance(
            pickupCoord = screenState.pickupCoords,
            dropoffCoord = screenState.dropoffCoords,
            extraStopCoordinates = extraStopCoords
        )
    }
    
    val duration = remember(
        screenState.pickupCoords,
        screenState.dropoffCoords,
        extraStopCoords,
        uiState.hasLocationChanged,
        uiState.calculatedDuration,
        uiState.originalApiDuration
    ) {
        viewModel.getDisplayDuration(
            pickupCoord = screenState.pickupCoords,
            dropoffCoord = screenState.dropoffCoords,
            extraStopCoordinates = extraStopCoords
        )
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "DURATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF808080),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = duration,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "DISTANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF808080),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = distance,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AirportFields(
    airport: String,
    airline: String,
    flightNo: String,
    city: String,
    airports: List<MobileDataAirport>,
    airlines: List<MobileDataAirline>,
    onAirportChange: (String) -> Unit,
    onAirlineChange: (String) -> Unit,
    onFlightNoChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    isOrigin: Boolean,
    onAirportSelected: (String, Pair<Double, Double>?) -> Unit,
    hasAirlineError: Boolean = false,
    hasFlightError: Boolean = false,
    airlineErrorMessage: String? = null,
    flightErrorMessage: String? = null
) {
    val airportOptions = remember(airports) { airports.map { it.displayName } }
    val airlineOptions = remember(airlines) { airlines.map { it.displayName } }

        CommonDropdown(
            label = "SELECT AIRPORT",
            placeholder = "Select airport",
            selectedValue = airport,
            options = airportOptions,
            onValueSelected = { selected ->
                onAirportChange(selected)
                onAirportSelected(selected, null)
            },
            isRequired = true,
            searchable = true
        )
        CommonDropdown(
            label = "SELECT AIRLINE",
            placeholder = "Select airline",
            selectedValue = airline,
            options = airlineOptions,
            onValueSelected = onAirlineChange,
            isRequired = true,
            searchable = true,
            errorMessage = airlineErrorMessage
        )
        CommonTextField(
            label = "FLIGHT / TAIL #",
            placeholder = "Enter flight number",
            text = flightNo,
            onValueChange = onFlightNoChange,
            errorMessage = flightErrorMessage
        )
        if(isOrigin) {
            CommonTextField(
                label = "ORIGIN AIRPORT / CITY",
                placeholder = "Enter city",
                text = city,
                onValueChange = onCityChange,
                isRequired = true
            )
        }
}

@Composable
private fun CruiseFields(
    portName: String,
    shipName: String,
    time: String,
    onPortChange: (String) -> Unit,
    onShipChange: (String) -> Unit,
    onTimeClick: () -> Unit,
    portErrorMessage: String? = null,
    shipErrorMessage: String? = null
) {
        CommonTextField(
            label = "CRUISE PORT",
            placeholder = "Enter cruise port",
            text = portName,
            onValueChange = onPortChange,
            isRequired = true,
            errorMessage = portErrorMessage
        )
        CommonTextField(
            label = "CRUISE SHIP NAME",
            placeholder = "Enter ship name",
            text = shipName,
            onValueChange = onShipChange,
            isRequired = true,
            errorMessage = shipErrorMessage
        )
        TimeField(
        label = "SHIP ARRIVAL TIME",
        value = time,
        onClick = onTimeClick,
        isRequired = true
    )
}

@Composable
private fun SaveButtonBar(enabled: Boolean, isLoading: Boolean, onSave: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().imePadding(),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Constants.BUTTON_PADDING_HORIZONTAL, vertical = Constants.BUTTON_PADDING_VERTICAL)
                    .height(Constants.BUTTON_HEIGHT),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) ShimmerCircle(size = 20.dp, strokeWidth = 2.dp)
                else Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun DateTimePickers(
    showDatePicker: Boolean, showTimePicker: Boolean, showShipTimePicker: Boolean,
    pickupDate: String, pickupTime: String, shipArrivalTime: String,
    onDateSelected: (Date) -> Unit, onTimeSelected: (Date) -> Unit, onShipTimeSelected: (Date) -> Unit,
    onDismissDatePicker: () -> Unit, onDismissTimePicker: () -> Unit, onDismissShipTimePicker: () -> Unit
) {
    if (showDatePicker) DatePickerDialog(selectedDate = BookingFormUtils.parseDateOrToday(pickupDate), onDateSelected = onDateSelected, onDismiss = onDismissDatePicker)
    if (showTimePicker) TimePickerDialog(selectedTime = BookingFormUtils.parseTimeOrNow(pickupTime), onTimeSelected = onTimeSelected, onDismiss = onDismissTimePicker)
    if (showShipTimePicker) TimePickerDialog(selectedTime = BookingFormUtils.parseTimeOrNow(shipArrivalTime), onTimeSelected = onShipTimeSelected, onDismiss = onDismissShipTimePicker)
}


@Composable
private fun LoadingState() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ShimmerCircle(size = 32.dp) } }

@Composable
private fun ErrorState(error: String?) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = error ?: "Error", color = MaterialTheme.colorScheme.error) } }

@Composable
private fun EmptyState() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "No data available") } }

// ============================================================================
// UTILITIES (Business Logic)
// ============================================================================

private object BookingFormUtils {

    // --- Validation Logic ---
    fun validateLocationConflict(
        pickupAddress: String,
        dropoffAddress: String,
        pickupCountry: String?,
        dropoffCountry: String?,
        transferType: String
    ): String? {
        val transferKey = transferTypeKey(transferType)
        
        // Determine which addresses are required based on transfer type
        val needsPickupAddress = transferKey !in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port", "cruise_to_airport", "cruise_port_to_city")
        val needsDropoffAddress = transferKey !in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport", "airport_to_cruise_port", "city_to_cruise_port")
        
        // Skip validation if required addresses are not filled yet
        if (needsPickupAddress && pickupAddress.isBlank()) return null
        if (needsDropoffAddress && dropoffAddress.isBlank()) return null
        
        // Only validate if both addresses are present
        if (needsPickupAddress && needsDropoffAddress && pickupAddress.isNotBlank() && dropoffAddress.isNotBlank()) {
            // 1. Check identical addresses
            if (pickupAddress.equals(dropoffAddress, ignoreCase = true)) {
                return "Pickup and drop-off cannot be the same location."
            }

            // 2. Check Country Mismatch (only if both countries are available)
            val nPickup = normalizeCountry(pickupCountry)
            val nDropoff = normalizeCountry(dropoffCountry)

            if (nPickup != null && nDropoff != null && nPickup != nDropoff) {
                return "Pickup and drop-off must be in the same country."
            }
        }
        
        // Also validate when one is address and other is airport (mixed types)
        if (needsPickupAddress && !needsDropoffAddress && pickupAddress.isNotBlank() && dropoffAddress.isNotBlank()) {
            // Pickup is address, dropoff is airport - check country match
            val nPickup = normalizeCountry(pickupCountry)
            val nDropoff = normalizeCountry(dropoffCountry)
            if (nPickup != null && nDropoff != null && nPickup != nDropoff) {
                return "Pickup and drop-off must be in the same country."
            }
        }
        
        if (!needsPickupAddress && needsDropoffAddress && pickupAddress.isNotBlank() && dropoffAddress.isNotBlank()) {
            // Pickup is airport, dropoff is address - check country match
            val nPickup = normalizeCountry(pickupCountry)
            val nDropoff = normalizeCountry(dropoffCountry)
            if (nPickup != null && nDropoff != null && nPickup != nDropoff) {
                return "Pickup and drop-off must be in the same country."
            }
        }

        return null
    }

    private fun normalizeCountry(country: String?): String? {
        if (country.isNullOrBlank()) return null
        val normalized = country.trim().uppercase(Locale.ROOT)
            .replace(".", "").replace(",", "")

        val synonyms = mapOf(
            "USA" to "UNITED STATES", "US" to "UNITED STATES",
            "UK" to "UNITED KINGDOM", "UAE" to "UNITED ARAB EMIRATES"
        )
        return synonyms[normalized] ?: normalized
    }

    // --- Formatting & Mapping ---
    fun isCharterTour(type: String) = normalizeServiceType(type) == "Charter/Tour"

    fun isCharterHoursValid(type: String, hours: String): Boolean {
        if (!isCharterTour(type)) return true
        return (hours.toIntOrNull() ?: 0) >= Constants.MIN_CHARTER_HOURS
    }

    fun mapPreviewToFormState(preview: AdminBookingPreviewData): BookingFormState {
        return BookingFormState(
            serviceType = normalizeServiceType(preview.serviceType.orEmpty()),
            transferType = normalizeTransferType(preview.transferType.orEmpty()),
            passengerName = preview.passengerName.orEmpty(),
            passengerEmail = preview.passengerEmail.orEmpty(),
            passengerCellIsd = preview.passengerCellIsd.takeIf { !it.isNullOrBlank() } ?: Constants.DEFAULT_CELL_ISD,
            passengerCell = preview.passengerCell.orEmpty(),
            numberOfVehicles = preview.numberOfVehicles?.toString() ?: Constants.DEFAULT_VEHICLES,
            numberOfHours = if (isCharterTour(preview.serviceType.orEmpty())) preview.numberOfHours?.toString().orEmpty() else "",
            pickupDate = preview.pickupDate.orEmpty(),
            pickupTime = normalizeTimeToHHmmss(preview.pickupTime.orEmpty()),
            meetGreetChoice = preview.meetGreetChoiceName.takeIf { !it.isNullOrBlank() }
                ?: preview.meetGreetChoices?.let { choices ->
                    // Convert 1-based integer to 0-based index for MEET_GREET_OPTIONS
                    if (choices > 0 && choices <= MEET_GREET_OPTIONS.size) {
                        MEET_GREET_OPTIONS[choices - 1]
                    } else {
                        ""
                    }
                } ?: "",
            pickupAddress = preview.pickupAddress.orEmpty(),
            dropoffAddress = preview.dropoffAddress.orEmpty(),
            extraStops = preview.extraStops.orEmpty().map {
                ExtraStopFormState(
                    address = it.address.orEmpty(),
                    rate = it.rate.orEmpty(),
                    instructions = it.bookingInstructions.orEmpty(),
                    latitude = it.latitude?.toDoubleOrNull(),
                    longitude = it.longitude?.toDoubleOrNull()
                )
            },
            pickupAirport = preview.pickupAirportName.takeIf { !it.isNullOrBlank() } ?: preview.pickupAirport.orEmpty(),
            pickupAirline = preview.pickupAirlineName.takeIf { !it.isNullOrBlank() } ?: preview.pickupAirline.orEmpty(),
            pickupFlightNumber = preview.pickupFlight.orEmpty(),
            pickupOriginCity = preview.originAirportCity.orEmpty(),
            dropoffAirport = preview.dropoffAirportName.takeIf { !it.isNullOrBlank() } ?: preview.dropoffAirport.orEmpty(),
            dropoffAirline = preview.dropoffAirlineName.takeIf { !it.isNullOrBlank() } ?: preview.dropoffAirline.orEmpty(),
            dropoffFlightNumber = preview.dropoffFlight.orEmpty(),
            dropoffDestinationCity = preview.departingAirportCity.orEmpty(),
            cruisePort = preview.cruisePort.orEmpty(),
            cruiseShipName = preview.cruiseName.orEmpty(),
            shipArrivalTime = preview.cruiseTime.orEmpty(),
            specialInstructions = preview.bookingInstructions.orEmpty()
        )
    }

    fun normalizeServiceType(raw: String): String {
        val t = raw.trim()
        return when {
            t.equals("one way", true) || t.equals("one way?", true) || t.equals("oneway", true) -> "One Way"
            t.contains("charter", true) || t.contains("tour", true) -> "Charter/Tour"
            else -> t
        }
    }

    fun normalizeTransferType(raw: String): String {
        val t = raw.trim()
        val mapped = TRANSFER_TYPE_OPTIONS.firstOrNull { it.label.equals(t, true) }?.value
        return mapped ?: t
    }

    fun transferTypeKey(value: String) = normalizeTransferType(value).lowercase()

    fun serviceTypeOptions(current: String) = (listOf(normalizeServiceType(current)).filter { it.isNotBlank() } + listOf("One Way", "Charter/Tour")).distinct()

    fun transferTypeOptions(current: String) = (listOf(transferTypeLabelFromValueOrRaw(current)) + TRANSFER_TYPE_OPTIONS.map { it.label }).distinct()

    fun transferTypeLabelFromValueOrRaw(value: String) = TRANSFER_TYPE_OPTIONS.firstOrNull { it.value.equals(value, true) }?.label ?: value

    fun transferTypeValueFromLabelOrNull(label: String) = TRANSFER_TYPE_OPTIONS.firstOrNull { it.label.equals(label, true) }?.value

    fun meetGreetOptions(current: String, transferType: String): List<String> {
        val key = transferTypeKey(transferType)
        val filtered = when {
            key.contains("airport") -> listOf(MEET_GREET_OPTIONS[0], MEET_GREET_OPTIONS[2], MEET_GREET_OPTIONS[1])
            key.contains("cruise") -> listOf(MEET_GREET_OPTIONS[6], MEET_GREET_OPTIONS[1])
            else -> listOf(MEET_GREET_OPTIONS[1], MEET_GREET_OPTIONS[4])
        }
        return (listOf(current).filter { it.isNotBlank() } + filtered + MEET_GREET_OPTIONS).distinct()
    }

    fun serviceHoursForPayload(type: String, hours: String) = if (isCharterTour(type)) hours.toIntOrNull() else 0

    // Date Utilities
    fun parseDateOrToday(v: String): Date = runCatching { SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).parse(v) }.getOrNull() ?: Date()
    fun parseTimeOrNow(v: String): Date = runCatching { SimpleDateFormat(Constants.TIME_FORMAT, Locale.US).parse(v) }.getOrNull() ?: Date()
    fun formatDateYMD(d: Date): String = SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).format(d)
    fun formatTimeHHmmss(d: Date): String = SimpleDateFormat(Constants.TIME_FORMAT, Locale.US).format(d)
    fun normalizeTimeToHHmmss(time: String): String = runCatching { formatTimeHHmmss(parseTimeOrNow(time)) }.getOrDefault(time)

    private data class TransferTypeOption(val label: String, val value: String)
    private val TRANSFER_TYPE_OPTIONS = listOf(
        TransferTypeOption("Airport To City ?", "airport_to_city"),
        TransferTypeOption("Airport To Airport ?", "airport_to_airport"),
        TransferTypeOption("Airport To Cruise Port ?", "airport_to_cruise_port"),
        TransferTypeOption("City To City ?", "city_to_city"),
        TransferTypeOption("City To Airport ?", "city_to_airport"),
        TransferTypeOption("City To Cruise Port ?", "city_to_cruise_port"),
        TransferTypeOption("Cruise Port To Airport ?", "cruise_to_airport"),
        TransferTypeOption("Cruise Port To City ?", "cruise_port_to_city")
    )

    private val MEET_GREET_OPTIONS = listOf(
        "Driver -  Airport - Text/call after plane lands with curbside meet location",
        "Driver - Text/call when on location",
        "Driver - Airport - Inside baggage meet with pax name sign – Text/call meet location",
        "Driver -  Check-in hotel or flight desk",
        "Driver - Text/call passenger for curbside pick up and location",
        "Driver - Train Station – Gate Meet with name sign - text/call on location",
        "Driver - Cruise Ship – Text/call when on location"
    )
}