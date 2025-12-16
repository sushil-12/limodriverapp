package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.notification.DriverBookingReminderData
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import com.limo1800driver.app.data.socket.DriverActiveRide
import com.limo1800driver.app.data.socket.DriverSocketService
import com.limo1800driver.app.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DriverBookingReminderViewModel @Inject constructor(
    private val socketService: DriverSocketService,
    private val tokenManager: TokenManager,
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {

    val reminder: StateFlow<DriverBookingReminderData?> = socketService.bookingReminder

    private val _passenger = MutableStateFlow(PassengerUiState())
    val passenger: StateFlow<PassengerUiState> = _passenger.asStateFlow()

    init {
        // When a reminder arrives, fetch passenger info for the bookingId (so the sheet matches the design).
        viewModelScope.launch {
            reminder.collectLatest { r ->
                if (r == null) {
                    _passenger.value = PassengerUiState()
                    return@collectLatest
                }
                fetchPassengerForBooking(r.bookingId)
            }
        }
    }

    fun dismiss() {
        socketService.clearBookingReminder()
        _passenger.value = PassengerUiState()
    }

    fun sendDriverEnRoute(reminder: DriverBookingReminderData) {
        // Seed a local active ride immediately so Dashboard -> RideInProgress navigation and Ride UI
        // doesn't wait for the backend to push a fresh `active_ride` event.
        socketService.setActiveRide(
            DriverActiveRide(
                bookingId = reminder.bookingId,
                customerId = reminder.customerId,
                status = "en_route_pu",
                statusDisplay = null,
                pickupAddress = reminder.pickupAddress,
                pickupLatitude = reminder.pickupLat,
                pickupLongitude = reminder.pickupLng,
                pickupDatetime = reminder.formattedDatetime,
                dropoffAddress = reminder.dropoffAddress,
                dropoffLatitude = reminder.dropoffLat,
                dropoffLongitude = reminder.dropoffLng,
                instructions = null,
                passengers = null,
                luggage = null,
                customerName = passenger.value.name.takeIf { it.isNotBlank() && it != "Passenger" },
                customerPhone = passenger.value.phone.takeIf { it.isNotBlank() && it != "—" }
            )
        )

        socketService.emitBookingStatusUpdate(
            bookingId = reminder.bookingId,
            customerId = reminder.customerId,
            status = "en_route_pu",
            driverId = resolveDriverId(),
            currentLocation = null,
            pickupInfo = Triple(reminder.pickupLat, reminder.pickupLng, reminder.pickupAddress),
            dropoffInfo = Triple(reminder.dropoffLat, reminder.dropoffLng, reminder.dropoffAddress)
        )
        dismiss()
    }

    private suspend fun fetchPassengerForBooking(bookingId: Int) {
        _passenger.value = _passenger.value.copy(isLoading = true, error = null)
        dashboardRepository.getBookingPreview(bookingId)
            .onSuccess { response ->
                val data = response.data
                val name = data?.passengerName?.trim().orEmpty()
                val isd = data?.passengerCellIsd?.trim().orEmpty()
                val cell = data?.passengerCell?.trim().orEmpty()
                val phone = formatPhone(isd, cell)

                _passenger.value = PassengerUiState(
                    isLoading = false,
                    name = name.ifBlank { "Passenger" },
                    phone = phone ?: "—",
                    error = null
                )
            }
            .onFailure { e ->
                _passenger.value = PassengerUiState(
                    isLoading = false,
                    name = "Passenger",
                    phone = "—",
                    error = e.message
                )
            }
    }

    private fun resolveDriverId(): String {
        val token = tokenManager.getAccessToken()
        return extractUserIdFromJwt(token) ?: "unknown"
    }

    private fun extractUserIdFromJwt(token: String?): String? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadJson = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val obj = JSONObject(payloadJson)
            obj.optString("sub")
                .ifBlank { obj.optString("user_id") }
                .ifBlank { obj.optString("id") }
                .ifBlank { null }
        } catch (e: Exception) {
            Timber.tag("DriverBookingReminderVM").e(e, "Failed to parse JWT for driverId")
            null
        }
    }
}

data class PassengerUiState(
    val isLoading: Boolean = false,
    val name: String = "Passenger",
    val phone: String = "—",
    val error: String? = null
)

private fun formatPhone(isd: String, number: String): String? {
    val n = number.trim().takeIf { it.isNotEmpty() } ?: return null
    val i = isd.trim().takeIf { it.isNotEmpty() }
    return if (i == null) n else "${if (i.startsWith("+")) i else "+$i"}$n"
}


