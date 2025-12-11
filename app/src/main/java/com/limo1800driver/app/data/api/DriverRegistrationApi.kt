package com.limo1800driver.app.data.api

import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Driver Registration API service interface
 * Handles all driver registration-related API calls
 */
interface DriverRegistrationApi {
    
    // Basic Info
    @POST("api/mobile/v1/driver/registration/complete/basic_info")
    suspend fun completeBasicInfo(@Body request: BasicInfoRequest): BaseResponse<BasicInfoCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/basic_info")
    suspend fun getBasicInfoStep(): BaseResponse<BasicInfoStepResponse>
    
    // Company Info
    @POST("api/mobile/v1/driver/registration/complete/company_info")
    suspend fun completeCompanyInfo(@Body request: CompanyInfoRequest): BaseResponse<CompanyInfoCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/company_info")
    suspend fun getCompanyInfoStep(): BaseResponse<CompanyInfoStepResponse>
    
    // Company Documents
    @POST("api/mobile/v1/driver/registration/complete/company_documents")
    suspend fun completeCompanyDocuments(@Body request: CompanyDocumentsRequest): BaseResponse<CompanyDocumentsCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/company_documents")
    suspend fun getCompanyDocumentsStep(): BaseResponse<CompanyDocumentsStepResponse>
    
    // Privacy & Terms
    @POST("api/mobile/v1/driver/registration/complete/privacy_terms")
    suspend fun completePrivacyTerms(@Body request: PrivacyTermsRequest): BaseResponse<PrivacyTermsCompleteResponse>
    
    // Driving License
    @POST("api/mobile/v1/driver/registration/complete/driving_license")
    suspend fun completeDrivingLicense(@Body request: DrivingLicenseRequest): BaseResponse<DrivingLicenseCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/driving_license")
    suspend fun getDrivingLicenseStep(): BaseResponse<DrivingLicenseStepResponse>
    
    // Bank Details
    @POST("api/mobile/v1/driver/registration/complete/bank_details")
    suspend fun completeBankDetails(@Body request: BankDetailsRequest): BaseResponse<BankDetailsCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/bank_details")
    suspend fun getBankDetailsStep(): BaseResponse<BankDetailsStepResponse>
    
    // Profile Picture
    @POST("api/mobile/v1/driver/registration/complete/profile_picture")
    suspend fun completeProfilePicture(@Body request: ProfilePictureRequest): BaseResponse<ProfilePictureCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/profile_picture")
    suspend fun getProfilePictureStep(): BaseResponse<ProfilePictureStepResponse>
    
    // Vehicle Insurance
    @POST("api/mobile/v1/driver/registration/complete/vehicle_insurance")
    suspend fun completeVehicleInsurance(@Body request: VehicleInsuranceRequest): BaseResponse<VehicleInsuranceCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/vehicle_insurance")
    suspend fun getVehicleInsuranceStep(): BaseResponse<VehicleInsuranceStepResponse>
    
    // Vehicle Details
    @POST("api/mobile/v1/driver/registration/complete/vehicle_details")
    suspend fun completeVehicleDetails(@Body request: VehicleDetailsRequest): BaseResponse<VehicleDetailsCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/vehicle_details")
    suspend fun getVehicleDetailsStep(): BaseResponse<VehicleDetailsStepResponse>
    
    // Vehicle Rate Settings
    @POST("api/mobile/v1/driver/registration/complete/vehicle_rate_settings")
    suspend fun completeVehicleRateSettings(@Body request: VehicleRateSettingsRequest): BaseResponse<VehicleRateSettingsCompleteResponse>
    
    @GET("api/mobile/v1/driver/registration/step/vehicle_rate_settings")
    suspend fun getVehicleRateSettingsStep(): BaseResponse<VehicleRateSettingsStepResponse>
    
    // Vehicle Info (for rate header/amenities)
    @GET("api/affiliate/get-vehicle-info/{vehicleId}")
    suspend fun getVehicleInfo(@Path("vehicleId") vehicleId: String): BaseResponse<VehicleInfoResponse>

    // Image Upload
    @POST("api/add-single-image")
    suspend fun uploadSingleImage(@Body request: ImageUploadRequest): BaseResponse<ImageUploadResponse>
    
    // Vehicle Types & Options
    @GET("api/vehicle-types")
    suspend fun getVehicleTypes(): BaseResponse<List<VehicleType>>
    
    @GET("api/all-amenities")
    suspend fun getAllAmenities(): BaseResponse<List<Amenity>>
    
    @GET("api/all-special-amenities")
    suspend fun getAllSpecialAmenities(): BaseResponse<List<SpecialAmenity>>
    
    @GET("api/all-vehicle-interior")
    suspend fun getAllVehicleInterior(): BaseResponse<List<VehicleInterior>>
    
    // Organization Types
    @GET("api/get-organisation-types")
    suspend fun getOrganisationTypes(): BaseResponse<List<OrganisationType>>
    
    // Languages
    @GET("api/get-languages")
    suspend fun getLanguages(): BaseResponse<List<com.limo1800driver.app.data.model.registration.Language>>
    
    // All Steps
    @GET("api/mobile/v1/driver/registration/all-steps")
    suspend fun getAllSteps(): BaseResponse<com.limo1800driver.app.data.model.registration.RegistrationStepsData>
}

