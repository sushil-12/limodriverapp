package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.AdminReservationRatesData
import com.limo1800driver.app.data.model.dashboard.MobileDataAirline
import com.limo1800driver.app.data.model.dashboard.MobileDataAirport
import com.limo1800driver.app.data.model.dashboard.EditReservationExtraStopRequest
import com.limo1800driver.app.data.model.dashboard.EditReservationRequest
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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
    val error: String? = null
)

@HiltViewModel
class EditBookingViewModel @Inject constructor(
    private val repository: DriverDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditBookingUiState())
    val uiState: StateFlow<EditBookingUiState> = _uiState.asStateFlow()

    fun load(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            val previewResult = repository.getBookingPreview(bookingId)
            val ratesResult = repository.getReservationRates(bookingId)

            var preview: AdminBookingPreviewData? = null
            var rates: AdminReservationRatesData? = null
            var error: String? = null

            previewResult.onSuccess { resp ->
                if (resp.success) preview = resp.data else error = resp.message
            }.onFailure { e -> error = e.message }

            ratesResult.onSuccess { resp ->
                if (resp.success) rates = resp.data else error = error ?: resp.message
            }.onFailure { e -> error = error ?: e.message }

            _uiState.value = _uiState.value.copy(isLoading = false, preview = preview, rates = rates, error = error)
        }
    }

    fun loadMobileDataIfNeeded() {
        viewModelScope.launch {
            // Avoid refetching if already loaded
            if (_uiState.value.airports.isNotEmpty() && _uiState.value.airlines.isNotEmpty()) return@launch

            if (_uiState.value.airlines.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoadingAirlines = true, airlinesError = null)
                repository.getMobileDataAirlines()
                    .onSuccess { resp ->
                        if (resp.success && resp.data != null) {
                            _uiState.value = _uiState.value.copy(
                                airlines = resp.data.airlinesData,
                                isLoadingAirlines = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoadingAirlines = false,
                                airlinesError = resp.message
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(isLoadingAirlines = false, airlinesError = e.message)
                    }
            }

            if (_uiState.value.airports.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoadingAirports = true, airportsError = null)
                repository.getMobileDataAirports()
                    .onSuccess { resp ->
                        if (resp.success && resp.data != null) {
                            _uiState.value = _uiState.value.copy(
                                airports = resp.data.airportsData,
                                isLoadingAirports = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoadingAirports = false,
                                airportsError = resp.message
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(isLoadingAirports = false, airportsError = e.message)
                    }
            }
        }
    }

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
        extraStops: List<EditReservationExtraStopRequest>? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, successMessage = null)
            repository.editReservation(
                EditReservationRequest(
                    bookingId = bookingId,
                    pickupAddress = pickupAddress,
                    dropoffAddress = dropoffAddress,
                    pickupDate = pickupDate,
                    pickupTime = pickupTime,
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
                    pickupAirportName = pickupAirportName,
                    pickupAirlineName = pickupAirlineName,
                    pickupFlight = pickupFlight,
                    originAirportCity = originAirportCity,
                    cruisePort = cruisePort,
                    cruiseName = cruiseName,
                    cruiseTime = cruiseTime,
                    dropoffAirportName = dropoffAirportName,
                    dropoffAirlineName = dropoffAirlineName,
                    dropoffFlight = dropoffFlight,
                    departingAirportCity = departingAirportCity,
                    extraStops = extraStops,
                    rates = rates
                )
            ).onSuccess { resp ->
                _uiState.value = _uiState.value.copy(isSaving = false, successMessage = resp.message)
            }.onFailure { e ->
                Timber.e(e, "Failed to edit reservation")
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}


