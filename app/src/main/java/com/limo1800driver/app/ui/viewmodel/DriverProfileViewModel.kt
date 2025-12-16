package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.notification.DriverFirebaseSubscriptionManager
import com.limo1800driver.app.data.model.dashboard.DriverProfileData
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Driver Profile
 * Manages driver profile information
 */
@HiltViewModel
class DriverProfileViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository,
    private val firebaseSubscriptionManager: DriverFirebaseSubscriptionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DriverProfileUiState())
    val uiState: StateFlow<DriverProfileUiState> = _uiState.asStateFlow()
    
    /**
     * Fetch driver profile from API
     */
    fun fetchDriverProfile() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            dashboardRepository.getDriverProfile()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profile = response.data,
                            error = null
                        )
                        Timber.d("Driver profile loaded successfully")

                        // iOS parity: subscribe to topic == userId (string)
                        val topicUserId = response.data.userId ?: response.data.driverId
                        if (topicUserId != null) {
                            firebaseSubscriptionManager.subscribeToUserTopic(topicUserId.toString())
                        } else {
                            Timber.tag("DriverFCM").w("Cannot subscribe to topic: userId/driverId missing in profile")
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        Timber.e("Driver profile API error: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load driver profile"
                    )
                    Timber.e(exception, "Failed to fetch driver profile")
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
     * Get full driver name
     */
    fun getFullName(): String {
        val profile = _uiState.value.profile ?: return ""
        val firstName = profile.driverFirstName ?: ""
        val lastName = profile.driverLastName ?: ""
        return "$firstName $lastName".trim()
    }
}

/**
 * UI State for Driver Profile
 */
data class DriverProfileUiState(
    val isLoading: Boolean = false,
    val profile: DriverProfileData? = null,
    val error: String? = null
)

