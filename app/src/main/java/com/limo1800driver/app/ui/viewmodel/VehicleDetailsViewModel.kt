package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.*
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
 * ViewModel for Vehicle Details Screen
 * Tag: VehicleDetailsVM
 */
@HiltViewModel
class VehicleDetailsViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailsUiState())
    val uiState: StateFlow<VehicleDetailsUiState> = _uiState.asStateFlow()

    fun fetchVehicleDetailsStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleDetailsVM").d("Fetching vehicle details step")
                val result = registrationRepository.getVehicleDetailsStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("VehicleDetailsVM").d("Vehicle details step fetched successfully")
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
                        Timber.tag("VehicleDetailsVM").e(error, "Failed to fetch vehicle details step")
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

    fun fetchVehicleTypes() {
        viewModelScope.launch {
            try {
                Timber.tag("VehicleDetailsVM").d("Fetching vehicle types")
                val result = registrationRepository.getVehicleTypes()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                vehicleTypes = response.data
                            )
                            Timber.tag("VehicleDetailsVM").d("Vehicle types fetched: ${response.data.size}")
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("VehicleDetailsVM").e(error, "Failed to fetch vehicle types")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("VehicleDetailsVM").e(e, "Error fetching vehicle types")
            }
        }
    }

    fun completeVehicleDetails(request: VehicleDetailsRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleDetailsVM").d("Completing vehicle details")
                val result = registrationRepository.completeVehicleDetails(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Vehicle details completed successfully"
                            )
                            Timber.tag("VehicleDetailsVM").d("Vehicle details completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("VehicleDetailsVM").e(error, "Failed to complete vehicle details")
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

data class VehicleDetailsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: VehicleDetailsStepPrefillData? = null,
    val isCompleted: Boolean = false,
    val vehicleTypes: List<VehicleType> = emptyList()
)

