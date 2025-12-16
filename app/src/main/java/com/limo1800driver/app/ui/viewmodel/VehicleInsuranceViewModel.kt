package com.limo1800driver.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.*
import com.limo1800driver.app.data.network.ImageUploadService
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
 * ViewModel for Vehicle Insurance Screen
 * Tag: VehicleInsuranceVM
 */
@HiltViewModel
class VehicleInsuranceViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val imageUploadService: ImageUploadService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleInsuranceUiState())
    val uiState: StateFlow<VehicleInsuranceUiState> = _uiState.asStateFlow()

    fun fetchVehicleInsuranceStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleInsuranceVM").d("Fetching vehicle insurance step")
                val result = registrationRepository.getVehicleInsuranceStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("VehicleInsuranceVM").d("Vehicle insurance step fetched successfully")
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
                        Timber.tag("VehicleInsuranceVM").e(error, "Failed to fetch vehicle insurance step")
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

    fun completeVehicleInsurance(request: VehicleInsuranceRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("VehicleInsuranceVM").d("Completing vehicle insurance")
                val result = registrationRepository.completeVehicleInsurance(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Vehicle insurance completed successfully"
                            )
                            Timber.tag("VehicleInsuranceVM").d("Vehicle insurance completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("VehicleInsuranceVM").e(error, "Failed to complete vehicle insurance")
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

    suspend fun uploadImage(bitmap: Bitmap): Result<Int> {
        return try {
            val result = imageUploadService.uploadImageAndGetId(bitmap)
            result.fold(
                onSuccess = { imageId ->
                    Timber.tag("VehicleInsuranceVM").d("Image uploaded successfully, ID: $imageId")
                    Result.success(imageId)
                },
                onFailure = { error ->
                    Timber.tag("VehicleInsuranceVM").e(error, "Failed to upload image")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.tag("VehicleInsuranceVM").e(e, "Error uploading image")
            Result.failure(e)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset one-shot navigation flags so the screen doesn't auto-navigate when revisited.
     */
    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(success = false, nextStep = null)
    }
}

data class VehicleInsuranceUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: VehicleInsuranceStepPrefillData? = null,
    val isCompleted: Boolean = false
)

