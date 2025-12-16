package com.limo1800driver.app.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverWalletDetailsData
import com.limo1800driver.app.data.model.dashboard.TransferDetails
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
 * ViewModel for Wallet Screen
 * Manages wallet balance, transactions, and search
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    /**
     * Fetch wallet data
     */
    fun fetchWalletData() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            dashboardRepository.getDriverWallet(page = 1, perPage = 20)
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val walletData = response.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            balance = walletData.balance?.currentBalance?.toDoubleOrNull() ?: 0.0,
                            currencySymbol = walletData.balance?.currencySymbol ?: "$",
                            transactions = walletData.allTransfers?.data ?: emptyList(),
                            allTransactions = walletData.allTransfers?.data ?: emptyList(),
                            currentPage = 1,
                            canLoadMore = false, // No pagination in new structure
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message ?: "Failed to load wallet"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to fetch wallet"
                    )
                    Timber.e(exception, "Failed to fetch wallet")
                }
        }
    }

    /**
     * Load more transactions
     */
    fun loadMoreTransactions() {
        // Guard clause: stop if already loading or cannot load more
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            // Logic for pagination (currently disabled based on your comments)
            // If you need to add pagination back, you would call the repository here

            // For now, just reset the loading state
            _uiState.value = _uiState.value.copy(isLoadingMore = false)
        }
    }

    /**
     * Search transactions
     */
    fun searchTransactions(query: String) {
        if (query.isBlank()) {
            // Reset to all transactions
            _uiState.value = _uiState.value.copy(
                transactions = _uiState.value.allTransactions,
                searchText = ""
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchText = query)

            val filtered = _uiState.value.allTransactions.filter { transaction ->
                transaction.description?.contains(query, ignoreCase = true) == true ||
                        transaction.status?.contains(query, ignoreCase = true) == true ||
                        transaction.reservationId?.toString()?.contains(query) == true ||
                        transaction.transferGroup?.contains(query, ignoreCase = true) == true
            }

            _uiState.value = _uiState.value.copy(
                isSearching = false,
                transactions = filtered
            )
        }
    }

    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchText = "",
            transactions = _uiState.value.allTransactions
        )
    }

    /**
     * Fetch wallet details
     */
    fun fetchWalletDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetails = true)

            dashboardRepository.getDriverWalletDetails()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingDetails = false,
                            walletDetails = response.data
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingDetails = false,
                            error = response.message
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetails = false,
                        error = exception.message
                    )
                    Timber.e(exception, "Failed to fetch wallet details")
                }
        }
    }

    /**
     * Format transaction date
     */
    fun formatTransactionDate(dateString: String?): String {
        if (dateString == null) return ""
        return try {
            val date = dateFormat.parse(dateString)
            if (date != null) {
                displayDateFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Get transaction description
     */
    fun getTransactionDescription(transaction: TransferDetails): String {
        return transaction.description ?: transaction.status ?: "Transaction"
    }

    /**
     * Get transaction status color
     */
    fun getTransactionStatusColor(status: String?): Color {
        return when (status?.lowercase()) {
            "completed", "paid" -> Color(0xFF4CAF50) // Green
            "pending" -> Color(0xFFFF9800) // Orange
            "failed", "cancelled" -> Color(0xFFF44336) // Red
            else -> Color.Gray
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
 * UI State for Wallet
 */
data class WalletUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val isSearching: Boolean = false,
    val balance: Double = 0.0,
    val availableBalance: Double = 0.0,
    val paidOutBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val currencySymbol: String = "$",
    val transactions: List<TransferDetails> = emptyList(),
    val allTransactions: List<TransferDetails> = emptyList(),
    val currentPage: Int = 1,
    val canLoadMore: Boolean = false,
    val searchText: String = "",
    val walletDetails: DriverWalletDetailsData? = null,
    val error: String? = null
)