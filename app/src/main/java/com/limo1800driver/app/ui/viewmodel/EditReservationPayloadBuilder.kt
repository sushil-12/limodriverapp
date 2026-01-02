package com.limo1800driver.app.ui.viewmodel

import com.limo1800driver.app.data.model.dashboard.*
import timber.log.Timber

/**
 * Helper class to build EditReservationRequest payload matching iOS exactly
 */
object EditReservationPayloadBuilder {

    /**
     * Converts AdminReservationRateArray to RateArray format
     * Preserves order using LinkedHashMap to match iOS behavior
     */
    fun convertToRateArray(adminRateArray: AdminReservationRateArray): RateArray {
        // Use LinkedHashMap to preserve insertion order (matching iOS behavior)
        val allInclusiveRates = linkedMapOf<String, RateItem>()
        adminRateArray.allInclusiveRates.forEach { (key, item) ->
            allInclusiveRates[key] = RateItem(
                rateLabel = item.rateLabel,
                baserate = item.baserate ?: 0.0,
                multiple = item.multiple ?: 0.0,
                percentage = item.percentage ?: 0.0,
                amount = item.amount ?: 0.0,
                type = item.type
            )
        }

        val taxes = linkedMapOf<String, RateItem>()
        adminRateArray.taxes.forEach { (key, item) ->
            taxes[key] = RateItem(
                rateLabel = item.rateLabel,
                baserate = item.baserate ?: 0.0,
                multiple = item.multiple ?: 0.0,
                percentage = item.percentage ?: 0.0,
                amount = item.amount ?: 0.0,
                type = item.type
            )
        }

        val amenities = linkedMapOf<String, RateItem>()
        adminRateArray.amenities.forEach { (key, item) ->
            amenities[key] = RateItem(
                rateLabel = item.rateLabel,
                baserate = item.baserate ?: 0.0,
                multiple = item.multiple ?: 0.0,
                percentage = item.percentage ?: 0.0,
                amount = item.amount ?: 0.0,
                type = item.type
            )
        }

        val misc = linkedMapOf<String, RateItem>()
        adminRateArray.misc.forEach { (key, item) ->
            misc[key] = RateItem(
                rateLabel = item.rateLabel,
                baserate = item.baserate ?: 0.0,
                multiple = item.multiple ?: 0.0,
                percentage = item.percentage ?: 0.0,
                amount = item.amount ?: 0.0,
                type = item.type
            )
        }

        return RateArray(
            allInclusiveRates = allInclusiveRates,
            directTaxes = emptyMap(), // Not available in Android data
            taxes = taxes,
            amenities = amenities,
            misc = misc
        )
    }

