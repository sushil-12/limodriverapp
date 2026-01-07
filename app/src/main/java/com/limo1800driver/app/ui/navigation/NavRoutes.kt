package com.limo1800driver.app.ui.navigation

/**
 * Navigation routes for the app
 */
object NavRoutes {
    const val Onboarding = "onboarding"
    const val PhoneEntry = "phone_entry"
    const val Otp = "otp"
    
    // Registration routes
    const val BasicInfo = "basic_info"
    const val CompanyInfo = "company_info"
    const val CompanyDocuments = "company_documents"
    const val PrivacyTerms = "privacy_terms"
    const val UserProfileDetails = "user_profile_details"
    const val DrivingLicense = "driving_license"
    const val BankDetails = "bank_details"
    const val ProfilePicture = "profile_picture"
    const val VehicleSelection = "vehicle_selection"
    const val VehicleDetailsStep = "vehicle_details_step"
    const val VehicleDetails = "vehicle_details"
    const val VehicleAmenities = "vehicle_amenities"
    const val VehicleDetailsImageUpload = "vehicle_details_image_upload"
    const val VehicleInsurance = "vehicle_insurance"
    const val VehicleRates = "vehicle_rates"
    
    // Welcome Banner (shown after registration completion)
    const val WelcomeBanner = "welcome_banner"
    
    // Dashboard
    const val Dashboard = "dashboard"
    
    // Dashboard Menu Screens
    const val PreArrangedRides = "pre_arranged_rides"
    const val MyActivity = "my_activity"
    const val Wallet = "wallet"
    const val Notifications = "notifications"
    const val AccountSettings = "account_settings"

    // Booking flows (ported from iOS driver)
    // NOTE: argument patterns are defined in MainActivity NavHost.
    const val BookingPreview = "booking_preview"
    const val FinalizeBooking = "finalize_booking"
    const val FinalizeRates = "finalize_rates"
    const val EditBooking = "edit_booking"
    const val BookingMap = "booking_map"

    // Live ride flow
    const val RideInProgress = "ride_in_progress"

    // In-app chat (mirrors iOS ChatView)
    const val Chat = "chat"

    // Account Settings -> Vehicles (edit flows)
    // These routes reuse the registration screens, but return back to Account Settings on success.
    const val VehicleDetailsStepFromAccountSettings = "vehicle_details_step_from_account_settings"
    const val VehicleInsuranceFromAccountSettings = "vehicle_insurance_from_account_settings"
    const val VehicleRatesFromAccountSettings = "vehicle_rates_from_account_settings"
    
    // Account Settings -> Profile picture edit (reuse registration screen, return back on success)
    const val ProfilePictureFromAccountSettings = "profile_picture_from_account_settings"

    // Account Settings -> Company Info edit (reuse registration screen, return back on success)
    const val CompanyInfoFromAccountSettings = "company_info_from_account_settings"

    // Account Settings -> Company Documents edit (reuse registration screen, return back on success)
    const val CompanyDocumentsFromAccountSettings = "company_documents_from_account_settings"

    // Account Settings -> Basic Info edit (reuse registration screen, return back on success)
    const val BasicInfoFromAccountSettings = "basic_info_from_account_settings"

    // Account Settings -> Profile View (view-only profile information)
    const val ProfileView = "profile_view"

    // Account Settings -> Vehicle Details (iOS-style coordinator: form -> amenities -> image upload)
    const val VehicleDetailsCoordinatorFromAccountSettings = "vehicle_details_coordinator_from_account_settings"

    // WebView for external links
    const val WebView = "webview"
}

