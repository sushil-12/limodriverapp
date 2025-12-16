package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.RecentTransfer
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
 * ViewModel for Driver Earnings
 * Manages earnings summary and recent payments
 */
@HiltViewModel
class DriverEarningsViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DriverEarningsUiState())
    val uiState: StateFlow<DriverEarningsUiState> = _uiState.asStateFlow()
    
    /**
     * Fetch earnings summary from API
     */
    fun fetchEarningsSummary(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !forceRefresh) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            dashboardRepository.getDriverEarningsSummary()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val earningsData = response.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            earningsSummary = earningsData,
                            error = null
                        )
                        Timber.d("Earnings summary loaded successfully")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message ?: "Failed to load earnings"
                        )
                        Timber.e("Failed to load earnings: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Network error"
                    )
                    Timber.e(exception, "Error loading earnings summary")
                }
        }
    }
    
    /**
     * Get currency symbol from currency code
     */
    fun getCurrencySymbol(currency: String?): String? {
        if (currency == null) return null
        // Map common currency codes to symbols
        return when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "CAD" -> "C$"
            "AUD" -> "A$"
            else -> currency
        }
    }
    
    /**
     * Format date string
     */
    fun formatDate(dateString: String?): String {
        if (dateString == null) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            try {
                // Try alternative format
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e2: Exception) {
                dateString
            }
        }
    }
    
    
    /**
     * Get recent payments (filtered to only PAID status)
     */
    val recentPayments: List<RecentTransfer>
        get() = _uiState.value.earningsSummary?.stripeData?.recentTransfers
            ?.filter { it.status?.uppercase() == "PAID" } ?: emptyList()
    
    /**
     * Get total earnings (paid balance)
     */
    val totalEarnings: String
        get() = _uiState.value.earningsSummary?.stripeData?.paidBalance ?: "0.00"
    
    /**
     * Get currency symbol
     */
    val currencySymbol: String?
        get() = getCurrencySymbol(_uiState.value.earningsSummary?.currency)
    
    /**
     * Get next payout date
     */
    val nextPayoutDate: String?
        get() = _uiState.value.earningsSummary?.stripeData?.nextPayoutDate
}

/**
 * UI State for Driver Earnings
 */
data class DriverEarningsUiState(
    val isLoading: Boolean = false,
    val earningsSummary: com.limo1800driver.app.data.model.dashboard.DriverEarningsSummaryData? = null,
    val error: String? = null
)

