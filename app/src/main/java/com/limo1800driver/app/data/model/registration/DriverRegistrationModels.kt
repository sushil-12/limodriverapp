package com.limo1800driver.app.data.model.registration

import com.google.gson.annotations.SerializedName

// ==================== Basic Info ====================

data class BasicInfoRequest(
    @SerializedName("affiliate_type")
    val affiliateType: String,
    
    @SerializedName("first_name")
    val firstName: String,
    
    @SerializedName("last_name")
    val lastName: String,
    
    @SerializedName("gender")
    val gender: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("dob")
    val dob: String,
    
    @SerializedName("first_year_business")
    val firstYearBusiness: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("latitude")
    val latitude: Double?,
    
    @SerializedName("longitude")
    val longitude: Double?,
    
    @SerializedName("zipCode")
    val zipCode: String?,
    
    @SerializedName("country")
    val country: String?,
    
    @SerializedName("state")
    val state: String?,
    
    @SerializedName("city")
    val city: String?
)

data class BasicInfoCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class BasicInfoStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: BasicInfoStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class BasicInfoStepPrefillData(
    @SerializedName("affiliate_type")
    val affiliateType: String?,
    
    @SerializedName("first_name")
    val firstName: String?,
    
    @SerializedName("last_name")
    val lastName: String?,
    
    @SerializedName("gender")
    val gender: String?,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("dob")
    val dob: String?,
    
    @SerializedName("first_year_business")
    val firstYearBusiness: String?,
    
    @SerializedName("address")
    val address: String?
)

// ==================== Company Info ====================

data class CompanyInfoRequest(
    @SerializedName("company_name")
    val companyName: String,
    
    @SerializedName("dispatch_email")
    val dispatchEmail: String,
    
    @SerializedName("doing_business_as")
    val doingBusinessAs: String,
    
    @SerializedName("dispatch_isd")
    val dispatchIsd: String,
    
    @SerializedName("dispatch_phone_number")
    val dispatchPhoneNumber: String,
    
    @SerializedName("dispatch_country")
    val dispatchCountry: String,
    
    @SerializedName("business_telephone")
    val businessTelephone: String,
    
    @SerializedName("buisness_telephone_isd")
    val businessTelephoneIsd: String,
    
    @SerializedName("business_telephone_country")
    val businessTelephoneCountry: String,
    
    @SerializedName("fax_isd")
    val faxIsd: String?,
    
    @SerializedName("fax_number")
    val faxNumber: String?,
    
    @SerializedName("fax_country")
    val faxCountry: String?
)

data class CompanyInfoCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class CompanyInfoStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: CompanyInfoStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class CompanyInfoStepPrefillData(
    @SerializedName("company_name")
    val companyName: String?,
    
    @SerializedName("dispatch_email")
    val dispatchEmail: String?,
    
    @SerializedName("doing_business_as")
    val doingBusinessAs: String?,
    
    @SerializedName("dispatch_isd")
    val dispatchIsd: String?,
    
    @SerializedName("dispatch_phone_number")
    val dispatchPhoneNumber: String?,
    
    @SerializedName("dispatch_country")
    val dispatchCountry: String?,
    
    @SerializedName("business_telephone")
    val businessTelephone: String?,
    
    @SerializedName("buisness_telephone_isd")
    val businessTelephoneIsd: String?,
    
    @SerializedName("business_telephone_country")
    val businessTelephoneCountry: String?,
    
    @SerializedName("fax_isd")
    val faxIsd: String?,
    
    @SerializedName("fax_number")
    val faxNumber: String?,
    
    @SerializedName("fax_country")
    val faxCountry: String?
)

// ==================== Company Documents ====================

data class CompanyDocumentsRequest(
    @SerializedName("language")
    val language: List<String>,
    
    @SerializedName("transport_authority_reg_no")
    val transportAuthorityRegNo: String,
    
    @SerializedName("buisness_front_photo")
    val businessFrontPhoto: Int,
    
    @SerializedName("buisness_back_photo")
    val businessBackPhoto: Int,
    
    @SerializedName("permit_image")
    val permitImage: Int,
    
    @SerializedName("organisation_type")
    val organisationType: List<String>
)

