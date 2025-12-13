package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverWalletDetailsData
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Driver Wallet
 * Manages wallet balance and transactions
 */
@HiltViewModel
class DriverWalletViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DriverWalletUiState())
    val uiState: StateFlow<DriverWalletUiState> = _uiState.asStateFlow()
    
    /**
     * Fetch driver wallet from API
     */
    fun fetchDriverWallet(page: Int = 1) {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            dashboardRepository.getDriverWallet(page = page, perPage = 10)
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val transfers = response.data.transactions
                            ?: response.data.allTransfers?.data
                            ?: emptyList()

                        val newTransactions = if (page == 1) {
                            transfers
                        } else {
                            (_uiState.value.transactions + transfers).distinctBy { it.id }
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            balance =
                                response.data.stripeBalance?.totalBalance?.toDoubleOrNull()
                                    ?: response.data.balance?.currentBalance?.toDoubleOrNull()
                                    ?: 0.0,
                            currencySymbol =
                                response.data.balance?.currencySymbol
                                    ?: response.data.currencySymbol
                                    ?: "$",
                            transactions = newTransactions,
                            canLoadMore =
                                response.data.pagination?.let { it.currentPage < it.lastPage }
                                    ?: response.data.allTransfers?.let { pageInfo ->
                                        (pageInfo.currentPage != null && pageInfo.lastPage != null && pageInfo.currentPage < pageInfo.lastPage)
                                    }
                                    ?: false,
                            error = null
                        )
                        Timber.d("Driver wallet loaded successfully")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        Timber.e("Driver wallet API error: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load driver wallet"
                    )
                    Timber.e(exception, "Failed to fetch driver wallet")
                }
        }
    }
    
    /**
     * Fetch wallet details
     */
    fun fetchWalletDetails() {
        viewModelScope.launch {
            dashboardRepository.getDriverWalletDetails()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        _uiState.value = _uiState.value.copy(
                            walletDetails = response.data
                        )
                        Timber.d("Wallet details loaded successfully")
                    }
                }
                .onFailure { exception ->
                    Timber.e(exception, "Failed to fetch wallet details")
                }
        }
    }
    
    /**
     * Load more transactions
     */
    fun loadMoreTransactions() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        
        val nextPage = (_uiState.value.transactions.size / 10) + 1
        fetchDriverWallet(page = nextPage)
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Driver Wallet
 */
data class DriverWalletUiState(
    val isLoading: Boolean = false,
    val balance: Double = 0.0,
    val currencySymbol: String = "$",
    val transactions: List<com.limo1800driver.app.data.model.dashboard.WalletTransaction> = emptyList(),
    val walletDetails: DriverWalletDetailsData? = null,
    val canLoadMore: Boolean = false,
    val error: String? = null
)

