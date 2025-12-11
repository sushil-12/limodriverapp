package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.RegistrationStepsData
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
 * ViewModel for Vehicle Details Step Screen (Navigation Hub)
 * Tag: VehicleDetailsStepVM
 */
@HiltViewModel
class VehicleDetailsStepViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val tokenManager: TokenManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailsStepUiState())
    val uiState: StateFlow<VehicleDetailsStepUiState> = _uiState.asStateFlow()

    fun fetchAllSteps() {
        viewModelScope.launch {
            try {
                Timber.tag("VehicleDetailsStepVM").d("Fetching all registration steps")
                val result = registrationRepository.getAllSteps()
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            _uiState.value = _uiState.value.copy(
                                allSteps = response.data
                            )
                            Timber.tag("VehicleDetailsStepVM").d("All steps fetched successfully")
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("VehicleDetailsStepVM").e(error, "Failed to fetch all steps")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("VehicleDetailsStepVM").e(e, "Error fetching all steps")
            }
        }
    }

    fun ensureUserNameAndLocation() {
        viewModelScope.launch {
            try {
                // Try to get from basic info step
                val result = registrationRepository.getBasicInfoStep()
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data?.data != null) {
                            val prefill = response.data.data
                            val firstName = prefill.firstName ?: ""
                            val lastName = prefill.lastName ?: ""
                            val name = "$firstName $lastName".trim()
                            val address = prefill.address ?: ""
                            
                            if (name.isNotEmpty()) {
                                _uiState.value = _uiState.value.copy(userName = name)
                            }
                            if (address.isNotEmpty()) {
                                _uiState.value = _uiState.value.copy(userLocation = address)
                            }
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("VehicleDetailsStepVM").e(error, "Failed to fetch basic info")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("VehicleDetailsStepVM").e(e, "Error ensuring user name and location")
            }
        }
    }
}

data class VehicleDetailsStepUiState(
    val allSteps: RegistrationStepsData? = null,
    val userName: String = "",
    val userLocation: String = ""
)