data class CompanyDocumentsCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class CompanyDocumentsStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: CompanyDocumentsStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class CompanyDocumentsStepPrefillData(
    @SerializedName("language")
    val language: String?,
    
    @SerializedName("transport_authority_reg_no")
    val transportAuthorityRegNo: String?,
    
    @SerializedName("buisness_front_photo")
    val businessFrontPhoto: ImageData?,
    
    @SerializedName("buisness_back_photo")
    val businessBackPhoto: ImageData?,
    
    @SerializedName("permit_image")
    val permitImage: ImageData?,
    
    @SerializedName("organisation_type")
    val organisationType: String?
)

data class ImageData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("url")
    val url: String?
)

// ==================== Privacy & Terms ====================

data class PrivacyTermsRequest(
    @SerializedName("privacy_accepted")
    val privacyAccepted: Boolean,
    
    @SerializedName("terms_accepted")
    val termsAccepted: Boolean
)

data class PrivacyTermsCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

// ==================== Driving License ====================

data class DrivingLicenseRequest(
    @SerializedName("licence_front_photo")
    val licenceFrontPhoto: Int?,

    @SerializedName("licence_back_photo")
    val licenceBackPhoto: Int?,

    @SerializedName("license_number")
    val licenseNumber: String?,

    @SerializedName("expiry_date")
    val expiryDate: String?,

    @SerializedName("optionalCertification")
    val optionalCertification: OptionalCertificationRequest? = null
)

data class OptionalCertificationRequest(
    @SerializedName("veteran")
    val veteran: Int? = null,

    @SerializedName("dod_clearance")
    val dodClearance: Int? = null,

    @SerializedName("foid_card")
    val foidCard: Int? = null,

    @SerializedName("child_school_bus")
    val childSchoolBus: Int? = null,

    @SerializedName("background_certified")
    val backgroundCertified: Int? = null,

    @SerializedName("ex_police")
    val exPolice: Int? = null
)

