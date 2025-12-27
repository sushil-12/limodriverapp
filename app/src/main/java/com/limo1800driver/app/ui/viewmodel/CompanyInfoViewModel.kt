package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.CompanyInfoCompleteResponse
import com.limo1800driver.app.data.model.registration.CompanyInfoRequest
import com.limo1800driver.app.data.model.registration.CompanyInfoStepPrefillData
import com.limo1800driver.app.data.model.registration.CompanyInfoStepResponse
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
 * ViewModel for Company Info Screen
 * Tag: CompanyInfoVM
 */
@HiltViewModel
class CompanyInfoViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyInfoUiState())
    val uiState: StateFlow<CompanyInfoUiState> = _uiState.asStateFlow()

    fun fetchCompanyInfoStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("CompanyInfoVM").d("Fetching company info step")
                val result = registrationRepository.getCompanyInfoStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("CompanyInfoVM").d("Company info step fetched successfully")
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
                        Timber.tag("CompanyInfoVM").e(error, "Failed to fetch company info step")
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

    fun completeCompanyInfo(request: CompanyInfoRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("CompanyInfoVM").d("Completing company info")
                val result = registrationRepository.completeCompanyInfo(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Company info completed successfully"
                            )
                            Timber.tag("CompanyInfoVM").d("Company info completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("CompanyInfoVM").e(error, "Failed to complete company info")
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

    /**
     * Get basic info data for prefill
     */
    fun getBasicInfoData(): BasicInfoPrefillData {
        return BasicInfoPrefillData(
            email = tokenManager.getBasicInfoEmail()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset success state (important for back navigation)
     */
    fun resetSuccessState() {
        _uiState.value = _uiState.value.copy(success = false, nextStep = null)
    }
}

data class CompanyInfoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: CompanyInfoStepPrefillData? = null,
    val isCompleted: Boolean = false
)

data class BasicInfoPrefillData(
    val email: String? = null
)

