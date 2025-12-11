package com.limo1800driver.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.DrivingLicenseCompleteResponse
import com.limo1800driver.app.data.model.registration.DrivingLicenseRequest
import com.limo1800driver.app.data.model.registration.DrivingLicenseStepPrefillData
import com.limo1800driver.app.data.model.registration.DrivingLicenseStepResponse
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
 * ViewModel for Driving License Screen
 * Tag: DrivingLicenseVM
 */
@HiltViewModel
class DrivingLicenseViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val imageUploadService: ImageUploadService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrivingLicenseUiState())
    val uiState: StateFlow<DrivingLicenseUiState> = _uiState.asStateFlow()

    fun fetchDrivingLicenseStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = registrationRepository.getDrivingLicenseStep()
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data?.data,
                                isCompleted = response.data?.isCompleted ?: false
                            )
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
                        Timber.tag("DrivingLicenseVM").e(error, "Failed to fetch driving license step")
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

    fun completeDrivingLicense(request: DrivingLicenseRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("DrivingLicenseVM").d("Completing driving license")
                val result = registrationRepository.completeDrivingLicense(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Driving license completed successfully"
                            )
                            Timber.tag("DrivingLicenseVM").d("Driving license completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("DrivingLicenseVM").e(error, "Failed to complete driving license")
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
    
    /**
     * Upload image and return image ID
     */
    suspend fun uploadImage(bitmap: Bitmap): Result<Int> {
        return try {
            val result = imageUploadService.uploadImageAndGetId(bitmap)
            result.fold(
                onSuccess = { imageId ->
                    Timber.tag("DrivingLicenseVM").d("Image uploaded successfully, ID: $imageId")
                    Result.success(imageId)
                },
                onFailure = { error ->
                    Timber.tag("DrivingLicenseVM").e(error, "Failed to upload image")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.tag("DrivingLicenseVM").e(e, "Error uploading image")
            Result.failure(e)
        }
    }
}

data class DrivingLicenseUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: DrivingLicenseStepPrefillData? = null,
    val isCompleted: Boolean = false
)

