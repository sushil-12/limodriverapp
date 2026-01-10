package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.repository.DriverAuthRepository
import com.limo1800driver.app.domain.usecase.auth.VerifyOTPUseCase
import com.limo1800driver.app.domain.validation.OTPValidationService
import com.limo1800driver.app.domain.validation.ValidationResult
import com.limo1800driver.app.ui.state.OtpUiEvent
import com.limo1800driver.app.ui.state.OtpUiState
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
 * ViewModel for OTP Screen
 * Follows single responsibility principle and handles OTP verification logic
 * Tag: OtpVM
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    private val verifyOTPUseCase: VerifyOTPUseCase,
    private val authRepository: DriverAuthRepository,
    private val otpValidationService: OTPValidationService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var resendCooldownJob: Job? = null

    /**
     * Set initial data from previous screen
     */
    fun setInitialData(tempUserId: String, phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            tempUserId = tempUserId,
            phoneNumber = phoneNumber,
            otp = "", // Clear any previous OTP
            isFormValid = false,
            error = null,
            message = null,
            isLoading = false,
            success = false,
            nextAction = null
        )
        // Cancel any existing cooldown job
        resendCooldownJob?.cancel()
        // Set up resend cooldown timer
        startResendCooldown()
    }

    /**
     * Handle UI events
     */
    fun onEvent(event: OtpUiEvent) {
        when (event) {
            is OtpUiEvent.OtpChanged -> {
                handleOtpChanged(event.otp)
            }
            is OtpUiEvent.VerifyOtp -> {
                verifyOtp()
            }
            is OtpUiEvent.ResendOtp -> {
                resendOtp()
            }
            is OtpUiEvent.ClearError -> {
                clearError()
            }
        }
    }

    /**
     * Handle OTP input changes
     */
    private fun handleOtpChanged(otp: String) {
        // Clean the OTP (remove spaces) for validation and storage
        val cleanOtp = otp.replace(" ", "")
        val validationResult = otpValidationService.validateOTP(cleanOtp)
        
        _uiState.value = _uiState.value.copy(
            otp = cleanOtp, // Store clean OTP without spaces
            isFormValid = validationResult is ValidationResult.Success,
            error = if (validationResult is ValidationResult.Error) validationResult.message else null,
            success = false, // Reset success state when OTP changes
            message = null // Clear message when user starts typing
        )
    }

    /**
     * Verify OTP
     */
    private fun verifyOtp() {
        val currentState = _uiState.value
        if (!currentState.isReadyForSubmission()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("OtpVM").d("Verifying OTP for temp_user_id: ${currentState.tempUserId}")
                val result = verifyOTPUseCase(
                    tempUserId = currentState.tempUserId,
                    otp = currentState.otp
                )
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            // Log the full response for debugging
                            // Check both locations: top-level and inside user object
                            val registrationState = response.data.driverRegistrationState 
                                ?: response.data.user.driverRegistrationState
                            
                            Timber.tag("OtpVM").d("OTP verification response received:")
                            Timber.tag("OtpVM").d("  - driverRegistrationState (top-level): ${response.data.driverRegistrationState}")
                            Timber.tag("OtpVM").d("  - driverRegistrationState (user-level): ${response.data.user.driverRegistrationState}")
                            Timber.tag("OtpVM").d("  - final driverRegistrationState: $registrationState")
                            registrationState?.let { state ->
                                Timber.tag("OtpVM").d("  - current_step: ${state.currentStep}")
                                Timber.tag("OtpVM").d("  - next_step: ${state.nextStep}")
                                Timber.tag("OtpVM").d("  - is_completed: ${state.isCompleted}")
                                Timber.tag("OtpVM").d("  - progress_percentage: ${state.progressPercentage}")
                            }
                            
                            // Determine next action based on registration state
                            val nextAction = determineNextAction(registrationState)
                            
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "OTP verified successfully",
                                nextAction = nextAction
                            )
                            Timber.tag("OtpVM").d("OTP verified successfully, navigating to: $nextAction")
                        } else {
                            Timber.tag("OtpVM").w("OTP verification failed: ${response.message}")
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = errorHandler.handleError(error)
                        Timber.tag("OtpVM").e(error, "Failed to verify OTP ${errorMessage}")

                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        Timber.tag("OtpVM").e(error, "Failed to verify OTP")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.tag("OtpVM").e(e, "Unexpected error verifying OTP")
            }
        }
    }

    /**
     * Determine next action based on registration state
     * Optimized to properly handle next_step from API response
     */
    private fun determineNextAction(driverRegistrationState: com.limo1800driver.app.data.model.auth.DriverRegistrationState?): String {
        Timber.tag("OtpVM").d("Determining next action from registration state: $driverRegistrationState")
        
        return when {
            // If no registration state, start from basic_info
            driverRegistrationState == null -> {
                Timber.tag("OtpVM").d("No registration state, defaulting to basic_info")
                "basic_info"
            }
            
            // If registration is completed, go to dashboard
            driverRegistrationState.isCompleted -> {
                Timber.tag("OtpVM").d("Registration completed, navigating to dashboard")
                "dashboard"
            }
            
            // Priority: Use next_step from API if it's not null or empty
            driverRegistrationState.nextStep != null && driverRegistrationState.nextStep.isNotBlank() -> {
                val nextStep = driverRegistrationState.nextStep.trim()
                Timber.tag("OtpVM").d("Using next_step from API: $nextStep (current_step: ${driverRegistrationState.currentStep})")
                nextStep
            }
            
            // Fallback: Use current_step if next_step is not available
            driverRegistrationState.currentStep.isNotBlank() -> {
                val currentStep = driverRegistrationState.currentStep.trim()
                Timber.tag("OtpVM").d("next_step not available, using current_step: $currentStep")
                currentStep
            }
            
            // Final fallback: Start from basic_info
            else -> {
                Timber.tag("OtpVM").d("No valid step found, defaulting to basic_info")
                "basic_info"
            }
        }
    }

    /**
     * Resend OTP
     * Note: UI manages its own cooldown timer, so we don't check canResend here
     */
    private fun resendOtp() {
        val currentState = _uiState.value

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isLoading = true,
                error = null,
                otp = "", // Clear the old OTP when resending
                isFormValid = false,
                success = false // Reset success state
            )
            
            try {
                Timber.tag("OtpVM").d("Resending OTP for temp_user_id: ${currentState.tempUserId}")
                val result = authRepository.resendOTP(currentState.tempUserId)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = false, // Don't set success=true for resend, just show message
                                message = "OTP resent successfully. Please enter the new code.",
                                resendCooldown = response.data?.cooldownRemaining ?: 60
                            )
                            startResendCooldown()
                            Timber.tag("OtpVM").d("OTP resent successfully")
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = errorHandler.handleError(error)

                        // Check if this is a rate limit error and start cooldown
                        val isRateLimitError = errorMessage.contains("wait", ignoreCase = true) ||
                                              errorMessage.contains("60 seconds", ignoreCase = true) ||
                                              errorMessage.contains("Too many requests", ignoreCase = true) ||
                                              errorMessage.contains("cooldown", ignoreCase = true)

                        if (isRateLimitError) {
                            // Start 60-second cooldown for rate limit errors
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorMessage,
                                resendCooldown = 60
                            )
                            startResendCooldown()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        }

                        Timber.tag("OtpVM").e(error, "Failed to resend OTP")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.tag("OtpVM").e(e, "Unexpected error resending OTP")
            }
        }
    }

    /**
     * Start resend cooldown timer
     */
    private fun startResendCooldown() {
        resendCooldownJob?.cancel()
        resendCooldownJob = viewModelScope.launch {
            val initialCooldown = _uiState.value.resendCooldown
            if (initialCooldown <= 0) {
                _uiState.value = _uiState.value.copy(canResend = true, resendCooldown = 0)
                return@launch
            }

            for (i in initialCooldown downTo 0) {
                _uiState.value = _uiState.value.copy(
                    resendCooldown = i,
                    canResend = i == 0
                )
                if (i > 0) delay(1000)
            }
        }
    }

    /**
     * Clear error state
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        resendCooldownJob?.cancel()
    }
}

