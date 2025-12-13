package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverUpdate
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Notifications Screen
 * Handles notifications list, search, and filtering
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    
    init {
        fetchNotifications()
    }
    
    /**
     * Fetch notifications from API
     */
    fun fetchNotifications() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            dashboardRepository.getDriverUpdates()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val updates = response.data.updates ?: emptyList()
                        
                        // Filter by search text if present
                        val filteredUpdates = if (_uiState.value.searchText.isNotBlank()) {
                            updates.filter { update ->
                                update.message?.contains(_uiState.value.searchText, ignoreCase = true) == true ||
                                update.type?.contains(_uiState.value.searchText, ignoreCase = true) == true ||
                                update.bookingId?.toString()?.contains(_uiState.value.searchText) == true
                            }
                        } else {
                            updates
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            notifications = filteredUpdates,
                            allNotifications = updates,
                            error = null
                        )
                        Timber.d("Notifications loaded: ${filteredUpdates.size}")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message ?: "Failed to load notifications"
                        )
                        Timber.e("Notifications API error: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to fetch notifications"
                    )
                    Timber.e(exception, "Failed to fetch notifications")
                }
        }
    }
    
    /**
     * Refresh notifications
     */
    fun refreshNotifications() {
        fetchNotifications()
    }
    
    /**
     * Update search text with debounce
     */
    fun updateSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
            // Filter notifications based on search text
            val filtered = if (text.isBlank()) {
                _uiState.value.allNotifications
            } else {
                _uiState.value.allNotifications.filter { notification ->
                    notification.message?.contains(text, ignoreCase = true) == true ||
                    notification.type?.contains(text, ignoreCase = true) == true ||
                    notification.bookingId?.toString()?.contains(text) == true
                }
            }
            _uiState.value = _uiState.value.copy(notifications = filtered)
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchText = "",
            notifications = _uiState.value.allNotifications
        )
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Notifications
 */
data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<DriverUpdate> = emptyList(),
    val allNotifications: List<DriverUpdate> = emptyList(),
    val searchText: String = "",
    val error: String? = null
)

