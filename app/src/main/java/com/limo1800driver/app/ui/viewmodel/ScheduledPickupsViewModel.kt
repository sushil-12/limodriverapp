package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Scheduled Pickups
 * Manages scheduled pickups list with pagination
 */
@HiltViewModel
class ScheduledPickupsViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduledPickupsUiState())
    val uiState: StateFlow<ScheduledPickupsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        fetchScheduledPickups(resetData = true)
    }

    /**
     * Fetch scheduled pickups from API
     */
    fun fetchScheduledPickups(resetData: Boolean = false) {
        if (_uiState.value.isLoading && !resetData) return

        viewModelScope.launch {
            if (resetData) {
                _uiState.value = _uiState.value.copy(
                    bookings = emptyList(),
                    currentPage = 1
                )
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val startDate = dateFormat.format(Date()) // Today's date
            val endDate = dateFormat.format(Date()) // Today's date for now, can be extended to show future dates
            val searchText = _uiState.value.searchText.takeIf { it.isNotBlank() }

            dashboardRepository.getScheduledPickups(
                page = _uiState.value.currentPage,
                perPage = _uiState.value.perPage,
                startDate = startDate,
                endDate = endDate,
                search = searchText
            )
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val newBookings = if (resetData) {
                            response.data.bookings
                        } else {
                            _uiState.value.bookings + response.data.bookings
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            bookings = newBookings,
                            pagination = response.data.pagination,
                            canLoadMore = response.data.pagination?.let {
                                it.currentPage < it.lastPage
                            } ?: false,
                            error = null
                        )
                        Timber.d("Scheduled pickups loaded: ${newBookings.size}")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        Timber.e("Scheduled pickups API error: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load scheduled pickups"
                    )
                    Timber.e(exception, "Failed to fetch scheduled pickups")
                }
        }
    }

    /**
     * Load more scheduled pickups (pagination)
     */
    fun loadMoreScheduledPickups() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return

        _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage + 1)
        fetchScheduledPickups()
    }

    /**
     * Update search text with debounce
     */
    fun updateSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
            fetchScheduledPickups(resetData = true)
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Scheduled Pickups
 */
data class ScheduledPickupsUiState(
    val isLoading: Boolean = false,
    val bookings: List<DriverBooking> = emptyList(),
    val pagination: com.limo1800driver.app.data.model.dashboard.BookingPagination? = null,
    val currentPage: Int = 1,
    val perPage: Int = 10,
    val canLoadMore: Boolean = false,
    val searchText: String = "",
    val error: String? = null
)
