package com.limo1800driver.app.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverWalletDetailsData
import com.limo1800driver.app.data.model.dashboard.WalletTransaction
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
    
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val legacyDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    private fun String?.toDoubleSafe(): Double = this?.toDoubleOrNull() ?: 0.0

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

                        val transfers = walletData.transactions
                            ?: walletData.allTransfers?.data
                            ?: emptyList()

                        val currencySymbol =
                            walletData.balance?.currencySymbol
                                ?: walletData.currencySymbol
                                ?: "$"

                        // The backend provides "total_balance" as "total earnings".
                        val totalEarnings =
                            walletData.stripeBalance?.totalBalance.toDoubleSafe()
                                .takeIf { it != 0.0 }
                                ?: walletData.balance?.currentBalance.toDoubleSafe()

                        val availableBalance = walletData.stripeBalance?.availableBalance.toDoubleSafe()
                        val pendingBalance = walletData.stripeBalance?.pendingBalance.toDoubleSafe()
                        val paidOutBalance =
                            walletData.stripeBalance?.paidBalance.toDoubleSafe()
                                .takeIf { it != 0.0 }
                                ?: walletData.balance?.totalPaidAmount.toDoubleSafe()

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            balance = totalEarnings,
                            availableBalance = availableBalance,
                            pendingBalance = pendingBalance,
                            paidOutBalance = paidOutBalance,
                            currencySymbol = currencySymbol,
                            transactions = transfers,
                            allTransactions = transfers,
                            currentPage = walletData.allTransfers?.currentPage ?: 1,
                            canLoadMore =
                                walletData.pagination?.let { it.currentPage < it.lastPage }
                                    ?: walletData.allTransfers?.let { page ->
                                        (page.currentPage != null && page.lastPage != null && page.currentPage < page.lastPage)
                                    }
                                    ?: false,
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
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        
        viewModelScope.launch {
            val nextPage = _uiState.value.currentPage + 1
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            dashboardRepository.getDriverWallet(page = nextPage, perPage = 20)
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val newTransactions = response.data.transactions
                            ?: response.data.allTransfers?.data
                            ?: emptyList()

                        val combined = (_uiState.value.transactions + newTransactions).distinctBy { it.id }
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            transactions = combined,
                            allTransactions = combined,
                            currentPage = response.data.allTransfers?.currentPage ?: nextPage,
                            canLoadMore =
                                response.data.pagination?.let { it.currentPage < it.lastPage }
                                    ?: response.data.allTransfers?.let { page ->
                                        (page.currentPage != null && page.lastPage != null && page.currentPage < page.lastPage)
                                    }
                                    ?: false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingMore = false)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                    Timber.e(exception, "Failed to load more transactions")
                }
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
                transaction.type?.contains(query, ignoreCase = true) == true ||
                transaction.bookingId?.toString()?.contains(query) == true
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
            val date =
                isoDateFormat.parse(dateString)
                    ?: legacyDateFormat.parse(dateString)
            date?.let { displayDateFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * Get transaction description
     */
    fun getTransactionDescription(transaction: WalletTransaction): String {
        return transaction.description ?: transaction.type ?: "Transaction"
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
    val availableBalance: Double = 0.0, // TODO: Get from API
    val paidOutBalance: Double = 0.0, // TODO: Get from API
    val pendingBalance: Double = 0.0, // TODO: Get from API
    val currencySymbol: String = "$",
    val transactions: List<WalletTransaction> = emptyList(),
    val allTransactions: List<WalletTransaction> = emptyList(),
    val currentPage: Int = 1,
    val canLoadMore: Boolean = false,
    val searchText: String = "",
    val walletDetails: DriverWalletDetailsData? = null,
    val error: String? = null
)