data class DrivingLicenseCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class DrivingLicenseStepResponse(
    @SerializedName("step")
    val step: String,

    @SerializedName("data")
    val data: DrivingLicenseStepPrefillData?,

    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class DrivingLicenseStepPrefillData(
    @SerializedName("license_number")
    val licenseNumber: String?,

    @SerializedName("licence_front_photo")
    val licenceFrontPhoto: ImageData?,

    @SerializedName("licence_back_photo")
    val licenceBackPhoto: ImageData?,

    @SerializedName("optionalCertification")
    val optionalCertification: OptionalCertificationPrefill? = null
)

data class OptionalCertificationPrefill(
    @SerializedName("veteran")
    val veteran: ImageData? = null,

    @SerializedName("dod_clearance")
    val dodClearance: ImageData? = null,

    @SerializedName("foid_card")
    val foidCard: ImageData? = null,

    @SerializedName("child_school_bus")
    val childSchoolBus: ImageData? = null,

    // Some payloads send an extra "child_school" key; we just ignore it by not declaring.

    @SerializedName("background_certified")
    val backgroundCertified: ImageData? = null,

    @SerializedName("ex_police")
    val exPolice: ImageData? = null
)

// ==================== Bank Details ====================

data class BankDetailsRequest(
    @SerializedName("bank_name")
    val bankName: String,

    @SerializedName("bank_address")
    val bankAddress: String,

    @SerializedName("account_holder_first_name")
    val accountHolderFirstName: String,

    @SerializedName("account_holder_last_name")
    val accountHolderLastName: String,

    @SerializedName("account_number")
    val accountNumber: String,

    @SerializedName("routing_number")
    val routingNumber: String,

    @SerializedName("account_type")
    val accountType: String,

    @SerializedName("currency")
    val currency: String,

    @SerializedName("social_security_number")
    val socialSecurityNumber: String,

    @SerializedName("badge_city")
    val badgeCity: String,

    @SerializedName("business_id")
    val businessId: String? = null
)

data class BankDetailsCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class BankDetailsStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: BankDetailsStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class BankDetailsStepPrefillData(
    /**
     * Backend payloads vary: some send `account_holder_name`, others send
     * `account_holder_first_name` + `account_holder_last_name`.
     */
    @SerializedName("account_holder_name")
    val accountHolderName: String? = null,

    @SerializedName("account_holder_first_name")
    val accountHolderFirstName: String? = null,

    @SerializedName("account_holder_last_name")
    val accountHolderLastName: String? = null,

    @SerializedName("bank_name")
    val bankName: String? = null,

    @SerializedName("bank_address")
    val bankAddress: String? = null,

    @SerializedName("account_number")
    val accountNumber: String? = null,

    @SerializedName("routing_number")
    val routingNumber: String? = null,

    @SerializedName("account_type")
    val accountType: String? = null,

    @SerializedName("currency")
    val currency: String? = null,

    @SerializedName("country")
    val country: String? = null,

    @SerializedName("social_security_number")
    val socialSecurityNumber: String? = null,

    @SerializedName("business_id")
    val businessId: String? = null,

    @SerializedName("badge_city")
    val badgeCity: String? = null
)

// ==================== Profile Picture ====================

data class ProfilePictureRequest(
    @SerializedName("profile_image")
    val profileImage: String
)

data class ProfilePictureCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class ProfilePictureStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: ProfilePictureStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class ProfilePictureStepPrefillData(
    @SerializedName("profile_image")
    val profileImage: ProfileImageData?
)

data class ProfileImageData(
    @SerializedName("id")
    val id: String?,

    @SerializedName("url")
    val url: String?
)

// ==================== Vehicle Insurance ====================

data class VehicleInsuranceRequest(
    @SerializedName("name_of_insurance_company")
    val nameOfInsuranceCompany: String,
    
    @SerializedName("agency_name")
    val agencyName: String?,
    
    @SerializedName("insurance_policy_number")
    val insurancePolicyNumber: String,
    
    @SerializedName("agent_telephone_number")
    val agentTelephoneNumber: String,
    
    @SerializedName("agent_telephone_isd")
    val agentTelephoneIsd: String,
    
    @SerializedName("agent_telephone_country")
    val agentTelephoneCountry: String,
    
    @SerializedName("agent_email")
    val agentEmail: String?,
    
    @SerializedName("insurance_limit")
    val insuranceLimit: String,
    
    @SerializedName("policy_expiry_date")
    val policyExpiryDate: String,
    
    @SerializedName("insurance_policy_front_photo")
    val insurancePolicyFrontPhoto: Int
) {
    // Helper map for logging / ad-hoc calls
    fun toApiFormat(): Map<String, Any> = mapOf(
        "name_of_insurance_company" to nameOfInsuranceCompany,
        "agency_name" to (agencyName ?: ""),
        "insurance_policy_number" to insurancePolicyNumber,
        "agent_telephone_number" to agentTelephoneNumber,
        "agent_telephone_isd" to agentTelephoneIsd,
        "agent_telephone_country" to agentTelephoneCountry,
        "agent_email" to (agentEmail ?: ""),
        "insurance_limit" to insuranceLimit,
        "policy_expiry_date" to policyExpiryDate,
        "insurance_policy_front_photo" to insurancePolicyFrontPhoto
    )
}

data class VehicleInsuranceCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class VehicleInsuranceStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: VehicleInsuranceStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class VehicleInsuranceStepPrefillData(
    @SerializedName("name_of_insurance_company")
    val nameOfInsuranceCompany: String?,
    
    @SerializedName("agency_name")
    val agencyName: String?,
    
    @SerializedName("insurance_policy_number")
    val insurancePolicyNumber: String?,
    
    @SerializedName("agent_telephone_number")
    val agentTelephoneNumber: String?,
    
    @SerializedName("agent_telephone_isd")
    val agentTelephoneIsd: String?,
    
    @SerializedName("agent_telephone_country")
    val agentTelephoneCountry: String?,
    
    @SerializedName("agent_email")
    val agentEmail: String?,
    
    @SerializedName("insurance_limit")
    val insuranceLimit: String?,
    
    @SerializedName("policy_expiry_date")
    val policyExpiryDate: String?,

    /**
     * Backend currently returns an object like:
     * "insurance_front_photo": { "id": "8003", "url": "https://..." }
     */
    @SerializedName("insurance_front_photo")
    val insuranceFrontPhoto: UploadedDocumentPhoto? = null,

    /**
     * Legacy/alternate backends may return just the image id as string/int.
     */
    @SerializedName("insurance_policy_front_photo")
    val insurancePolicyFrontPhoto: String? = null
)

data class UploadedDocumentPhoto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("url")
    val url: String? = null
)

