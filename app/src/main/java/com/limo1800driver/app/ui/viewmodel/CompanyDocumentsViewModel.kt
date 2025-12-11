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
 * ViewModel for Company Documents Screen
 * Tag: CompanyDocumentsVM
 */
@HiltViewModel
class CompanyDocumentsViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val imageUploadService: ImageUploadService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyDocumentsUiState())
    val uiState: StateFlow<CompanyDocumentsUiState> = _uiState.asStateFlow()

    fun fetchCompanyDocumentsStep() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("CompanyDocumentsVM").d("Fetching company documents step")
                val result = registrationRepository.getCompanyDocumentsStep()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prefillData = response.data.data,
                                isCompleted = response.data.isCompleted ?: false
                            )
                            Timber.tag("CompanyDocumentsVM").d("Company documents step fetched successfully")
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
                        Timber.tag("CompanyDocumentsVM").e(error, "Failed to fetch company documents step")
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

    fun fetchLanguages() {
        viewModelScope.launch {
            try {
                Timber.tag("CompanyDocumentsVM").d("Fetching languages")
                val result = registrationRepository.getLanguages()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                languages = response.data
                            )
                            Timber.tag("CompanyDocumentsVM").d("Languages fetched: ${response.data.size}")
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("CompanyDocumentsVM").e(error, "Failed to fetch languages")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("CompanyDocumentsVM").e(e, "Error fetching languages")
            }
        }
    }

    fun fetchOrganisationTypes() {
        viewModelScope.launch {
            try {
                Timber.tag("CompanyDocumentsVM").d("Fetching organisation types")
                val result = registrationRepository.getOrganisationTypes()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                organisationTypes = response.data
                            )
                            Timber.tag("CompanyDocumentsVM").d("Organisation types fetched: ${response.data.size}")
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("CompanyDocumentsVM").e(error, "Failed to fetch organisation types")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("CompanyDocumentsVM").e(e, "Error fetching organisation types")
            }
        }
    }

    fun completeCompanyDocuments(request: CompanyDocumentsRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Timber.tag("CompanyDocumentsVM").d("Completing company documents")
                val result = registrationRepository.completeCompanyDocuments(request)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = true,
                                nextStep = response.data.nextStep,
                                message = "Company documents completed successfully"
                            )
                            Timber.tag("CompanyDocumentsVM").d("Company documents completed, next_step: ${response.data.nextStep}")
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
                        Timber.tag("CompanyDocumentsVM").e(error, "Failed to complete company documents")
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
                    Timber.tag("CompanyDocumentsVM").d("Image uploaded successfully, ID: $imageId")
                    Result.success(imageId)
                },
                onFailure = { error ->
                    Timber.tag("CompanyDocumentsVM").e(error, "Failed to upload image")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.tag("CompanyDocumentsVM").e(e, "Error uploading image")
            Result.failure(e)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CompanyDocumentsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val nextStep: String? = null,
    val prefillData: CompanyDocumentsStepPrefillData? = null,
    val isCompleted: Boolean = false,
    val languages: List<Language> = emptyList(),
    val organisationTypes: List<OrganisationType> = emptyList()
)

