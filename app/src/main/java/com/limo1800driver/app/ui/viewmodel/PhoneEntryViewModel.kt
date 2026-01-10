package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.domain.usecase.auth.SendVerificationCodeUseCase
import com.limo1800driver.app.domain.validation.CountryCode
import com.limo1800driver.app.domain.validation.PhoneValidationService
import com.limo1800driver.app.domain.validation.ValidationResult
import com.limo1800driver.app.ui.state.PhoneEntryUiEvent
import com.limo1800driver.app.ui.state.PhoneEntryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhoneEntryViewModel @Inject constructor(
    private val sendVerificationCodeUseCase: SendVerificationCodeUseCase,
    private val phoneValidationService: PhoneValidationService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneEntryUiState())
    val uiState: StateFlow<PhoneEntryUiState> = _uiState.asStateFlow()

    fun onEvent(event: PhoneEntryUiEvent) {
        when (event) {
            is PhoneEntryUiEvent.PhoneNumberChanged -> {
                handlePhoneNumberChanged(event.phoneNumber)
            }
            is PhoneEntryUiEvent.CountryCodeChanged -> {
                handleCountryCodeChanged(event.countryCode, event.phoneLength)
            }
            is PhoneEntryUiEvent.SendVerificationCode -> {
                sendVerificationCode()
            }
            is PhoneEntryUiEvent.ClearError -> {
                clearError()
            }
        }
    }

    private fun handlePhoneNumberChanged(phoneNumber: String) {
        // Clean to digits only
        val rawNumber = phoneNumber.replace(Regex("[^0-9]"), "")

        // Limit to max 15 digits
        val limitedRawNumber = if (rawNumber.length > 15) rawNumber.substring(0, 15) else rawNumber

        val displayNumber = limitedRawNumber

        val validationResult = if (limitedRawNumber.isNotEmpty()) {
            phoneValidationService.validatePhoneNumber(
                limitedRawNumber,
                _uiState.value.selectedCountryCode,
                _uiState.value.phoneLength // Ignored internally
            )
        } else {
            ValidationResult.Success
        }

        // Show error only if input is non-empty and invalid (e.g., too long or wrong format)
        // No specific "remove X digits" - generic invalid message
        val shouldShowError = limitedRawNumber.isNotEmpty() && validationResult is ValidationResult.Error

        _uiState.value = _uiState.value.copy(
            phoneNumber = displayNumber,
            rawPhoneNumber = limitedRawNumber,
            isFormValid = validationResult is ValidationResult.Success && limitedRawNumber.isNotEmpty(),
            error = if (shouldShowError) {
                (validationResult as ValidationResult.Error).message
            } else null
        )
    }

    private fun handleCountryCodeChanged(countryCode: CountryCode, phoneLength: Int) {
        val currentRawPhoneNumber = _uiState.value.rawPhoneNumber

        if (currentRawPhoneNumber.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                selectedCountryCode = countryCode,
                phoneLength = phoneLength,
                isFormValid = false,
                error = null
            )
            return
        }

        val validationResult = phoneValidationService.validatePhoneNumber(
            currentRawPhoneNumber,
            countryCode,
            phoneLength // Ignored
        )

        val shouldShowError = currentRawPhoneNumber.isNotEmpty() && validationResult is ValidationResult.Error

        _uiState.value = _uiState.value.copy(
            selectedCountryCode = countryCode,
            phoneLength = phoneLength,
            isFormValid = validationResult is ValidationResult.Success,
            error = if (shouldShowError) (validationResult as ValidationResult.Error).message else null
        )
    }

    private fun sendVerificationCode() {
        val currentState = _uiState.value

        if (currentState.rawPhoneNumber.isEmpty()) {
            _uiState.value = currentState.copy(
                error = "Please enter your phone number",
                isFormValid = false
            )
            return
        }

        // Strict validation for submission
        if (!phoneValidationService.isValidForSubmission(
                currentState.rawPhoneNumber,
                currentState.selectedCountryCode
            )) {
            _uiState.value = currentState.copy(
                error = "Please enter a valid phone number",
                isFormValid = false
            )
            return
        }

        if (!currentState.isReadyForSubmission()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                Timber.tag("PhoneEntryVM").d("Sending verification code for: ${currentState.selectedCountryCode.code}${currentState.rawPhoneNumber}")
                val result = sendVerificationCodeUseCase(
                    phoneNumber = currentState.rawPhoneNumber,
                    countryCode = currentState.selectedCountryCode.code,
                    countryShortCode = currentState.selectedCountryCode.shortCode
                )

                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            val fullPhoneNumber = "${currentState.selectedCountryCode.code}${currentState.rawPhoneNumber}"

                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "OTP sent successfully",
                                tempUserId = response.data.tempUserId,
                                phoneNumberWithCountryCode = fullPhoneNumber
                            )
                            Timber.tag("PhoneEntryVM").d("Verification code sent successfully")
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
                        Timber.tag("PhoneEntryVM").e(error, "Failed to send verification code")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.tag("PhoneEntryVM").e(e, "Unexpected error sending verification code")
            }
        }
    }

    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}