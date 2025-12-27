package com.limo1800driver.app.data.repository

import android.content.Context
import com.limo1800driver.app.data.api.DriverRegistrationApi
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.auth.DriverRegistrationState
import com.limo1800driver.app.data.model.registration.*
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.storage.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Driver Registration repository
 * Handles all registration-related data operations
 * Tag: DriverRegistrationRepo
 */
@Singleton
class DriverRegistrationRepository @Inject constructor(
    private val registrationApi: DriverRegistrationApi,
    private val errorHandler: ErrorHandler,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "DriverRegistrationRepo"
    }

    // Local cache for multi-step vehicle submission
    private var _localVehicleDraft: VehicleDetailsRequest? = null

    fun updateLocalDraft(partialData: VehicleDetailsRequest) {
        _localVehicleDraft = if (_localVehicleDraft == null) {
            partialData
        } else {
            val current = _localVehicleDraft!!
            current.copy(
                vehicleId = partialData.vehicleId ?: current.vehicleId,
                typeOfService = if (partialData.typeOfService.isNotEmpty()) partialData.typeOfService else current.typeOfService,
                vehicleType = partialData.vehicleType.takeIf { it != 0 } ?: current.vehicleType,
                make = partialData.make.takeIf { it != 0 } ?: current.make,
                model = partialData.model.takeIf { it != 0 } ?: current.model,
                year = partialData.year.takeIf { it != 0 } ?: current.year,
                color = partialData.color.takeIf { it != 0 } ?: current.color,
                seats = partialData.seats.takeIf { it != 0 } ?: current.seats,
                luggage = partialData.luggage.takeIf { it != 0 } ?: current.luggage,
                numberOfVehicles = partialData.numberOfVehicles.takeIf { it != 0 } ?: current.numberOfVehicles,
                licensePlate = partialData.licensePlate.ifEmpty { current.licensePlate },
                nonCharterCancelPolicy = partialData.nonCharterCancelPolicy.takeIf { it != 0 } ?: current.nonCharterCancelPolicy,
                charterCancelPolicy = partialData.charterCancelPolicy.takeIf { it != 0 } ?: current.charterCancelPolicy,
                amenities = if (partialData.amenities.isNotEmpty()) partialData.amenities else current.amenities,
                specialAmenities = partialData.specialAmenities ?: current.specialAmenities,
                vehicleInterior = partialData.vehicleInterior ?: current.vehicleInterior,
                vehicleImage1 = partialData.vehicleImage1 ?: current.vehicleImage1,
                vehicleImage2 = partialData.vehicleImage2 ?: current.vehicleImage2,
                vehicleImage3 = partialData.vehicleImage3 ?: current.vehicleImage3,
                vehicleImage4 = partialData.vehicleImage4 ?: current.vehicleImage4,
                vehicleImage5 = partialData.vehicleImage5 ?: current.vehicleImage5,
                vehicleImage6 = partialData.vehicleImage6 ?: current.vehicleImage6
            )
        }
    }

    fun getLocalDraft(): VehicleDetailsRequest? = _localVehicleDraft
    
    // ==================== Basic Info ====================
    
    suspend fun completeBasicInfo(request: BasicInfoRequest): Result<BaseResponse<BasicInfoCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing basic info")
                val response = registrationApi.completeBasicInfo(request)
                if (response.success && response.data != null) {
                    val nextStep = response.data.nextStep
                    val isCompleted = response.data.isCompleted
                    updateRegistrationState("basic_info", nextStep, isCompleted)
                    Timber.tag(TAG).d("Basic info completed successfully, next_step: $nextStep")
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete basic info")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getBasicInfoStep(): Result<BaseResponse<BasicInfoStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching basic info step")
                val response = registrationApi.getBasicInfoStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch basic info step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Company Info ====================
    
    suspend fun completeCompanyInfo(request: CompanyInfoRequest): Result<BaseResponse<CompanyInfoCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing company info")
                val response = registrationApi.completeCompanyInfo(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("company_info", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete company info")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getCompanyInfoStep(): Result<BaseResponse<CompanyInfoStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching company info step")
                val response = registrationApi.getCompanyInfoStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch company info step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Company Documents ====================
    
    suspend fun completeCompanyDocuments(request: CompanyDocumentsRequest): Result<BaseResponse<CompanyDocumentsCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing company documents")
                val response = registrationApi.completeCompanyDocuments(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("company_documents", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete company documents")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getCompanyDocumentsStep(): Result<BaseResponse<CompanyDocumentsStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching company documents step")
                val response = registrationApi.getCompanyDocumentsStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch company documents step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Privacy & Terms ====================
    
    suspend fun completePrivacyTerms(request: PrivacyTermsRequest): Result<BaseResponse<PrivacyTermsCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing privacy & terms")
                val response = registrationApi.completePrivacyTerms(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("privacy_terms", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete privacy & terms")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Driving License ====================
    
    suspend fun completeDrivingLicense(request: DrivingLicenseRequest): Result<BaseResponse<DrivingLicenseCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing driving license")
                val response = registrationApi.completeDrivingLicense(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("driving_license", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete driving license")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getDrivingLicenseStep(): Result<BaseResponse<DrivingLicenseStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching driving license step")
                val response = registrationApi.getDrivingLicenseStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch driving license step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Bank Details ====================
    
    suspend fun completeBankDetails(request: BankDetailsRequest): Result<BaseResponse<BankDetailsCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing bank details")
                val response = registrationApi.completeBankDetails(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("bank_details", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete bank details")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getBankDetailsStep(): Result<BaseResponse<BankDetailsStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching bank details step")
                val response = registrationApi.getBankDetailsStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch bank details step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Profile Picture ====================
    
    suspend fun completeProfilePicture(request: ProfilePictureRequest): Result<BaseResponse<ProfilePictureCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing profile picture")
                val response = registrationApi.completeProfilePicture(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("profile_picture", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete profile picture")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getProfilePictureStep(): Result<BaseResponse<ProfilePictureStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching profile picture step")
                val response = registrationApi.getProfilePictureStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch profile picture step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Vehicle Insurance ====================
    
    suspend fun completeVehicleInsurance(request: VehicleInsuranceRequest): Result<BaseResponse<VehicleInsuranceCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing vehicle insurance")
                val response = registrationApi.completeVehicleInsurance(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("vehicle_insurance", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete vehicle insurance")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getVehicleInsuranceStep(): Result<BaseResponse<VehicleInsuranceStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle insurance step")
                val response = registrationApi.getVehicleInsuranceStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle insurance step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Vehicle Details ====================

    suspend fun getVehicleMakes(): Result<BaseResponse<List<VehicleOption>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = registrationApi.getVehicleMakes()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }

    suspend fun getVehicleModels(makeId: Int): Result<BaseResponse<List<VehicleOption>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = registrationApi.getVehicleModels(makeId)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }

    suspend fun getVehicleYears(): Result<BaseResponse<List<VehicleOption>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = registrationApi.getVehicleYears()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }

    suspend fun getVehicleColors(): Result<BaseResponse<List<VehicleOption>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = registrationApi.getVehicleColors()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun completeVehicleDetails(request: VehicleDetailsRequest): Result<BaseResponse<VehicleDetailsCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing vehicle details")
                val response = registrationApi.completeVehicleDetails(request)
                if (response.success && response.data != null) {
                    response.data.nextStep?.let { tokenManager.saveNextStep(it) }
                    // Persist vehicle id when provided in request (edit) so downstream steps can use it
                    request.vehicleId?.let { tokenManager.saveSelectedVehicleId(it.toString()) }
                    updateRegistrationState("vehicle_details", response.data.nextStep, response.data.isCompleted)
                    // Refresh cached VehicleDetailsStepResponse after completion to get updated vehicle_id
                    getVehicleDetailsStep()
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete vehicle details")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getVehicleDetailsStep(): Result<BaseResponse<VehicleDetailsStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle details step")
                val response = registrationApi.getVehicleDetailsStep()
                // Cache the response if successful (iOS pattern)
                // Note: response.data is VehicleDetailsStepResponse, which contains the step data
                if (response.success && response.data != null) {
                    tokenManager.saveVehicleDetailsStepResponse(response.data)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle details step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Vehicle Rate Settings ====================
    
    suspend fun completeVehicleRateSettings(request: VehicleRateSettingsRequest): Result<BaseResponse<VehicleRateSettingsCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing vehicle rate settings")
                val response = registrationApi.completeVehicleRateSettings(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("vehicle_rate_settings", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete vehicle rate settings")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getVehicleRateSettingsStep(vehicleId: String? = null): Result<BaseResponse<VehicleRateSettingsStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle rate settings step")
                val response = registrationApi.getVehicleRateSettingsStep(vehicleId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle rate settings step")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }

    // ==================== Vehicle Info ====================
    suspend fun getVehicleInfo(vehicleId: String): Result<VehicleInfoResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle info for id=$vehicleId")
                val response = registrationApi.getVehicleInfo(vehicleId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle info")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Image Upload ====================
    
    suspend fun uploadSingleImage(request: ImageUploadRequest): Result<BaseResponse<ImageUploadResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Uploading single image")
                val response = registrationApi.uploadSingleImage(request)
                Timber.tag(TAG).d("Image uploaded successfully: ${response.data?.imageUrl}")
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to upload image")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    // ==================== Vehicle Types & Options ====================
    
    suspend fun getVehicleTypes(): Result<BaseResponse<List<VehicleType>>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle types")
                val response = registrationApi.getVehicleTypes()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle types")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getAllAmenities(): Result<BaseResponse<List<Amenity>>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching all amenities")
                val response = registrationApi.getAllAmenities()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch amenities")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getAllSpecialAmenities(): Result<BaseResponse<List<SpecialAmenity>>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching all special amenities")
                val response = registrationApi.getAllSpecialAmenities()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch special amenities")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getAllVehicleInterior(): Result<BaseResponse<List<VehicleInterior>>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching all vehicle interior")
                val response = registrationApi.getAllVehicleInterior()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle interior")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getOrganisationTypes(): Result<BaseResponse<List<OrganisationType>>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching organisation types")
                val response = registrationApi.getOrganisationTypes()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch organisation types")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getLanguages(): Result<BaseResponse<List<com.limo1800driver.app.data.model.registration.Language>>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching languages")
                val response = registrationApi.getLanguages()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch languages")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    suspend fun getAllSteps(): Result<BaseResponse<com.limo1800driver.app.data.model.registration.RegistrationStepsData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching all registration steps")
                val response = registrationApi.getAllSteps()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch all steps")
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    /**
     * Helper function to update registration state after step completion
     */
    private fun updateRegistrationState(currentStep: String, nextStep: String?, isCompleted: Boolean = false) {
        val existingState = tokenManager.getDriverRegistrationState()
        val updatedState = DriverRegistrationState(
            currentStep = currentStep,
            progressPercentage = existingState?.progressPercentage ?: 0,
            isCompleted = isCompleted,
            nextStep = nextStep,
            steps = existingState?.steps,
            completedSteps = existingState?.completedSteps,
            totalSteps = existingState?.totalSteps,
            completedCount = existingState?.completedCount
        )
        tokenManager.saveDriverRegistrationState(updatedState)
        Timber.tag(TAG).d("Registration state updated: current_step=$currentStep, next_step=$nextStep")
    }

    /**
     * Get email verification status
     */
    suspend fun getEmailVerificationStatus(): Result<EmailVerificationData> = withContext(Dispatchers.IO) {
        try {
            val response = registrationApi.getEmailVerificationStatus()
            if (response.success) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get email verification status")
            Result.failure(Exception(errorHandler.handleError(e)))
        }
    }
}

