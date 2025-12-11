package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.Amenity
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
 * ViewModel for Vehicle Amenities Screen
 * Tag: VehicleAmenitiesVM
 */
@HiltViewModel
class VehicleAmenitiesViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleAmenitiesUiState())
    val uiState: StateFlow<VehicleAmenitiesUiState> = _uiState.asStateFlow()

    fun fetchAmenities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleAmenitiesVM").d("Fetching amenities")
                
                // Fetch chargeable amenities (all amenities)
                val chargeableResult = registrationRepository.getAllAmenities()
                // Fetch non-chargeable amenities (special amenities)
                val nonChargeableResult = registrationRepository.getAllSpecialAmenities()
                
                chargeableResult.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                chargeableAmenities = response.data
                            )
                            Timber.tag("VehicleAmenitiesVM").d("Chargeable amenities fetched: ${response.data.size}")
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("VehicleAmenitiesVM").e(error, "Failed to fetch chargeable amenities")
                    }
                )
                
                nonChargeableResult.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                nonChargeableAmenities = response.data.map { 
                                    // Convert SpecialAmenity to Amenity for UI consistency
                                    com.limo1800driver.app.data.model.registration.Amenity(
                                        id = it.id,
                                        name = it.name,
                                        description = it.description
                                    )
                                }
                            )
                            Timber.tag("VehicleAmenitiesVM").d("Non-chargeable amenities fetched: ${response.data.size}")
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorHandler.handleError(error)
                        )
                        Timber.tag("VehicleAmenitiesVM").e(error, "Failed to fetch non-chargeable amenities")
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

    fun saveSelectedAmenities(amenityIds: List<Int>) {
        viewModelScope.launch {
            // Store selected amenities - they will be used when completing vehicle details
            _uiState.value = _uiState.value.copy(
                selectedAmenities = amenityIds,
                success = true
            )
            Timber.tag("VehicleAmenitiesVM").d("Selected amenities saved: ${amenityIds.size}")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class VehicleAmenitiesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val nextStep: String? = null,
    val chargeableAmenities: List<Amenity> = emptyList(),
    val nonChargeableAmenities: List<Amenity> = emptyList(),
    val selectedAmenities: List<Int> = emptyList()
)

