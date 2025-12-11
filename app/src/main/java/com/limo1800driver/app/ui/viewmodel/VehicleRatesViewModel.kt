package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.registration.VehicleInfoData
import com.limo1800driver.app.data.model.registration.VehicleRateSettingsRequest
import com.limo1800driver.app.data.model.registration.VehicleRateSettingsStepPrefillData
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
 * ViewModel for Vehicle Rates Screen
 * Tag: VehicleRatesVM
 */
@HiltViewModel
class VehicleRatesViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleRatesUiState())
    val uiState: StateFlow<VehicleRatesUiState> = _uiState.asStateFlow()

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun fetchVehicleRateSettingsStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleRatesVM").d("Fetching vehicle rate settings step")
                val result = registrationRepository.getVehicleRateSettingsStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("VehicleRatesVM").d("Vehicle rate settings step fetched successfully")
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
                        Timber.tag("VehicleRatesVM").e(error, "Failed to fetch vehicle rate settings step")
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

    fun fetchVehicleInfo() {
        viewModelScope.launch {
            val vehicleId = tokenManager.getSelectedVehicleId()
            if (vehicleId.isNullOrEmpty()) {
                Timber.tag("VehicleRatesVM").w("No selected vehicle id found for vehicle info")
                return@launch
            }
            try {
                Timber.tag("VehicleRatesVM").d("Fetching vehicle info for id=$vehicleId")
                val result = registrationRepository.getVehicleInfo(vehicleId)

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _uiState.value = _uiState.value.copy(vehicleInfo = response.data)
                        } else {
                            Timber.tag("VehicleRatesVM").w("Vehicle info fetch unsuccessful: ${response.message}")
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("VehicleRatesVM").e(error, "Failed to fetch vehicle info")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("VehicleRatesVM").e(e, "Vehicle info request crashed")
            }
        }
    }

    fun completeVehicleRateSettings(request: VehicleRateSettingsRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleRatesVM").d("Completing vehicle rate settings")
                val result = registrationRepository.completeVehicleRateSettings(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Vehicle rate settings completed successfully"
                            )
                            Timber.tag("VehicleRatesVM").d("Vehicle rate settings completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("VehicleRatesVM").e(error, "Failed to complete vehicle rate settings")
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
}

data class VehicleRatesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: VehicleRateSettingsStepPrefillData? = null,
    val isCompleted: Boolean = false,
    val vehicleInfo: VehicleInfoData? = null
)

