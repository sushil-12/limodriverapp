package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.PrivacyTermsCompleteResponse
import com.limo1800driver.app.data.model.registration.PrivacyTermsRequest
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Privacy & Terms Screen
 * Tag: PrivacyTermsVM
 */
@HiltViewModel
class PrivacyTermsViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyTermsUiState())
    val uiState: StateFlow<PrivacyTermsUiState> = _uiState.asStateFlow()

    fun completePrivacyTerms(privacyAccepted: Boolean, termsAccepted: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("PrivacyTermsVM").d("Completing privacy & terms")
                val request = PrivacyTermsRequest(
                    privacyAccepted = privacyAccepted,
                    termsAccepted = termsAccepted
                )
                val result = registrationRepository.completePrivacyTerms(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Privacy & terms accepted successfully"
                            )
                            Timber.tag("PrivacyTermsVM").d("Privacy & terms completed, next_step: ${response.data.nextStep}")
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = errorHandler.handleError(error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        Timber.tag("PrivacyTermsVM").e(error, "Failed to complete privacy & terms")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class PrivacyTermsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null
)