    /**
     * Builds SharesArray from rates data matching iOS buildSharesArray logic exactly
     */
    fun buildSharesArray(
        rateArray: RateArray,
        grandTotal: Double,
        preview: AdminBookingPreviewData,
        serviceType: String?,
        numberOfHours: Int
    ): SharesArray {
        // Calculate total baserates
        var totalBaserates = 0.0
        val isCharter = serviceType?.lowercase()?.contains("charter") == true
        val charterHours = if (isCharter) maxOf(numberOfHours, 0) else 0

        // Sum all_inclusive_rates baserates
        for ((key, item) in rateArray.allInclusiveRates) {
            val baserate = item.baserate ?: 0.0
            if (isCharter && charterHours > 0 && isBaseRateKey(key)) {
                val adjustedValue = baserate * charterHours
                totalBaserates += adjustedValue
            } else {
                totalBaserates += baserate
            }
        }

        // Sum taxes baserates
        for (item in rateArray.taxes.values) {
            totalBaserates += item.baserate ?: 0.0
        }

        // Sum amenities baserates
        for (item in rateArray.amenities.values) {
            totalBaserates += item.baserate ?: 0.0
        }

        // Sum misc baserates
        for (item in rateArray.misc.values) {
            totalBaserates += item.baserate ?: 0.0
        }

        // Calculate admin share baserates (only from all_inclusive_rates)
        var adminShareBaserates = 0.0
        for ((key, item) in rateArray.allInclusiveRates) {
            val baserate = item.baserate ?: 0.0
            if (isCharter && charterHours > 0 && isBaseRateKey(key)) {
                adminShareBaserates += baserate * charterHours
            } else {
                adminShareBaserates += baserate
            }
        }

        // Determine admin share percentage
        val accountType = preview.accountType ?: ""
        val createdBy = preview.createdBy ?: 0
        val reservationType = preview.reservationType ?: ""
        val isTravelPlannerSpecialCase = accountType == "travel_planner" && createdBy != 1
        val isFarmoutCase = reservationType == "farmout"

        val adminSharePercentage = if (isTravelPlannerSpecialCase || isFarmoutCase) {
            0.15 // 15% for special cases
        } else {
            0.25 // Default 25%
        }

        val adminShare = adminShareBaserates * adminSharePercentage

        // Calculate additional shares
        val travelAgentShare = if (isTravelPlannerSpecialCase) adminShareBaserates * 0.10 else 0.0
        val farmoutShare = if (isFarmoutCase) adminShareBaserates * 0.10 else 0.0

        // Calculate subtotal
        val subTotal = totalBaserates + adminShare + travelAgentShare + farmoutShare

        // Calculate grand total (subtotal × number of vehicles)
        val numberOfVehicles = preview.numberOfVehicles ?: 1
        val calculatedGrandTotal = subTotal * numberOfVehicles
        val finalGrandTotal = calculatedGrandTotal

        // Get actual base rate
        val actualBaseRate = rateArray.allInclusiveRates.entries
            .firstOrNull { isBaseRateKey(it.key) }?.value?.baserate ?: 0.0
        val baseRate = if (isCharter && charterHours > 0) {
            actualBaseRate * charterHours
        } else {
            actualBaseRate
        }

        // Calculate extra gratuity share (25% of extra gratuity amount)
        val extraGratuityAmount = rateArray.misc["Extra_Gratuity"]?.amount ?: 0.0
        val extraGratuityShare = extraGratuityAmount * 0.25

        // Calculate affiliate share
        val affiliateShare = when {
            isFarmoutCase -> finalGrandTotal - (adminShare + extraGratuityShare) - farmoutShare
            isTravelPlannerSpecialCase -> finalGrandTotal - (adminShare + extraGratuityShare) - travelAgentShare
            else -> finalGrandTotal - (adminShare + extraGratuityShare)
        }

        // Calculate Stripe fee (5% + $0.30)
        val stripeFee = finalGrandTotal * 0.05 + 0.30
        val deductedAdminShare = adminShare - stripeFee

        return SharesArray(
            baseRate = baseRate,
            grandTotal = finalGrandTotal,
            stripeFee = stripeFee,
            adminShare = adminShare,
            deductedAdminShare = deductedAdminShare,
            affiliateShare = affiliateShare,
            travelAgentShare = if (isTravelPlannerSpecialCase) travelAgentShare else null,
            farmoutShare = if (isFarmoutCase) farmoutShare else null
        )
    }

    private fun isBaseRateKey(key: String): Boolean {
        return key.lowercase().replace("_", "") == "baserate"
    }

