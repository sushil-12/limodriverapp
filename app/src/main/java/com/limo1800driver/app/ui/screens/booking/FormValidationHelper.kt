package com.limo1800driver.app.ui.screens.booking

/**
 * Helper class for form validation matching iOS behavior
 * Validates all fields based on transfer type and service type
 */
object FormValidationHelper {
    
    /**
     * Validate form and return map of field errors
     * Returns empty map if validation passes
     */
    fun validateForm(formState: BookingFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        // Always required fields
        if (formState.serviceType.isBlank()) {
            errors["serviceType"] = "Service Type is required"
        }
        
        if (formState.transferType.isBlank()) {
            errors["transferType"] = "Transfer Type is required"
        }
        
        if (formState.pickupDate.isBlank()) {
            errors["pickupDate"] = "Travel Date is required"
        }
        
        if (formState.pickupTime.isBlank()) {
            errors["pickupTime"] = "Pickup Time is required"
        }
        
        if (formState.meetGreetChoice.isBlank()) {
            errors["meetGreetChoice"] = "Meet and Greet Choices is required"
        }
        
        if (formState.numberOfVehicles.isBlank()) {
            errors["numberOfVehicles"] = "Number of Vehicles is required"
        }
        
        // Charter/Tour specific validation
        val normalizedServiceType = normalizeServiceType(formState.serviceType)
        if (normalizedServiceType.equals("Charter/Tour", ignoreCase = true)) {
            if (formState.numberOfHours.isBlank()) {
                errors["numberOfHours"] = "Number of Hours is required"
            } else {
                val hours = formState.numberOfHours.toIntOrNull()
                if (hours == null) {
                    errors["numberOfHours"] = "Only numeric values are allowed"
                } else if (hours < 2) {
                    errors["numberOfHours"] = "Number of Hours must be at least 2"
                }
            }
        }
        
        // Transfer type specific validation
        val transferTypeKey = transferTypeKey(formState.transferType)
        when (transferTypeKey) {
            "city_to_city" -> validateCityToCity(formState, errors)
            "city_to_airport" -> validateCityToAirport(formState, errors)
            "airport_to_city" -> validateAirportToCity(formState, errors)
            "airport_to_airport" -> validateAirportToAirport(formState, errors)
            "airport_to_cruise_port" -> validateAirportToCruisePort(formState, errors)
            "city_to_cruise_port" -> validateCityToCruisePort(formState, errors)
            "cruise_to_airport" -> validateCruisePortToAirport(formState, errors)
            "cruise_port_to_city" -> validateCruisePortToCity(formState, errors)
        }
        
        return errors
    }
    
    /**
     * Check if a field is required based on transfer type and service type
     */
    fun isFieldRequired(fieldName: String, formState: BookingFormState): Boolean {
        val transferType = formState.transferType.lowercase()
        val normalizedServiceType = normalizeServiceType(formState.serviceType)
        
        return when (fieldName) {
            "serviceType", "transferType", "pickupDate", "pickupTime", "meetGreetChoice", "numberOfVehicles" -> true
            "numberOfHours" -> normalizedServiceType.equals("Charter/Tour", ignoreCase = true)
            "pickupAddress" -> transferType.contains("city") || transferType.contains("cruise")
            "dropoffAddress" -> transferType.contains("city") || transferType.contains("cruise")
            "pickupAirport" -> transferType.contains("airport") && !transferType.contains("to airport")
            "pickupAirline" -> transferType.contains("airport") && !transferType.contains("to airport")
            "pickupOriginCity" -> transferType.contains("airport") && !transferType.contains("to airport")
            "dropoffAirport" -> transferType.contains("airport") && !transferType.contains("airport to")
            "dropoffAirline" -> transferType.contains("airport") && !transferType.contains("airport to")
            "dropoffDestinationCity" -> transferType == "airport to airport"
            "cruiseShipName" -> transferType.contains("cruise")
            "shipArrivalTime" -> transferType.contains("cruise")
            "cruisePort" -> transferType == "cruise port to city"
            else -> false
        }
    }
    