// ==================== Vehicle Details ====================
data class VehicleOption(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
) {
    override fun toString(): String = name
}



data class VehicleDetailsRequest(
    @SerializedName("vehicle_id")
    val vehicleId: Int? = null,

    @SerializedName("type_of_service")
    val typeOfService: List<String> = emptyList(),

    @SerializedName("vehicle_type")
    val vehicleType: Int,

    // Changed to Int (ID) based on JSON: "make": 1
    @SerializedName("make")
    val make: Int,

    // Changed to Int (ID) based on JSON: "model": 5
    @SerializedName("model")
    val model: Int,

    // Changed to Int (ID) based on JSON: "year": 105
    @SerializedName("year")
    val year: Int,

    // Changed to Int (ID) based on JSON: "color": 2
    @SerializedName("color")
    val color: Int,

    // Changed to Int based on JSON: "seats": 6
    @SerializedName("seats")
    val seats: Int,

    // Changed to Int based on JSON: "luggage": 4
    @SerializedName("luggage")
    val luggage: Int,

    // Added field from JSON
    @SerializedName("number_of_vehicles")
    val numberOfVehicles: Int = 1,

    @SerializedName("license_plate")
    val licensePlate: String,

    // Updated key to camelCase & type to Int based on JSON: "nonCharterCancelPolicy": 24
    @SerializedName("nonCharterCancelPolicy")
    val nonCharterCancelPolicy: Int,

    // Updated key to camelCase & type to Int based on JSON: "charterCancelPolicy": 48
    @SerializedName("charterCancelPolicy")
    val charterCancelPolicy: Int,

    @SerializedName("amenities")
    val amenities: List<String> = emptyList(),

    @SerializedName("special_amenities")
    val specialAmenities: List<String>? = null,

    @SerializedName("vehicle_interior")
    val vehicleInterior: List<String>? = null,

    @SerializedName("vehicle_image_1")
    val vehicleImage1: Int? = null,

    @SerializedName("vehicle_image_2")
    val vehicleImage2: Int? = null,

    @SerializedName("vehicle_image_3")
    val vehicleImage3: Int? = null,

    @SerializedName("vehicle_image_4")
    val vehicleImage4: Int? = null,

    @SerializedName("vehicle_image_5")
    val vehicleImage5: Int? = null,

    @SerializedName("vehicle_image_6")
    val vehicleImage6: Int? = null
)

