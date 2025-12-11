package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import android.graphics.Bitmap
import com.limo1800driver.app.data.model.registration.ProfilePictureCompleteResponse
import com.limo1800driver.app.data.model.registration.ProfilePictureRequest
import com.limo1800driver.app.data.model.registration.ProfilePictureStepPrefillData
import com.limo1800driver.app.data.model.registration.ProfilePictureStepResponse
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
 * ViewModel for Profile Picture Screen
 * Tag: ProfilePictureVM
 */
@HiltViewModel
class ProfilePictureViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val imageUploadService: ImageUploadService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilePictureUiState())
    val uiState: StateFlow<ProfilePictureUiState> = _uiState.asStateFlow()

    fun fetchProfilePictureStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("ProfilePictureVM").d("Fetching profile picture step")
                val result = registrationRepository.getProfilePictureStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("ProfilePictureVM").d("Profile picture step fetched successfully")
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
                        Timber.tag("ProfilePictureVM").e(error, "Failed to fetch profile picture step")
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

    fun completeProfilePicture(request: ProfilePictureRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("ProfilePictureVM").d("Completing profile picture")
                val result = registrationRepository.completeProfilePicture(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Profile picture completed successfully"
                            )
                            Timber.tag("ProfilePictureVM").d("Profile picture completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("ProfilePictureVM").e(error, "Failed to complete profile picture")
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
    suspend fun uploadImage(bitmap: Bitmap): Result<String> {
        return try {
            val result = imageUploadService.uploadImage(bitmap)
            result.fold(
                onSuccess = { imageId ->
                    Timber.tag("ProfilePictureVM").d("Image uploaded successfully, ID: $imageId")
                    Result.success(imageId)
                },
                onFailure = { error ->
                    Timber.tag("ProfilePictureVM").e(error, "Failed to upload image")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.tag("ProfilePictureVM").e(e, "Error uploading image")
            Result.failure(e)
        }
    }
}

data class ProfilePictureUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: ProfilePictureStepPrefillData? = null,
    val isCompleted: Boolean = false
)

