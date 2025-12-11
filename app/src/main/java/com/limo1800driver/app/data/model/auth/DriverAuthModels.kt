package com.limo1800driver.app.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Request model for phone number validation and OTP request
 */
data class LoginRegisterRequest(
    @SerializedName("phone_isd")
    val phoneIsd: String,
    
    @SerializedName("phone_country")
    val phoneCountry: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("user_type")
    val userType: String = "driver"
)

/**
 * Response data for phone number validation
 */
data class AuthData(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("otp_type")
    val otpType: String,
    
    @SerializedName("temp_user_id")
    val tempUserId: String,
    
    @SerializedName("expires_in")
    val expiresIn: Int,
    
    @SerializedName("cooldown_remaining")
    val cooldownRemaining: Int
)

/**
 * Request model for OTP verification
 */
data class VerifyOTPRequest(
    @SerializedName("temp_user_id")
    val tempUserId: String,
    
    @SerializedName("otp")
    val otp: String
)

/**
 * User model from API response
 * Handles is_profile_completed as either Boolean or Int (1/0) to match iOS behavior
 */
data class DriverUser(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("role")
    val role: Int,
    
    @SerializedName("is_profile_completed")
    val isProfileCompleted: Boolean?,
    
    @SerializedName("last_login_at")
    val lastLoginAt: String,
    
    @SerializedName("created_from")
    val createdFrom: String?,
    
    @SerializedName("driver_registration_state")
    val driverRegistrationState: DriverRegistrationState?
)

/**
 * Driver registration state tracking
 */
data class DriverRegistrationState(
    @SerializedName("current_step")
    val currentStep: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?,
    
    @SerializedName("steps")
    val steps: DriverRegistrationSteps?,
    
    @SerializedName("completed_steps")
    val completedSteps: List<String>?,
    
    @SerializedName("total_steps")
    val totalSteps: Int?,
    
    @SerializedName("completed_count")
    val completedCount: Int?
)

/**
 * Driver registration steps tracking
 */
data class DriverRegistrationSteps(
    @SerializedName("phone_verified")
    val phoneVerified: Boolean?,
    
    @SerializedName("basic_info")
    val basicInfo: Boolean?,
    
    @SerializedName("company_info")
    val companyInfo: Boolean?,
    
    @SerializedName("company_documents")
    val companyDocuments: Boolean?,
    
    @SerializedName("privacy_terms")
    val privacyTerms: Boolean?,
    
    @SerializedName("driving_license")
    val drivingLicense: Boolean?,
    
    @SerializedName("bank_details")
    val bankDetails: Boolean?,
    
    @SerializedName("profile_picture")
    val profilePicture: Boolean?,
    
    @SerializedName("vehicle_insurance")
    val vehicleInsurance: Boolean?,
    
    @SerializedName("vehicle_details")
    val vehicleDetails: Boolean?,
    
    @SerializedName("vehicle_rate_settings")
    val vehicleRateSettings: Boolean?
)

/**
 * OTP verification response data
 */
data class VerifyOTPData(
    @SerializedName("user")
    val user: DriverUser,
    
    @SerializedName("token")
    val token: String,
    
    @SerializedName("token_type")
    val tokenType: String,
    
    @SerializedName("expires_in")
    val expiresIn: Int,
    
    @SerializedName("action")
    val action: String,
    
    @SerializedName("driver_registration_state")
    val driverRegistrationState: DriverRegistrationState?
)

