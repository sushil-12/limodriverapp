package com.limo1800driver.app.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.limo1800driver.app.ui.components.ShimmerCircle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.EditReservationExtraStopRequest
import com.limo1800driver.app.ui.booking.components.DatePickerDialog
import com.limo1800driver.app.ui.booking.components.TimePickerDialog
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonMenuHeader
import com.limo1800driver.app.ui.components.CommonRateCalculatorComponent
import com.limo1800driver.app.ui.components.CommonTextArea
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.LocationAutocomplete
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.viewmodel.EditBookingViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val passengerCellIsd: String = "+1",
    val passengerCell: String = "",
    val numberOfVehicles: String = "1",
    val numberOfHours: String = "",
    val pickupDate: String = "",
    val pickupTime: String = "",
    val meetGreetChoice: String = "",

    val pickupAddress: String = "",
    val dropoffAddress: String = "",
    val extraStops: List<ExtraStopFormState> = emptyList(),

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

@Composable
fun EditBookingDetailsAndRatesScreen(
    bookingId: Int,
    source: String,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val viewModel: EditBookingViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val dynamicRates = remember { mutableStateMapOf<String, String>() }
    val taxIsPercent = remember { mutableStateMapOf<String, Boolean>() }

    val numberOfHoursState = remember { mutableStateOf("") }
    val numberOfVehiclesState = remember { mutableStateOf("1") }

    var formState by remember { mutableStateOf(BookingFormState()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showShipTimePicker by remember { mutableStateOf(false) }
    
    // Location coordinate tracking for validation
    var pickupCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var dropoffCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var pickupCountry by remember { mutableStateOf<String?>(null) }
    var dropoffCountry by remember { mutableStateOf<String?>(null) }
    
    // Extra stop coordinate tracking
    var extraStopCoordinates by remember { mutableStateOf<Map<Int, Pair<Double, Double>>>(emptyMap()) }
    var extraStopCountries by remember { mutableStateOf<Map<Int, String?>>(emptyMap()) }
    var extraStopSelectedFlags by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    
    // Validation state
    var extraStopValidationErrors by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isPerformingSubmitValidation by remember { mutableStateOf(false) }
    var fieldErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var directionsValidationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bookingId) {
        Timber.tag("BookingFlow").d("🎬 EditBookingDetailsAndRatesScreen initialized")
        Timber.tag("BookingFlow").d("   - bookingId: $bookingId")
        Timber.tag("BookingFlow").d("   - source: $source")
        if (bookingId != 0) {
            Timber.tag("BookingFlow").d("📥 Loading booking data for bookingId: $bookingId")
            viewModel.load(bookingId)
        } else {
            Timber.tag("BookingFlow").w("⚠️ Invalid bookingId: $bookingId")
        }
    }

    // iOS parity: airports/airlines come from backend `/api/mobile-data`
    LaunchedEffect(Unit) {
        viewModel.loadMobileDataIfNeeded()
    }

    LaunchedEffect(state.preview) {
        val preview = state.preview ?: return@LaunchedEffect
        Timber.tag("BookingFlow").d("📋 Preview data loaded")
        Timber.tag("BookingFlow").d("   - pickupAddress: ${preview.pickupAddress}")
        Timber.tag("BookingFlow").d("   - dropoffAddress: ${preview.dropoffAddress}")
        Timber.tag("BookingFlow").d("   - pickupLatitude: ${preview.pickupLatitude}, pickupLongitude: ${preview.pickupLongitude}")
        Timber.tag("BookingFlow").d("   - dropoffLatitude: ${preview.dropoffLatitude}, dropoffLongitude: ${preview.dropoffLongitude}")
        Timber.tag("BookingFlow").d("   - dropoffAirportLatitude: ${preview.dropoffAirportLatitude}, dropoffAirportLongitude: ${preview.dropoffAirportLongitude}")
        Timber.tag("BookingFlow").d("   - transferType: ${preview.transferType}")
        
        val mapped = mapPreviewToFormState(preview)
        formState = mapped
        numberOfVehiclesState.value = mapped.numberOfVehicles.ifBlank { "1" }
        numberOfHoursState.value = mapped.numberOfHours
        
        Timber.tag("BookingFlow").d("📝 Form state mapped:")
        Timber.tag("BookingFlow").d("   - serviceType: ${mapped.serviceType}")
        Timber.tag("BookingFlow").d("   - transferType: ${mapped.transferType}")
        Timber.tag("BookingFlow").d("   - pickupDate (raw): ${preview.pickupDate}")
        Timber.tag("BookingFlow").d("   - pickupDate (mapped): ${mapped.pickupDate}")
        Timber.tag("BookingFlow").d("   - pickupDate (display): ${formatDateForDisplay(mapped.pickupDate)}")
        Timber.tag("BookingFlow").d("   - pickupTime (raw): ${preview.pickupTime}")
        Timber.tag("BookingFlow").d("   - pickupTime (mapped): ${mapped.pickupTime}")
        Timber.tag("BookingFlow").d("   - pickupTime (display): ${formatTimeForDisplay(mapped.pickupTime)}")
        
        // Initialize coordinates from preview
        // For pickup: use address coordinates (city_to_airport has city pickup)
        preview.pickupLatitude?.toDoubleOrNull()?.let { lat ->
            preview.pickupLongitude?.toDoubleOrNull()?.let { lon ->
                pickupCoordinates = Pair(lat, lon)
                Timber.tag("BookingFlow").d("✅ Pickup coordinates initialized: ($lat, $lon)")
            }
        }
        
        // For dropoff: use airport coordinates if transfer type is city_to_airport, airport_to_airport, or cruise_to_airport
        // Otherwise use address coordinates
        // Check both the raw preview value and the mapped display value
        val rawTransferType = preview.transferType.orEmpty().lowercase()
        val transferTypeKey = transferTypeKey(mapped.transferType)
        Timber.tag("BookingFlow").d("🔑 Transfer type - raw: '$rawTransferType', mapped: '${mapped.transferType}', key: '$transferTypeKey'")
        val isDropoffAirport = transferTypeKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") ||
                               rawTransferType in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport")
        Timber.tag("BookingFlow").d("   - isDropoffAirport: $isDropoffAirport")
        
        if (isDropoffAirport) {
            // Use airport coordinates for airport dropoff
            // Note: dropoffAirportLatitude/Longitude are already Double? in AdminBookingPreviewData
            val airportLat = preview.dropoffAirportLatitude
            val airportLon = preview.dropoffAirportLongitude
            Timber.tag("BookingFlow").d("   - dropoffAirportLat: $airportLat, dropoffAirportLon: $airportLon")
            if (airportLat != null && airportLon != null && airportLat != 0.0 && airportLon != 0.0) {
                dropoffCoordinates = Pair(airportLat, airportLon)
                Timber.tag("BookingFlow").d("✅ Dropoff airport coordinates initialized: ($airportLat, $airportLon)")
            } else {
                Timber.tag("BookingFlow").w("⚠️ Dropoff airport coordinates are invalid or (0.0, 0.0) - lat: $airportLat, lon: $airportLon")
                // Fallback to address coordinates if airport coordinates not available
                preview.dropoffLatitude?.toDoubleOrNull()?.let { lat ->
                    preview.dropoffLongitude?.toDoubleOrNull()?.let { lon ->
                        if (lat != 0.0 && lon != 0.0) {
                            dropoffCoordinates = Pair(lat, lon)
                            Timber.tag("BookingFlow").d("✅ Dropoff coordinates initialized from address (fallback): ($lat, $lon)")
                        }
                    }
                }
            }
        } else {
            // Use address coordinates for non-airport dropoff
            Timber.tag("BookingFlow").d("   - Using address coordinates for dropoff")
            preview.dropoffLatitude?.toDoubleOrNull()?.let { lat ->
                preview.dropoffLongitude?.toDoubleOrNull()?.let { lon ->
                    if (lat != 0.0 && lon != 0.0) {
                        dropoffCoordinates = Pair(lat, lon)
                        Timber.tag("BookingFlow").d("✅ Dropoff coordinates initialized: ($lat, $lon)")
                    }
                }
            }
        }
        
        // Initialize last known values in ViewModel to prevent false change detection
        // This ensures that the initial state is set correctly
        val initialExtraStopCoords = mapped.extraStops.mapIndexedNotNull { index, stop ->
            stop.latitude?.let { lat ->
                stop.longitude?.let { lon -> Pair(lat, lon) }
            }
        }
        val initialExtraStopAddresses = mapped.extraStops.map { it.address }
        
        viewModel.updateLastKnownValues(
            pickupAddress = mapped.pickupAddress,
            dropoffAddress = mapped.dropoffAddress,
            pickupCoord = pickupCoordinates,
            dropoffCoord = dropoffCoordinates,
            serviceType = mapped.serviceType,
            numberOfVehicles = mapped.numberOfVehicles,
            numberOfHours = mapped.numberOfHours,
            extraStopCoordinates = initialExtraStopCoords,
            extraStopAddresses = initialExtraStopAddresses
        )
        Timber.tag("BookingFlow").d("✅ Last known values initialized:")
        Timber.tag("BookingFlow").d("   - extraStops: ${initialExtraStopAddresses.size} (${initialExtraStopAddresses.joinToString(", ")})")
        Timber.tag("BookingFlow").d("   - extraStopCoords: ${initialExtraStopCoords.size}")
        
        // Mark initial load complete after a delay
        Timber.tag("BookingFlow").d("⏳ Waiting 1s before marking initial load complete...")
        kotlinx.coroutines.delay(1000)
        viewModel.markInitialLoadComplete()
        Timber.tag("BookingFlow").d("✅ Initial load marked complete")
    }

    // Observe rates updates from API and update dynamicRates
    LaunchedEffect(state.latestRatesUpdate) {
        val ratesUpdate = state.latestRatesUpdate
        if (ratesUpdate != null && ratesUpdate.isNotEmpty()) {
            Timber.tag("BookingFlow").d("💰 Updating dynamicRates from API response:")
            ratesUpdate.forEach { (key, value) ->
                Timber.tag("BookingFlow").d("   - $key: $value")
                dynamicRates[key] = value
            }
            Timber.tag("BookingFlow").d("✅ dynamicRates updated: $dynamicRates")
        }
    }

    // Update coordinates when airports are selected (for airport-based transfer types)
    LaunchedEffect(formState.pickupAirport, formState.dropoffAirport, state.airports) {
        val transferTypeKey = transferTypeKey(formState.transferType)
        
        // Update pickup coordinates if transfer type uses airport pickup
        if (transferTypeKey in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port")) {
            if (formState.pickupAirport.isNotBlank()) {
                val airportData = state.airports.firstOrNull { 
                    it.displayName.equals(formState.pickupAirport, ignoreCase = true) 
                }
                airportData?.let { airport ->
                    // MobileDataAirport uses 'lat' and 'long' properties (both Double?)
                    val lat = airport.lat
                    val lon = airport.long
                    if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                        pickupCoordinates = Pair(lat, lon)
                        pickupCountry = airport.country
                        Timber.tag("BookingFlow").d("✅ Pickup airport coordinates updated: ($lat, $lon)")
                    }
                }
            }
        }
        
        // Update dropoff coordinates if transfer type uses airport dropoff
        if (transferTypeKey in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport")) {
            if (formState.dropoffAirport.isNotBlank()) {
                val airportData = state.airports.firstOrNull { 
                    it.displayName.equals(formState.dropoffAirport, ignoreCase = true) 
                }
                airportData?.let { airport ->
                    // MobileDataAirport uses 'lat' and 'long' properties (both Double?)
                    val lat = airport.lat
                    val lon = airport.long
                    if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                        dropoffCoordinates = Pair(lat, lon)
                        dropoffCountry = airport.country
                        Timber.tag("BookingFlow").d("✅ Dropoff airport coordinates updated: ($lat, $lon)")
                    }
                }
            }
        }
    }

    // Validate route with Directions API when coordinates change
    LaunchedEffect(pickupCoordinates, dropoffCoordinates, extraStopCoordinates) {
        if (pickupCoordinates != null && dropoffCoordinates != null) {
            val extraStopCoordsList = extraStopCoordinates.values.toList()
            val error = viewModel.validateAddressesWithDirectionsApi(
                pickupCoordinates,
                dropoffCoordinates,
                extraStopCoordsList
            )
            directionsValidationError = error
            if (error != null) {
                val updated = fieldErrors.toMutableMap().apply {
                    // Only set error if addresses are filled
                    if (formState.pickupAddress.isNotBlank()) {
                        put("pickupAddress", error)
                    }
                    if (formState.dropoffAddress.isNotBlank()) {
                        put("dropoffAddress", error)
                    }
                }
                fieldErrors = updated
            } else {
                // Clear directions validation errors if route is valid
                val updated = fieldErrors.toMutableMap().apply {
                    if (get("pickupAddress") == directionsValidationError) {
                        remove("pickupAddress")
                    }
                    if (get("dropoffAddress") == directionsValidationError) {
                        remove("dropoffAddress")
                    }
                }
                fieldErrors = updated
                directionsValidationError = null
            }
        }
    }

    // If user switches to Charter/Tour, default hours to 2 (iOS behavior) and keep rates in sync.
    LaunchedEffect(formState.serviceType) {
        val normalized = normalizeServiceType(formState.serviceType)
        if (normalized.equals("Charter/Tour", ignoreCase = true)) {
            if (formState.numberOfHours.isBlank()) {
                formState = formState.copy(numberOfHours = "2")
                numberOfHoursState.value = "2"
            }
        } else {
            // One Way: hours are not applicable
            if (formState.numberOfHours.isNotBlank()) {
                formState = formState.copy(numberOfHours = "")
                numberOfHoursState.value = ""
            }
        }
    }

    LaunchedEffect(state.successMessage) {
        if (!state.successMessage.isNullOrBlank()) {
            viewModel.consumeSuccess()
            onCompleted()
        }
    }
    
    // Monitor location and parameter changes for rates recalculation
    // Use a debounce to avoid too many API calls
    // For extra stops, monitor both the count and addresses to detect additions/removals
    val extraStopsCount = formState.extraStops.size
    val extraStopsAddresses = formState.extraStops.map { it.address }.joinToString("|")
    val extraStopsCoords = extraStopCoordinates.values.toList().map { "${it.first},${it.second}" }.joinToString("|")
    
    LaunchedEffect(
        formState.pickupAddress,
        formState.dropoffAddress,
        formState.pickupAirport,
        formState.dropoffAirport,
        pickupCoordinates,
        dropoffCoordinates,
        formState.serviceType,
        formState.numberOfHours,
        formState.numberOfVehicles,
        formState.transferType,
        formState.pickupTime,
        extraStopsCount,
        extraStopsAddresses,
        extraStopsCoords
    ) {
        Timber.tag("BookingFlow").d("📍 LaunchedEffect triggered - pickupAddress: ${formState.pickupAddress}, dropoffAddress: ${formState.dropoffAddress}")
        Timber.tag("BookingFlow").d("📍 Coordinates - pickup: $pickupCoordinates, dropoff: $dropoffCoordinates")
        Timber.tag("BookingFlow").d("📍 isInitialLoad: ${state.isInitialLoad}")
        
        // Skip during initial load
        if (state.isInitialLoad) {
            Timber.tag("BookingFlow").d("⏭️ Skipping rates update - initial load in progress")
            return@LaunchedEffect
        }
        
        // Debounce to avoid too many API calls
        Timber.tag("BookingFlow").d("⏳ Debouncing rates update (500ms)...")
        kotlinx.coroutines.delay(500)
        
        val preview = state.preview
        if (preview == null) {
            Timber.tag("BookingFlow").w("⚠️ Skipping rates update - preview is null")
            return@LaunchedEffect
        }
        
        val vehicleId = preview.vehicleId
        if (vehicleId == null) {
            Timber.tag("BookingFlow").w("⚠️ Skipping rates update - vehicleId is null")
            return@LaunchedEffect
        }
        
        // Only trigger if we have valid coordinates for both pickup and dropoff
        if (pickupCoordinates == null || dropoffCoordinates == null) {
            Timber.tag("BookingFlow").w("⚠️ Skipping rates update - missing coordinates (pickup: $pickupCoordinates, dropoff: $dropoffCoordinates)")
            return@LaunchedEffect
        }
        
        // Collect extra stop coordinates in order
        val extraStopCoordsList = formState.extraStops.mapIndexedNotNull { index, stop ->
            extraStopCoordinates[index] ?: stop.latitude?.let { lat ->
                stop.longitude?.let { lon -> Pair(lat, lon) }
            }
        }
        val extraStopAddresses = formState.extraStops.map { it.address }
        
        Timber.tag("BookingFlow").d("🚀 Triggering rates update API call")
        Timber.tag("BookingFlow").d("   - bookingId: $bookingId")
        Timber.tag("BookingFlow").d("   - vehicleId: $vehicleId")
        Timber.tag("BookingFlow").d("   - pickupAddress: ${formState.pickupAddress}")
        Timber.tag("BookingFlow").d("   - dropoffAddress: ${formState.dropoffAddress}")
        Timber.tag("BookingFlow").d("   - pickupCoord: $pickupCoordinates")
        Timber.tag("BookingFlow").d("   - dropoffCoord: $dropoffCoordinates")
        Timber.tag("BookingFlow").d("   - serviceType: ${formState.serviceType}")
        Timber.tag("BookingFlow").d("   - numberOfVehicles: ${formState.numberOfVehicles}")
        Timber.tag("BookingFlow").d("   - numberOfHours: ${formState.numberOfHours}")
        Timber.tag("BookingFlow").d("   - transferType: ${formState.transferType}")
        Timber.tag("BookingFlow").d("   - pickupTime: ${formState.pickupTime}")
        Timber.tag("BookingFlow").d("   - extraStops: ${extraStopAddresses.size} (addresses: $extraStopAddresses)")
        Timber.tag("BookingFlow").d("   - extraStopCoords: ${extraStopCoordsList.size}")
        
        viewModel.checkLocationChangesAndUpdateRates(
            bookingId = bookingId,
            vehicleId = vehicleId,
            pickupAddress = formState.pickupAddress,
            dropoffAddress = formState.dropoffAddress,
            pickupCoord = pickupCoordinates,
            dropoffCoord = dropoffCoordinates,
            serviceType = formState.serviceType,
            numberOfVehicles = formState.numberOfVehicles,
            numberOfHours = formState.numberOfHours,
            transferType = formState.transferType,
            pickupTime = formState.pickupTime,
            extraStopCoordinates = extraStopCoordsList,
            extraStopAddresses = extraStopAddresses
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.White)) {
            CommonMenuHeader(
                title = "Edit a Booking (#$bookingId)",
                subtitle = "Booking Details",
                onBackClick = onBack
            )

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ShimmerCircle(size = 32.dp)
                    }
                }

                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                    }
                }

                state.preview == null || state.rates == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No data available")
                    }
                }

                else -> {
                    val rates = state.rates!!

                    val preview = state.preview!!
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            // Extra padding at bottom so sticky button doesn't cover content.
                            .padding(bottom = 100.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Location Error Banner
                        if (state.showLocationErrorBanner && state.locationValidationError != null) {
                            LocationErrorBanner(
                                message = state.locationValidationError!!,
                                onDismiss = { viewModel.hideLocationErrorBanner() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Single-screen flow (matches iOS EditBookingDetailsAndRatesView)

                        CommonDropdown(
                            label = "SERVICE TYPE",
                            placeholder = "Select Service Type",
                            selectedValue = formState.serviceType,
                            options = serviceTypeOptions(formState.serviceType),
                            onValueSelected = { selected ->
                                val normalized = normalizeServiceType(selected)
                                formState = formState.copy(serviceType = normalized)
                                // Clear error when field is updated
                                fieldErrors = fieldErrors.toMutableMap().apply { remove("serviceType") }
                                // Apply default immediately so the Hours field shows "2" once Charter/Tour is picked.
                                if (normalized.equals("Charter/Tour", ignoreCase = true) && formState.numberOfHours.isBlank()) {
                                    formState = formState.copy(numberOfHours = "2")
                                    numberOfHoursState.value = "2"
                                }
                            },
                            isRequired = true,
                            errorMessage = fieldErrors["serviceType"]
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CommonDropdown(
                            label = "TRANSFER TYPE",
                            placeholder = "Select Transfer Type",
                            selectedValue = transferTypeLabelFromValueOrRaw(formState.transferType),
                            options = transferTypeOptions(formState.transferType),
                            onValueSelected = { selectedLabel ->
                                val value = transferTypeValueFromLabelOrNull(selectedLabel) ?: selectedLabel
                                formState = formState.copy(transferType = value)
                                // Clear error when field is updated
                                fieldErrors = fieldErrors.toMutableMap().apply { remove("transferType") }
                            },
                            isRequired = true,
                            errorMessage = fieldErrors["transferType"]
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        // Client accounts + passenger details are intentionally hidden for this flow.

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                PickerField(
                                    label = "TRAVEL DATE",
                                    value = formatDateForDisplay(formState.pickupDate),
                                    placeholder = "Select Date",
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray) },
                                    onClick = { 
                                        Timber.tag("BookingFlow").d("📅 Date picker button clicked")
                                        showDatePicker = true 
                                    },
                                    isRequired = true,
                                    errorMessage = fieldErrors["pickupDate"]
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PickerField(
                                    label = "PICKUP TIME",
                                    value = formatTimeForDisplay(formState.pickupTime),
                                    placeholder = "Select Time",
                                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray) },
                                    onClick = { 
                                        Timber.tag("BookingFlow").d("🕐 Time picker button clicked")
                                        showTimePicker = true 
                                    },
                                    isRequired = true,
                                    errorMessage = fieldErrors["pickupTime"]
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        CommonTextField(
                            label = "NUMBER OF VEHICLES",
                            placeholder = "1",
                            text = formState.numberOfVehicles,
                            onValueChange = {
                                formState = formState.copy(numberOfVehicles = it)
                                numberOfVehiclesState.value = it
                                // Clear error when field is updated
                                fieldErrors = fieldErrors.toMutableMap().apply { remove("numberOfVehicles") }
                            },
                            keyboardType = KeyboardType.Number,
                            isRequired = true,
                            errorMessage = fieldErrors["numberOfVehicles"]
                        )
                        if (normalizeServiceType(formState.serviceType).equals("Charter/Tour", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CommonTextField(
                                label = "NUMBER OF HOURS",
                                placeholder = "Min 2 hours",
                                text = formState.numberOfHours,
                                onValueChange = {
                                    formState = formState.copy(numberOfHours = it)
                                    numberOfHoursState.value = it
                                    // Clear error when field is updated
                                    fieldErrors = fieldErrors.toMutableMap().apply { remove("numberOfHours") }
                                },
                                keyboardType = KeyboardType.Number,
                                isRequired = true,
                                errorMessage = fieldErrors["numberOfHours"]
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Distance and Time Information Section (matching iOS TravelInfoSection)
                        val transferTypeKey = transferTypeKey(formState.transferType)
                        val shouldShowDistanceAndTime = when {
                            transferTypeKey == "city_to_city" -> 
                                formState.pickupAddress.isNotBlank() && formState.dropoffAddress.isNotBlank()
                            transferTypeKey == "airport_to_city" -> 
                                formState.pickupAirport.isNotBlank() && formState.dropoffAddress.isNotBlank()
                            transferTypeKey == "city_to_airport" -> 
                                formState.pickupAddress.isNotBlank() && formState.dropoffAirport.isNotBlank()
                            transferTypeKey == "airport_to_airport" -> 
                                formState.pickupAirport.isNotBlank() && formState.dropoffAirport.isNotBlank()
                            transferTypeKey == "airport_to_cruise_port" -> 
                                formState.pickupAirport.isNotBlank() && formState.dropoffAddress.isNotBlank()
                            transferTypeKey == "city_to_cruise_port" -> 
                                formState.pickupAddress.isNotBlank() && formState.dropoffAddress.isNotBlank()
                            transferTypeKey == "cruise_to_airport" -> 
                                formState.pickupAddress.isNotBlank() && formState.dropoffAirport.isNotBlank()
                            transferTypeKey == "cruise_port_to_city" -> 
                                formState.pickupOriginCity.isNotBlank() && formState.dropoffAddress.isNotBlank()
                            else -> formState.pickupAddress.isNotBlank() && formState.dropoffAddress.isNotBlank()
                        }
                        
                        if (shouldShowDistanceAndTime) {
                            val extraStopCoordsList = formState.extraStops.mapIndexedNotNull { index, stop ->
                                extraStopCoordinates[index] ?: stop.latitude?.let { lat ->
                                    stop.longitude?.let { lon -> Pair(lat, lon) }
                                }
                            }
                            val displayDistance = viewModel.getDisplayDistance(
                                pickupCoordinates,
                                dropoffCoordinates,
                                extraStopCoordsList
                            )
                            val displayDuration = viewModel.getDisplayDuration(
                                pickupCoordinates,
                                dropoffCoordinates,
                                extraStopCoordsList
                            )
                            // iOS format: "time • distance" (time first)
                            val travelInfoSummary = "$displayDuration • $displayDistance"
                            
                            TravelInfoSection(
                                travelInfo = travelInfoSummary,
                                showDivider = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Locations (not wrapped in a card to match iOS)
                        DynamicLocationSection(
                            formState = formState,
                            onFormChange = { newState ->
                                formState = newState
                                // Validate locations when they change
                                validateLocations(newState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry, state.airports, viewModel)
                            },
                            onOpenShipTimePicker = { showShipTimePicker = true },
                            airports = state.airports,
                            airlines = state.airlines,
                            onPickupLocationSelected = { _, _, _, _, _, lat, lon, countryCode, _ ->
                                pickupCoordinates = lat?.let { l -> lon?.let { Pair(l, it) } }
                                pickupCountry = countryCode
                                validateLocations(formState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry, state.airports, viewModel)
                            },
                            onDropoffLocationSelected = { _, _, _, _, _, lat, lon, countryCode, _ ->
                                dropoffCoordinates = lat?.let { l -> lon?.let { Pair(l, it) } }
                                dropoffCountry = countryCode
                                validateLocations(formState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry, state.airports, viewModel)
                            },
                            onDropoffAirportSelected = { selectedAirport, coords ->
                                Timber.tag("BookingFlow").d("🛬 Dropoff airport selected in parent: $selectedAirport, coords: $coords")
                                if (coords != null) {
                                    dropoffCoordinates = coords
                                    // Find airport country
                                    val airportData = state.airports.firstOrNull { 
                                        it.displayName.equals(selectedAirport, ignoreCase = true) 
                                    }
                                    dropoffCountry = airportData?.country
                                    Timber.tag("BookingFlow").d("✅ Dropoff coordinates updated: $coords, country: ${dropoffCountry}")
                                    validateLocations(formState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry, state.airports, viewModel)
                                }
                            },
                            fieldErrors = fieldErrors,
                            onFieldErrorChange = { fieldErrors = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        // Extra Stops (no card wrapper; iOS shows a simple add button and fields appear after adding)
                        ExtraStopsSection(
                            formState = formState,
                            onFormChange = { newState ->
                                formState = newState
                            },
                            pickupCoordinates = pickupCoordinates,
                            dropoffCoordinates = dropoffCoordinates,
                            pickupCountry = pickupCountry,
                            dropoffCountry = dropoffCountry,
                            transferType = formState.transferType,
                            airports = state.airports,
                            extraStopCoordinates = extraStopCoordinates,
                            extraStopCountries = extraStopCountries,
                            extraStopSelectedFlags = extraStopSelectedFlags,
                            onExtraStopCoordinatesChange = { index, coord ->
                                extraStopCoordinates = extraStopCoordinates.toMutableMap().apply { put(index, coord) }
                                validateExtraStops(
                                    formState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry,
                                    state.airports, formState.transferType, isPerformingSubmitValidation,
                                    extraStopCoordinates.toMutableMap().apply { put(index, coord) },
                                    extraStopCountries, extraStopSelectedFlags
                                ) { errors ->
                                    extraStopValidationErrors = errors
                                }
                            },
                            onExtraStopCountryChange = { index, country ->
                                extraStopCountries = extraStopCountries.toMutableMap().apply { put(index, country) }
                                validateExtraStops(
                                    formState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry,
                                    state.airports, formState.transferType, isPerformingSubmitValidation,
                                    extraStopCoordinates, extraStopCountries.toMutableMap().apply { put(index, country) },
                                    extraStopSelectedFlags
                                ) { errors ->
                                    extraStopValidationErrors = errors
                                }
                            },
                            onExtraStopSelected = { index ->
                                extraStopSelectedFlags = extraStopSelectedFlags.toMutableMap().apply { put(index, true) }
                                validateExtraStops(
                                    formState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry,
                                    state.airports, formState.transferType, isPerformingSubmitValidation,
                                    extraStopCoordinates, extraStopCountries,
                                    extraStopSelectedFlags.toMutableMap().apply { put(index, true) }
                                ) { errors ->
                                    extraStopValidationErrors = errors
                                }
                            },
                            validationErrors = extraStopValidationErrors,
                            onValidationErrorsChange = { errors ->
                                extraStopValidationErrors = errors
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Meet & Greet (moved down; keep it right before Special Instructions)
                        CommonDropdown(
                            label = "MEET AND GREET CHOICES",
                            placeholder = "Select meet and greet",
                            selectedValue = formState.meetGreetChoice,
                            options = meetGreetOptions(
                                current = formState.meetGreetChoice,
                                transferType = formState.transferType
                            ),
                            onValueSelected = { 
                                formState = formState.copy(meetGreetChoice = it)
                                // Clear error when field is updated
                                fieldErrors = fieldErrors.toMutableMap().apply { remove("meetGreetChoice") }
                            },
                            isRequired = true,
                            errorMessage = fieldErrors["meetGreetChoice"]
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CommonTextArea(
                            label = "SPECIAL INSTRUCTIONS / EXACT BUILDING NAME",
                            placeholder = "Enter instructions",
                            text = formState.specialInstructions,
                            onValueChange = { formState = formState.copy(specialInstructions = it) },
                            minLines = 3,
                            maxLines = 6,
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        CommonRateCalculatorComponent(
                            rateArray = rates.rateArray,
                            dynamicRates = dynamicRates,
                            taxIsPercent = taxIsPercent,
                            serviceType = normalizeServiceType(formState.serviceType),
                            numberOfHours = numberOfHoursState,
                            numberOfVehicles = numberOfVehiclesState,
                            accountType = state.preview?.accountType,
                            createdBy = state.preview?.createdBy,
                            reservationType = state.preview?.reservationType,
                            currencySymbol = state.preview?.currencySymbol,
                            isEditable = true,
                            showSummary = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Sticky Bottom Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .imePadding(),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // Perform validation before saving
                        isPerformingSubmitValidation = true
                        
                        // Validate extra stops
                        validateExtraStops(
                            formState, pickupCoordinates, dropoffCoordinates, pickupCountry, dropoffCountry,
                            state.airports, formState.transferType, true,
                            extraStopCoordinates, extraStopCountries, extraStopSelectedFlags
                        ) { errors ->
                            extraStopValidationErrors = errors
                        }
                        
                        // Validate form
                        val validationErrors = FormValidationHelper.validateForm(formState)
                        fieldErrors = validationErrors
                        
                        // Check location validation
                        val locationError = LocationValidationHelper.locationConflictReason(
                            pickupAddress = formState.pickupAddress,
                            dropoffAddress = formState.dropoffAddress,
                            pickupAirport = formState.pickupAirport.takeIf { it.isNotBlank() },
                            dropoffAirport = formState.dropoffAirport.takeIf { it.isNotBlank() },
                            transferType = formState.transferType,
                            pickupCoord = pickupCoordinates,
                            dropoffCoord = dropoffCoordinates,
                            pickupCountryFromLocation = pickupCountry,
                            dropoffCountryFromLocation = dropoffCountry,
                            pickupAirportCountry = state.airports.firstOrNull { 
                                it.displayName.equals(formState.pickupAirport, ignoreCase = true) 
                            }?.country,
                            dropoffAirportCountry = state.airports.firstOrNull { 
                                it.displayName.equals(formState.dropoffAirport, ignoreCase = true) 
                            }?.country
                        )
                        
                        if (locationError != null) {
                            viewModel.setLocationValidationError(locationError)
                        }
                        
                        // Validate with Directions API during submit
                        var directionsError: String? = null
                        if (pickupCoordinates != null && dropoffCoordinates != null) {
                            val extraStopCoordsList = extraStopCoordinates.values.toList()
                            runBlocking {
                                directionsError = viewModel.validateAddressesWithDirectionsApi(
                                    pickupCoordinates,
                                    dropoffCoordinates,
                                    extraStopCoordsList
                                )
                                if (directionsError != null) {
                                    val updated = fieldErrors.toMutableMap().apply {
                                        put("pickupAddress", directionsError ?: "")
                                        put("dropoffAddress", directionsError ?: "")
                                    }
                                    fieldErrors = updated
                                }
                            }
                        }
                        
                        // If there are validation errors, don't proceed
                        if (validationErrors.isNotEmpty() || extraStopValidationErrors.isNotEmpty() || locationError != null || directionsError != null) {
                            isPerformingSubmitValidation = false
                            return@Button
                        }
                        
                        isPerformingSubmitValidation = false
                        
                        val preview = state.preview ?: return@Button
                        val payload: Map<String, Any> = dynamicRates.mapValues { (_, v) ->
                            v.toDoubleOrNull() ?: v
                        }

                        viewModel.saveEditReservation(
                            bookingId = bookingId,
                            pickupAddress = formState.pickupAddress,
                            dropoffAddress = formState.dropoffAddress,
                            pickupDate = formState.pickupDate,
                            pickupTime = formState.pickupTime,
                            vehicleId = preview.vehicleId,
                            rates = payload,
                            serviceType = normalizeServiceType(formState.serviceType),
                            transferType = formState.transferType,
                            numberOfHours = serviceHoursForPayload(formState.serviceType, formState.numberOfHours),
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
                            // Filter out empty extra stops before sending (matching iOS validExtraStopsAndRates logic)
                            extraStops = formState.extraStops
                                .filter { it.address.isNotBlank() }
                                .map {
                                    EditReservationExtraStopRequest(
                                        address = it.address,
                                        rate = it.rate.takeIf { it.isNotBlank() } ?: "out_town", // Default rate
                                        bookingInstructions = it.instructions.takeIf { it.isNotBlank() },
                                        latitude = it.latitude?.toString(),
                                        longitude = it.longitude?.toString()
                                    )
                                },
                            // Pass coordinates from preview (user may have changed addresses but coordinates come from preview)
                            pickupLatitude = preview.pickupLatitude,
                            pickupLongitude = preview.pickupLongitude,
                            dropoffLatitude = preview.dropoffLatitude,
                            dropoffLongitude = preview.dropoffLongitude,
                            // Airport/Airline IDs - these would need to be extracted from selected options
                            // For now, pass null and let builder use names
                            pickupAirportId = null,
                            dropoffAirportId = null,
                            pickupAirlineId = null,
                            dropoffAirlineId = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(50.dp),
                    enabled = !state.isSaving && isCharterHoursValid(formState.serviceType, formState.numberOfHours),
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (state.isSaving) {
                        ShimmerCircle(
                            size = 20.dp,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Monitor picker state changes
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            Timber.tag("BookingFlow").d("📅 showDatePicker changed to: true")
            Timber.tag("BookingFlow").d("   - Current pickupDate: '${formState.pickupDate}'")
            Timber.tag("BookingFlow").d("   - Display format: '${formatDateForDisplay(formState.pickupDate)}'")
        }
    }
    
    LaunchedEffect(showTimePicker) {
        if (showTimePicker) {
            Timber.tag("BookingFlow").d("🕐 showTimePicker changed to: true")
            Timber.tag("BookingFlow").d("   - Current pickupTime: '${formState.pickupTime}'")
            Timber.tag("BookingFlow").d("   - Display format: '${formatTimeForDisplay(formState.pickupTime)}'")
        }
    }

    // Dialogs - moved inside composable scope
    if (showDatePicker) {
        Timber.tag("BookingFlow").d("📅 DatePickerDialog showing")
        val initial = parseDateOrToday(formState.pickupDate)
        Timber.tag("BookingFlow").d("   - Initial date parsed: $initial")
        DatePickerDialog(
            selectedDate = initial,
            onDateSelected = { date ->
                val formattedDate = formatDateYMD(date)
                Timber.tag("BookingFlow").d("📅 Date selected: $formattedDate (from $date)")
                formState = formState.copy(pickupDate = formattedDate)
                showDatePicker = false
            },
            onDismiss = { 
                Timber.tag("BookingFlow").d("📅 DatePickerDialog dismissed")
                showDatePicker = false 
            }
        )
    }

    if (showTimePicker) {
        Timber.tag("BookingFlow").d("🕐 TimePickerDialog showing")
        val initial = parseTimeOrNow(formState.pickupTime)
        Timber.tag("BookingFlow").d("   - Initial time parsed: $initial")
        TimePickerDialog(
            selectedTime = initial,
            onTimeSelected = { date ->
                val formattedTime = formatTimeHHmmss(date)
                Timber.tag("BookingFlow").d("🕐 Time selected: $formattedTime (from $date)")
                formState = formState.copy(pickupTime = formattedTime)
                showTimePicker = false
            },
            onDismiss = { 
                Timber.tag("BookingFlow").d("🕐 TimePickerDialog dismissed")
                showTimePicker = false 
            }
        )
    }

    if (showShipTimePicker) {
        val initial = parseTimeOrNow(formState.shipArrivalTime)
        TimePickerDialog(
            selectedTime = initial,
            onTimeSelected = { date ->
                formState = formState.copy(shipArrivalTime = formatTimeHHmmss(date))
                showShipTimePicker = false
            },
            onDismiss = { showShipTimePicker = false }
        )
    }
}

private fun isCharterHoursValid(serviceType: String, numberOfHours: String): Boolean {
    val normalized = normalizeServiceType(serviceType)
    if (!normalized.equals("Charter/Tour", ignoreCase = true)) return true
    val hours = numberOfHours.toIntOrNull() ?: return false
    return hours >= 2
}

/**
 * Get service type display format matching iOS getServiceTypeDisplay
 */
private fun getServiceTypeDisplay(serviceType: String): String {
    val trimmed = serviceType.trim()
    val normalized = trimmed.lowercase()
        .replace("_", "")
        .replace("-", "")
        .replace(" ", "")
        .replace("?", "")
    
    return when {
        normalized.isEmpty() || normalized == "oneway" -> "One Way"
        normalized.contains("charter") || normalized.contains("tour") -> "Charter/Tour"
        else -> if (trimmed.isEmpty()) "One Way" else trimmed
    }
}

/**
 * Get transfer type display format matching iOS getTransferTypeDisplay
 */
private fun getTransferTypeDisplay(transferType: String): String {
    return when (transferType.lowercase()) {
        "airport_to_city" -> "Airport To City"
        "airport_to_airport" -> "Airport To Airport"
        "airport_to_cruise_port" -> "Airport To Cruise Port"
        "city_to_city" -> "City To City"
        "city_to_airport" -> "City To Airport"
        "city_to_cruise_port" -> "City To Cruise Port"
        "cruise_port_to_airport", "cruise_to_airport" -> "Cruise Port To Airport"
        "cruise_port_to_city" -> "Cruise Port to City"
        else -> transferType.replace("_", " ").split(" ").joinToString(" ") { 
            it.replaceFirstChar { char -> char.uppercaseChar() }
        }
    }
}

/**
 * Format date from API format (yyyy-MM-dd) to display format (MMM dd, yyyy)
 * Matching iOS formatPickupDate
 */
private fun formatDateForDisplay(dateString: String): String {
    if (dateString.isBlank()) return dateString
    
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Format time from API format (HH:mm:ss) to display format (h:mm a)
 * Matching iOS formatPickupTime
 */
private fun formatTimeForDisplay(timeString: String): String {
    if (timeString.isBlank()) return timeString
    
    return try {
        val inputFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val date = inputFormat.parse(timeString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            timeString
        }
    } catch (e: Exception) {
        timeString
    }
}

private fun mapPreviewToFormState(preview: AdminBookingPreviewData): BookingFormState {
    // Use display format functions matching iOS
    val serviceTypeDisplay = getServiceTypeDisplay(preview.serviceType.orEmpty())
    val transferTypeDisplay = getTransferTypeDisplay(preview.transferType.orEmpty())
    
    return BookingFormState(
        serviceType = serviceTypeDisplay,
        transferType = transferTypeDisplay,
        passengerName = preview.passengerName.orEmpty(),
        passengerEmail = preview.passengerEmail.orEmpty(),
        passengerCellIsd = preview.passengerCellIsd?.takeIf { it.isNotBlank() } ?: "+1",
        passengerCell = preview.passengerCell.orEmpty(),
        numberOfVehicles = preview.numberOfVehicles?.toString() ?: "1",
        numberOfHours = if (serviceTypeDisplay.equals("Charter/Tour", ignoreCase = true)) {
            // Default to 2 hours when backend has no value (iOS behavior)
            preview.numberOfHours?.toString().orEmpty().ifBlank { "2" }
        } else {
            ""
        },
        // Store dates/times in API format (yyyy-MM-dd, HH:mm:ss) for consistency
        // Format for display will be handled in the UI components
        // Normalize date/time to API format (yyyy-MM-dd and HH:mm:ss)
        // The preview might have them in different formats, so we parse and reformat
        pickupDate = normalizeDateToAPIFormat(preview.pickupDate.orEmpty()),
        pickupTime = normalizeTimeToAPIFormat(preview.pickupTime.orEmpty()),
        meetGreetChoice = preview.meetGreetChoiceName.orEmpty(),
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
        pickupAirport = preview.pickupAirportName ?: preview.pickupAirport.orEmpty(),
        pickupAirline = preview.pickupAirlineName ?: preview.pickupAirline.orEmpty(),
        pickupFlightNumber = preview.pickupFlight.orEmpty(),
        pickupOriginCity = preview.originAirportCity.orEmpty(),
        dropoffAirport = preview.dropoffAirportName ?: preview.dropoffAirport.orEmpty(),
        dropoffAirline = preview.dropoffAirlineName ?: preview.dropoffAirline.orEmpty(),
        dropoffFlightNumber = preview.dropoffFlight.orEmpty(),
        dropoffDestinationCity = preview.departingAirportCity.orEmpty(),
        cruisePort = preview.cruisePort.orEmpty(),
        cruiseShipName = preview.cruiseName.orEmpty(),
        shipArrivalTime = preview.cruiseTime.orEmpty(),
        specialInstructions = preview.bookingInstructions.orEmpty()
    )
}

private fun serviceTypeOptions(current: String): List<String> {
    // iOS EditBookingDetailsAndRatesView supports only:
    // - One Way
    // - Charter/Tour
    val normalizedCurrent = normalizeServiceType(current)
    val base = listOf("One Way", "Charter/Tour")
    return (listOf(normalizedCurrent).filter { it.isNotBlank() } + base).distinct()
}

private fun transferTypeOptions(current: String): List<String> {
    val baseLabels = transferTypeDropdownOptions().map { it.label }
    val currentLabel = transferTypeLabelFromValueOrNull(current) ?: current
    return (listOf(currentLabel).filter { it.isNotBlank() } + baseLabels).distinct()
}

private fun meetGreetOptions(current: String, transferType: String): List<String> {
    // Options provided by you (iOS-aligned)
    val all = listOf(
        "Driver -  Airport - Text/call after plane lands with curbside meet location",
        "Driver - Text/call when on location",
        "Driver - Airport - Inside baggage meet with pax name sign – Text/call meet location",
        "Driver -  Check-in hotel or flight desk",
        "Driver - Text/call passenger for curbside pick up and location",
        "Driver - Train Station – Gate Meet with name sign - text/call on location",
        "Driver - Cruise Ship – Text/call when on location"
    )

    val key = transferTypeKey(transferType)
    val filtered = when {
        // Any airport involvement
        key.contains("airport") -> listOf(
            all[0],
            all[2],
            all[1],
            all[4],
            all[3]
        )
        // Any cruise involvement
        key.contains("cruise") -> listOf(
            all[6],
            all[1],
            all[4]
        )
        // City-to-city or unknown: keep generic + common pickup patterns
        else -> listOf(
            all[1],
            all[4],
            all[3],
            all[5]
        )
    }

    // Keep current selection visible even if it doesn't match filter (backend may return legacy values)
    return (listOf(current).filter { it.isNotBlank() } + filtered + all).distinct()
}

@Composable
private fun BookingIdPillRow(
    bookingId: Int,
    accountType: String?
) {
    val tag = when {
        accountType.isNullOrBlank() -> ""
        accountType.contains("travel", ignoreCase = true) -> "TA"
        accountType.contains("corporate", ignoreCase = true) -> "CA"
        else -> accountType.take(2).uppercase()
    }.let { if (it.isBlank()) "" else " ($it)" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("BOOKING", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.size(10.dp))
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "#$bookingId$tag",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun TwoColStaticInfoRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row {
            Text(leftLabel, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.size(4.dp))
            Text(leftValue, color = Color.Black, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
        Row {
            Text(rightLabel, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.size(4.dp))
            Text(rightValue, color = Color.Black, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

private data class TransferTypeOption(val label: String, val value: String)

// Transfer type dropdown options (label/value) — provided by you and matched to iOS.
private fun transferTypeDropdownOptions(): List<TransferTypeOption> = listOf(
    TransferTypeOption(label = "Airport To City ?", value = "airport_to_city"),
    TransferTypeOption(label = "Airport To Airport ?", value = "airport_to_airport"),
    TransferTypeOption(label = "Airport To Cruise Port ?", value = "airport_to_cruise_port"),
    TransferTypeOption(label = "City To City ?", value = "city_to_city"),
    TransferTypeOption(label = "City To Airport ?", value = "city_to_airport"),
    TransferTypeOption(label = "City To Cruise Port ?", value = "city_to_cruise_port"),
    TransferTypeOption(label = "Cruise Port To Airport ?", value = "cruise_to_airport"),
    TransferTypeOption(label = "Cruise Port To City ?", value = "cruise_port_to_city")
)

private fun transferTypeValueFromLabelOrNull(label: String): String? {
    val trimmed = label.trim()
    // Try exact match first
    return transferTypeDropdownOptions().firstOrNull { it.label.equals(trimmed, ignoreCase = true) }?.value
        // If no match, try without question mark (for display format)
        ?: transferTypeDropdownOptions().firstOrNull { 
            it.label.replace("?", "").trim().equals(trimmed, ignoreCase = true) 
        }?.value
        // If still no match, try with question mark added
        ?: transferTypeDropdownOptions().firstOrNull { 
            trimmed.equals(it.label.replace("?", "").trim(), ignoreCase = true) 
        }?.value
}

private fun transferTypeLabelFromValueOrNull(value: String): String? =
    transferTypeDropdownOptions().firstOrNull { it.value.equals(value.trim(), ignoreCase = true) }?.label

private fun transferTypeLabelFromValueOrRaw(value: String): String =
    transferTypeLabelFromValueOrNull(value) ?: value

private fun normalizeTransferType(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    // If already an API value, keep it.
    if (transferTypeLabelFromValueOrNull(trimmed) != null) return trimmed
    // If it's a label, convert to API value.
    transferTypeValueFromLabelOrNull(trimmed)?.let { return it }
    // Handle legacy values we used earlier
    return when (trimmed.lowercase(Locale.getDefault())) {
        "airport_to_cruise" -> "airport_to_cruise_port"
        "city_to_cruise" -> "city_to_cruise_port"
        "cruise_to_city" -> "cruise_port_to_city"
        else -> trimmed
    }
    // Otherwise, keep raw.
}

private fun transferTypeKey(value: String): String {
    val normalized = normalizeTransferType(value)
    val result = normalized.lowercase(Locale.getDefault())
    Timber.tag("BookingFlow").d("🔧 transferTypeKey: '$value' -> normalizeTransferType: '$normalized' -> key: '$result'")
    return result
}

private fun normalizeServiceType(raw: String): String {
    val t = raw.trim()
    if (t.isBlank()) return ""
    return when {
        t.equals("one way", ignoreCase = true) -> "One Way"
        t.equals("one way?", ignoreCase = true) -> "One Way"
        t.contains("charter", ignoreCase = true) -> "Charter/Tour"
        t.contains("tour", ignoreCase = true) -> "Charter/Tour"
        else -> t
    }
}

private fun serviceHoursForPayload(serviceType: String, numberOfHours: String): Int? {
    val normalized = normalizeServiceType(serviceType)
    return if (normalized.equals("Charter/Tour", ignoreCase = true)) {
        numberOfHours.toIntOrNull()
    } else {
        0
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black
            )
            content()
        }
    }
}

@Composable
private fun BookingServiceSection(
    formState: BookingFormState,
    onFormChange: (BookingFormState) -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit
) {
    CommonTextField(
        label = "SERVICE TYPE",
        placeholder = "Select Service Type",
        text = formState.serviceType,
        onValueChange = { onFormChange(formState.copy(serviceType = it)) },
        isRequired = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    CommonTextField(
        label = "TRANSFER TYPE",
        placeholder = "Select Transfer Type",
        text = formState.transferType,
        onValueChange = { onFormChange(formState.copy(transferType = it)) },
        isRequired = true
    )

    if (formState.serviceType.contains("charter", ignoreCase = true)) {
        Spacer(modifier = Modifier.height(12.dp))
        CommonTextField(
            label = "NUMBER OF HOURS",
            placeholder = "Min 2 hours",
            text = formState.numberOfHours,
            onValueChange = { onFormChange(formState.copy(numberOfHours = it)) },
            keyboardType = KeyboardType.Number,
            isRequired = true
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    CommonTextField(
        label = "MEET AND GREET CHOICES",
        placeholder = "Select meet and greet",
        text = formState.meetGreetChoice,
        onValueChange = { onFormChange(formState.copy(meetGreetChoice = it)) },
        isRequired = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) {
            PickerField(
                label = "TRAVEL DATE",
                value = formatDateForDisplay(formState.pickupDate),
                placeholder = "Select Date",
                icon = { androidx.compose.material3.Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray) },
                onClick = onOpenDatePicker,
                isRequired = true
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            PickerField(
                label = "PICKUP TIME",
                value = formatTimeForDisplay(formState.pickupTime),
                placeholder = "Select Time",
                icon = { androidx.compose.material3.Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray) },
                onClick = onOpenTimePicker,
                isRequired = true
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    CommonTextField(
        label = "NUMBER OF VEHICLES",
        placeholder = "1",
        text = formState.numberOfVehicles,
        onValueChange = { onFormChange(formState.copy(numberOfVehicles = it)) },
        keyboardType = KeyboardType.Number,
        isRequired = true
    )
}

/**
 * Validate locations and update ViewModel state
 */
private fun validateLocations(
    formState: BookingFormState,
    pickupCoord: Pair<Double, Double>?,
    dropoffCoord: Pair<Double, Double>?,
    pickupCountryFromLocation: String?,
    dropoffCountryFromLocation: String?,
    airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport>,
    viewModel: EditBookingViewModel
) {
    // Get airport countries if applicable
    val pickupAirportCountry = if (formState.pickupAirport.isNotBlank()) {
        airports.firstOrNull { it.displayName.equals(formState.pickupAirport, ignoreCase = true) }?.country
    } else null
    
    val dropoffAirportCountry = if (formState.dropoffAirport.isNotBlank()) {
        airports.firstOrNull { it.displayName.equals(formState.dropoffAirport, ignoreCase = true) }?.country
    } else null
    
    val error = LocationValidationHelper.locationConflictReason(
        pickupAddress = formState.pickupAddress,
        dropoffAddress = formState.dropoffAddress,
        pickupAirport = formState.pickupAirport.takeIf { it.isNotBlank() },
        dropoffAirport = formState.dropoffAirport.takeIf { it.isNotBlank() },
        transferType = formState.transferType,
        pickupCoord = pickupCoord,
        dropoffCoord = dropoffCoord,
        pickupCountryFromLocation = pickupCountryFromLocation,
        dropoffCountryFromLocation = dropoffCountryFromLocation,
        pickupAirportCountry = pickupAirportCountry,
        dropoffAirportCountry = dropoffAirportCountry
    )
    
    viewModel.setLocationValidationError(error)
}

@Composable
private fun DynamicLocationSection(
    formState: BookingFormState,
    onFormChange: (BookingFormState) -> Unit,
    onOpenShipTimePicker: () -> Unit,
    airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport>,
    airlines: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirline>,
    onPickupLocationSelected: ((String, String, String, String, String, Double?, Double?, String?, String) -> Unit)? = null,
    onDropoffLocationSelected: ((String, String, String, String, String, Double?, Double?, String?, String) -> Unit)? = null,
    onDropoffAirportSelected: ((String, Pair<Double, Double>?) -> Unit)? = null,
    fieldErrors: Map<String, String>,
    onFieldErrorChange: (Map<String, String>) -> Unit
) {
    val transferType = transferTypeKey(formState.transferType)
    Timber.tag("BookingFlow").d("🎯 DynamicLocationSection - transferType: '${formState.transferType}' -> key: '$transferType'")

    Text("Pickup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(8.dp))

    when {
        transferType in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port") -> {
            AirportFields(
                airport = formState.pickupAirport,
                airline = formState.pickupAirline,
                flightNo = formState.pickupFlightNumber,
                city = formState.pickupOriginCity,
                airports = airports,
                airlines = airlines,
                onAirportChange = { 
                    onFormChange(formState.copy(pickupAirport = it))
                    // Clear error when field is updated
                    val updated = fieldErrors.toMutableMap().apply { remove("pickupAirport") }
                    onFieldErrorChange(updated)
                },
                onAirlineChange = { 
                    onFormChange(formState.copy(pickupAirline = it))
                    // Clear error when field is updated
                    val updated = fieldErrors.toMutableMap().apply { remove("pickupAirline") }
                    onFieldErrorChange(updated)
                },
                onFlightNoChange = { onFormChange(formState.copy(pickupFlightNumber = it)) },
                onCityChange = { 
                    onFormChange(formState.copy(pickupOriginCity = it))
                    // Clear error when field is updated
                    val updated = fieldErrors.toMutableMap().apply { remove("pickupOriginCity") }
                    onFieldErrorChange(updated)
                },
                isOrigin = true,
                onAirportSelected = { selectedAirport, coords ->
                    Timber.tag("BookingFlow").d("🛫 Pickup airport selected: $selectedAirport, coords: $coords")
                    // Update pickup coordinates if this is an airport pickup
                    // Note: For airport pickup, coordinates come from airport data, not location autocomplete
                    // This will be handled by the LaunchedEffect monitoring formState.pickupAirport
                },
                airportError = fieldErrors["pickupAirport"],
                airlineError = fieldErrors["pickupAirline"],
                cityError = fieldErrors["pickupOriginCity"]
            )
        }
        transferType in setOf("cruise_to_airport", "cruise_port_to_city") -> {
            CruiseFields(
                portName = formState.cruisePort,
                shipName = formState.cruiseShipName,
                time = formState.shipArrivalTime,
                onPortChange = { onFormChange(formState.copy(cruisePort = it)) },
                onShipChange = { onFormChange(formState.copy(cruiseShipName = it)) },
                onTimeClick = onOpenShipTimePicker
            )
        }
        else -> {
            LocationAutocomplete(
                label = "PICKUP ADDRESS",
                value = formState.pickupAddress,
                onValueChange = { newValue ->
                    Timber.tag("BookingFlow").d("✏️ Pickup address changed: $newValue")
                    onFormChange(formState.copy(pickupAddress = newValue))
                    // Clear error when user types
                    val updated = fieldErrors.toMutableMap().apply { remove("pickupAddress") }
                    onFieldErrorChange(updated)
                },
                onLocationSelected = { fullAddress, name, city, state, displayText, latitude, longitude, countryCode, placeLabel ->
                    Timber.tag("BookingFlow").d("📍 Pickup location selected:")
                    Timber.tag("BookingFlow").d("   - displayText: $displayText")
                    Timber.tag("BookingFlow").d("   - latitude: $latitude, longitude: $longitude")
                    Timber.tag("BookingFlow").d("   - countryCode: $countryCode")
                    
                    // Only accept selection if coordinates are present (validation)
                    if (latitude != null && longitude != null) {
                        Timber.tag("BookingFlow").d("✅ Valid pickup location with coordinates")
                        onFormChange(formState.copy(pickupAddress = displayText))
                        onPickupLocationSelected?.invoke(fullAddress, name, city, state, displayText, latitude, longitude, countryCode, placeLabel)
                        // Clear error on valid selection
                        val updated = fieldErrors.toMutableMap().apply { remove("pickupAddress") }
                        onFieldErrorChange(updated)
                    } else {
                        Timber.tag("BookingFlow").w("⚠️ Invalid pickup location - missing coordinates")
                        // Show error if coordinates are missing
                        val updated = fieldErrors.toMutableMap().apply { 
                            put("pickupAddress", "Please select a valid location from the suggestions")
                        }
                        onFieldErrorChange(updated)
                    }
                },
                placeholder = "Search pickup location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                errorMessage = fieldErrors["pickupAddress"]
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Dropoff", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(8.dp))

    when {
        transferType in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> {
            Timber.tag("BookingFlow").d("🛫 Showing AirportFields for dropoff (transferType: $transferType)")
            AirportFields(
                airport = formState.dropoffAirport,
                airline = formState.dropoffAirline,
                flightNo = formState.dropoffFlightNumber,
                city = formState.dropoffDestinationCity,
                airports = airports,
                airlines = airlines,
                onAirportChange = { 
                    onFormChange(formState.copy(dropoffAirport = it))
                    // Clear error when field is updated
                    val updated = fieldErrors.toMutableMap().apply { remove("dropoffAirport") }
                    onFieldErrorChange(updated)
                },
                onAirlineChange = { 
                    onFormChange(formState.copy(dropoffAirline = it))
                    // Clear error when field is updated
                    val updated = fieldErrors.toMutableMap().apply { remove("dropoffAirline") }
                    onFieldErrorChange(updated)
                },
                onFlightNoChange = { onFormChange(formState.copy(dropoffFlightNumber = it)) },
                onCityChange = { 
                    onFormChange(formState.copy(dropoffDestinationCity = it))
                    // Clear error when field is updated
                    val updated = fieldErrors.toMutableMap().apply { remove("dropoffDestinationCity") }
                    onFieldErrorChange(updated)
                },
                isOrigin = false,
                onAirportSelected = { selectedAirport, coords ->
                    Timber.tag("BookingFlow").d("🛬 Dropoff airport selected: $selectedAirport, coords: $coords")
                    // Update dropoff coordinates when airport is selected
                    onDropoffAirportSelected?.invoke(selectedAirport, coords)
                },
                airportError = fieldErrors["dropoffAirport"],
                airlineError = fieldErrors["dropoffAirline"],
                cityError = fieldErrors["dropoffDestinationCity"]
            )
        }
        transferType in setOf("airport_to_cruise_port", "city_to_cruise_port") -> {
            LocationAutocomplete(
                label = "DROPOFF ADDRESS (PORT)",
                value = formState.dropoffAddress,
                onValueChange = { newValue ->
                    Timber.tag("BookingFlow").d("✏️ Dropoff address (port) changed: $newValue")
                    onFormChange(formState.copy(dropoffAddress = newValue))
                    // Clear error when user types
                    val updated = fieldErrors.toMutableMap().apply { remove("dropoffAddress") }
                    onFieldErrorChange(updated)
                },
                onLocationSelected = { fullAddress, name, city, state, displayText, latitude, longitude, countryCode, placeLabel ->
                    Timber.tag("BookingFlow").d("📍 Dropoff location (port) selected:")
                    Timber.tag("BookingFlow").d("   - displayText: $displayText")
                    Timber.tag("BookingFlow").d("   - latitude: $latitude, longitude: $longitude")
                    Timber.tag("BookingFlow").d("   - countryCode: $countryCode")
                    
                    // Only accept selection if coordinates are present (validation)
                    if (latitude != null && longitude != null) {
                        Timber.tag("BookingFlow").d("✅ Valid dropoff location (port) with coordinates")
                        onFormChange(formState.copy(dropoffAddress = displayText))
                        onDropoffLocationSelected?.invoke(fullAddress, name, city, state, displayText, latitude, longitude, countryCode, placeLabel)
                        // Clear error on valid selection
                        val updated = fieldErrors.toMutableMap().apply { remove("dropoffAddress") }
                        onFieldErrorChange(updated)
                    } else {
                        Timber.tag("BookingFlow").w("⚠️ Invalid dropoff location (port) - missing coordinates")
                        // Show error if coordinates are missing
                        val updated = fieldErrors.toMutableMap().apply { 
                            put("dropoffAddress", "Please select a valid location from the suggestions")
                        }
                        onFieldErrorChange(updated)
                    }
                },
                placeholder = "Search port location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                errorMessage = fieldErrors["dropoffAddress"]
            )
            Spacer(modifier = Modifier.height(12.dp))
            CruiseFields(
                portName = formState.cruisePort,
                shipName = formState.cruiseShipName,
                time = formState.shipArrivalTime,
                onPortChange = { onFormChange(formState.copy(cruisePort = it)) },
                onShipChange = { onFormChange(formState.copy(cruiseShipName = it)) },
                onTimeClick = onOpenShipTimePicker
            )
        }
        else -> {
            LocationAutocomplete(
                label = "DROP-OFF ADDRESS",
                value = formState.dropoffAddress,
                onValueChange = { newValue ->
                    Timber.tag("BookingFlow").d("✏️ Dropoff address changed: $newValue")
                    onFormChange(formState.copy(dropoffAddress = newValue))
                    // Clear error when user types
                    val updated = fieldErrors.toMutableMap().apply { remove("dropoffAddress") }
                    onFieldErrorChange(updated)
                },
                onLocationSelected = { fullAddress, name, city, state, displayText, latitude, longitude, countryCode, placeLabel ->
                    Timber.tag("BookingFlow").d("📍 Dropoff location selected:")
                    Timber.tag("BookingFlow").d("   - displayText: $displayText")
                    Timber.tag("BookingFlow").d("   - latitude: $latitude, longitude: $longitude")
                    Timber.tag("BookingFlow").d("   - countryCode: $countryCode")
                    
                    // Only accept selection if coordinates are present (validation)
                    if (latitude != null && longitude != null) {
                        Timber.tag("BookingFlow").d("✅ Valid dropoff location with coordinates")
                        onFormChange(formState.copy(dropoffAddress = displayText))
                        onDropoffLocationSelected?.invoke(fullAddress, name, city, state, displayText, latitude, longitude, countryCode, placeLabel)
                        // Clear error on valid selection
                        val updated = fieldErrors.toMutableMap().apply { remove("dropoffAddress") }
                        onFieldErrorChange(updated)
                    } else {
                        Timber.tag("BookingFlow").w("⚠️ Invalid dropoff location - missing coordinates")
                        // Show error if coordinates are missing
                        val updated = fieldErrors.toMutableMap().apply { 
                            put("dropoffAddress", "Please select a valid location from the suggestions")
                        }
                        onFieldErrorChange(updated)
                    }
                },
                placeholder = "Search drop-off location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                errorMessage = fieldErrors["dropoffAddress"]
            )
        }
    }
}

/**
 * Validate extra stops and update validation errors
 */
private fun validateExtraStops(
    formState: BookingFormState,
    pickupCoord: Pair<Double, Double>?,
    dropoffCoord: Pair<Double, Double>?,
    pickupCountry: String?,
    dropoffCountry: String?,
    airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport>,
    transferType: String,
    isPerformingSubmitValidation: Boolean,
    extraStopCoordinates: Map<Int, Pair<Double, Double>>,
    extraStopCountries: Map<Int, String?>,
    extraStopSelectedFlags: Map<Int, Boolean>,
    onValidationErrorsChange: (Map<Int, String>) -> Unit
) {
    val errors = mutableMapOf<Int, String>()
    
    // Get airport countries if applicable
    val pickupAirportCountry = if (formState.pickupAirport.isNotBlank()) {
        airports.firstOrNull { it.displayName.equals(formState.pickupAirport, ignoreCase = true) }?.country
    } else null
    
    val dropoffAirportCountry = if (formState.dropoffAirport.isNotBlank()) {
        airports.firstOrNull { it.displayName.equals(formState.dropoffAirport, ignoreCase = true) }?.country
    } else null
    
    formState.extraStops.forEachIndexed { index, stop ->
        if (stop.address.isNotBlank()) {
            val stopCoord = extraStopCoordinates[index]
            val stopCountry = extraStopCountries[index]
            val isSelected = extraStopSelectedFlags[index] == true
            
            // Check selection requirement at submit time
            if (isPerformingSubmitValidation && !isSelected) {
                errors[index] = "Please select the extra stop from suggestions so we can validate it."
            } else {
                // Validate the stop
                val error = LocationValidationHelper.validateExtraStop(
                    stopAddress = stop.address,
                    stopCoord = stopCoord,
                    stopCountry = stopCountry,
                    pickupAddress = formState.pickupAddress,
                    dropoffAddress = formState.dropoffAddress,
                    pickupAirport = formState.pickupAirport.takeIf { it.isNotBlank() },
                    dropoffAirport = formState.dropoffAirport.takeIf { it.isNotBlank() },
                    transferType = transferType,
                    pickupCoord = pickupCoord,
                    dropoffCoord = dropoffCoord,
                    pickupCountryFromLocation = pickupCountry,
                    dropoffCountryFromLocation = dropoffCountry,
                    pickupAirportCountry = pickupAirportCountry,
                    dropoffAirportCountry = dropoffAirportCountry,
                    isPerformingSubmitValidation = isPerformingSubmitValidation
                )
                
                if (error != null) {
                    errors[index] = error
                }
            }
        }
    }
    
    onValidationErrorsChange(errors)
}

@Composable
private fun ExtraStopsSection(
    formState: BookingFormState,
    onFormChange: (BookingFormState) -> Unit,
    pickupCoordinates: Pair<Double, Double>? = null,
    dropoffCoordinates: Pair<Double, Double>? = null,
    pickupCountry: String? = null,
    dropoffCountry: String? = null,
    transferType: String = "",
    airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport> = emptyList(),
    extraStopCoordinates: Map<Int, Pair<Double, Double>> = emptyMap(),
    extraStopCountries: Map<Int, String?> = emptyMap(),
    extraStopSelectedFlags: Map<Int, Boolean> = emptyMap(),
    onExtraStopCoordinatesChange: ((Int, Pair<Double, Double>) -> Unit)? = null,
    onExtraStopCountryChange: ((Int, String?) -> Unit)? = null,
    onExtraStopSelected: ((Int) -> Unit)? = null,
    validationErrors: Map<Int, String> = emptyMap(),
    onValidationErrorsChange: ((Map<Int, String>) -> Unit)? = null
) {
    val transferType = transferTypeKey(formState.transferType)
    val hasPickup = when {
        transferType in setOf("airport_to_city", "airport_to_airport", "airport_to_cruise_port") -> formState.pickupAirport.isNotBlank()
        transferType in setOf("cruise_to_airport", "cruise_port_to_city") -> formState.cruisePort.isNotBlank()
        else -> formState.pickupAddress.isNotBlank()
    }
    val hasDropoff = when {
        transferType in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> formState.dropoffAirport.isNotBlank()
        else -> formState.dropoffAddress.isNotBlank()
    }
    val areMainLocationsFilled = hasPickup && hasDropoff

    // Render existing stops first (better UX: user sees current stops before the add CTA).
    if (formState.extraStops.isNotEmpty()) {
        formState.extraStops.forEachIndexed { index, stop ->
            if (index > 0) Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                        onLocationSelected = { fullAddress, name, city, state, displayText, latitude, longitude, countryCode, placeLabel ->
                            Timber.tag("BookingFlow").d("📍 Extra stop $index location selected:")
                            Timber.tag("BookingFlow").d("   - displayText: $displayText")
                            Timber.tag("BookingFlow").d("   - latitude: $latitude, longitude: $longitude")
                            Timber.tag("BookingFlow").d("   - countryCode: $countryCode")
                            
                            // Only accept selection if coordinates are present (validation)
                            if (latitude != null && longitude != null) {
                                val coord = Pair(latitude, longitude)
                                
                                // Validate country immediately before accepting the selection
                                val pickupAirportCountry = if (formState.pickupAirport.isNotBlank()) {
                                    airports.firstOrNull { 
                                        it.displayName.equals(formState.pickupAirport, ignoreCase = true) 
                                    }?.country
                                } else null
                                
                                val dropoffAirportCountry = if (formState.dropoffAirport.isNotBlank()) {
                                    airports.firstOrNull { 
                                        it.displayName.equals(formState.dropoffAirport, ignoreCase = true) 
                                    }?.country
                                } else null
                                
                                // Validate the extra stop immediately
                                val validationError = LocationValidationHelper.validateExtraStop(
                                    stopAddress = displayText,
                                    stopCoord = coord,
                                    stopCountry = countryCode,
                                    pickupAddress = formState.pickupAddress,
                                    dropoffAddress = formState.dropoffAddress,
                                    pickupAirport = formState.pickupAirport.takeIf { it.isNotBlank() },
                                    dropoffAirport = formState.dropoffAirport.takeIf { it.isNotBlank() },
                                    transferType = transferType,
                                    pickupCoord = pickupCoordinates,
                                    dropoffCoord = dropoffCoordinates,
                                    pickupCountryFromLocation = pickupCountry,
                                    dropoffCountryFromLocation = dropoffCountry,
                                    pickupAirportCountry = pickupAirportCountry,
                                    dropoffAirportCountry = dropoffAirportCountry,
                                    isPerformingSubmitValidation = false // Validate immediately on selection
                                )
                                
                                if (validationError == null) {
                                    // Validation passed - accept the selection
                                    Timber.tag("BookingFlow").d("✅ Valid extra stop location with coordinates")
                                    val list = formState.extraStops.toMutableList()
                                    list[index] = list[index].copy(
                                        address = displayText,
                                        latitude = latitude,
                                        longitude = longitude
                                    )
                                    onFormChange(formState.copy(extraStops = list))
                                    
                                    // Track coordinates and country
                                    onExtraStopCoordinatesChange?.invoke(index, coord)
                                    onExtraStopCountryChange?.invoke(index, countryCode)
                                    onExtraStopSelected?.invoke(index)
                                    
                                    // Clear error on valid selection
                                    val updated = validationErrors.toMutableMap().apply { remove(index) }
                                    onValidationErrorsChange?.invoke(updated)
                                } else {
                                    // Validation failed - show error and reject selection
                                    Timber.tag("BookingFlow").w("⚠️ Invalid extra stop location - $validationError")
                                    val updated = validationErrors.toMutableMap().apply { 
                                        put(index, validationError)
                                    }
                                    onValidationErrorsChange?.invoke(updated)
                                }
                            } else {
                                Timber.tag("BookingFlow").w("⚠️ Invalid extra stop location - missing coordinates")
                                // Show error if coordinates are missing
                                val updated = validationErrors.toMutableMap().apply { 
                                    put(index, "Please select a valid location from the suggestions")
                                }
                                onValidationErrorsChange?.invoke(updated)
                            }
                        },
                        placeholder = "Search stop location",
                        modifier = Modifier.fillMaxWidth(),
                        errorMessage = validationErrors[index]
                    )
                }

                IconButton(
                    onClick = {
                        val list = formState.extraStops.toMutableList()
                        list.removeAt(index)
                        onFormChange(formState.copy(extraStops = list))
                    },
                    modifier = Modifier.padding(top = 18.dp, start = 4.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove Stop",
                        tint = Color.Red
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Add button moved to the bottom (matches iOS flow and reads better after the list).
    Button(
        onClick = {
            val list = formState.extraStops.toMutableList()
            list.add(ExtraStopFormState())
            onFormChange(formState.copy(extraStops = list))
        },
        enabled = areMainLocationsFilled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(40.dp)
    ) {
        Text(
            text = "Add Extra Stop",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        )
    }

    if (!areMainLocationsFilled) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter pickup and drop off locations first to add stops.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AirportFields(
    airport: String,
    airline: String,
    flightNo: String,
    city: String,
    airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport>,
    airlines: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirline>,
    onAirportChange: (String) -> Unit,
    onAirlineChange: (String) -> Unit,
    onFlightNoChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    isOrigin: Boolean,
    onAirportSelected: ((String, Pair<Double, Double>?) -> Unit)? = null,
    airportError: String? = null,
    airlineError: String? = null,
    cityError: String? = null
) {
    val airportOptions = remember(airports) { airports.map { it.displayName } }
    val airlineOptions = remember(airlines) { airlines.map { it.displayName } }

    CommonDropdown(
        label = "SELECT AIRPORT",
        placeholder = "Select airport",
        selectedValue = airport,
        options = airportOptions,
        onValueSelected = { selectedAirport ->
            Timber.tag("BookingFlow").d("🛫 Airport selected: $selectedAirport")
            onAirportChange(selectedAirport)
            // Find airport coordinates
            val selectedAirportData = airports.firstOrNull { it.displayName.equals(selectedAirport, ignoreCase = true) }
            val coords = selectedAirportData?.let { 
                // MobileDataAirport uses 'lat' and 'long' properties (both Double?)
                val lat = it.lat
                val lon = it.long
                if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                    Pair(lat, lon)
                } else null
            }
            Timber.tag("BookingFlow").d("   - Airport coordinates: $coords")
            onAirportSelected?.invoke(selectedAirport, coords)
        },
        isRequired = true,
        searchable = true,
        errorMessage = airportError
    )
    Spacer(modifier = Modifier.height(8.dp))
    CommonDropdown(
        label = "SELECT AIRLINE",
        placeholder = "Select airline",
        selectedValue = airline,
        options = airlineOptions,
        onValueSelected = { selectedAirline ->
            Timber.tag("BookingFlow").d("✈️ Airline selected: $selectedAirline")
            onAirlineChange(selectedAirline)
        },
        isRequired = true,
        searchable = true,
        errorMessage = airlineError
    )
    Spacer(modifier = Modifier.height(8.dp))
    CommonTextField(
        label = "FLIGHT / TAIL #",
        placeholder = "Enter flight number",
        text = flightNo,
        onValueChange = onFlightNoChange
    )
    Spacer(modifier = Modifier.height(8.dp))
    if(isOrigin) {
        CommonTextField(
            label = if (isOrigin) "ORIGIN AIRPORT / CITY" else "DESTINATION AIRPORT / CITY",
            placeholder = "Enter city",
            text = city,
            onValueChange = onCityChange,
            isRequired = true,
            errorMessage = cityError
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
    onTimeClick: () -> Unit
) {
    CommonTextField(
        label = "CRUISE PORT",
        placeholder = "Enter cruise port",
        text = portName,
        onValueChange = onPortChange,
        isRequired = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    CommonTextField(
        label = "CRUISE SHIP NAME",
        placeholder = "Enter ship name",
        text = shipName,
        onValueChange = onShipChange,
        isRequired = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    PickerField(
        label = "SHIP ARRIVAL TIME",
        value = time,
        placeholder = "Select Time",
        icon = { androidx.compose.material3.Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray) },
        onClick = onTimeClick,
        isRequired = true
    )
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    placeholder: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    isRequired: Boolean = false,
    errorMessage: String? = null
) {
    // Use pointerInput to intercept taps before they reach CommonTextField
    // This ensures the entire field is clickable, not just the icon
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures {
                    Timber.tag("BookingFlow").d("📅 PickerField tapped: $label, value: '$value'")
                    onClick()
                }
            }
    ) {
        CommonTextField(
            label = label,
            placeholder = placeholder,
            text = value,
            onValueChange = {},
            isRequired = isRequired,
            readOnly = true,
            trailingIcon = {
                // Icon is also clickable as a fallback
                Box(
                    modifier = Modifier.clickable { 
                        Timber.tag("BookingFlow").d("📅 PickerField icon clicked: $label")
                        onClick() 
                    }
                ) {
                    icon()
                }
            },
            errorMessage = errorMessage
        )
    }
}

private fun parseDateOrToday(value: String): Date {
    if (value.isBlank()) return Date()
    
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 12)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    // Try API format first (yyyy-MM-dd) - parse directly to avoid timezone issues
    val apiFormatRegex = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    apiFormatRegex.find(value)?.let { matchResult ->
        val year = matchResult.groupValues[1].toIntOrNull()
        val month = matchResult.groupValues[2].toIntOrNull()
        val day = matchResult.groupValues[3].toIntOrNull()
        if (year != null && month != null && day != null && month in 1..12 && day in 1..31) {
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1) // Calendar months are 0-based
            calendar.set(Calendar.DAY_OF_MONTH, day)
            return calendar.time
        }
    }
    
    // Try display format (MMM dd, yyyy) - e.g., "Jan 01, 2026"
    val displayFormatRegex = Regex("""([A-Za-z]{3})\s+(\d{1,2}),\s+(\d{4})""")
    displayFormatRegex.find(value)?.let { matchResult ->
        val monthName = matchResult.groupValues[1]
        val day = matchResult.groupValues[2].toIntOrNull()
        val year = matchResult.groupValues[3].toIntOrNull()
        if (day != null && year != null) {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIndex = monthNames.indexOf(monthName)
            if (monthIndex >= 0 && day in 1..31) {
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthIndex)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                return calendar.time
            }
        }
    }
    
    // Try other formats using SimpleDateFormat as fallback
    val formats = listOf(
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy",
        "yyyy-MM-dd HH:mm:ss"
    )
    
    for (formatStr in formats) {
        val format = SimpleDateFormat(formatStr, Locale.getDefault())
        format.isLenient = false
        runCatching { 
            val parsed = format.parse(value)
            if (parsed != null) {
                val parsedCalendar = Calendar.getInstance()
                parsedCalendar.time = parsed
                calendar.set(Calendar.YEAR, parsedCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, parsedCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, parsedCalendar.get(Calendar.DAY_OF_MONTH))
                return calendar.time
            }
        }
    }
    
    // Fallback to today
    return Date()
}

private fun parseTimeOrNow(value: String): Date {
    if (value.isBlank()) return Calendar.getInstance().time
    
    // Try multiple time formats
    val formats = listOf(
        "HH:mm:ss",
        "HH:mm",
        "h:mm a",
        "h:mm:ss a",
        "HH:mm:ss.SSS",
        "HHmm"
    )
    
    for (f in formats) {
        val fmt = SimpleDateFormat(f, Locale.getDefault())
        val parsed = runCatching { fmt.parse(value) }.getOrNull()
        if (parsed != null) return parsed
    }
    
    // Fallback to current time
    return Calendar.getInstance().time
}

private fun formatDateYMD(date: Date): String {
    // Use Calendar to extract date components directly to avoid timezone issues
    val calendar = Calendar.getInstance()
    calendar.time = date
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
}

private fun formatTimeHHmmss(date: Date): String {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return fmt.format(date)
}

/**
 * Normalize date to API format (yyyy-MM-dd)
 * Accepts various input formats and converts to API format
 * Uses direct parsing to avoid timezone issues
 */
private fun normalizeDateToAPIFormat(dateString: String): String {
    if (dateString.isBlank()) return ""
    
    // If already in API format, return as-is
    val apiFormatRegex = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    apiFormatRegex.find(dateString)?.let { matchResult ->
        val year = matchResult.groupValues[1].toIntOrNull()
        val month = matchResult.groupValues[2].toIntOrNull()
        val day = matchResult.groupValues[3].toIntOrNull()
        if (year != null && month != null && day != null && month in 1..12 && day in 1..31) {
            return dateString // Already in correct format
        }
    }
    
    // Try display format (MMM dd, yyyy) - e.g., "Jan 01, 2026"
    val displayFormatRegex = Regex("""([A-Za-z]{3})\s+(\d{1,2}),\s+(\d{4})""")
    displayFormatRegex.find(dateString)?.let { matchResult ->
        val monthName = matchResult.groupValues[1]
        val day = matchResult.groupValues[2].toIntOrNull()
        val year = matchResult.groupValues[3].toIntOrNull()
        if (day != null && year != null) {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIndex = monthNames.indexOf(monthName)
            if (monthIndex >= 0 && day in 1..31) {
                return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthIndex + 1, day)
            }
        }
    }
    
    // Try other formats using SimpleDateFormat as fallback
    val inputFormats = listOf(
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy",
        "yyyy-MM-dd HH:mm:ss"
    )
    
    for (formatStr in inputFormats) {
        val format = SimpleDateFormat(formatStr, Locale.getDefault())
        format.isLenient = false
        runCatching {
            val date = format.parse(dateString)
            if (date != null) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
            }
        }
    }
    
    // If can't parse, return as-is (might already be in correct format)
    return dateString
}

/**
 * Normalize time to API format (HH:mm:ss)
 * Accepts various input formats and converts to API format
 */
private fun normalizeTimeToAPIFormat(timeString: String): String {
    if (timeString.isBlank()) return ""
    
    // Try to parse various formats and convert to API format
    val apiFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val inputFormats = listOf(
        "HH:mm:ss",            // API format
        "HH:mm",               // Short API format
        "h:mm a",              // Display format (12-hour with AM/PM)
        "h:mm:ss a",           // Display format with seconds
        "HHmm"                 // Compact format
    )
    
    for (formatStr in inputFormats) {
        val format = SimpleDateFormat(formatStr, Locale.getDefault())
        runCatching {
            val date = format.parse(timeString)
            if (date != null) return apiFormat.format(date)
        }
    }
    
    // If can't parse, return as-is (might already be in correct format)
    return timeString
}

/**
 * Reset form to initial values matching iOS resetForm behavior
 */
private fun resetFormToInitial(
    preview: AdminBookingPreviewData,
    onFormStateChange: (BookingFormState) -> Unit,
    onPickupCoordinatesChange: (Pair<Double, Double>?) -> Unit,
    onDropoffCoordinatesChange: (Pair<Double, Double>?) -> Unit,
    onPickupCountryChange: (String?) -> Unit,
    onDropoffCountryChange: (String?) -> Unit,
    onExtraStopCoordinatesChange: (Map<Int, Pair<Double, Double>>) -> Unit,
    onExtraStopCountriesChange: (Map<Int, String?>) -> Unit,
    onExtraStopSelectedFlagsChange: (Map<Int, Boolean>) -> Unit,
    onExtraStopValidationErrorsChange: (Map<Int, String>) -> Unit,
    onFieldErrorsChange: (Map<String, String>) -> Unit,
    onDynamicRatesChange: (Map<String, String>) -> Unit,
    onNumberOfVehiclesChange: (String) -> Unit,
    onNumberOfHoursChange: (String) -> Unit
) {
    // Reset form state to initial values from preview
    val initialFormState = mapPreviewToFormState(preview)
    onFormStateChange(initialFormState)
    
    // Reset coordinates
    preview.pickupLatitude?.toDoubleOrNull()?.let { lat ->
        preview.pickupLongitude?.toDoubleOrNull()?.let { lon ->
            onPickupCoordinatesChange(Pair(lat, lon))
        }
    } ?: onPickupCoordinatesChange(null)
    
    preview.dropoffLatitude?.toDoubleOrNull()?.let { lat ->
        preview.dropoffLongitude?.toDoubleOrNull()?.let { lon ->
            onDropoffCoordinatesChange(Pair(lat, lon))
        }
    } ?: onDropoffCoordinatesChange(null)
    
    // Reset countries
    onPickupCountryChange(null)
    onDropoffCountryChange(null)
    
    // Reset extra stops
    onExtraStopCoordinatesChange(emptyMap())
    onExtraStopCountriesChange(emptyMap())
    onExtraStopSelectedFlagsChange(emptyMap())
    onExtraStopValidationErrorsChange(emptyMap())
    
    // Reset validation errors
    onFieldErrorsChange(emptyMap())
    
    // Reset dynamic rates (would need to reload from API)
    onDynamicRatesChange(emptyMap())
    
    // Reset number of vehicles and hours
    onNumberOfVehiclesChange(initialFormState.numberOfVehicles.ifBlank { "1" })
    onNumberOfHoursChange(initialFormState.numberOfHours)
}

/**
 * Travel Info Section matching iOS TravelInfoSection design
 */
@Composable
private fun TravelInfoSection(
    travelInfo: String,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showDivider) {
            HorizontalDivider(
                color = Color.Gray.copy(alpha = 0.3f),
                thickness = 1.dp
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Total Travel Time",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            
            Text(
                text = travelInfo,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimoOrange
                )
            )
        }
    }
}

/**
 * Location Error Banner matching iOS LocationErrorBannerView design
 */
@Composable
private fun LocationErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = remember(message) {
        val trimmed = message.trim()
        val lowercased = trimmed.lowercase()
        
        when {
            lowercased.contains("country") -> Pair(
                "Pickup and drop countries must match",
                "Please select pickup and drop-off within the same country."
            )
            lowercased.contains("cannot be the same") || lowercased.contains("same location") -> {
                Pair("Invalid location selected", trimmed)
            }
            else -> {
                val periodIndex = trimmed.indexOf('.')
                if (periodIndex > 0 && periodIndex < trimmed.length - 1) {
                    val title = trimmed.substring(0, periodIndex).trim()
                    val remainder = trimmed.substring(periodIndex + 1).trim()
                    if (title.isNotBlank() && remainder.isNotBlank()) {
                        Pair(title, remainder)
                    } else {
                        Pair("Invalid location selected", trimmed)
                    }
                } else {
                    Pair("Invalid location selected", trimmed)
                }
            }
        }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF5F5), // Light red background
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFF07878) // Light red border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info icon in circle
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.shape.CircleShape
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFFFFF0F0), // Light red fill
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFED7575) // Red border
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFC71F1F), // Dark red
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // Error message
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = labels.first,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC71F1F) // Dark red
                    )
                )
                Text(
                    text = labels.second,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF941010) // Darker red
                    )
                )
            }
            
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFC71F1F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
