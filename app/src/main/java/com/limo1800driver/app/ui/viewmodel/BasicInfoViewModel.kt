package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.BasicInfoCompleteResponse
import com.limo1800driver.app.data.model.registration.BasicInfoRequest
import com.limo1800driver.app.data.model.registration.BasicInfoStepPrefillData
import com.limo1800driver.app.data.model.registration.BasicInfoStepResponse
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
 * ViewModel for Basic Info Screen
 * Handles basic information registration logic
 * Tag: BasicInfoVM
 */
@HiltViewModel
class BasicInfoViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BasicInfoUiState())
    val uiState: StateFlow<BasicInfoUiState> = _uiState.asStateFlow()

    /**
     * Fetch basic info step data for prefill
     */
    fun fetchBasicInfoStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("BasicInfoVM").d("Fetching basic info step")
                val result = registrationRepository.getBasicInfoStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("BasicInfoVM").d("Basic info step fetched successfully")
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
                        Timber.tag("BasicInfoVM").e(error, "Failed to fetch basic info step")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.tag("BasicInfoVM").e(e, "Unexpected error fetching basic info step")
            }
        }
    }

    /**
     * Complete basic info
     */
    fun completeBasicInfo(request: BasicInfoRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("BasicInfoVM").d("Completing basic info")
                val result = registrationRepository.completeBasicInfo(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            // Save basic info email for prefill in other screens
                            tokenManager.saveBasicInfoEmail(request.email)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Basic info completed successfully"
                            )
                            Timber.tag("BasicInfoVM").d("Basic info completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("BasicInfoVM").e(error, "Failed to complete basic info")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.tag("BasicInfoVM").e(e, "Unexpected error completing basic info")
            }
        }
    }

    /**
     * Reset success state (important for back navigation)
     */
    fun resetSuccessState() {
        _uiState.value = _uiState.value.copy(success = false, nextStep = null)
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Basic Info Screen
 */
data class BasicInfoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: BasicInfoStepPrefillData? = null,
    val isCompleted: Boolean = false
)

