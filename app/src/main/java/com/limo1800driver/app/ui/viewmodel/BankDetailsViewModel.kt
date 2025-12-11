package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.BankDetailsCompleteResponse
import com.limo1800driver.app.data.model.registration.BankDetailsRequest
import com.limo1800driver.app.data.model.registration.BankDetailsStepPrefillData
import com.limo1800driver.app.data.model.registration.BankDetailsStepResponse
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
 * ViewModel for Bank Details Screen
 * Tag: BankDetailsVM
 */
@HiltViewModel
class BankDetailsViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(BankDetailsUiState())
    val uiState: StateFlow<BankDetailsUiState> = _uiState.asStateFlow()

    fun fetchBankDetailsStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("BankDetailsVM").d("Fetching bank details step")
                val result = registrationRepository.getBankDetailsStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("BankDetailsVM").d("Bank details step fetched successfully")
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
                        Timber.tag("BankDetailsVM").e(error, "Failed to fetch bank details step")
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

    fun completeBankDetails(request: BankDetailsRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("BankDetailsVM").d("Completing bank details")
                val result = registrationRepository.completeBankDetails(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Bank details completed successfully"
                            )
                            Timber.tag("BankDetailsVM").d("Bank details completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("BankDetailsVM").e(error, "Failed to complete bank details")
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

data class BankDetailsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: BankDetailsStepPrefillData? = null,
    val isCompleted: Boolean = false
)

