package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class BookingPreviewUiState(
    val isLoading: Boolean = false,
    val preview: AdminBookingPreviewData? = null,
    val error: String? = null,
    val isAccepting: Boolean = false,
    val isRejecting: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class BookingPreviewViewModel @Inject constructor(
    private val repository: DriverDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingPreviewUiState())
    val uiState: StateFlow<BookingPreviewUiState> = _uiState.asStateFlow()

    fun load(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            repository.getBookingPreview(bookingId)
                .onSuccess { resp ->
                    if (resp.success && resp.data != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            preview = resp.data,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = resp.message)
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load booking preview")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun accept(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAccepting = true, error = null, successMessage = null)
            repository.acceptBooking(bookingId)
                .onSuccess { resp ->
                    _uiState.value = _uiState.value.copy(
                        isAccepting = false,
                        successMessage = resp.message
                    )
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to accept booking")
                    _uiState.value = _uiState.value.copy(isAccepting = false, error = e.message)
                }
        }
    }

    fun reject(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRejecting = true, error = null, successMessage = null)
            repository.rejectBooking(bookingId)
                .onSuccess { resp ->
                    _uiState.value = _uiState.value.copy(
                        isRejecting = false,
                        successMessage = resp.message
                    )
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to reject booking")
                    _uiState.value = _uiState.value.copy(isRejecting = false, error = e.message)
                }
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}


