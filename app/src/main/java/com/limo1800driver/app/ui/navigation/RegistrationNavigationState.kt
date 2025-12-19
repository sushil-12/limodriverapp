package com.limo1800driver.app.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registration navigation state manager
 * Tracks current registration step and handles navigation logic
 */
@Singleton
class RegistrationNavigationState @Inject constructor() {
    
    private val _currentStep = MutableStateFlow<String?>(null)
    val currentStep: StateFlow<String?> = _currentStep.asStateFlow()
    
    private val _nextStep = MutableStateFlow<String?>(null)
    val nextStep: StateFlow<String?> = _nextStep.asStateFlow()
    
    /**
     * Update current step
     */
    fun setCurrentStep(step: String?) {
        _currentStep.value = step
    }
    
    /**
     * Update next step from API response
     */
    fun setNextStep(step: String?) {
        _nextStep.value = step
    }
    
    /**
     * Get route for next step
     */
    fun getRouteForStep(step: String?): String {
        return when (step) {
            "basic_info" -> NavRoutes.BasicInfo
            "company_info" -> NavRoutes.CompanyInfo
            "company_documents" -> NavRoutes.CompanyDocuments
            "privacy_terms" -> NavRoutes.PrivacyTerms
            "user_profile_details" -> NavRoutes.UserProfileDetails
            "driving_license" -> NavRoutes.DrivingLicense
            "bank_details" -> NavRoutes.BankDetails
            "profile_picture" -> NavRoutes.ProfilePicture
            "vehicle_selection" -> NavRoutes.VehicleSelection
            // All vehicle-related steps land on the step hub
            "vehicle_insurance" -> NavRoutes.VehicleDetailsStep
            "vehicle_details" -> NavRoutes.VehicleDetailsStep
            "vehicle_rate_settings" -> NavRoutes.VehicleDetailsStep
            "dashboard" -> NavRoutes.Dashboard
            else -> NavRoutes.BasicInfo // Default fallback
        }
    }
    
    /**
     * Check if registration is complete
     */
    fun isRegistrationComplete(nextStep: String?): Boolean {
        return nextStep == "dashboard" || nextStep == null
    }
}

