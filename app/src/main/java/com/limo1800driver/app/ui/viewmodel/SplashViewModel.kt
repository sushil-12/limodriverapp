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
                // First, ensure we have the affiliate type available
                // This is crucial for proper step determination for gig operators
                if (tokenManager.getAffiliateType() == null) {
                    Timber.tag("SplashVM").d("Affiliate type not available locally, fetching basic info step")
                    try {
                        val basicInfoResult = registrationRepository.getBasicInfoStep()
                        basicInfoResult.fold(
                            onSuccess = { basicInfoResponse ->
                                if (basicInfoResponse.success && basicInfoResponse.data != null) {
                                    // Save affiliate type from prefill data
                                    basicInfoResponse.data.data?.affiliateType?.let { affiliateType ->
                                        tokenManager.saveAffiliateType(affiliateType)
                                        Timber.tag("SplashVM").d("Affiliate type fetched from basic info step: $affiliateType")
                                    }
                                }
                            },
                            onFailure = { basicInfoError ->
                                Timber.tag("SplashVM").w(basicInfoError, "Failed to fetch basic info step for affiliate type")
                            }
                        )
                    } catch (basicInfoException: Exception) {
                        Timber.tag("SplashVM").w(basicInfoException, "Error fetching basic info step")
                    }
                }

                // Always sync with API when authenticated to ensure we have latest state
                Timber.tag("SplashVM").d("Token found, syncing with API for latest registration state")
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = registrationRepository.getAllSteps()
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            // Determine next step from completion status
                            val nextStep = determineNextStepFromAllSteps(response.data)

                            // Check if all steps are completed
                            val allStepsCompleted = nextStep == null

                            // If all steps are completed, mark profile as completed
                            if (allStepsCompleted) {
                                tokenManager.saveProfileCompleted(true)
                                Timber.tag("SplashVM").d("All registration steps completed - marking profile as completed")
                            }

                            // Create a minimal registration state
                            val state = DriverRegistrationState(
                                currentStep = if (allStepsCompleted) "dashboard" else nextStep ?: "dashboard",
                                progressPercentage = calculateProgress(response.data),
                                isCompleted = allStepsCompleted,
                                nextStep = if (allStepsCompleted) null else nextStep,
                                steps = null,
                                completedSteps = null,
                                totalSteps = null,
                                completedCount = null
                            )

                            tokenManager.saveDriverRegistrationState(state)
                            Timber.tag("SplashVM").d("Registration state synced: next_step=$nextStep, all_completed=$allStepsCompleted")

                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onComplete(nextStep)
                        } else {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onComplete(null)
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("SplashVM").e(error, "Failed to fetch registration state, falling back to stored state")
                        // Fallback to stored state if API fails
                        val storedState = tokenManager.getDriverRegistrationState()
                        val nextStep = storedState?.nextStep ?: tokenManager.getNextStep()
                        Timber.tag("SplashVM").d("Using stored state: next_step=$nextStep")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onComplete(nextStep)
                    }
                )
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
        // Check if user is a gig operator to skip company-related steps
        val isGigOperator = tokenManager.getAffiliateType()?.lowercase() == "gig_operator"

        return when {
            steps.basicInfo?.isCompleted != true -> "basic_info"
            // Skip company info and company documents for gig operators
            !isGigOperator && steps.companyInfo?.isCompleted != true -> "company_info"
            !isGigOperator && steps.companyDocuments?.isCompleted != true -> "company_documents"
            steps.privacyTerms?.isCompleted != true -> "privacy_terms"
            steps.drivingLicense?.isCompleted != true -> "driving_license"
            steps.bankDetails?.isCompleted != true -> "bank_details"
            steps.profilePicture?.isCompleted != true -> "profile_picture"
            // Check if vehicle is selected when profile_picture is completed but vehicle_insurance is not
            steps.vehicleInsurance?.isCompleted != true -> {
                // If profile_picture is completed but no vehicle is selected, show vehicle selection first
                if (steps.profilePicture?.isCompleted == true && tokenManager.getSelectedVehicleId().isNullOrBlank()) {
                    "vehicle_selection"
                } else {
                    "vehicle_details_step" // Go directly to step management screen after vehicle selection
                }
            }
            steps.vehicleDetails?.isCompleted != true -> "vehicle_details"
            steps.vehicleRateSettings?.isCompleted != true -> "vehicle_rate_settings"
            else -> null // All steps completed
        }
    }
    
    /**
     * Calculate progress percentage
     */
    private fun calculateProgress(steps: com.limo1800driver.app.data.model.registration.RegistrationStepsData): Int {
        // Check if user is a gig operator to adjust total steps
        val isGigOperator = tokenManager.getAffiliateType()?.lowercase() == "gig_operator"
        val totalSteps = if (isGigOperator) 8 else 10 // Gig operators skip company info and documents
        var completed = 0

        if (steps.basicInfo?.isCompleted == true) completed++
        // Only count company steps for non-gig operators
        if (!isGigOperator && steps.companyInfo?.isCompleted == true) completed++
        if (!isGigOperator && steps.companyDocuments?.isCompleted == true) completed++
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