data class VehicleDetailsCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class VehicleDetailsStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: VehicleDetailsStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class VehicleDetailsStepPrefillData(
    @SerializedName("vehicle_id")
    val vehicleId: Int?,

    /**
     * Backend returns `vehicle_type` (string/int) in the step payload.
     * Older variants may have `vehicle_type_id`.
     */
    @SerializedName("vehicle_type")
    val vehicleType: String? = null,

    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int? = null,
    
    @SerializedName("make")
    val make: String?,
    
    @SerializedName("model")
    val model: String?,
    
    @SerializedName("year")
    val year: String?,
    
    @SerializedName("color")
    val color: String?,
    
    @SerializedName("license_plate")
    val licensePlate: String?,
    
    @SerializedName("type_of_service")
    val typeOfService: List<String>?,
    
    // API returns numeric values; keep String? so Gson can coerce numbers/strings safely
    @SerializedName("seats")
    val seats: String?,
    
    @SerializedName("luggage")
    val luggage: String?,

    @SerializedName("number_of_vehicles")
    val numberOfVehicles: String? = null,
    
    // API returns camelCase keys in this step response
    @SerializedName("nonCharterCancelPolicy")
    val nonCharterCancelPolicy: String? = null,

    // Legacy snake_case
    @SerializedName("non_charter_cancel_policy")
    val nonCharterCancelPolicyLegacy: String? = null,
    
    @SerializedName("charterCancelPolicy")
    val charterCancelPolicy: String? = null,

    @SerializedName("charter_cancel_policy")
    val charterCancelPolicyLegacy: String? = null,
    
    @SerializedName("vehicle_interior")
    val vehicleInterior: List<String>?,
    
    @SerializedName("vehicle_image_1")
    val vehicleImage1: VehicleImageRef?,
    
    @SerializedName("vehicle_image_2")
    val vehicleImage2: VehicleImageRef?,
    
    @SerializedName("vehicle_image_3")
    val vehicleImage3: VehicleImageRef?,
    
    @SerializedName("vehicle_image_4")
    val vehicleImage4: VehicleImageRef?,
    
    @SerializedName("vehicle_image_5")
    val vehicleImage5: VehicleImageRef?,
    
    @SerializedName("vehicle_image_6")
    val vehicleImage6: VehicleImageRef?,

    // API returns arrays of string ids in step payload
    @SerializedName("amenities")
    val amenities: List<String>? = null,

    @SerializedName("special_amenities")
    val specialAmenities: List<String>? = null
)

data class VehicleImageRef(
    @SerializedName("id") val id: Int?,
    @SerializedName("url") val url: String?
)

// ==================== Vehicle Rate Settings ====================

data class VehicleRateSettingsRequest(
    @SerializedName("vehicle_id")
    val vehicleId: Int? = null,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("milage_rate")
    val milageRate: Double? = null,
    
    @SerializedName("upto_miles")
    val uptoMiles: Int? = null,
    
    @SerializedName("after_mileage_rate")
    val afterMileageRate: Double? = null,
    
    @SerializedName("minimum_airport_arrival_rate")
    val minimumAirportArrivalRate: Double? = null,
    
    @SerializedName("minimum_airport_departure_rate")
    val minimumAirportDepartureRate: Double? = null,
    
    @SerializedName("minimum_city_rate")
    val minimumCityRate: Double? = null,
    
    @SerializedName("minimum_cruise_port_departure_rate")
    val minimumCruisePortDepartureRate: Double? = null,
    
    @SerializedName("minimum_cruise_port_arrival_rate")
    val minimumCruisePortArrivalRate: Double? = null,
    
    @SerializedName("hourly_rate")
    val hourlyRate: Double? = null,
    
    @SerializedName("hourly_rate_after_five_hours")
    val hourlyRateAfterFiveHours: Double? = null,
    
    @SerializedName("hours_day_rate")
    val hoursDayRate: Int? = null,
    
    @SerializedName("day_rate")
    val dayRate: Double? = null,
    
    @SerializedName("km_mile")
    val kmMile: String = "mile",
    
    @SerializedName("gratuity")
    val gratuity: Double? = null,
    
    @SerializedName("is_gratuity")
    val isGratuity: String? = null,
    
    @SerializedName("minimum_on_demand_rate")
    val minimumOnDemandRate: Double? = null,
    
    @SerializedName("per_person_group_ride_rate")
    val perPersonGroupRideRate: Double? = null,
    
    @SerializedName("early_late_charges")
    val earlyLateCharges: Double? = null,
    
    @SerializedName("holiday_charges")
    val holidayCharges: Double? = null,
    
    @SerializedName("friday_saturday_charges")
    val fridaySaturdayCharges: Double? = null,
    
    @SerializedName("airport_arrival_tax_per_us")
    val airportArrivalTaxPerUs: Double? = null,
    
    @SerializedName("airport_departure_tax_per_us")
    val airportDepartureTaxPerUs: Double? = null,
    
    @SerializedName("sea_port_tax_per_us")
    val seaPortTaxPerUs: Double? = null,
    
    @SerializedName("city_congestion_tax_per_us")
    val cityCongestionTaxPerUs: Double? = null,
    
    @SerializedName("city_tax")
    val cityTax: Double? = null,
    
    @SerializedName("city_tax_percent_flat")
    val cityTaxPercentFlat: String? = null,
    
    @SerializedName("state_tax")
    val stateTax: Double? = null,
    
    @SerializedName("state_tax_percent_flat")
    val stateTaxPercentFlat: String? = null,
    
    @SerializedName("vat")
    val vat: Double? = null,
    
    @SerializedName("vat_percent_flat")
    val vatPercentFlat: String? = null,
    
    @SerializedName("workmans_comp")
    val workmansComp: Double? = null,
    
    @SerializedName("workman_comp_percent_flat")
    val workmanCompPercentFlat: String? = null,
    
    @SerializedName("other_transportation_tax")
    val otherTransportationTax: Double? = null,
    
    @SerializedName("other_transportation_tax_percent_flat")
    val otherTransportationTaxPercentFlat: String? = null,
    
    @SerializedName("rate_range")
    val rateRange: Double? = null,
    
    @SerializedName("rate_range_percent_flat")
    val rateRangePercentFlat: String? = null,
    
    @SerializedName("in_town_extra_stop")
    val inTownExtraStop: Double? = null,
    
    @SerializedName("outside_town_extra_stop")
    val outsideTownExtraStop: Double? = null,
    
    @SerializedName("minimum_charter_hours")
    val minimumCharterHours: Int? = null,
    
    // Kilometer variants (only when km_mile == "kilometer")
    @SerializedName("kilometer_rate")
    val kilometerRate: Double? = null,
    
    @SerializedName("upto_km")
    val uptoKm: Int? = null,
    
    @SerializedName("after_kilometer_rate")
    val afterKilometerRate: Double? = null,
    
    // Amenities rates payload
    @SerializedName("amenities_rates")
    val amenitiesRates: Map<String, VehicleAmenityPayload>? = null
)

