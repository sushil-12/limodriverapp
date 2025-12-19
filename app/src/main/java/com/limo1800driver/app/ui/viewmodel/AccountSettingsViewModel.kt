package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverProfileData
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Account Settings Screen
 * Manages driver profile and account information
 */
@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository,
    private val registrationRepository: DriverRegistrationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()
    
    init {
        fetchProfileData()
    }
    
    /**
     * Fetch profile data
     */
    fun fetchProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Fetch driver profile
            dashboardRepository.getDriverProfile()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val profile = response.data
                        _uiState.value = _uiState.value.copy(
                            profile = profile,
                            affiliateType = profile.affiliateType
                        )
                    }
                }
                .onFailure { exception ->
                    Timber.e(exception, "Failed to fetch driver profile")
                }
            
            // Fetch basic info to get affiliate type
            registrationRepository.getBasicInfoStep()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val prefillData = response.data.data
                        _uiState.value = _uiState.value.copy(
                            affiliateType = prefillData?.affiliateType ?: _uiState.value.affiliateType
                        )
                    }
                }
                .onFailure { exception ->
                    Timber.e(exception, "Failed to fetch basic info")
                }
            
            // TODO: Fetch vehicle count from vehicle details step if needed
            // For now, vehicle count will be 0 or can be passed from elsewhere
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Get full driver name
     */
    fun getFullName(): String {
        val profile = _uiState.value.profile ?: return ""
        val firstName = profile.driverFirstName ?: ""
        val lastName = profile.driverLastName ?: ""
        return "$firstName $lastName".trim().ifEmpty { "Driver" }
    }
    
    /**
     * Get driver image URL
     */
    fun getDriverImageURL(): String? {
        return _uiState.value.profile?.driverImage
    }

    /**
     * Get driver rating
     */
    fun getDriverRating(): String {
        return _uiState.value.profile?.driverRating?.toString() ?: "0.0"
    }

    /**
     * Check if should show company details
     */
    fun shouldShowCompanyDetails(): Boolean {
        return _uiState.value.affiliateType?.lowercase() != "gig_operator"
    }
    
    /**
     * Refresh profile data - public method for manual refresh
     */
    fun refreshProfileData() {
        fetchProfileData()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Account Settings
 */
data class AccountSettingsUiState(
    val isLoading: Boolean = false,
    val profile: DriverProfileData? = null,
    val affiliateType: String? = null,
    val vehicleCount: Int = 0,
    val error: String? = null
)

