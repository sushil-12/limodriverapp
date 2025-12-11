package com.limo1800driver.app.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.registration.Amenity
import com.limo1800driver.app.data.model.registration.VehicleDetailsRequest
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import com.limo1800driver.app.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleAmenitiesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val chargeableAmenities: List<Amenity> = emptyList(),
    val nonChargeableAmenities: List<Amenity> = emptyList() // Assuming SpecialAmenity maps to this for UI
)

@HiltViewModel
class VehicleAmenitiesViewModel @Inject constructor(
    private val repository: DriverRegistrationRepository,
    private val tokenManager: TokenManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleAmenitiesUiState())
    val uiState: StateFlow<VehicleAmenitiesUiState> = _uiState.asStateFlow()

    // Selection State (IDs)
    val selectedChargeableIds = mutableStateListOf<String>()
    val selectedNonChargeableIds = mutableStateListOf<String>()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Fetch Options (Chargeable & Non-Chargeable)
                // Note: Assuming 'getAllAmenities' returns chargeable and 'getAllSpecialAmenities' returns non-chargeable 
                // based on your iOS logic mapping. Adjust calls if your backend differs.
                val chargeableDeferred = async { repository.getAllAmenities() }
                val nonChargeableDeferred = async { repository.getAllSpecialAmenities() }
                val stepDataDeferred = async { repository.getVehicleDetailsStep() }

                val chargeableResult = chargeableDeferred.await()
                val nonChargeableResult = nonChargeableDeferred.await()
                val stepDataResult = stepDataDeferred.await()

                val chargeableList = chargeableResult.getOrNull()?.data ?: emptyList()
                
                // Map SpecialAmenity to Amenity for UI consistency if needed, or keep separate lists
                val nonChargeableList = nonChargeableResult.getOrNull()?.data?.map { 
                    Amenity(it.id, it.name, it.description) 
                } ?: emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        chargeableAmenities = chargeableList,
                        nonChargeableAmenities = nonChargeableList
                    )
                }

                // 2. Prefill Selections
                val prefill = stepDataResult.getOrNull()?.data?.data
                prefill?.amenities?.let { savedIds ->
                    // Split saved IDs back into chargeable/non-chargeable buckets for UI
                    val savedStringIds = savedIds.map { it.toString() }
                    
                    selectedChargeableIds.clear()
                    selectedChargeableIds.addAll(
                        chargeableList.filter { savedStringIds.contains(it.id.toString()) }.map { it.id.toString() }
                    )

                    selectedNonChargeableIds.clear()
                    selectedNonChargeableIds.addAll(
                        nonChargeableList.filter { savedStringIds.contains(it.id.toString()) }.map { it.id.toString() }
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = errorHandler.handleError(e)) }
            }
        }
    }

    fun toggleChargeable(id: String) {
        if (selectedChargeableIds.contains(id)) selectedChargeableIds.remove(id)
        else selectedChargeableIds.add(id)
    }

    fun toggleNonChargeable(id: String) {
        if (selectedNonChargeableIds.contains(id)) selectedNonChargeableIds.remove(id)
        else selectedNonChargeableIds.add(id)
    }

    fun saveAmenities(onSuccess: () -> Unit) {
        if (selectedChargeableIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one chargeable amenity.") }
            return
        }
        if (selectedNonChargeableIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one non-chargeable amenity.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch current state to merge
                val currentStep = repository.getVehicleDetailsStep().getOrNull()?.data?.data
                    ?: throw Exception("Could not fetch current vehicle details")

                // Combine both lists into 'amenities'
                val combinedAmenities = selectedChargeableIds + selectedNonChargeableIds

                val request = VehicleDetailsRequest(
                    vehicleId = tokenManager.getSelectedVehicleId()?.toIntOrNull(),
                    // Preserve all other fields
                    vehicleType = currentStep.vehicleTypeId ?: 0,
                    make = currentStep.make?.toIntOrNull() ?: 0,
                    model = currentStep.model?.toIntOrNull() ?: 0,
                    year = currentStep.year?.toIntOrNull() ?: 0,
                    color = currentStep.color?.toIntOrNull() ?: 0,
                    seats = currentStep.seats?.toIntOrNull() ?: 0,
                    luggage = currentStep.luggage?.toIntOrNull() ?: 0,
                    licensePlate = currentStep.licensePlate ?: "",
                    nonCharterCancelPolicy = currentStep.nonCharterCancelPolicy?.toIntOrNull() ?: 24,
                    charterCancelPolicy = currentStep.charterCancelPolicy?.toIntOrNull() ?: 48,
                    typeOfService = currentStep.typeOfService ?: emptyList(),
                    
                    // Update Amenities
                    amenities = combinedAmenities,
                    
                    // Preserve Image/Special/Interior data if they exist from future steps (unlikely here but safe)
                    specialAmenities = currentStep.specialAmenities?.map { it.toString() },
                    vehicleInterior = currentStep.vehicleInterior,
                    vehicleImage1 = currentStep.vehicleImage1,
                    vehicleImage2 = currentStep.vehicleImage2,
                    vehicleImage3 = currentStep.vehicleImage3,
                    vehicleImage4 = currentStep.vehicleImage4,
                    vehicleImage5 = currentStep.vehicleImage5,
                    vehicleImage6 = currentStep.vehicleImage6
                )

                repository.completeVehicleDetails(request).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, success = true) }
                        onSuccess()
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, error = errorHandler.handleError(e)) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = errorHandler.handleError(e)) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}