data class VehicleAmenityPayload(
    @SerializedName("name")
    val name: String,
    @SerializedName("label")
    val label: String? = null,
    @SerializedName("price")
    val price: Double? = null
)

data class VehicleRateSettingsCompleteResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String?
)

data class VehicleRateSettingsStepResponse(
    @SerializedName("step")
    val step: String,
    
    @SerializedName("data")
    val data: VehicleRateSettingsStepPrefillData?,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean?
)

data class VehicleRateSettingsStepPrefillData(
    @SerializedName("vehicle_id")
    val vehicleId: Int? = null,

    // Legacy minimal fields
    @SerializedName("base_rate")
    val baseRate: String? = null,
    
    @SerializedName("per_mile_rate")
    val perMileRate: String? = null,
    
    @SerializedName("per_hour_rate")
    val perHourRate: String? = null,
    
    @SerializedName("currency")
    val currency: String? = null,
    
    // Expanded fields to mirror iOS/backend
    @SerializedName("milage_rate")
    val milageRate: Double? = null,
    
    @SerializedName("upto_miles")
    val uptoMiles: Int? = null,
    
    @SerializedName("after_mileage_rate")
    val afterMileageRate: Double? = null,
    
    @SerializedName("minimum_airport_arrival_rate")
    val minimumAirportArrivalRate: Double? = null,
    
    @SerializedName("minimum_airport_departure_rate")
    val minimumAirportDepartureRate: Double? = null,
    
    @SerializedName("minimum_city_rate")
    val minimumCityRate: Double? = null,
    
    @SerializedName("minimum_cruise_port_departure_rate")
    val minimumCruisePortDepartureRate: Double? = null,
    
    @SerializedName("minimum_cruise_port_arrival_rate")
    val minimumCruisePortArrivalRate: Double? = null,
    
    @SerializedName("hourly_rate")
    val hourlyRate: Double? = null,
    
    @SerializedName("hourly_rate_after_five_hours")
    val hourlyRateAfterFiveHours: Double? = null,
    
    @SerializedName("hours_day_rate")
    val hoursDayRate: Int? = null,
    
    @SerializedName("day_rate")
    val dayRate: Double? = null,
    
    @SerializedName("km_mile")
    val kmMile: String? = null,
    
    @SerializedName("gratuity")
    val gratuity: Double? = null,
    
    @SerializedName("is_gratuity")
    val isGratuity: String? = null,
    
    @SerializedName("minimum_on_demand_rate")
    val minimumOnDemandRate: Double? = null,
    
    @SerializedName("per_person_group_ride_rate")
    val perPersonGroupRideRate: Double? = null,
    
    @SerializedName("early_late_charges")
    val earlyLateCharges: Double? = null,
    
    @SerializedName("holiday_charges")
    val holidayCharges: Double? = null,
    
    @SerializedName("friday_saturday_charges")
    val fridaySaturdayCharges: Double? = null,
    
    @SerializedName("airport_arrival_tax_per_us")
    val airportArrivalTaxPerUs: Double? = null,
    
    @SerializedName("airport_departure_tax_per_us")
    val airportDepartureTaxPerUs: Double? = null,
    
    @SerializedName("sea_port_tax_per_us")
    val seaPortTaxPerUs: Double? = null,
    
    @SerializedName("city_congestion_tax_per_us")
    val cityCongestionTaxPerUs: Double? = null,
    
    @SerializedName("city_tax")
    val cityTax: Double? = null,
    
    @SerializedName("city_tax_percent_flat")
    val cityTaxPercentFlat: String? = null,
    
    @SerializedName("state_tax")
    val stateTax: Double? = null,
    
    @SerializedName("state_tax_percent_flat")
    val stateTaxPercentFlat: String? = null,
    
    @SerializedName("vat")
    val vat: Double? = null,
    
    @SerializedName("vat_percent_flat")
    val vatPercentFlat: String? = null,
    
    @SerializedName("workmans_comp")
    val workmansComp: Double? = null,
    
    @SerializedName("workman_comp_percent_flat")
    val workmanCompPercentFlat: String? = null,
    
    @SerializedName("other_transportation_tax")
    val otherTransportationTax: Double? = null,
    
    @SerializedName("other_transportation_tax_percent_flat")
    val otherTransportationTaxPercentFlat: String? = null,
    
    @SerializedName("rate_range")
    val rateRange: Double? = null,
    
    @SerializedName("rate_range_percent_flat")
    val rateRangePercentFlat: String? = null,
    
    @SerializedName("in_town_extra_stop")
    val inTownExtraStop: Double? = null,
    
    @SerializedName("outside_town_extra_stop")
    val outsideTownExtraStop: Double? = null,
    
    @SerializedName("minimum_charter_hours")
    val minimumCharterHours: Int? = null,
    
    @SerializedName("kilometer_rate")
    val kilometerRate: Double? = null,
    
    @SerializedName("upto_km")
    val uptoKm: Int? = null,
    
    @SerializedName("after_kilometer_rate")
    val afterKilometerRate: Double? = null,

    // Amenities (optional, returned by some backends)
    @SerializedName("amenities_rates")
    val amenitiesRates: Map<String, VehicleAmenityPayload>? = null
)

