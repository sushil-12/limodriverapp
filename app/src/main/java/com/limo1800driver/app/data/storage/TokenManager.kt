package com.limo1800driver.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.limo1800driver.app.data.model.auth.DriverRegistrationState
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
        private const val KEY_DRIVER_REGISTRATION_STATE = "driver_registration_state"
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
        return sharedPreferences.getString(KEY_AFFILIATE_TYPE, null)
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
     * Clear all data (for logout)
     */
    fun clearAll() {
        clearTokens()
        clearDriverRegistrationState()
        sharedPreferences.edit()
            .remove(KEY_SELECTED_VEHICLE_ID)
            .remove(KEY_AFFILIATE_TYPE)
            .apply()
    }
}

