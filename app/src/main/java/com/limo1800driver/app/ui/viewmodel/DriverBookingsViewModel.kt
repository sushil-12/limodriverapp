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
 * ViewModel for Driver Bookings
 * Manages booking list with pagination and filtering
 */
@HiltViewModel
class DriverBookingsViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DriverBookingsUiState())
    val uiState: StateFlow<DriverBookingsUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    init {
        setupCurrentWeek()
    }
    
    /**
     * Setup current week dates
     */
    private fun setupCurrentWeek() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        // Get start of week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.time
        
        // Get end of week (Sunday)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val weekEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            currentWeekStart = weekStart,
            currentWeekEnd = weekEnd,
            selectedWeekStart = weekStart,
            selectedWeekEnd = weekEnd
        )
        
        fetchBookings(resetData = true)
    }
    
    /**
     * Fetch bookings from API
     */
    fun fetchBookings(resetData: Boolean = false) {
        if (_uiState.value.isLoading && !resetData) return
        
        viewModelScope.launch {
            if (resetData) {
                _uiState.value = _uiState.value.copy(
                    bookings = emptyList(),
                    currentPage = 1
                )
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val startDate = dateFormat.format(_uiState.value.selectedWeekStart)
            val endDate = dateFormat.format(_uiState.value.selectedWeekEnd)
            val searchText = _uiState.value.searchText.takeIf { it.isNotBlank() }
            
            dashboardRepository.getDriverBookings(
                page = _uiState.value.currentPage,
                perPage = _uiState.value.perPage,
                startDate = startDate,
                endDate = endDate,
                search = searchText,
                status = null
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
                        Timber.d("Bookings loaded: ${newBookings.size}")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        Timber.e("Bookings API error: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load bookings"
                    )
                    Timber.e(exception, "Failed to fetch bookings")
                }
        }
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

            val startDate = dateFormat.format(_uiState.value.selectedWeekStart)
            val endDate = dateFormat.format(_uiState.value.selectedWeekEnd)
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
     * Load more bookings (pagination)
     */
    fun loadMoreBookings() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        
        _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage + 1)
        fetchBookings()
    }
    
    /**
     * Update search text with debounce
     */
    fun updateSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
            fetchBookings(resetData = true)
        }
    }
    
    /**
     * Navigate to previous week
     */
    fun goToPreviousWeek() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val newStart = calendar.time
        
        calendar.time = _uiState.value.selectedWeekEnd
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to next week
     */
    fun goToNextWeek() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val newStart = calendar.time
        
        calendar.time = _uiState.value.selectedWeekEnd
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to current week
     */
    fun goToCurrentWeek() {
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = _uiState.value.currentWeekStart,
            selectedWeekEnd = _uiState.value.currentWeekEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Driver Bookings
 */
data class DriverBookingsUiState(
    val isLoading: Boolean = false,
    val bookings: List<DriverBooking> = emptyList(),
    val pagination: com.limo1800driver.app.data.model.dashboard.BookingPagination? = null,
    val currentPage: Int = 1,
    val perPage: Int = 10,
    val canLoadMore: Boolean = false,
    val searchText: String = "",
    val currentWeekStart: Date = Date(),
    val currentWeekEnd: Date = Date(),
    val selectedWeekStart: Date = Date(),
    val selectedWeekEnd: Date = Date(),
    val error: String? = null
)