// ==================== Vehicle Info (for header + amenities metadata) ====================

data class VehicleInfoResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: VehicleInfoData?,

    @SerializedName("message")
    val message: String? = null,

    // Affiliate endpoint includes currency metadata in some responses
    @SerializedName("currency")
    val currency: VehicleCurrency? = null
)

data class VehicleCurrency(
    @SerializedName("countryName")
    val countryName: String? = null,

    @SerializedName("currency")
    val currency: String? = null,

    @SerializedName("currencyCountry")
    val currencyCountry: String? = null,

    @SerializedName("symbol")
    val symbol: String? = null,

    @SerializedName("dateFormat")
    val dateFormat: String? = null
)

data class VehicleInfoData(
    @SerializedName("vehicleType")
    val vehicleType: String?,

    @SerializedName("vehicleColor")
    val vehicleColor: String?,

    @SerializedName("vehicleYear")
    val vehicleYear: String?,

    @SerializedName("vehicleMake")
    val vehicleMake: String?,

    @SerializedName("vehicleModel")
    val vehicleModel: String?,

    @SerializedName("vehicle_image")
    val vehicleImage: String?,

    // Dictionary keyed by amenity id
    @SerializedName("amenities")
    val amenities: Map<String, VehicleAmenity>?
)

data class VehicleAmenity(
    @SerializedName("id")
    val id: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("label")
    val label: String?,

    // Price often returned as string; keep flexible
    @SerializedName("price")
    val price: String?
)