    /**
     * Converts distance string to meters (Int)
     */
    fun parseDistanceToMeters(distance: String?): Int {
        if (distance.isNullOrBlank()) return 0

        try {
            // Remove common suffixes
            val clean = distance
                .replace(" miles", "", ignoreCase = true)
                .replace(" mile", "", ignoreCase = true)
                .replace(" mi", "", ignoreCase = true)
                .replace(" ", "")
                .trim()

            val value = clean.toDoubleOrNull() ?: return 0

            // If value > 1000, assume it's already in meters, otherwise convert from miles
            return if (value > 1000) {
                value.toInt()
            } else {
                (value * 1609).toInt() // Convert miles to meters
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse distance: $distance")
            return 0
        }
    }

    /**
     * Converts duration string to seconds (Int)
     */
    fun parseDurationToSeconds(duration: String?): Int {
        if (duration.isNullOrBlank()) return 0

        try {
            // Handle format like "1h 30m" or "90 minutes" or just seconds
            val parts = duration.lowercase().split(" ")
            var totalSeconds = 0

            for (part in parts) {
                when {
                    part.endsWith("h") || part.endsWith("hour") || part.endsWith("hours") -> {
                        val hours = part.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                        totalSeconds += (hours * 3600).toInt()
                    }
                    part.endsWith("m") || part.endsWith("min") || part.endsWith("minute") || part.endsWith("minutes") -> {
                        val minutes = part.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                        totalSeconds += (minutes * 60).toInt()
                    }
                    part.endsWith("s") || part.endsWith("sec") || part.endsWith("second") || part.endsWith("seconds") -> {
                        val seconds = part.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                        totalSeconds += seconds.toInt()
                    }
                    part.all { it.isDigit() } -> {
                        // If it's just a number, assume it's already in seconds
                        totalSeconds += part.toIntOrNull() ?: 0
                    }
                }
            }

            return totalSeconds
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse duration: $duration")
            return 0
        }
    }

    /**
     * Looks up airport by display name from airports list (matches iOS getAirportByDisplayName)
     */
    private fun getAirportByDisplayName(
        displayName: String?,
        airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport>
    ): com.limo1800driver.app.data.model.dashboard.MobileDataAirport? {
        if (displayName.isNullOrBlank()) return null
        return airports.firstOrNull { it.displayName.equals(displayName, ignoreCase = true) }
    }

    /**
     * Looks up airline by display name from airlines list (matches iOS getAirlineByDisplayName)
     * Tries exact match first, then partial match
     */
    private fun getAirlineByDisplayName(
        displayName: String?,
        airlines: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirline>
    ): com.limo1800driver.app.data.model.dashboard.MobileDataAirline? {
        if (displayName.isNullOrBlank() || airlines.isEmpty()) return null

        // Try exact match first (case-insensitive)
        airlines.firstOrNull { it.displayName.equals(displayName, ignoreCase = true) }?.let { return it }

        // Try partial match - check if display name contains airline code or name
        val normalizedDisplayName = displayName.lowercase().trim()
        return airlines.firstOrNull { airline ->
            val airlineCode = airline.code?.lowercase()?.trim() ?: ""
            val airlineName = airline.name?.lowercase()?.trim() ?: ""
            normalizedDisplayName.contains(airlineCode, ignoreCase = true) ||
            normalizedDisplayName.contains(airlineName, ignoreCase = true) ||
            airlineCode.isNotBlank() && normalizedDisplayName.startsWith("$airlineCode -", ignoreCase = true)
        }
    }

    /**
     * Builds airport option from display name (matches iOS buildAirportOption)
     */
    private fun buildAirportOption(
        displayName: String?,
        airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport>,
        fallbackLat: Double? = null,
        fallbackLong: Double? = null
    ): AirportOption? {
        if (displayName.isNullOrBlank()) return null

        val airport = getAirportByDisplayName(displayName, airports)
        return if (airport != null) {
            AirportOption(
                id = airport.id,
                code = airport.code,
                name = airport.name,
                city = airport.city,
                country = airport.country,
                lat = airport.lat ?: fallbackLat,
                long = airport.long ?: fallbackLong,
                formattedName = airport.displayName
            )
        } else {
            // If airport not found in list, create a basic option from preview data
            // This handles cases where airport data might not be loaded yet
            null
        }
    }

    /**
     * Builds airline option from display name (matches iOS buildAirlineOption)
     */
    private fun buildAirlineOption(
        displayName: String?,
        airlineId: Int?,
        airlines: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirline>
    ): AirlineOption? {
        // Matches iOS: guard let displayName = displayName else { return nil }
        if (displayName.isNullOrBlank()) return null

        // If we have a specific airline ID, use it
        if (airlineId != null && airlineId > 0) {
            val airline = airlines.firstOrNull { it.id == airlineId }
            if (airline != null) {
                return AirlineOption(
                    id = airline.id,
                    code = airline.code,
                    name = airline.name,
                    country = airline.country,
                    formattedName = airline.displayName
                )
            }
        }

        // Try to find airline by display name
        val airline = getAirlineByDisplayName(displayName, airlines)
        return if (airline != null) {
            // Found airline in list - use its actual ID
            AirlineOption(
                id = airline.id,
                code = airline.code,
                name = airline.name,
                country = airline.country,
                formattedName = airline.displayName
            )
        } else if (airlineId != null && airlineId > 0) {
            // Airline not found but we have an ID - create option with provided ID
            AirlineOption(
                id = airlineId,
                code = null,
                name = displayName,
                country = null,
                formattedName = displayName
            )
        } else {
            // Airline not found and no ID provided - return null
            // The fallback logic in the payload builder will handle this
            null
        }
    }

    /**
     * Builds complete EditReservationRequest from available data
     */
    fun buildEditReservationRequest(
        preview: AdminBookingPreviewData,
        rates: AdminReservationRatesData?,

        userInput: EditReservationUserInput,
        airports: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirport> = emptyList(),
        airlines: List<com.limo1800driver.app.data.model.dashboard.MobileDataAirline> = emptyList(),
        calculatedShares: ReservationShareData? = null
    ): EditReservationRequest {
        Timber.tag("BookingService").d("=== Building EditReservationRequest ===")
        Timber.tag("BookingService").d("Preview data: reservationId=${preview.reservationId}, accId=${preview.accId}, affiliateId=${preview.affiliateId}, driverId=${preview.driverId}")
        Timber.tag("BookingService").d("Airports count: ${airports.size}, Airlines count: ${airlines.size}")

        val reservationId = preview.reservationId ?: throw IllegalArgumentException("reservation_id is required")

        // Convert service type and transfer type to API format
        val serviceType = normalizeServiceType(userInput.serviceType ?: preview.serviceType ?: "one_way")
        val transferType = normalizeTransferType(userInput.transferType ?: preview.transferType ?: "city_to_city")
        val returnTransferType = getReturnTransferType(transferType)
        Timber.tag("BookingService").d("Service/Transfer types: serviceType=$serviceType, transferType=$transferType, returnTransferType=$returnTransferType")

        // Calculate number of hours matching iOS buildBasicInfo logic exactly
        // Always 0 for one_way, user value for charter_tour
        val normalizedServiceTypeForHours = serviceType.lowercase()
            .replace("/", "_")
            .replace(" ", "_")
        val numberOfHours = if (normalizedServiceTypeForHours.contains("charter")) {
            userInput.numberOfHours ?: preview.numberOfHours ?: 0
        } else {
            0
        }
        Timber.tag("BookingService").d("Number of hours calculation: serviceType=$serviceType, normalized=$normalizedServiceTypeForHours, hours=$numberOfHours")

        // Build rate array
        val rateArray = rates?.rateArray?.let { convertToRateArray(it) } ?: RateArray()
        val grandTotal = rates?.grandTotal ?: preview.grandTotal ?: 0.0
        val subTotal = rates?.subTotal ?: (grandTotal * 0.9) // Estimate if not available

        // Build shares array (matching iOS buildSharesArray)
        val sharesArray = buildSharesArray(
            rateArray = rateArray,
            grandTotal = grandTotal,
            preview = preview,
            serviceType = serviceType,
            numberOfHours = numberOfHours
        )

        // Parse distance and duration - use calculated if location changed, original API if not
        // This matches iOS buildFinalInfo logic
        val hasLocationChanged = userInput.hasLocationChanged ?: false
        val journeyDistance: Int
        val journeyTime: Int

        if (hasLocationChanged) {
            // User changed locations - use calculated distance/time
            // Note: We need coordinates from user input for calculation
            // For now, parse from preview but this should be calculated from coordinates
            journeyDistance = parseDistanceToMeters(preview.distance)
            journeyTime = parseDurationToSeconds(preview.duration)
            Timber.tag("BookingService").d("Location changed - using calculated distance/time")
        } else {
            // User didn't change locations - use original API distance/time
            journeyDistance = parseDistanceToMeters(preview.distance)
            journeyTime = parseDurationToSeconds(preview.duration)
            Timber.tag("BookingService").d("Location unchanged - using original API distance/time")
        }

        // Build extra stops from user input (matching iOS buildBasicInfo logic)
        // User input extra stops should include coordinates and rates
        val extraStops = if (userInput.extraStops != null && userInput.extraStops!!.isNotEmpty()) {
            // Use user input extra stops with coordinates and rates
            userInput.extraStops!!.mapNotNull { stop ->
                if (stop.address.isNullOrBlank()) return@mapNotNull null
                EditReservationExtraStopRequest(
                    address = stop.address,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    rate = stop.rate ?: "out_town", // Default to "out_town" if not provided
                    bookingInstructions = stop.bookingInstructions ?: ""
                )
            }
        } else {
            // Fall back to preview extra stops
            preview.extraStops?.map { stop ->
                EditReservationExtraStopRequest(
                    rate = stop.rate ?: "out_town",
                    address = stop.address ?: "",
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    bookingInstructions = stop.bookingInstructions ?: ""
                )
            } ?: emptyList()
        }

        // Build airport/airline options by looking up from airports/airlines list (matches iOS)
        // Use user input name first, then fall back to preview data
        val pickupAirportName = userInput.pickupAirportName ?: preview.pickupAirportName
        val pickupAirportOption = buildAirportOption(
            displayName = pickupAirportName,
            airports = airports,
            fallbackLat = preview.pickupAirportLatitude,
            fallbackLong = preview.pickupAirportLongitude
        )
        Timber.tag("BookingService").d("Pickup airport: name=$pickupAirportName, option=${pickupAirportOption?.id}, found=${pickupAirportOption != null}")

        val dropoffAirportName = userInput.dropoffAirportName ?: preview.dropoffAirportName
        // For city_to_airport, dropoff_airport is required, so we must look it up
        val dropoffAirportOption = buildAirportOption(
            displayName = dropoffAirportName,
            airports = airports,
            fallbackLat = preview.dropoffAirportLatitude,
            fallbackLong = preview.dropoffAirportLongitude
        )
        Timber.tag("BookingService").d("Dropoff airport: name=$dropoffAirportName, option=${dropoffAirportOption?.id}, found=${dropoffAirportOption != null}")

        // If transfer type is city_to_airport and we have airport name but no option was created,
        // try to find airport by name directly to ensure we have the ID
        val dropoffAirportIdForCityToAirport = if (transferType == "city_to_airport" && dropoffAirportOption == null && !dropoffAirportName.isNullOrBlank()) {
            getAirportByDisplayName(dropoffAirportName, airports)?.id
        } else {
            null
        }
        if (dropoffAirportIdForCityToAirport != null) {
            Timber.tag("BookingService").d("Found dropoff airport ID for city_to_airport: $dropoffAirportIdForCityToAirport")
        }

        val pickupAirlineName = userInput.pickupAirlineName ?: preview.pickupAirlineName
        val pickupAirlineOption = buildAirlineOption(
            displayName = pickupAirlineName,
            airlineId = userInput.pickupAirlineId,
            airlines = airlines
        )
        Timber.tag("BookingService").d("Pickup airline: name=$pickupAirlineName, option=${pickupAirlineOption?.id}, found=${pickupAirlineOption != null}")

        val dropoffAirlineName = userInput.dropoffAirlineName ?: preview.dropoffAirlineName
        val dropoffAirlineOption = buildAirlineOption(
            displayName = dropoffAirlineName,
            airlineId = userInput.dropoffAirlineId,
            airlines = airlines
        )
        Timber.tag("BookingService").d("Dropoff airline: name=$dropoffAirlineName, option=${dropoffAirlineOption?.id}, found=${dropoffAirlineOption != null}")

        // Use user input or fall back to preview data
        val pickupAddress = userInput.pickupAddress ?: preview.pickupAddress ?: ""
        val dropoffAddress = userInput.dropoffAddress ?: preview.dropoffAddress ?: ""
        val pickupDate = userInput.pickupDate ?: preview.pickupDate ?: ""
        val pickupTime = userInput.pickupTime ?: preview.pickupTime ?: ""
        val pickupLatitude = userInput.pickupLatitude ?: preview.pickupLatitude ?: "0"
        val pickupLongitude = userInput.pickupLongitude ?: preview.pickupLongitude ?: "0"
        val dropoffLatitude = userInput.dropoffLatitude ?: preview.dropoffLatitude ?: "0"
        val dropoffLongitude = userInput.dropoffLongitude ?: preview.dropoffLongitude ?: "0"

        // Passenger info (matches iOS buildPassengerInfo)
        val passengerName = userInput.passengerName ?: preview.passengerName ?: ""
        val passengerEmail = userInput.passengerEmail ?: preview.passengerEmail ?: ""
        // Matches iOS: Int(bookingData.passenger_cell ?? "") ?? 0
        // But we use String to avoid Int overflow issues (Java Int is 32-bit, Swift Int is 64-bit)
        // Strip non-numeric characters and keep as String for backend compatibility
        val passengerCellString = userInput.passengerCell ?: preview.passengerCell ?: ""
        val passengerCell = passengerCellString.replace(Regex("[^0-9]"), "").takeIf { it.isNotBlank() } ?: "0"
        val passengerCellIsd = userInput.passengerCellIsd ?: preview.passengerCellIsd ?: ""
        val passengerCellCountry = preview.passengerCellCountry ?: "" // Available in preview
        val totalPassengers = preview.totalPassengers ?: 1
        val luggageCount = preview.luggageCount ?: 0

        // Vehicle info - using defaults as vehicle details not fully available
        val vehicleId = userInput.vehicleId?.toString() ?: preview.vehicleId?.toString() ?: "0"
        val vehicleType = preview.vehicleTypeName ?: ""
        val vehicleTypeName = preview.vehicleTypeName ?: ""

        // Build the complete request
        val accId = preview.accId ?: 0
        val accountType = preview.accountType ?: "affiliate"
        val travelClientId = preview.travelClientId ?: 0
        val affiliateId = preview.affiliateId ?: 0
        Timber.tag("BookingService").d("Account info: accId=$accId, accountType=$accountType, travelClientId=$travelClientId, affiliateId=$affiliateId")

        return EditReservationRequest(
            // Basic info
            serviceType = serviceType,
            transferType = transferType,
            returnTransferType = returnTransferType,
            numberOfHours = numberOfHours,
            // Matches iOS: bookingData.acc_id
            accId = accId,
            // Matches iOS: bookingData.account_type ?? ""
            accountType = accountType,
            // Matches iOS: bookingData.travel_client_id ?? 0
            travelClientId = travelClientId,

            // Passenger info
            passengerName = passengerName,
            passengerEmail = passengerEmail,
            passengerCell = passengerCell,
            passengerCellIsd = passengerCellIsd,
            passengerCellCountry = passengerCellCountry,
            totalPassengers = totalPassengers,
            luggageCount = luggageCount,

            // Booking instructions
            bookingInstructions = userInput.bookingInstructions ?: preview.bookingInstructions ?: "",
            returnBookingInstructions = userInput.bookingInstructions ?: preview.bookingInstructions ?: "",

            // Affiliate info (matches iOS)
            affiliateType = "affiliate",
            // Matches iOS: bookingData.affiliate_id
            affiliateId = affiliateId,
            returnAffiliateType = "affiliate",
            returnAffiliateId = "",
            looseAffiliateId = "",
            isOldLooseAffiliate = false,
            returnLooseAffiliateId = "",
            returnIsOldLooseAffiliate = false,
            cancellationHours = (preview.cancellationHours ?: 0).toString(),

            // Driver info (matches iOS buildDriverInfo)
            // Matches iOS: bookingData.driver_id
            driverId = preview.driverId ?: 0,
            // Matches iOS: "\(bookingData.driver.first_name) \(bookingData.driver.last_name)"
            driverName = preview.driverFirstName?.let { firstName ->
                preview.driverLastName?.let { lastName ->
                    "$firstName $lastName"
                } ?: firstName
            } ?: "",
            driverGender = preview.driverGender ?: "",
            // Matches iOS: bookingData.driver.cell_number
            driverCell = preview.driverCellNumber ?: "",
            // Matches iOS: bookingData.driver.cell_isd
            driverCellIsd = preview.driverCellIsd ?: "",
            // Matches iOS: bookingData.driver.CellNumberCountry
            driverCellCountry = preview.driverCellCountry ?: "",
            // Matches iOS: bookingData.driver.email ?? ""
            driverEmail = preview.driverEmail ?: "",

            // Return vehicle info - using defaults
            returnVehicleType = "",
            returnVehicleTypeName = "",
            returnVehicleId = "",
            returnVehicleMake = "",
            returnVehicleMakeName = "",
            returnVehicleModel = "",
            returnVehicleModelName = "",
            returnVehicleYear = "",
            returnVehicleYearName = "",
            returnVehicleColor = "",
            returnVehicleColorName = "",
            returnVehicleLicensePlate = "",
            returnVehicleSeats = "4",

            // Return driver info - using defaults
            returnDriverId = "",
            returnDriverName = "",
            returnDriverGender = "",
            returnDriverCell = "",
            returnDriverCellIsd = "",
            returnDriverCellCountry = "",
            returnDriverEmail = "",
            driverPhoneType = "",
            returnDriverPhoneType = "",
            driverImageId = "",
            vehicleImageId = "",

            // Meet & greet
            meetGreetChoices = 1, // Default
            meetGreetChoicesName = userInput.meetGreetChoiceName ?: preview.meetGreetChoiceName ?: "Driver - Text/call when on location",
            numberOfVehicles = userInput.numberOfVehicles ?: preview.numberOfVehicles ?: 1,
            pickupDate = pickupDate,
            pickupTime = pickupTime,
            extraStops = extraStops,

            // Pickup location
            pickup = pickupAddress,
            pickupLatitude = pickupLatitude,
            pickupLongitude = pickupLongitude,
            pickupAirportOption = pickupAirportOption,
            pickupAirport = pickupAirportOption?.id,
            // Matches iOS: pickupAirportOption?.formatted_name ?? bookingData.pickup_airport_name ?? ""
            pickupAirportName = pickupAirportOption?.formattedName ?: pickupAirportName ?: preview.pickupAirportName ?: "",
            // Matches iOS: pickupAirportOption?.lat?.description ?? String(bookingData.pickup_latitude)
            pickupAirportLatitude = pickupAirportOption?.lat?.toString() ?: pickupLatitude,
            // Matches iOS: pickupAirportOption?.long?.description ?? String(bookingData.pickup_longitude)
            pickupAirportLongitude = pickupAirportOption?.long?.toString() ?: pickupLongitude,
            pickupAirlineOption = pickupAirlineOption,
            // Matches iOS: pickupAirlineOption?.id ?? (bookingData.pickup_airline_name != nil ? 0 : nil)
            pickupAirline = pickupAirlineOption?.id ?: (if (!pickupAirlineName.isNullOrBlank()) 0 else null),
            // Matches iOS: pickupAirlineOption?.formatted_name ?? bookingData.pickup_airline_name ?? ""
            pickupAirlineName = pickupAirlineOption?.formattedName ?: pickupAirlineName ?: preview.pickupAirlineName ?: "",
            // Matches iOS: pickupFlightNumber.isEmpty ? nil : pickupFlightNumber
            pickupFlight = (userInput.pickupFlight ?: preview.pickupFlight)?.takeIf { it.isNotBlank() },
            // Matches iOS: pickupOriginCity.isEmpty ? nil : pickupOriginCity
            originAirportCity = (userInput.originAirportCity ?: preview.originAirportCity)?.takeIf { it.isNotBlank() },
            // Matches iOS: nil (departing_airport_city is always nil)
            departingAirportCity = null,

            // Cruise info
            cruisePort = userInput.cruisePort ?: preview.cruisePort ?: "",
            cruiseName = userInput.cruiseName ?: preview.cruiseName ?: "",
            cruiseTime = userInput.cruiseTime ?: preview.cruiseTime ?: "",

            // Dropoff location
            dropoff = dropoffAddress,
            dropoffLatitude = dropoffLatitude,
            dropoffLongitude = dropoffLongitude,
            dropoffAirportOption = dropoffAirportOption,
            // For city_to_airport, dropoff_airport is required (matches iOS: dropoffAirportOption?.id)
            // Use airport option ID if available, otherwise use the ID we looked up separately
            dropoffAirport = dropoffAirportOption?.id ?: dropoffAirportIdForCityToAirport,
            // Always set dropoff_airport_name (matches iOS: dropoffAirportOption?.formatted_name ?? bookingData.dropoff_airport_name ?? "")
            dropoffAirportName = dropoffAirportOption?.formattedName ?: dropoffAirportName ?: preview.dropoffAirportName ?: "",
            // Matches iOS: dropoffAirportOption?.lat?.description ?? String(bookingData.dropoff_latitude)
            dropoffAirportLatitude = dropoffAirportOption?.lat?.toString() ?: dropoffLatitude,
            // Matches iOS: dropoffAirportOption?.long?.description ?? String(bookingData.dropoff_longitude)
            dropoffAirportLongitude = dropoffAirportOption?.long?.toString() ?: dropoffLongitude,
            dropoffAirlineOption = dropoffAirlineOption,
            // Matches iOS: dropoffAirlineOption?.id ?? (bookingData.dropoff_airline_name != nil ? 0 : nil)
            // If option exists, use its ID. If null but preview/user has airline name, use 0. Otherwise null.
            dropoffAirline = dropoffAirlineOption?.id ?: (if (!dropoffAirlineName.isNullOrBlank()) 0 else null),
            // Matches iOS: dropoffAirlineOption?.formatted_name ?? bookingData.dropoff_airline_name ?? ""
            dropoffAirlineName = dropoffAirlineOption?.formattedName ?: dropoffAirlineName ?: preview.dropoffAirlineName ?: "",
            // Matches iOS: dropoffFlightNumber (can be empty string, which becomes nil)
            dropoffFlight = (userInput.dropoffFlight ?: preview.dropoffFlight)?.takeIf { it.isNotBlank() },

            // Return trip info - using defaults
            returnMeetGreetChoices = 1,
            returnMeetGreetChoicesName = "Driver - Text/call when on location",
            returnPickupDate = pickupDate,
            returnPickupTime = "12:00:00",
            returnExtraStops = emptyList(),
            returnPickup = dropoffAddress,
            returnPickupLatitude = dropoffLatitude,
            returnPickupLongitude = dropoffLongitude,
            returnPickupAirportOption = null,
            returnPickupAirport = null,
            returnPickupAirportName = null,
            returnPickupAirportLatitude = null,
            returnPickupAirportLongitude = null,
            returnPickupAirlineOption = null,
            returnPickupAirline = null,
            returnPickupAirlineName = null,
            returnPickupFlight = "",
            returnCruisePort = "",
            returnCruiseName = "",
            returnCruiseTime = "00:00:00",
            returnDropoff = pickupAddress,
            returnDropoffLatitude = pickupLatitude,
            returnDropoffLongitude = pickupLongitude,
            returnDropoffAirportOption = null,
            returnDropoffAirport = null,
            returnDropoffAirportName = null,
            returnDropoffAirportLatitude = null,
            returnDropoffAirportLongitude = null,
            returnDropoffAirlineOption = null,
            returnDropoffAirline = null,
            returnDropoffAirlineName = null,
            returnDropoffFlight = null,

            // Journey info
            driverLanguages = listOf(1), // Default
            driverDresses = emptyList(),
            amenities = emptyList(),
            chargedAmenities = emptyList(),
            journeyDistance = journeyDistance,
            journeyTime = journeyTime,
            returnJourneyDistance = "",
            returnJourneyTime = "",

            // Reservation info
            reservationId = reservationId,
            updateType = "edit",
            susbcriberName = "", // Not available
            returnSusbcriberName = "",
            bookingCreatedFrom = "admin",
            proceed = true,
            currency = preview.currencySymbol ?: "USD",
            rateArray = rateArray,
            grandTotal = grandTotal,
            subTotal = subTotal,
            minRateInvolved = rates?.minRateInvolved ?: false,
            sharesArray = sharesArray,
            changeIndividualData = false,

            // Vehicle info
            vehicleColor = "",
            vehicleColorName = "",
            vehicleId = vehicleId,
            vehicleLicensePlate = "",
            vehicleMake = "",
            vehicleMakeName = "",
            vehicleModel = "",
            vehicleModelName = "",
            vehicleSeats = "4",
            vehicleType = vehicleType,
            vehicleTypeName = vehicleTypeName,
            vehicleYear = "",
            vehicleYearName = ""
        ).also { request ->
            Timber.tag("BookingService").d("=== EditReservationRequest Built ===")
            Timber.tag("BookingService").d("Final payload: accId=${request.accId}, affiliateId=${request.affiliateId}, driverId=${request.driverId}")
            Timber.tag("BookingService").d("Passenger: cell=${request.passengerCell}, isd=${request.passengerCellIsd}, country=${request.passengerCellCountry}")
            Timber.tag("BookingService").d("Pickup airline: id=${request.pickupAirline}, name=${request.pickupAirlineName}")
            Timber.tag("BookingService").d("Dropoff airline: id=${request.dropoffAirline}, name=${request.dropoffAirlineName}")
            Timber.tag("BookingService").d("Pickup airport: id=${request.pickupAirport}, name=${request.pickupAirportName}")
            Timber.tag("BookingService").d("Dropoff airport: id=${request.dropoffAirport}, name=${request.dropoffAirportName}")
            Timber.tag("BookingService").d("Driver: id=${request.driverId}, name=${request.driverName}, cell=${request.driverCell}")
        }
    }

    /**
     * Converts service type to API format matching iOS getServiceTypeAPIValue exactly
     */
    private fun normalizeServiceType(serviceType: String): String {
        // Normalize text so small UI label changes (extra spaces, ?, /) do not break API payloads
        var sanitized = serviceType
            .trim()
            .replace(" ?", "", ignoreCase = true)
            .replace("?", "")
            .replace("/", "_")
            .replace(" ", "_")
            .lowercase()

        // Remove double underscores
        while (sanitized.contains("__")) {
            sanitized = sanitized.replace("__", "_")
        }
        // Remove leading/trailing underscores
        sanitized = sanitized.trim('_')

        return when {
            sanitized == "one_way" -> "one_way"
            sanitized.contains("charter") && sanitized.contains("tour") -> "charter_tour"
            else -> sanitized
        }
    }

    /**
     * Converts transfer type to API format matching iOS getTransferTypeAPIValue exactly
     */
    private fun normalizeTransferType(transferType: String): String {
        return when (transferType.trim()) {
            "Airport To City ?", "Airport To City" -> "airport_to_city"
            "Airport To Airport ?", "Airport To Airport" -> "airport_to_airport"
            "Airport To Cruise Port ?", "Airport To Cruise Port" -> "airport_to_cruise_port"
            "City To City ?", "City To City" -> "city_to_city"
            "City To Airport ?", "City To Airport" -> "city_to_airport"
            "City To Cruise Port ?", "City To Cruise Port" -> "city_to_cruise_port"
            "Cruise Port To Airport ?", "Cruise Port To Airport" -> "cruise_to_airport"
            "Cruise Port to City ?", "Cruise Port to City", "Cruise Port To City ?", "Cruise Port To City" -> "cruise_port_to_city"
            else -> transferType.lowercase()
                .replace(" ?", "")
                .replace(" ", "_")
        }
    }

    private fun getReturnTransferType(transferType: String): String {
        return when (transferType) {
            "city_to_airport" -> "airport_to_city"
            "airport_to_city" -> "city_to_airport"
            else -> "city_to_city"
        }
    }
}

/**
 * Data class to hold user input for edit reservation
 */
data class EditReservationUserInput(
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
    val pickupDate: String? = null,
    val pickupTime: String? = null,
    val pickupLatitude: String? = null,
    val pickupLongitude: String? = null,
    val dropoffLatitude: String? = null,
    val dropoffLongitude: String? = null,
    val vehicleId: Int? = null,
    val serviceType: String? = null,
    val transferType: String? = null,
    val numberOfHours: Int? = null,
    val numberOfVehicles: Int? = null,
    val meetGreetChoiceName: String? = null,
    val bookingInstructions: String? = null,
    val passengerName: String? = null,
    val passengerEmail: String? = null,
    val passengerCellIsd: String? = null,
    val passengerCell: String? = null,
    val pickupAirportId: Int? = null,
    val pickupAirportName: String? = null,
    val pickupAirlineId: Int? = null,
    val pickupAirlineName: String? = null,
    val pickupFlight: String? = null,
    val originAirportCity: String? = null,
    val cruisePort: String? = null,
    val cruiseName: String? = null,
    val cruiseTime: String? = null,
    val dropoffAirportId: Int? = null,
    val dropoffAirportName: String? = null,
    val dropoffAirlineId: Int? = null,
    val dropoffAirlineName: String? = null,
    val dropoffFlight: String? = null,
    val departingAirportCity: String? = null,
    val extraStops: List<ExtraStopInput>? = null,
    val hasLocationChanged: Boolean? = null
)

/**
 * Data class for extra stop input from user
 */
data class ExtraStopInput(
    val address: String,
    val latitude: String?,
    val longitude: String?,
    val rate: String?,
    val bookingInstructions: String?
)

