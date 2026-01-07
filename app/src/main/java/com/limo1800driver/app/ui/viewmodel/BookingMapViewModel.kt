package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.AffiliateBookingPreviewData
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import com.limo1800driver.app.data.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookingMapViewModel @Inject constructor(
    private val repository: DriverDashboardRepository
) : ViewModel() {

    private val _bookingPreviewState = MutableStateFlow<UiState<AffiliateBookingPreviewData>>(UiState.Loading)
    val bookingPreviewState: StateFlow<UiState<AffiliateBookingPreviewData>> = _bookingPreviewState

    fun fetchBookingPreview(bookingId: Int) {
        viewModelScope.launch {
            _bookingPreviewState.value = UiState.Loading
            try {
                val result = repository.getAffiliateBookingPreview(bookingId)
                result.fold(
                    onSuccess = { response ->
                        if (response.success == true && response.data != null) {
                            _bookingPreviewState.value = UiState.Success(response.data)
                            Timber.d("Successfully fetched booking preview for booking: $bookingId")
                        } else {
                            _bookingPreviewState.value = UiState.Error(
                                response.message ?: "Failed to fetch booking details"
                            )
                        }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to fetch booking preview")
                        _bookingPreviewState.value = UiState.Error(
                            exception.message ?: "Failed to fetch booking details"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while fetching booking preview")
                _bookingPreviewState.value = UiState.Error(
                    e.message ?: "Failed to fetch booking details"
                )
            }
        }
    }
}