// ==================== Image Upload ====================

data class ImageUploadRequest(
    @SerializedName("image")
    val image: String // Base64 encoded image
)

data class ImageUploadResponse(
    @SerializedName("ID")
    val idInt: Int?,
    
    @SerializedName("id")
    val idString: String?,
    
    @SerializedName("image")
    val image: String?,
    
    @SerializedName("image_url")
    val imageUrl: String?
) {
    // Helper to get the image ID as Int
    fun getImageId(): Int? {
        return idInt ?: idString?.toIntOrNull()
    }
    
    // Helper to get the image URL
//    fun getImageUrl(): String? {
//        return imageUrl ?: image
//    }
}

// ==================== Vehicle Types & Options ====================

data class VehicleType(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("ID")
    val idInt: Int?,
    
    @SerializedName("vehicle_name")
    val vehicleName: String?,
    
    @SerializedName("vehicle_image")
    val vehicleImage: String?,
    
    @SerializedName("sort_order")
    val sortOrder: Int?,
    
    @SerializedName("status")
    val status: String?,
    
    @SerializedName("vehicle_count")
    val vehicleCount: Int?,
    
    @SerializedName("vehicle_id")
    val vehicleId: String?
) {
    // Helper to get identifier
    fun getIdentifier(): String {
        return id ?: (idInt?.toString() ?: "")
    }
}

data class Amenity(
    // Some responses send encrypted string IDs and also an "ID" integer.
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("ID")
    val idInt: Int?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String?
) {
    // Prefer numeric ID when available to avoid sending encrypted strings back
    fun getIdentifier(): String = idInt?.toString() ?: (id ?: "")
}

data class SpecialAmenity(
    @SerializedName("id")
    val id: String?,

    @SerializedName("ID")
    val idInt: Int?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String?
) {
    fun getIdentifier(): String = idInt?.toString() ?: (id ?: "")
}

data class VehicleInterior(
    // Some backends send encrypted string IDs along with numeric "ID"
    @SerializedName("id")
    val id: String?,

    @SerializedName("ID")
    val idInt: Int?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String?
) {
    fun getIdentifier(): String = idInt?.toString() ?: (id ?: "")
}

data class OrganisationType(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String?
)

// ==================== All Steps Response ====================

data class AllStepsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: RegistrationStepsData?,
    
    @SerializedName("timestamp")
    val timestamp: String?,
    
    @SerializedName("code")
    val code: Int?
)

data class RegistrationStepsData(
    @SerializedName("basic_info")
    val basicInfo: StepStatusResponse?,
    
    @SerializedName("company_info")
    val companyInfo: StepStatusResponse?,
    
    @SerializedName("company_documents")
    val companyDocuments: StepStatusResponse?,
    
    @SerializedName("privacy_terms")
    val privacyTerms: StepStatusResponse?,
    
    @SerializedName("driving_license")
    val drivingLicense: StepStatusResponse?,
    
    @SerializedName("bank_details")
    val bankDetails: StepStatusResponse?,
    
    @SerializedName("profile_picture")
    val profilePicture: StepStatusResponse?,
    
    @SerializedName("vehicle_insurance")
    val vehicleInsurance: StepStatusResponse?,
    
    @SerializedName("vehicle_details")
    val vehicleDetails: StepStatusResponse?,
    
    @SerializedName("vehicle_rate_settings")
    val vehicleRateSettings: StepStatusResponse?
)

data class StepStatusResponse(
    @SerializedName("is_completed")
    val isCompleted: Boolean
)

// ==================== Language ====================

data class Language(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String
)

