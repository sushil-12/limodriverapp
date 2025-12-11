package com.limo1800driver.app.data.repository

import com.limo1800driver.app.data.api.DriverAuthApi
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.auth.AuthData
import com.limo1800driver.app.data.model.auth.LoginRegisterRequest
import com.limo1800driver.app.data.model.auth.VerifyOTPData
import com.limo1800driver.app.data.model.auth.VerifyOTPRequest
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.storage.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Driver Authentication repository
 * Handles all authentication-related data operations
 * Tag: DriverAuthRepo
 */
@Singleton
class DriverAuthRepository @Inject constructor(
    private val authApi: DriverAuthApi,
    private val tokenManager: TokenManager,
    private val errorHandler: ErrorHandler
) {
    
    companion object {
        private const val TAG = "DriverAuthRepo"
    }
    
    /**
     * Send verification code to phone number
     */
    suspend fun sendVerificationCode(
        phoneNumber: String,
        countryCode: String,
        countryShortCode: String
    ): Result<BaseResponse<AuthData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Sending verification code to: $countryCode$phoneNumber")
                val request = LoginRegisterRequest(
                    phoneIsd = countryCode,
                    phoneCountry = countryShortCode,
                    phone = phoneNumber,
                    userType = "driver"
                )
                
                val response = authApi.loginOrRegister(request)
                Timber.tag(TAG).d("Verification code sent successfully")
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to send verification code")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    /**
     * Verify OTP code
     */
    suspend fun verifyOTP(
        tempUserId: String,
        otp: String
    ): Result<BaseResponse<VerifyOTPData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Verifying OTP for temp_user_id: $tempUserId")
                val request = VerifyOTPRequest(
                    tempUserId = tempUserId,
                    otp = otp
                )
                
                val response = authApi.verifyOTP(request)
                
                // Save tokens and registration state if verification is successful
                if (response.success && response.data != null) {
                    tokenManager.saveTokens(
                        accessToken = response.data.token,
                        tokenType = response.data.tokenType,
                        expiresIn = response.data.expiresIn
                    )
                    
                    // Save full driver registration state if available
                    response.data.driverRegistrationState?.let { state ->
                        tokenManager.saveDriverRegistrationState(state)
                        Timber.tag(TAG).d("Driver registration state saved: current_step=${state.currentStep}, next_step=${state.nextStep}")
                    } ?: run {
                        // Fallback: store next step if no full state available
                        val nextStep = when {
                            response.data.action.isNotEmpty() -> response.data.action
                            else -> "basic_info"
                        }
                        tokenManager.saveNextStep(nextStep)
                    }
                    
                    Timber.tag(TAG).d("OTP verified successfully, tokens and state saved")
                }
                
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to verify OTP")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    /**
     * Resend OTP code
     */
    suspend fun resendOTP(tempUserId: String): Result<BaseResponse<AuthData>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Resending OTP for temp_user_id: $tempUserId")
                val request = mapOf("temp_user_id" to tempUserId)
                val response = authApi.resendOTP(request)
                Timber.tag(TAG).d("OTP resent successfully")
                Result.success(response)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to resend OTP")
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return tokenManager.isAuthenticated()
    }
    
    /**
     * Get current access token
     */
    fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }
    
    /**
     * Clear authentication data
     */
    fun logout() {
        tokenManager.clearTokens()
        Timber.tag(TAG).d("User logged out")
    }
}

