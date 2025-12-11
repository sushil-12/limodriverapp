package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverDashboardStatsData
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Dashboard Stats
 * Manages dashboard statistics (earnings, rides, online time)
 */
@HiltViewModel
class DashboardStatsViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardStatsUiState())
    val uiState: StateFlow<DashboardStatsUiState> = _uiState.asStateFlow()
    
    init {
        fetchDashboardStats()
    }
    
    /**
     * Fetch dashboard stats from API
     */
    fun fetchDashboardStats(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !forceRefresh) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            dashboardRepository.getDashboardStats()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            stats = response.data,
                            error = null
                        )
                        Timber.d("Dashboard stats loaded successfully")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        Timber.e("Dashboard stats API error: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load dashboard stats"
                    )
                    Timber.e(exception, "Failed to fetch dashboard stats")
                }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Get formatted monthly earnings
     */
    fun getMonthlyEarnings(): String {
        val earnings = _uiState.value.stats?.monthly?.earnings ?: 0.0
        return String.format("%.2f", earnings)
    }
    
    /**
     * Get today's rides count
     */
    fun getTodayRides(): String {
        val rides = _uiState.value.stats?.today?.rides ?: 0
        return String.format("%02d", rides)
    }
    
    /**
     * Get currency symbol
     */
    fun getCurrencySymbol(): String {
        return _uiState.value.stats?.currencySymbol ?: "$"
    }
}

/**
 * UI State for Dashboard Stats
 */
data class DashboardStatsUiState(
    val isLoading: Boolean = false,
    val stats: DriverDashboardStatsData? = null,
    val error: String? = null
)

