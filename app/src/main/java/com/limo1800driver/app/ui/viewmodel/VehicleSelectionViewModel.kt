package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.VehicleType
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
 * ViewModel for Vehicle Selection Screen
 * Tag: VehicleSelectionVM
 */
@HiltViewModel
class VehicleSelectionViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val tokenManager: TokenManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleSelectionUiState())
    val uiState: StateFlow<VehicleSelectionUiState> = _uiState.asStateFlow()

    fun fetchVehicleTypes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleSelectionVM").d("Fetching vehicle types")
                val result = registrationRepository.getVehicleTypes()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                vehicles = response.data
                            )
                            Timber.tag("VehicleSelectionVM").d("Vehicle types fetched: ${response.data.size}")
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
                        Timber.tag("VehicleSelectionVM").e(error, "Failed to fetch vehicle types")
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

    suspend fun saveSelectedVehicle(vehicle: VehicleType) {
        try {
            // Save vehicle ID to storage for later use (only if it's a real vehicle ID, not a type ID)
            val vehicleId = vehicle.vehicleId ?: "0" // Use "0" for new vehicles, not the type identifier
            tokenManager.saveSelectedVehicleId(vehicleId)
            Timber.tag("VehicleSelectionVM").d("Saved vehicle ID: $vehicleId")
            _uiState.value = _uiState.value.copy(success = true)
        } catch (e: Exception) {
            Timber.tag("VehicleSelectionVM").e(e, "Failed to save selected vehicle")
            _uiState.value = _uiState.value.copy(
                error = errorHandler.handleError(e)
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class VehicleSelectionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val vehicles: List<VehicleType> = emptyList()
)

