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
            phoneNumber = phoneNumber
        )
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
            error = if (validationResult is ValidationResult.Error) validationResult.message else null
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
                            // Determine next action based on registration state
                            val nextAction = determineNextAction(response.data.driverRegistrationState)
                            
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "OTP verified successfully",
                                nextAction = nextAction
                            )
                            Timber.tag("OtpVM").d("OTP verified successfully, next action: $nextAction")
                        } else {
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = errorHandler.handleError(error)
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
     */
    private fun determineNextAction(driverRegistrationState: com.limo1800driver.app.data.model.auth.DriverRegistrationState?): String {
        return when {
            driverRegistrationState == null -> "basic_info" // Start registration
            driverRegistrationState.isCompleted -> "dashboard" // Registration complete
            driverRegistrationState.nextStep != null -> driverRegistrationState.nextStep // Use next step from API
            else -> "basic_info" // Default fallback
        }
    }

    /**
     * Resend OTP
     */
    private fun resendOtp() {
        val currentState = _uiState.value
        if (!currentState.canResend) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("OtpVM").d("Resending OTP for temp_user_id: ${currentState.tempUserId}")
                val result = authRepository.resendOTP(currentState.tempUserId)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "OTP resent successfully",
                                resendCooldown = response.data?.cooldownRemaining ?: 60
                            )
                            startResendCooldown()
                            Timber.tag("OtpVM").d("OTP resent successfully")
                        } else {
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = errorHandler.handleError(error)
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        Timber.tag("OtpVM").e(error, "Failed to resend OTP")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = currentState.copy(
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

