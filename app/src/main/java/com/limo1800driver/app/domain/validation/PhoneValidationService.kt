package com.limo1800driver.app.domain.validation

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneValidationService @Inject constructor() {

    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * Validate phone number
     * @param rawNumber Raw digits without country code
     * @param countryCode Selected country
     * @param phoneLength Unused now - kept for compatibility if needed
     * @return ValidationResult
     */
    fun validatePhoneNumber(
        rawNumber: String,
        countryCode: CountryCode,
        phoneLength: Int // Ignored with libphonenumber
    ): ValidationResult {
        if (rawNumber.isEmpty()) {
            return ValidationResult.Success
        }

        try {
            val fullNumber = "${countryCode.code}$rawNumber"
            val phoneNumber: Phonenumber.PhoneNumber = phoneUtil.parse(fullNumber, countryCode.shortCode)

            // For real-time: use isPossibleNumber (lenient for partial inputs)
            // If too long or invalid, it will fail
            return if (phoneUtil.isPossibleNumber(phoneNumber)) {
                ValidationResult.Success
            } else {
                ValidationResult.Error("Please enter a valid phone number")
            }
        } catch (e: Exception) {
            return ValidationResult.Error("Please enter a valid phone number")
        }
    }

    /**
     * Strict validation for submission
     */
    fun isValidForSubmission(
        rawNumber: String,
        countryCode: CountryCode
    ): Boolean {
        try {
            val fullNumber = "${countryCode.code}$rawNumber"
            val phoneNumber: Phonenumber.PhoneNumber = phoneUtil.parse(fullNumber, countryCode.shortCode)
            return phoneUtil.isValidNumber(phoneNumber)
        } catch (e: Exception) {
            return false
        }
    }
}