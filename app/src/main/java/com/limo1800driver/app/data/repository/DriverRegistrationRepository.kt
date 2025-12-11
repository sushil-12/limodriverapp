package com.limo1800driver.app.data.repository

import com.limo1800driver.app.data.api.DriverRegistrationApi
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.auth.DriverRegistrationState
import com.limo1800driver.app.data.model.registration.*
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.storage.TokenManager
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
    private val tokenManager: TokenManager
) {
    
    companion object {
        private const val TAG = "DriverRegistrationRepo"
    }
    
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    suspend fun getVehicleModels(makeId: Int): Result<BaseResponse<List<VehicleOption>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = registrationApi.getVehicleModels(makeId)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    suspend fun getVehicleYears(): Result<BaseResponse<List<VehicleOption>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = registrationApi.getVehicleYears()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    suspend fun getVehicleColors(): Result<BaseResponse<List<VehicleOption>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = registrationApi.getVehicleColors()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    suspend fun completeVehicleDetails(request: VehicleDetailsRequest): Result<BaseResponse<VehicleDetailsCompleteResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Completing vehicle details")
                val response = registrationApi.completeVehicleDetails(request)
                if (response.success && response.data != null) {
                    updateRegistrationState("vehicle_details", response.data.nextStep, response.data.isCompleted)
                }
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete vehicle details")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    suspend fun getVehicleDetailsStep(): Result<BaseResponse<VehicleDetailsStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle details step")
                val response = registrationApi.getVehicleDetailsStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle details step")
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    suspend fun getVehicleRateSettingsStep(): Result<BaseResponse<VehicleRateSettingsStepResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle rate settings step")
                val response = registrationApi.getVehicleRateSettingsStep()
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle rate settings step")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }

    // ==================== Vehicle Info ====================
    suspend fun getVehicleInfo(vehicleId: String): Result<BaseResponse<VehicleInfoResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Fetching vehicle info for id=$vehicleId")
                val response = registrationApi.getVehicleInfo(vehicleId)
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to fetch vehicle info")
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
                Result.failure(Exception(errorHandler.handleApiError(e)))
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
}

