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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ExtraStopFormState(
    val address: String = "",
    val rate: String = "",
    val instructions: String = ""
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

    val dynamicRates = remember { mutableStateMapOf<String, String>() }
    val taxIsPercent = remember { mutableStateMapOf<String, Boolean>() }

    val numberOfHoursState = remember { mutableStateOf("") }
    val numberOfVehiclesState = remember { mutableStateOf("1") }

    var formState by remember { mutableStateOf(BookingFormState()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showShipTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
        if (bookingId != 0) viewModel.load(bookingId)
    }

    // iOS parity: airports/airlines come from backend `/api/mobile-data`
    LaunchedEffect(Unit) {
        viewModel.loadMobileDataIfNeeded()
    }

    LaunchedEffect(state.preview) {
        val preview = state.preview ?: return@LaunchedEffect
        val mapped = mapPreviewToFormState(preview)
        formState = mapped
        numberOfVehiclesState.value = mapped.numberOfVehicles.ifBlank { "1" }
        numberOfHoursState.value = mapped.numberOfHours
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
                        CircularProgressIndicator(color = LimoOrange)
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
                        // Single-screen flow (matches iOS EditBookingDetailsAndRatesView)

                        CommonDropdown(
                            label = "SERVICE TYPE",
                            placeholder = "Select Service Type",
                            selectedValue = formState.serviceType,
                            options = serviceTypeOptions(formState.serviceType),
                            onValueSelected = { selected ->
                                val normalized = normalizeServiceType(selected)
                                formState = formState.copy(serviceType = normalized)
                                // Apply default immediately so the Hours field shows "2" once Charter/Tour is picked.
                                if (normalized.equals("Charter/Tour", ignoreCase = true) && formState.numberOfHours.isBlank()) {
                                    formState = formState.copy(numberOfHours = "2")
                                    numberOfHoursState.value = "2"
                                }
                            },
                            isRequired = true
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
                            },
                            isRequired = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        // Client accounts + passenger details are intentionally hidden for this flow.

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                PickerField(
                                    label = "TRAVEL DATE",
                                    value = formState.pickupDate,
                                    placeholder = "Select Date",
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray) },
                                    onClick = { showDatePicker = true },
                                    isRequired = true
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PickerField(
                                    label = "PICKUP TIME",
                                    value = formState.pickupTime,
                                    placeholder = "Select Time",
                                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray) },
                                    onClick = { showTimePicker = true },
                                    isRequired = true
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
                            },
                            keyboardType = KeyboardType.Number,
                            isRequired = true
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
                                },
                                keyboardType = KeyboardType.Number,
                                isRequired = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        // Locations (not wrapped in a card to match iOS)
                        DynamicLocationSection(
                            formState = formState,
                            onFormChange = { formState = it },
                            onOpenShipTimePicker = { showShipTimePicker = true },
                            airports = state.airports,
                            airlines = state.airlines
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        // Extra Stops (no card wrapper; iOS shows a simple add button and fields appear after adding)
                        ExtraStopsSection(
                            formState = formState,
                            onFormChange = { formState = it }
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
                            onValueSelected = { formState = formState.copy(meetGreetChoice = it) },
                            isRequired = true
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
                            extraStops = formState.extraStops.map {
                                EditReservationExtraStopRequest(
                                    address = it.address.ifBlank { null },
                                    rate = it.rate.ifBlank { null },
                                    bookingInstructions = it.instructions.ifBlank { null }
                                )
                            }
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
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
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

    // Dialogs (ported from limouserapp)
    if (showDatePicker) {
        val initial = parseDateOrToday(formState.pickupDate)
        DatePickerDialog(
            selectedDate = initial,
            onDateSelected = { date ->
                formState = formState.copy(pickupDate = formatDateYMD(date))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val initial = parseTimeOrNow(formState.pickupTime)
        TimePickerDialog(
            selectedTime = initial,
            onTimeSelected = { date ->
                formState = formState.copy(pickupTime = formatTimeHHmmss(date))
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showShipTimePicker) {
        val initial = parseTimeOrNow(formState.shipArrivalTime)
        TimePickerDialog(
            selectedTime = initial,
            onTimeSelected = { date ->
                formState = formState.copy(shipArrivalTime = formatTimeHHmmss(date))
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

private fun mapPreviewToFormState(preview: AdminBookingPreviewData): BookingFormState {
    val normalizedServiceType = normalizeServiceType(preview.serviceType.orEmpty())
    return BookingFormState(
        serviceType = normalizedServiceType,
        // Store API value if possible (fallback to raw backend value if unknown)
        transferType = normalizeTransferType(preview.transferType.orEmpty()),
        passengerName = preview.passengerName.orEmpty(),
        passengerEmail = preview.passengerEmail.orEmpty(),
        passengerCellIsd = preview.passengerCellIsd?.takeIf { it.isNotBlank() } ?: "+1",
        passengerCell = preview.passengerCell.orEmpty(),
        numberOfVehicles = preview.numberOfVehicles?.toString() ?: "1",
        numberOfHours = if (normalizedServiceType.equals("Charter/Tour", ignoreCase = true)) {
            // Default to 2 hours when backend has no value (iOS behavior)
            preview.numberOfHours?.toString().orEmpty().ifBlank { "2" }
        } else {
            ""
        },
        pickupDate = preview.pickupDate.orEmpty(),
        pickupTime = preview.pickupTime.orEmpty(),
        meetGreetChoice = preview.meetGreetChoiceName.orEmpty(),
        pickupAddress = preview.pickupAddress.orEmpty(),
        dropoffAddress = preview.dropoffAddress.orEmpty(),
        extraStops = preview.extraStops.orEmpty().map {
            ExtraStopFormState(
                address = it.address.orEmpty(),
                rate = it.rate.orEmpty(),
                instructions = it.bookingInstructions.orEmpty()
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

private fun transferTypeValueFromLabelOrNull(label: String): String? =
    transferTypeDropdownOptions().firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }?.value

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

private fun transferTypeKey(value: String): String =
    normalizeTransferType(value).lowercase(Locale.getDefault())

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
                value = formState.pickupDate,
                placeholder = "Select Date",
                icon = { androidx.compose.material3.Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray) },
                onClick = onOpenDatePicker,
                isRequired = true
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            PickerField(
                label = "PICKUP TIME",
                value = formState.pickupTime,
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

@Composable
private fun DynamicLocationSection(
    formState: BookingFormState,
    onFormChange: (BookingFormState) -> Unit,
    onOpenShipTimePicker: () -> Unit,
    airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport>,
    airlines: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirline>
) {
    val transferType = transferTypeKey(formState.transferType)

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
                onAirportChange = { onFormChange(formState.copy(pickupAirport = it)) },
                onAirlineChange = { onFormChange(formState.copy(pickupAirline = it)) },
                onFlightNoChange = { onFormChange(formState.copy(pickupFlightNumber = it)) },
                onCityChange = { onFormChange(formState.copy(pickupOriginCity = it)) },
                isOrigin = true
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
                onValueChange = { onFormChange(formState.copy(pickupAddress = it)) },
                onLocationSelected = { _, _, _, _, displayText, _, _, _, _ ->
                    onFormChange(formState.copy(pickupAddress = displayText))
                },
                placeholder = "Search pickup location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Dropoff", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(8.dp))

    when {
        transferType in setOf("city_to_airport", "airport_to_airport", "cruise_to_airport") -> {
            AirportFields(
                airport = formState.dropoffAirport,
                airline = formState.dropoffAirline,
                flightNo = formState.dropoffFlightNumber,
                city = formState.dropoffDestinationCity,
                airports = airports,
                airlines = airlines,
                onAirportChange = { onFormChange(formState.copy(dropoffAirport = it)) },
                onAirlineChange = { onFormChange(formState.copy(dropoffAirline = it)) },
                onFlightNoChange = { onFormChange(formState.copy(dropoffFlightNumber = it)) },
                onCityChange = { onFormChange(formState.copy(dropoffDestinationCity = it)) },
                isOrigin = false
            )
        }
        transferType in setOf("airport_to_cruise_port", "city_to_cruise_port") -> {
            LocationAutocomplete(
                label = "DROPOFF ADDRESS (PORT)",
                value = formState.dropoffAddress,
                onValueChange = { onFormChange(formState.copy(dropoffAddress = it)) },
                onLocationSelected = { _, _, _, _, displayText, _, _, _, _ ->
                    onFormChange(formState.copy(dropoffAddress = displayText))
                },
                placeholder = "Search port location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth()
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
                onValueChange = { onFormChange(formState.copy(dropoffAddress = it)) },
                onLocationSelected = { _, _, _, _, displayText, _, _, _, _ ->
                    onFormChange(formState.copy(dropoffAddress = displayText))
                },
                placeholder = "Search drop-off location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExtraStopsSection(
    formState: BookingFormState,
    onFormChange: (BookingFormState) -> Unit
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
                        onLocationSelected = { _, _, _, _, displayText, _, _, _, _ ->
                            val list = formState.extraStops.toMutableList()
                            list[index] = list[index].copy(address = displayText)
                            onFormChange(formState.copy(extraStops = list))
                        },
                        placeholder = "Search stop location",
                        modifier = Modifier.fillMaxWidth()
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
    isOrigin: Boolean
) {
    val airportOptions = remember(airports) { airports.map { it.displayName } }
    val airlineOptions = remember(airlines) { airlines.map { it.displayName } }

    CommonDropdown(
        label = "SELECT AIRPORT",
        placeholder = "Select airport",
        selectedValue = airport,
        options = airportOptions,
        onValueSelected = onAirportChange,
        isRequired = true,
        searchable = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    CommonDropdown(
        label = "SELECT AIRLINE",
        placeholder = "Select airline",
        selectedValue = airline,
        options = airlineOptions,
        onValueSelected = onAirlineChange,
        isRequired = true,
        searchable = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    CommonTextField(
        label = "FLIGHT / TAIL #",
        placeholder = "Enter flight number",
        text = flightNo,
        onValueChange = onFlightNoChange
    )
    Spacer(modifier = Modifier.height(8.dp))
    CommonTextField(
        label = if (isOrigin) "ORIGIN AIRPORT / CITY" else "DESTINATION AIRPORT / CITY",
        placeholder = "Enter city",
        text = city,
        onValueChange = onCityChange,
        isRequired = true
    )
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
    isRequired: Boolean = false
) {
    Box {
        CommonTextField(
            label = label,
            placeholder = placeholder,
            text = value,
            onValueChange = {},
            isRequired = isRequired,
            readOnly = true,
            trailingIcon = icon
        )
        Box(
            modifier = Modifier
                .size(15.dp)
                .clickable { onClick() }
        )
    }
}

private fun parseDateOrToday(value: String): Date {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return runCatching { fmt.parse(value) }.getOrNull() ?: Date()
}

private fun parseTimeOrNow(value: String): Date {
    val formats = listOf("HH:mm:ss", "HH:mm", "h:mm a")
    for (f in formats) {
        val fmt = SimpleDateFormat(f, Locale.getDefault())
        val parsed = runCatching { fmt.parse(value) }.getOrNull()
        if (parsed != null) return parsed
    }
    return Calendar.getInstance().time
}

private fun formatDateYMD(date: Date): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return fmt.format(date)
}

private fun formatTimeHHmmss(date: Date): String {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return fmt.format(date)
}
