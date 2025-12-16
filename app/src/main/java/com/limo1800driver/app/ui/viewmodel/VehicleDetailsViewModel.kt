package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.registration.*
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import com.limo1800driver.app.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VehicleDetailsViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val tokenManager: TokenManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailsUiState())
    val uiState: StateFlow<VehicleDetailsUiState> = _uiState.asStateFlow()

    private var prefillVehicleId: Int? = null

    // Form State (IDs)
    var selectedVehicleTypeId = MutableStateFlow<Int?>(null)
    var selectedMakeId = MutableStateFlow<Int?>(null)
    var selectedModelId = MutableStateFlow<Int?>(null)
    var selectedYearId = MutableStateFlow<Int?>(null)
    var selectedColorId = MutableStateFlow<Int?>(null)
    
    // Text Inputs
    var licensePlate = MutableStateFlow("")
    var seats = MutableStateFlow("4")
    var luggage = MutableStateFlow("2")
    var numberOfVehicles = MutableStateFlow("1")
    var nonCharterPolicy = MutableStateFlow<Int?>(null)
    var charterPolicy = MutableStateFlow<Int?>(null)
    var selectedServiceTypes = MutableStateFlow<List<String>>(emptyList())

    init {
        loadInitialData()
    }

    private fun parseCancelPolicyHours(value: String?): Int? {
        val v = value?.trim()?.toIntOrNull() ?: return null
        return when (v) {
            1 -> 24
            2 -> 48
            3 -> 72
            24, 48, 72 -> v
            else -> v
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Fetch Options concurrently
                val types = registrationRepository.getVehicleTypes().getOrNull()?.data ?: emptyList()
                val makes = registrationRepository.getVehicleMakes().getOrNull()?.data ?: emptyList()
                val years = registrationRepository.getVehicleYears().getOrNull()?.data ?: emptyList()
                val colors = registrationRepository.getVehicleColors().getOrNull()?.data ?: emptyList()
                
                // 2. Fetch Prefill Data
                val stepResponse = registrationRepository.getVehicleDetailsStep()
                val prefill = stepResponse.getOrNull()?.data?.data

                _uiState.update { 
                    it.copy(
                        vehicleTypes = types,
                        makes = makes,
                        years = years,
                        colors = colors,
                        prefillData = prefill,
                        isLoading = false
                    )
                }

                // 3. Apply Prefill
                prefill?.let { data ->
                    prefillVehicleId = data.vehicleId ?: prefillVehicleId
                    // Handle prefill IDs (assuming backend sends IDs in string fields or Int fields)
                    // If your prefill object uses String, convert to Int.
                    selectedVehicleTypeId.value =
                        data.vehicleType?.toIntOrNull()
                            ?: data.vehicleTypeId
                            ?: tokenManager.getSelectedVehicleId()?.toIntOrNull()
                    
                    // Safe parsing for fields that might be strings in prefill but need to be Ints
                    selectedMakeId.value = data.make?.toIntOrNull()
                    selectedModelId.value = data.model?.toIntOrNull()
                    selectedYearId.value = data.year?.toIntOrNull()
                    selectedColorId.value = data.color?.toIntOrNull()
                    
                    licensePlate.value = data.licensePlate ?: ""
                    seats.value = data.seats ?: "4"
                    luggage.value = data.luggage ?: "2"
                    numberOfVehicles.value = data.numberOfVehicles ?: "1"
                    nonCharterPolicy.value =
                        parseCancelPolicyHours(data.nonCharterCancelPolicy ?: data.nonCharterCancelPolicyLegacy) ?: 24
                    charterPolicy.value =
                        parseCancelPolicyHours(data.charterCancelPolicy ?: data.charterCancelPolicyLegacy) ?: 48
                    selectedServiceTypes.value = data.typeOfService ?: emptyList()

                    // If we have a selected Make, fetch Models immediately
                    selectedMakeId.value?.let { makeId ->
                        fetchModels(makeId)
                    }
                }
                
                // Fallback: If no vehicle type selected, try token
                if (selectedVehicleTypeId.value == null) {
                    selectedVehicleTypeId.value = tokenManager.getSelectedVehicleId()?.toIntOrNull()
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = errorHandler.handleError(e)) }
            }
        }
    }

    fun fetchModels(makeId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            val result = registrationRepository.getVehicleModels(makeId)
            _uiState.update { 
                it.copy(
                    isLoadingModels = false, 
                    models = result.getOrNull()?.data ?: emptyList() 
                ) 
            }
        }
    }

    fun onMakeSelected(makeId: Int) {
        selectedMakeId.value = makeId
        selectedModelId.value = null // Reset model
        fetchModels(makeId)
    }

    fun saveStep1AndNavigate(onSuccess: () -> Unit) {
        if (selectedVehicleTypeId.value == null) return setError("Vehicle Type is required")
        if (selectedMakeId.value == null) return setError("Make is required")
        if (selectedModelId.value == null) return setError("Model is required")
        if (selectedYearId.value == null) return setError("Year is required")
        if (selectedColorId.value == null) return setError("Color is required")
        if (licensePlate.value.isBlank()) return setError("License Plate is required")
        if (selectedServiceTypes.value.isEmpty()) return setError("Please select at least one service type")

        val request = VehicleDetailsRequest(
            vehicleId = prefillVehicleId ?: tokenManager.getSelectedVehicleId()?.toIntOrNull(),
            typeOfService = selectedServiceTypes.value,
            vehicleType = selectedVehicleTypeId.value!!,
            make = selectedMakeId.value!!,
            model = selectedModelId.value!!,
            year = selectedYearId.value!!,
            color = selectedColorId.value!!,
            seats = seats.value.toIntOrNull() ?: 4,
            luggage = luggage.value.toIntOrNull() ?: 2,
            numberOfVehicles = numberOfVehicles.value.toIntOrNull() ?: 1,
            licensePlate = licensePlate.value.trim(),
            nonCharterCancelPolicy = nonCharterPolicy.value ?: 24,
            charterCancelPolicy = charterPolicy.value ?: 48
        )

        registrationRepository.updateLocalDraft(request)
        _uiState.update { it.copy(success = true, error = null) }
        onSuccess()
    }

    private fun setError(msg: String) {
        _uiState.update { it.copy(error = msg) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class VehicleDetailsUiState(
    val isLoading: Boolean = false,
    val isLoadingModels: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: VehicleDetailsStepPrefillData? = null,
    val isCompleted: Boolean = false,
    
    // Dropdown Data Sources
    val vehicleTypes: List<VehicleType> = emptyList(),
    val makes: List<VehicleOption> = emptyList(),
    val models: List<VehicleOption> = emptyList(),
    val years: List<VehicleOption> = emptyList(),
    val colors: List<VehicleOption> = emptyList()
)