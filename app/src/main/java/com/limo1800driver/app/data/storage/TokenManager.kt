package com.limo1800driver.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.limo1800driver.app.data.model.auth.DriverRegistrationState
import com.limo1800driver.app.data.model.dashboard.DriverProfileData
import com.limo1800driver.app.data.model.registration.VehicleDetailsStepResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token management using EncryptedSharedPreferences
 * Handles storage and retrieval of authentication tokens
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "secure_token_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_SELECTED_VEHICLE_ID = "selected_vehicle_id"
        private const val KEY_AFFILIATE_TYPE = "affiliate_type"
        private const val KEY_NEXT_STEP = "registration_next_step"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        private const val KEY_WELCOME_SCREEN_SEEN = "welcome_screen_seen"
        private const val KEY_IS_PROFILE_COMPLETED = "is_profile_completed"
        private const val KEY_DRIVER_REGISTRATION_STATE = "driver_registration_state"
        private const val KEY_VEHICLE_DETAILS_STEP_RESPONSE = "vehicle_details_step_response"
        private const val KEY_BASIC_INFO_EMAIL = "basic_info_email"
        private const val KEY_DRIVER_PROFILE_DATA = "driver_profile_data"
        private const val KEY_DRIVER_PROFILE_CACHE_TIME = "driver_profile_cache_time"

        // Cache driver profile for 30 minutes (30 * 60 * 1000ms)
        private const val DRIVER_PROFILE_CACHE_DURATION_MS = 30 * 60 * 1000L
    }
    
    private val gson = Gson()
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Save authentication tokens securely
     */
    fun saveTokens(
        accessToken: String,
        tokenType: String,
        expiresIn: Int,
        refreshToken: String? = null
    ) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
        
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_TOKEN_TYPE, tokenType)
            .putInt(KEY_EXPIRES_IN, expiresIn)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime)
            .apply()
        
        refreshToken?.let {
            sharedPreferences.edit()
                .putString(KEY_REFRESH_TOKEN, it)
                .apply()
        }
    }
    
    /**
     * Get access token if valid
     */
    fun getAccessToken(): String? {
        val token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        val expiry = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0L)
        
        return if (token != null) {
            // For JWT tokens, we should validate the token itself, not just expiry
            // For now, return the token if it exists (JWT validation should be done server-side)
            token
        } else {
            null
        }
    }
    
    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return getAccessToken() != null
    }
    
    /**
     * Clear all stored tokens
     */
    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_TYPE)
            .remove(KEY_EXPIRES_IN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
    }
    
    /**
     * Get token type
     */
    fun getTokenType(): String? {
        return sharedPreferences.getString(KEY_TOKEN_TYPE, null)
    }
    
    /**
     * Get token expiry time
     */
    fun getTokenExpiry(): Long {
        return sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0L)
    }
    
    /**
     * Save selected vehicle ID
     */
    fun saveSelectedVehicleId(vehicleId: String) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_VEHICLE_ID, vehicleId)
            .apply()
    }
    
    /**
     * Get selected vehicle ID
     */
    fun getSelectedVehicleId(): String? {
        return sharedPreferences.getString(KEY_SELECTED_VEHICLE_ID, null)
    }

    /**
     * Save next registration step for resume
     */
    fun saveNextStep(step: String?) {
        sharedPreferences.edit()
            .putString(KEY_NEXT_STEP, step)
            .apply()
    }

    /**
     * Get stored next registration step
     */
    fun getNextStep(): String? {
        return sharedPreferences.getString(KEY_NEXT_STEP, null)
    }

    /**
     * Persist onboarding seen flag
     */
    fun saveOnboardingSeen(seen: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_SEEN, seen)
            .apply()
    }

    /**
     * Check if onboarding already seen
     */
    fun hasSeenOnboarding(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_SEEN, false)
    }
    
    /**
     * Persist welcome screen seen flag
     */
    fun saveWelcomeScreenSeen(seen: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_WELCOME_SCREEN_SEEN, seen)
            .apply()
    }

    /**
     * Check if welcome screen already seen
     */
    fun hasSeenWelcomeScreen(): Boolean {
        return sharedPreferences.getBoolean(KEY_WELCOME_SCREEN_SEEN, false)
    }
    
    /**
     * Save profile completion status
     */
    fun saveProfileCompleted(completed: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_PROFILE_COMPLETED, completed)
            .apply()
    }
    
    /**
     * Check if profile is completed
     */
    fun isProfileCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PROFILE_COMPLETED, false)
    }
    
    /**
     * Save affiliate type
     */
    fun saveAffiliateType(affiliateType: String) {
        sharedPreferences.edit()
            .putString(KEY_AFFILIATE_TYPE, affiliateType)
            .apply()
    }
    
    /**
     * Get affiliate type
     */
    fun getAffiliateType(): String? {
        // First try to get from stored value
        val storedAffiliateType = sharedPreferences.getString(KEY_AFFILIATE_TYPE, null)
        if (storedAffiliateType != null) {
            return storedAffiliateType
        }

        // Fallback to cached profile data if available
        val cachedProfile = getCachedDriverProfileData()
        return cachedProfile?.affiliateType
    }
    
    /**
     * Save driver registration state
     */
    fun saveDriverRegistrationState(state: DriverRegistrationState) {
        val json = gson.toJson(state)
        sharedPreferences.edit()
            .putString(KEY_DRIVER_REGISTRATION_STATE, json)
            .apply()

        // Also save next step for quick access
        saveNextStep(state.nextStep)
    }
    
    /**
     * Get driver registration state
     */
    fun getDriverRegistrationState(): DriverRegistrationState? {
        val json = sharedPreferences.getString(KEY_DRIVER_REGISTRATION_STATE, null)
        return if (json != null) {
            try {
                gson.fromJson(json, DriverRegistrationState::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Clear driver registration state
     */
    fun clearDriverRegistrationState() {
        sharedPreferences.edit()
            .remove(KEY_DRIVER_REGISTRATION_STATE)
            .remove(KEY_NEXT_STEP)
            .apply()
    }
    
    /**
     * Save vehicle details step response (for caching vehicle_id)
     */
    fun saveVehicleDetailsStepResponse(response: VehicleDetailsStepResponse) {
        val json = gson.toJson(response)
        sharedPreferences.edit()
            .putString(KEY_VEHICLE_DETAILS_STEP_RESPONSE, json)
            .apply()
        // Also save vehicle_id if available for quick access
        response.data?.vehicleId?.let { 
            saveSelectedVehicleId(it.toString()) 
        }
    }
    
    /**
     * Get cached vehicle details step response
     */
    fun getVehicleDetailsStepResponse(): VehicleDetailsStepResponse? {
        val json = sharedPreferences.getString(KEY_VEHICLE_DETAILS_STEP_RESPONSE, null)
        return if (json != null) {
            try {
                gson.fromJson(json, VehicleDetailsStepResponse::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Save basic info email for prefill in other screens
     */
    fun saveBasicInfoEmail(email: String) {
        sharedPreferences.edit()
            .putString(KEY_BASIC_INFO_EMAIL, email)
            .apply()
    }

    /**
     * Get basic info email
     */
    fun getBasicInfoEmail(): String? {
        return sharedPreferences.getString(KEY_BASIC_INFO_EMAIL, null)
    }

    /**
     * Save driver profile data with cache timestamp
     */
    fun saveDriverProfileData(profileData: DriverProfileData) {
        val json = gson.toJson(profileData)
        val currentTime = System.currentTimeMillis()

        sharedPreferences.edit()
            .putString(KEY_DRIVER_PROFILE_DATA, json)
            .putLong(KEY_DRIVER_PROFILE_CACHE_TIME, currentTime)
            .apply()
    }

    /**
     * Get cached driver profile data if not expired
     */
    fun getCachedDriverProfileData(): DriverProfileData? {
        val cacheTime = sharedPreferences.getLong(KEY_DRIVER_PROFILE_CACHE_TIME, 0L)
        val currentTime = System.currentTimeMillis()

        // Check if cache is still valid (not expired)
        if (currentTime - cacheTime > DRIVER_PROFILE_CACHE_DURATION_MS) {
            // Cache expired, clear it
            clearDriverProfileCache()
            return null
        }

        val json = sharedPreferences.getString(KEY_DRIVER_PROFILE_DATA, null)
        return if (json != null) {
            try {
                gson.fromJson(json, DriverProfileData::class.java)
            } catch (e: Exception) {
                // If deserialization fails, clear cache
                clearDriverProfileCache()
                null
            }
        } else {
            null
        }
    }

    /**
     * Check if driver profile cache is valid (not expired)
     */
    fun isDriverProfileCacheValid(): Boolean {
        val cacheTime = sharedPreferences.getLong(KEY_DRIVER_PROFILE_CACHE_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        return currentTime - cacheTime <= DRIVER_PROFILE_CACHE_DURATION_MS
    }

    /**
     * Clear driver profile cache
     */
    fun clearDriverProfileCache() {
        sharedPreferences.edit()
            .remove(KEY_DRIVER_PROFILE_DATA)
            .remove(KEY_DRIVER_PROFILE_CACHE_TIME)
            .apply()
    }

    /**
     * Update cached driver profile data with new firstName and lastName
     * This ensures the profile screen shows updated data immediately
     */
    fun updateCachedProfileName(firstName: String, lastName: String) {
        val cachedProfile = getCachedDriverProfileData()
        if (cachedProfile != null) {
            val updatedProfile = cachedProfile.copy(
                driverFirstName = firstName,
                driverLastName = lastName
            )
            saveDriverProfileData(updatedProfile)
        }
    }

    /**
     * Update cached driver profile data with new driverImage URL
     * This ensures the profile screen shows updated image immediately
     */
    fun updateCachedProfileImage(driverImage: String?) {
        val cachedProfile = getCachedDriverProfileData()
        if (cachedProfile != null) {
            val updatedProfile = cachedProfile.copy(
                driverImage = driverImage
            )
            saveDriverProfileData(updatedProfile)
        }
    }

    /**
     * Clear all data (for logout)
     */
    fun clearAll() {
        clearTokens()
        clearDriverRegistrationState()
        sharedPreferences.edit()
            .remove(KEY_SELECTED_VEHICLE_ID)
            .remove(KEY_AFFILIATE_TYPE)
            .remove(KEY_VEHICLE_DETAILS_STEP_RESPONSE)
            .remove(KEY_IS_PROFILE_COMPLETED)
            .remove(KEY_BASIC_INFO_EMAIL)
            .remove(KEY_DRIVER_PROFILE_DATA)
            .remove(KEY_DRIVER_PROFILE_CACHE_TIME)
            .apply()
    }
}

