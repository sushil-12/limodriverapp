package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for My Activity Screen
 * Manages weekly activity data with week navigation
 */
@HiltViewModel
class MyActivityViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyActivityUiState())
    val uiState: StateFlow<MyActivityUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val weekRangeFormat = SimpleDateFormat("MMM dd", Locale.US)
    
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
        
        fetchActivityData()
    }
    
    /**
     * Fetch activity data for selected week
     */
    fun fetchActivityData() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val startDate = dateFormat.format(_uiState.value.selectedWeekStart)
            val endDate = dateFormat.format(_uiState.value.selectedWeekEnd)
            
            // Fetch both activity and earnings summary
            val activityResult = dashboardRepository.getDriverAllActivity(
                page = 1,
                perPage = 100,
                startDate = startDate,
                endDate = endDate
            )
            
            val earningsResult = dashboardRepository.getDriverEarningsSummary(
                startDate = startDate,
                endDate = endDate
            )
            
            activityResult.onSuccess { activityResponse ->
                if (activityResponse.success && activityResponse.data != null) {
                    val activityData = activityResponse.data
                    
                    // Flatten rides from all date groups into bookings
                    val bookings = activityData.data?.flatMap { dateGroup ->
                        dateGroup.rides?.map { ride ->
                            // Convert ActivityRide to DriverBooking format
                            DriverBooking(
                                bookingId = ride.rideId,
                                individualAccountId = 0, // Not available in activity API
                                travelClientId = null,
                                pickupDate = dateGroup.dateKey,
                                pickupTime = ride.time,
                                dropoffLongitude = null,
                                dropoffLatitude = null,
                                pickupLongitude = null,
                                pickupLatitude = null,
                                changedFields = null,
                                serviceType = null,
                                transferType = "one_way", // Default value
                                paymentStatus = ride.status,
                                createdByRole = "driver",
                                paymentMethod = null,
                                bookingStatus = ride.status,
                                affiliateId = 0, // Not available in activity API
                                passengerName = null,
                                passengerEmail = null,
                                passengerCellIsd = null,
                                passengerCellCountry = null,
                                passengerCell = null,
                                passengerImage = null,
                                pickupAddress = ride.pickup,
                                accountType = null,
                                dropoffAddress = ride.destination,
                                affiliateChargedAmount = null,
                                vehicleCatName = null,
                                affiliateType = null,
                                companyName = null,
                                affiliateDispatchIsd = "",
                                affiliateDispatchNumber = "",
                                dispatchEmail = "",
                                gigCellIsd = "",
                                gigCellMobile = "",
                                gigEmail = "",
                                reservationType = null,
                                chargeObjectId = null,
                                grandTotal = ride.earnings,
                                currency = ride.currency,
                                farmoutAffiliate = null,
                                currencySymbol = ride.currency,
                                driverRating = null,
                                passengerRating = null,
                                showChangedFields = null
                            )
                        } ?: emptyList()
                    } ?: emptyList()
                    
                    earningsResult.onSuccess { earningsResponse ->
                        if (earningsResponse.success && earningsResponse.data != null) {
                            val earnings = earningsResponse.data
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                bookings = bookings,
                                totalEarnings = earnings.totalEarnings ?: activityData.weeklySummary?.earnings ?: 0.0,
                                totalRides = earnings.totalRides ?: activityData.weeklySummary?.rides ?: bookings.size,
                                currencySymbol = earnings.currencySymbol ?: bookings.firstOrNull()?.currencySymbol ?: "$",
                                error = null
                            )
                        } else {
                            // Use activity data if earnings API fails
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                bookings = bookings,
                                totalEarnings = activityData.weeklySummary?.earnings ?: calculateEarningsFromBookings(bookings),
                                totalRides = activityData.weeklySummary?.rides ?: bookings.size,
                                currencySymbol = bookings.firstOrNull()?.currencySymbol ?: "$",
                                error = null
                            )
                        }
                    }.onFailure { earningsException ->
                        // Use activity data if earnings API fails
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            bookings = bookings,
                            totalEarnings = activityData.weeklySummary?.earnings ?: calculateEarningsFromBookings(bookings),
                            totalRides = activityData.weeklySummary?.rides ?: bookings.size,
                            currencySymbol = bookings.firstOrNull()?.currencySymbol ?: "$",
                            error = null
                        )
                        Timber.e(earningsException, "Failed to fetch earnings summary")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = activityResponse.message ?: "Failed to load activity"
                    )
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to fetch activity"
                )
                Timber.e(exception, "Failed to fetch activity")
            }
        }
    }
    
    /**
     * Calculate earnings from bookings
     */
    private fun calculateEarningsFromBookings(bookings: List<DriverBooking>): Double {
        return bookings.sumOf { booking ->
            booking.grandTotal ?: 0.0
        }
    }
    
    /**
     * Go to previous week
     */
    fun goToPreviousWeek() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.time
        
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val weekEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = weekStart,
            selectedWeekEnd = weekEnd
        )
        
        fetchActivityData()
    }
    
    /**
     * Go to next week
     */
    fun goToNextWeek() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.time
        
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val weekEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = weekStart,
            selectedWeekEnd = weekEnd
        )
        
        fetchActivityData()
    }
    
    /**
     * Get formatted date range string
     */
    fun getDateRangeString(): String {
        val startStr = weekRangeFormat.format(_uiState.value.selectedWeekStart)
        val endStr = weekRangeFormat.format(_uiState.value.selectedWeekEnd)
        return "$startStr - $endStr"
    }
    
    /**
     * Get formatted total earnings
     */
    fun getFormattedTotalEarnings(): String {
        val earnings = _uiState.value.totalEarnings
        return String.format("%.2f", earnings)
    }
    
    /**
     * Get formatted online time (placeholder - API doesn't provide this yet)
     */
    fun getFormattedOnlineTime(): String {
        // TODO: Calculate from bookings or get from API
        return "0 hr 0min"
    }
    
    /**
     * Check if can go to previous week
     */
    fun canGoPrevious(): Boolean {
        // Allow going back up to 4 weeks
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.currentWeekStart
        calendar.add(Calendar.WEEK_OF_YEAR, -4)
        return _uiState.value.selectedWeekStart.after(calendar.time)
    }
    
    /**
     * Check if can go to next week
     */
    fun canGoNext(): Boolean {
        // Don't allow going to future weeks
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        return _uiState.value.selectedWeekStart.before(today.time) ||
               _uiState.value.selectedWeekStart == today.time
    }
    
    /**
     * Group bookings by date
     */
    fun getGroupedBookings(): Map<String, List<DriverBooking>> {
        return _uiState.value.bookings.groupBy { booking ->
            booking.pickupDate
        }
    }
    
    /**
     * Get daily total for a date
     */
    fun getDailyTotal(date: String): Double {
        return getGroupedBookings()[date]?.sumOf { booking ->
            booking.grandTotal ?: 0.0
        } ?: 0.0
    }
    
    /**
     * Get formatted daily total
     */
    fun getFormattedDailyTotal(date: String): String {
        val total = getDailyTotal(date)
        return String.format("%.2f", total)
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for My Activity
 */
data class MyActivityUiState(
    val isLoading: Boolean = false,
    val bookings: List<DriverBooking> = emptyList(),
    val totalEarnings: Double = 0.0,
    val totalRides: Int = 0,
    val currencySymbol: String = "$",
    val currentWeekStart: Date = Date(),
    val currentWeekEnd: Date = Date(),
    val selectedWeekStart: Date = Date(),
    val selectedWeekEnd: Date = Date(),
    val error: String? = null
)