    private fun validateCityToCity(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupAddress.isBlank()) {
            errors["pickupAddress"] = "Pickup Address is required"
        }
        if (formState.dropoffAddress.isBlank()) {
            errors["dropoffAddress"] = "Drop-off Address is required"
        }
    }
    
    private fun validateCityToAirport(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupAddress.isBlank()) {
            errors["pickupAddress"] = "Pickup Address is required"
        }
        if (formState.dropoffAirport.isBlank()) {
            errors["dropoffAirport"] = "Select Airport is required"
        }
        if (formState.dropoffAirline.isBlank()) {
            errors["dropoffAirline"] = "Select Airline is required"
        }
    }
    
    private fun validateAirportToCity(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupAirport.isBlank()) {
            errors["pickupAirport"] = "Select Airport is required"
        }
        if (formState.pickupAirline.isBlank()) {
            errors["pickupAirline"] = "Select Airline is required"
        }
        if (formState.pickupOriginCity.isBlank()) {
            errors["pickupOriginCity"] = "Origin Airport / City is required"
        }
        if (formState.dropoffAddress.isBlank()) {
            errors["dropoffAddress"] = "Drop-off Address is required"
        }
    }
    
    private fun validateAirportToAirport(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupAirport.isBlank()) {
            errors["pickupAirport"] = "Select Airport is required"
        }
        if (formState.pickupAirline.isBlank()) {
            errors["pickupAirline"] = "Select Airline is required"
        }
        if (formState.pickupOriginCity.isBlank()) {
            errors["pickupOriginCity"] = "Origin Airport / City is required"
        }
        if (formState.dropoffAirport.isBlank()) {
            errors["dropoffAirport"] = "Select Airport is required"
        }
        if (formState.dropoffAirline.isBlank()) {
            errors["dropoffAirline"] = "Select Airline is required"
        }
        if (formState.dropoffDestinationCity.isBlank()) {
            errors["dropoffDestinationCity"] = "Destination Airport / City is required"
        }
    }
    
    private fun validateAirportToCruisePort(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupAirport.isBlank()) {
            errors["pickupAirport"] = "Select Airport is required"
        }
        if (formState.pickupAirline.isBlank()) {
            errors["pickupAirline"] = "Select Airline is required"
        }
        if (formState.pickupOriginCity.isBlank()) {
            errors["pickupOriginCity"] = "Origin Airport / City is required"
        }
        if (formState.dropoffAddress.isBlank()) {
            errors["dropoffAddress"] = "Drop-off Address is required"
        }
        if (formState.cruiseShipName.isBlank()) {
            errors["cruiseShipName"] = "Cruise Ship Name is required"
        }
        if (formState.shipArrivalTime.isBlank()) {
            errors["shipArrivalTime"] = "Ship Arrival Time is required"
        }
    }
    
    private fun validateCityToCruisePort(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupAddress.isBlank()) {
            errors["pickupAddress"] = "Pickup Address is required"
        }
        if (formState.dropoffAddress.isBlank()) {
            errors["dropoffAddress"] = "Drop-off Address is required"
        }
        if (formState.cruiseShipName.isBlank()) {
            errors["cruiseShipName"] = "Cruise Ship Name is required"
        }
        if (formState.shipArrivalTime.isBlank()) {
            errors["shipArrivalTime"] = "Ship Arrival Time is required"
        }
    }
    
    private fun validateCruisePortToAirport(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupAddress.isBlank()) {
            errors["pickupAddress"] = "Pickup Address is required"
        }
        if (formState.cruiseShipName.isBlank()) {
            errors["cruiseShipName"] = "Cruise Ship Name is required"
        }
        if (formState.shipArrivalTime.isBlank()) {
            errors["shipArrivalTime"] = "Ship Arrival Time is required"
        }
        if (formState.dropoffAirport.isBlank()) {
            errors["dropoffAirport"] = "Select Airport is required"
        }
        if (formState.dropoffAirline.isBlank()) {
            errors["dropoffAirline"] = "Select Airline is required"
        }
    }
    
    private fun validateCruisePortToCity(formState: BookingFormState, errors: MutableMap<String, String>) {
        if (formState.pickupOriginCity.isBlank()) {
            errors["cruisePort"] = "Cruise Port is required"
        }
        if (formState.cruiseShipName.isBlank()) {
            errors["cruiseShipName"] = "Cruise Ship Name is required"
        }
        if (formState.shipArrivalTime.isBlank()) {
            errors["shipArrivalTime"] = "Ship Arrival Time is required"
        }
        if (formState.pickupAddress.isBlank()) {
            errors["pickupAddress"] = "Pickup Address is required"
        }
        if (formState.dropoffAddress.isBlank()) {
            errors["dropoffAddress"] = "Drop-off Address is required"
        }
    }
    
    /**
     * Normalize service type for comparison
     */
    private fun normalizeServiceType(serviceType: String): String {
        return serviceType.trim()
    }
    
    /**
     * Get transfer type key for comparison
     */
    private fun transferTypeKey(transferType: String): String {
        return transferType.lowercase()
            .replace(" ", "_")
            .replace("?", "")
            .trim()
    }
}

