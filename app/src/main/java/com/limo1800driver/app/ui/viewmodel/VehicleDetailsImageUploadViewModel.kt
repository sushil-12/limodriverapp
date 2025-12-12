package com.limo1800driver.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.registration.*
import com.limo1800driver.app.data.network.ImageUploadService
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
import timber.log.Timber
import javax.inject.Inject

data class ImageUploadUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val specialAmenitiesOptions: List<SpecialAmenity> = emptyList(),
    val interiorOptions: List<VehicleInterior> = emptyList(),
    // Images: Index (0-5) -> ID
    val uploadedImageIds: MutableMap<Int, Int> = mutableMapOf(),
    // Bitmaps for display
    val displayedImages: MutableMap<Int, Bitmap> = mutableMapOf()
)

@HiltViewModel
class VehicleDetailsImageUploadViewModel @Inject constructor(
    private val repository: DriverRegistrationRepository,
    private val tokenManager: TokenManager,
    private val imageUploadService: ImageUploadService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageUploadUiState())
    val uiState: StateFlow<ImageUploadUiState> = _uiState.asStateFlow()

    val selectedSpecialAmenities = mutableStateListOf<String>()
    val selectedInteriors = mutableStateListOf<String>()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Fetch options and current state
                val specialDeferred = async { repository.getAllSpecialAmenities() }
                val interiorDeferred = async { repository.getAllVehicleInterior() }
                val stepDataDeferred = async { repository.getVehicleDetailsStep() }

                val specialResult = specialDeferred.await()
                val interiorResult = interiorDeferred.await()
                val stepDataResult = stepDataDeferred.await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        specialAmenitiesOptions = specialResult.getOrNull()?.data ?: emptyList(),
                        interiorOptions = interiorResult.getOrNull()?.data ?: emptyList()
                    )
                }

                // Prefill
                val prefill = stepDataResult.getOrNull()?.data?.data
                prefill?.let { data ->
                    selectedSpecialAmenities.clear()
                    data.specialAmenities?.let { ids -> selectedSpecialAmenities.addAll(ids.map { it.toString() }) }

                    selectedInteriors.clear()
                    data.interior?.let { ids -> selectedInteriors.addAll(ids.map { it.toString() }) }
                    
                    // For images, we only have IDs in prefill. We can't display them unless we have URLs or bitmaps.
                    // Assuming for now we just store the IDs so validation passes if they were already uploaded.
                    val imageIds = mapOf(
                        0 to data.vehicleImage1,
                        1 to data.vehicleImage2,
                        2 to data.vehicleImage3,
                        3 to data.vehicleImage4,
                        4 to data.vehicleImage5,
                        5 to data.vehicleImage6
                    )
                    
                    val currentIds = _uiState.value.uploadedImageIds
                    imageIds.forEach { (idx, id) ->
                        if (id != null && id != 0) currentIds[idx] = id
                    }
                    _uiState.update { it.copy(uploadedImageIds = currentIds) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = errorHandler.handleError(e)) }
            }
        }
    }

    fun toggleSpecialAmenity(id: String) {
        if (selectedSpecialAmenities.contains(id)) selectedSpecialAmenities.remove(id)
        else selectedSpecialAmenities.add(id)
    }

    fun toggleInterior(id: String) {
        if (selectedInteriors.contains(id)) selectedInteriors.remove(id)
        else selectedInteriors.add(id)
    }

    fun uploadImage(index: Int, bitmap: Bitmap) {
        viewModelScope.launch {
            val currentDisplay = _uiState.value.displayedImages.toMutableMap()
            currentDisplay[index] = bitmap
            _uiState.update { it.copy(displayedImages = currentDisplay) }

            try {
                val result = imageUploadService.uploadImageAndGetId(bitmap)
                result.fold(
                    onSuccess = { id ->
                        val currentIds = _uiState.value.uploadedImageIds.toMutableMap()
                        currentIds[index] = id
                        _uiState.update { it.copy(uploadedImageIds = currentIds) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = "Failed to upload image: ${e.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = errorHandler.handleError(e)) }
            }
        }
    }

    fun submitFinalDetails() {
        if (_uiState.value.uploadedImageIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please upload at least one vehicle image.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val draft = repository.getLocalDraft()
                if (draft == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Session expired. Please go back and fill vehicle details.") }
                    return@launch
                }

                val uploaded = _uiState.value.uploadedImageIds
                val finalRequest = draft.copy(
                    specialAmenities = selectedSpecialAmenities.toList(),
                    vehicleInterior = selectedInteriors.toList(),
                    vehicleImage1 = uploaded[0],
                    vehicleImage2 = uploaded[1],
                    vehicleImage3 = uploaded[2],
                    vehicleImage4 = uploaded[3],
                    vehicleImage5 = uploaded[4],
                    vehicleImage6 = uploaded[5]
                )

                val result = repository.completeVehicleDetails(finalRequest)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, success = true) }
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
    
    fun clearError() { _uiState.update { it.copy(error = null) } }
}