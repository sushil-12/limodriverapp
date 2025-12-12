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
                    Amenity(it.id, it.idInt, it.name, it.description) 
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
                        chargeableList.filter { savedStringIds.contains(it.getIdentifier()) }.map { it.getIdentifier() }
                    )

                    selectedNonChargeableIds.clear()
                    selectedNonChargeableIds.addAll(
                        nonChargeableList.filter { savedStringIds.contains(it.getIdentifier()) }.map { it.getIdentifier() }
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

    fun saveStep2AndNavigate(onSuccess: () -> Unit) {
        if (selectedChargeableIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one chargeable amenity.") }
            return
        }
        if (selectedNonChargeableIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one non-chargeable amenity.") }
            return
        }

        val combinedAmenities = selectedChargeableIds + selectedNonChargeableIds

        val partial = VehicleDetailsRequest(
            // keep vehicle_id from existing draft; do not override with token default
            vehicleId = null,
            vehicleType = 0,
            make = 0,
            model = 0,
            year = 0,
            color = 0,
            seats = 0,
            luggage = 0,
            numberOfVehicles = 0,
            licensePlate = "",
            nonCharterCancelPolicy = 0,
            charterCancelPolicy = 0,
            amenities = combinedAmenities
        )

        repository.updateLocalDraft(partial)
        _uiState.update { it.copy(success = true, error = null) }
        onSuccess()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}