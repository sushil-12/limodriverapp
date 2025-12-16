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
    // API Date format (Standard ISO Date)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Display Formats
    private val displayRangeFormat = SimpleDateFormat("MMM dd", Locale.US)
    private val displayMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val displayYearFormat = SimpleDateFormat("yyyy", Locale.US)

    init {
        setupCurrentWeek()
    }

    /**
     * Setup current week dates
     */
    private fun setupCurrentWeek() {
        val calendar = getCleanCalendar()

        // Calculate Monday of current week
        calendar.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        val weekStart = calendar.time

        // Calculate Sunday of current week
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        // Ensure end date covers the full day for logic, though API sends YYYY-MM-DD
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val weekEnd = calendar.time

        _uiState.value = _uiState.value.copy(
            currentWeekStart = weekStart,
            currentWeekEnd = weekEnd,
            selectedWeekStart = weekStart,
            selectedWeekEnd = weekEnd,
            selectedTimePeriod = TimePeriod.WEEKLY
        )

        fetchBookings(resetData = true)
    }

    /**
     * Helper to get a Calendar instance with time set to 00:00:00
     */
    private fun getCleanCalendar(): Calendar {
        val calendar = Calendar.getInstance(Locale.US)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
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

            val startDate = apiDateFormat.format(_uiState.value.selectedWeekStart)
            val endDate = apiDateFormat.format(_uiState.value.selectedWeekEnd)
            val searchText = _uiState.value.searchText.takeIf { it.isNotBlank() }

            Timber.d("PreArrangedRidesViewModel - Fetching: period=${_uiState.value.selectedTimePeriod}, start=$startDate, end=$endDate, page=${_uiState.value.currentPage}")

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
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load bookings"
                    )
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
     * Set search text
     */
    fun setSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            refreshBookings()
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

    fun refreshBookings() {
        _uiState.value = _uiState.value.copy(currentPage = 1)
        fetchBookings(resetData = true)
    }

    /**
     * Handle time period change logic
     */
    fun handleTimePeriodChange(timePeriod: TimePeriod) {
        // Reset to "Current" time context whenever switching tabs (Standard behavior)
        val calendar = getCleanCalendar()
        calendar.firstDayOfWeek = Calendar.MONDAY

        var start: Date
        var end: Date

        when (timePeriod) {
            TimePeriod.WEEKLY -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                start = calendar.time

                calendar.add(Calendar.DAY_OF_MONTH, 6)
                end = calendar.time
            }
            TimePeriod.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                start = calendar.time

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                end = calendar.time
            }
            TimePeriod.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1) // Jan 1st
                start = calendar.time

                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                end = calendar.time
            }
            TimePeriod.CUSTOM -> {
                // Keep existing selection or default to current week if null
                start = _uiState.value.selectedWeekStart
                end = _uiState.value.selectedWeekEnd
            }
        }

        // Ensure end of day time for end date (just for safety)
        val endCalendar = Calendar.getInstance(Locale.US)
        endCalendar.time = end
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        end = endCalendar.time

        _uiState.value = _uiState.value.copy(
            selectedTimePeriod = timePeriod,
            selectedWeekStart = start,
            selectedWeekEnd = end,
            currentPage = 1
        )

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

    // --- Navigation Logic ---

    fun goToPreviousPeriod() {
        navigatePeriod(-1)
    }

    fun goToNextPeriod() {
        navigatePeriod(1)
    }

    private fun navigatePeriod(direction: Int) {
        val calendar = getCleanCalendar()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.firstDayOfWeek = Calendar.MONDAY

        var newStart: Date
        var newEnd: Date

        when (_uiState.value.selectedTimePeriod) {
            TimePeriod.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, direction)
                newStart = calendar.time

                calendar.add(Calendar.DAY_OF_MONTH, 6)
                newEnd = calendar.time
            }
            TimePeriod.MONTHLY -> {
                calendar.add(Calendar.MONTH, direction)
                calendar.set(Calendar.DAY_OF_MONTH, 1) // Ensure 1st
                newStart = calendar.time

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                newEnd = calendar.time
            }
            TimePeriod.YEARLY -> {
                calendar.add(Calendar.YEAR, direction)
                calendar.set(Calendar.DAY_OF_YEAR, 1) // Jan 1
                newStart = calendar.time

                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                newEnd = calendar.time
            }
            TimePeriod.CUSTOM -> {
                // Shift both dates by the duration of the current range
                val duration = _uiState.value.selectedWeekEnd.time - _uiState.value.selectedWeekStart.time
                // Approx days
                val daysDiff = ((duration / (1000 * 60 * 60 * 24)) + 1).toInt()

                calendar.add(Calendar.DAY_OF_MONTH, daysDiff * direction)
                newStart = calendar.time

                val endCal = getCleanCalendar()
                endCal.time = _uiState.value.selectedWeekEnd
                endCal.add(Calendar.DAY_OF_MONTH, daysDiff * direction)
                newEnd = endCal.time
            }
        }

        // Ensure end time is EOD
        val endCalFix = Calendar.getInstance(Locale.US)
        endCalFix.time = newEnd
        endCalFix.set(Calendar.HOUR_OF_DAY, 23)
        endCalFix.set(Calendar.MINUTE, 59)
        newEnd = endCalFix.time

        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd,
            currentPage = 1
        )
        fetchBookings(resetData = true)
    }

    // --- Helper for Date String Display ---

    fun getDateRangeString(): String {
        val start = _uiState.value.selectedWeekStart
        val end = _uiState.value.selectedWeekEnd

        return when (_uiState.value.selectedTimePeriod) {
            TimePeriod.WEEKLY -> {
                val startStr = displayRangeFormat.format(start)
                val endStr = displayRangeFormat.format(end)
                "$startStr - $endStr"
            }
            TimePeriod.MONTHLY -> {
                displayMonthFormat.format(start)
            }
            TimePeriod.YEARLY -> {
                displayYearFormat.format(start)
            }
            TimePeriod.CUSTOM -> {
                val startStr = displayRangeFormat.format(start)
                val endStr = displayRangeFormat.format(end)
                "$startStr - $endStr"
            }
        }
    }

    fun canGoPrevious(): Boolean {
        // Simple logic: Allow going back up to ~5 years
        val calendar = getCleanCalendar()
        calendar.add(Calendar.YEAR, -5)
        return _uiState.value.selectedWeekStart.after(calendar.time)
    }

    fun canGoNext(): Boolean {
        // Prevent going into future periods (beyond current date context)
        // If current selection end date is >= today, disable next
        val today = getCleanCalendar()
        return _uiState.value.selectedWeekEnd.before(today.time)
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