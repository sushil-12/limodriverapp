package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.auth.DriverRegistrationState
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import com.limo1800driver.app.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Splash Screen
 * Fetches current registration state if needed
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /**
     * Check and sync registration state if needed
     */
    fun syncRegistrationState(onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                // If we have a token but no stored registration state, fetch it
                if (tokenManager.isAuthenticated() && tokenManager.getDriverRegistrationState() == null) {
                    Timber.tag("SplashVM").d("Token found but no registration state, fetching current state")
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    
                    val result = registrationRepository.getAllSteps()
                    result.fold(
                        onSuccess = { response ->
                            if (response.success && response.data != null) {
                                // Determine next step from completion status
                                val nextStep = determineNextStepFromAllSteps(response.data)
                                
                                // Create a minimal registration state
                                val state = DriverRegistrationState(
                                    currentStep = nextStep ?: "dashboard",
                                    progressPercentage = calculateProgress(response.data),
                                    isCompleted = nextStep == null,
                                    nextStep = nextStep,
                                    steps = null,
                                    completedSteps = null,
                                    totalSteps = null,
                                    completedCount = null
                                )
                                
                                tokenManager.saveDriverRegistrationState(state)
                                Timber.tag("SplashVM").d("Registration state synced: next_step=$nextStep")
                                
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                onComplete(nextStep)
                            } else {
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                onComplete(null)
                            }
                        },
                        onFailure = { error ->
                            Timber.tag("SplashVM").e(error, "Failed to fetch registration state")
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onComplete(null)
                        }
                    )
                } else {
                    // Use stored state or proceed normally
                    val storedState = tokenManager.getDriverRegistrationState()
                    val nextStep = storedState?.nextStep ?: tokenManager.getNextStep()
                    Timber.tag("SplashVM").d("Using stored state: next_step=$nextStep")
                    onComplete(nextStep)
                }
            } catch (e: Exception) {
                Timber.tag("SplashVM").e(e, "Error syncing registration state")
                _uiState.value = _uiState.value.copy(isLoading = false)
                onComplete(null)
            }
        }
    }
    
    /**
     * Determine next step from all steps completion status
     */
    private fun determineNextStepFromAllSteps(steps: com.limo1800driver.app.data.model.registration.RegistrationStepsData): String? {
        return when {
            steps.basicInfo?.isCompleted != true -> "basic_info"
            steps.companyInfo?.isCompleted != true -> "company_info"
            steps.companyDocuments?.isCompleted != true -> "company_documents"
            steps.privacyTerms?.isCompleted != true -> "privacy_terms"
            steps.drivingLicense?.isCompleted != true -> "driving_license"
            steps.bankDetails?.isCompleted != true -> "bank_details"
            steps.profilePicture?.isCompleted != true -> "profile_picture"
            steps.vehicleInsurance?.isCompleted != true -> "vehicle_insurance"
            steps.vehicleDetails?.isCompleted != true -> "vehicle_details"
            steps.vehicleRateSettings?.isCompleted != true -> "vehicle_rate_settings"
            else -> null // All steps completed
        }
    }
    
    /**
     * Calculate progress percentage
     */
    private fun calculateProgress(steps: com.limo1800driver.app.data.model.registration.RegistrationStepsData): Int {
        val totalSteps = 10
        var completed = 0
        
        if (steps.basicInfo?.isCompleted == true) completed++
        if (steps.companyInfo?.isCompleted == true) completed++
        if (steps.companyDocuments?.isCompleted == true) completed++
        if (steps.privacyTerms?.isCompleted == true) completed++
        if (steps.drivingLicense?.isCompleted == true) completed++
        if (steps.bankDetails?.isCompleted == true) completed++
        if (steps.profilePicture?.isCompleted == true) completed++
        if (steps.vehicleInsurance?.isCompleted == true) completed++
        if (steps.vehicleDetails?.isCompleted == true) completed++
        if (steps.vehicleRateSettings?.isCompleted == true) completed++
        
        return (completed * 100) / totalSteps
    }
}

data class SplashUiState(
    val isLoading: Boolean = false
)

