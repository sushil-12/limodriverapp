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
 * ViewModel for Pre-arranged Rides Screen
 * Manages booking list with time period filters and search
 */
@HiltViewModel
class PreArrangedRidesViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PreArrangedRidesUiState())
    val uiState: StateFlow<PreArrangedRidesUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd", Locale.US)
    
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
                            currentPage = response.data.pagination?.currentPage ?: 1,
                            totalPages = response.data.pagination?.lastPage ?: 1,
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
     * Set search text (for TextField binding) - matching user app pattern
     */
    fun setSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        handleSearch(text)
    }
    
    /**
     * Handle search - matches iOS onChange behavior with debounce
     */
    private fun handleSearch(searchText: String) {
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            delay(500)
            if (_uiState.value.searchText == searchText) {
                refreshBookings()
            }
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(searchText = "")
        refreshBookings()
    }
    
    /**
     * Refresh bookings
     */
    fun refreshBookings() {
        _uiState.value = _uiState.value.copy(currentPage = 1)
        fetchBookings(resetData = true)
    }
    
    /**
     * Handle time period change
     */
    fun handleTimePeriodChange(timePeriod: TimePeriod) {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        when (timePeriod) {
            TimePeriod.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.time
                
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val weekEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedTimePeriod = timePeriod,
                    selectedWeekStart = weekStart,
                    selectedWeekEnd = weekEnd,
                    currentPage = 1
                )
            }
            TimePeriod.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.time
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val monthEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedTimePeriod = timePeriod,
                    selectedWeekStart = monthStart,
                    selectedWeekEnd = monthEnd,
                    currentPage = 1
                )
            }
            TimePeriod.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val yearStart = calendar.time
                
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yearEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedTimePeriod = timePeriod,
                    selectedWeekStart = yearStart,
                    selectedWeekEnd = yearEnd,
                    currentPage = 1
                )
            }
            TimePeriod.CUSTOM -> {
                // Keep current dates, just change period
                _uiState.value = _uiState.value.copy(
                    selectedTimePeriod = timePeriod
                )
            }
        }
        
        fetchBookings(resetData = true)
    }
    
    /**
     * Handle custom date range selection
     */
    fun handleDateRangeSelection(startDate: Date, endDate: Date) {
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = startDate,
            selectedWeekEnd = endDate,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to previous week
     */
    fun goToPreviousWeek() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val newStart = calendar.time
        
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to previous month
     */
    fun goToPreviousMonth() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val newStart = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to previous year
     */
    fun goToPreviousYear() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.YEAR, -1)
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        val newStart = calendar.time
        
        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to previous period
     */
    fun goToPreviousPeriod() {
        val calendar = Calendar.getInstance()
        val period = _uiState.value.selectedTimePeriod
        
        when (period) {
            TimePeriod.WEEKLY -> {
                calendar.time = _uiState.value.selectedWeekStart
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val newStart = calendar.time
                
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
            TimePeriod.MONTHLY -> {
                calendar.time = _uiState.value.selectedWeekStart
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val newStart = calendar.time
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
            TimePeriod.YEARLY -> {
                calendar.time = _uiState.value.selectedWeekStart
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val newStart = calendar.time
                
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
            TimePeriod.CUSTOM -> {
                // For custom, move back by the same duration
                val duration = _uiState.value.selectedWeekEnd.time - _uiState.value.selectedWeekStart.time
                calendar.time = _uiState.value.selectedWeekStart
                calendar.timeInMillis -= duration
                val newStart = calendar.time
                
                calendar.timeInMillis += duration
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
        }
        
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to next week
     */
    fun goToNextWeek() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val newStart = calendar.time
        
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to next month
     */
    fun goToNextMonth() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val newStart = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to next year
     */
    fun goToNextYear() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.YEAR, 1)
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        val newStart = calendar.time
        
        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val newEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }
    
    /**
     * Navigate to next period
     */
    fun goToNextPeriod() {
        val calendar = Calendar.getInstance()
        val period = _uiState.value.selectedTimePeriod
        
        when (period) {
            TimePeriod.WEEKLY -> {
                calendar.time = _uiState.value.selectedWeekStart
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val newStart = calendar.time
                
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
            TimePeriod.MONTHLY -> {
                calendar.time = _uiState.value.selectedWeekStart
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val newStart = calendar.time
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
            TimePeriod.YEARLY -> {
                calendar.time = _uiState.value.selectedWeekStart
                calendar.add(Calendar.YEAR, 1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val newStart = calendar.time
                
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
            TimePeriod.CUSTOM -> {
                // For custom, move forward by the same duration
                val duration = _uiState.value.selectedWeekEnd.time - _uiState.value.selectedWeekStart.time
                calendar.time = _uiState.value.selectedWeekEnd
                calendar.timeInMillis += 1 // Move to next day
                val newStart = calendar.time
                
                calendar.timeInMillis += duration
                val newEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = newStart,
                    selectedWeekEnd = newEnd,
                    currentPage = 1
                )
            }
        }
        
        fetchBookings(resetData = true)
    }
    
    /**
     * Get formatted date range string
     */
    fun getDateRangeString(): String {
        val startStr = displayDateFormat.format(_uiState.value.selectedWeekStart)
        val endStr = displayDateFormat.format(_uiState.value.selectedWeekEnd)
        return "$startStr - $endStr"
    }
    
    /**
     * Check if can go to previous period
     */
    fun canGoPrevious(): Boolean {
        // Allow going back up to 1 year
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.currentWeekStart
        calendar.add(Calendar.YEAR, -1)
        return _uiState.value.selectedWeekStart.after(calendar.time)
    }
    
    /**
     * Check if can go to next period
     */
    fun canGoNext(): Boolean {
        // Don't allow going to future periods beyond current date
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        return _uiState.value.selectedWeekStart.before(today.time) ||
               _uiState.value.selectedWeekStart == today.time
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Pre-arranged Rides
 */
data class PreArrangedRidesUiState(
    val isLoading: Boolean = false,
    val bookings: List<DriverBooking> = emptyList(),
    val pagination: com.limo1800driver.app.data.model.dashboard.BookingPagination? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val perPage: Int = 20,
    val canLoadMore: Boolean = false,
    val searchText: String = "",
    val isSearching: Boolean = false,
    val selectedTimePeriod: TimePeriod = TimePeriod.WEEKLY,
    val currentWeekStart: Date = Date(),
    val currentWeekEnd: Date = Date(),
    val selectedWeekStart: Date = Date(),
    val selectedWeekEnd: Date = Date(),
    val error: String? = null
)

