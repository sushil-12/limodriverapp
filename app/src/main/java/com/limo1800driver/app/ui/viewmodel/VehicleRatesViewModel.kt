package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.registration.VehicleAmenity
import com.limo1800driver.app.data.model.registration.VehicleAmenityPayload
import com.limo1800driver.app.data.model.registration.VehicleInfoData
import com.limo1800driver.app.data.model.registration.VehicleRateSettingsRequest
import com.limo1800driver.app.data.model.registration.VehicleRateSettingsStepPrefillData
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import com.limo1800driver.app.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.collections.forEach
import kotlin.math.abs

// Parity enums
enum class RateStep { BASE_RATES, AMENITIES_TAXES }
enum class AdjustmentType { PERCENT, FLAT }
enum class DistanceUnit { MILE, KILOMETER }

data class VehicleRatesState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val nextStep: String? = null,

    // Navigation
    val currentStep: RateStep = RateStep.BASE_RATES,

    // Vehicle header
    val vehicleName: String = "",
    val vehicleTags: List<String> = emptyList(), // color/year/make/model
    val vehicleImageUrl: String? = null,

    // Currency + unit
    val currencyOptions: List<String> = listOf(
        "United States - $",
        "Canada - CA$",
        "United Kingdom - £",
        "Euro - €"
    ),
    val selectedCurrency: String = "United States - $",
    val isCurrencySheetVisible: Boolean = false,
    val distanceUnit: DistanceUnit = DistanceUnit.MILE,

    // Adjustment slider
    val adjustmentType: AdjustmentType = AdjustmentType.FLAT,
    val adjustmentValue: Float = 0f, // -10..10

    // Base rates (mile/km)
    val perMile: String? = null,
    val upToMiles: String? = null,
    val additionalPerMile: String? = null,

    // Monetary fields (as strings for TextField binding)
    val minAirportDep: String = "",
    val minAirportArr: String = "",
    val minCityIntercity: String = "",
    val minCruiseArr: String = "",
    val minCruiseDep: String = "",
    val hourlyRate: String = "",
    val discountAfter5Hrs: String = "",
    val dayRateExt: String = "",
    val minCharterHrs: String? = null,
    val numHoursDayExt: String? = null,
    val sharedRideRate: String? = null,
    val earlyLateSurge: String? = null,
    val holidaySurge: String? = null,
    val friSatSurge: String? = null,
    val extraStopSame: String = "",
    val extraStopDiff: String = "",
    val seaPortTax: String = "",
    val cityCongestionTax: String = "",
    val cityTax: String = "",
    val stateTax: String = "",
    val vat: String = "",
    val workmansComp: String = "",
    val otherTransportTax: String = "",
    val airportArrivalTax: String = "",
    val airportDepTax: String = "",

    // Toggles for percent/flat taxes
    val cityTaxIsFlat: Boolean = true,
    val stateTaxIsFlat: Boolean = true,
    val vatIsFlat: Boolean = true,
    val workmansCompIsFlat: Boolean = true,
    val otherTransportTaxIsFlat: Boolean = true,

    // Options lists
    val perMileOptions: List<String> = listOf(
        "2.50","2.75","3","3.25","3.40","3.50","3.60","3.75","4","4.25","4.50","4.75",
        "5","5.25","5.50","5.75","6","6.25","6.75","7","7.5","8","9","10","11","12","13","14","15"
    ),
    val uptoMilesOptions: List<String> = listOf("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15"),
    val minCharterHoursOptions: List<String> = listOf("2","3","4","5"),
    val hoursDayRateOptions: List<String> = listOf("8","10","12"),
    val surgeOptions: List<String> = listOf("0","10","15","20","25","30","35","40","45","50","55","60","65","70","75","80","85","90","95","100"),
    val sharedRideOptions: List<String> = listOf("0","10","15","20","25","30","35","40","45","50","55","60","65","70","75","80","85","90","95","100"),

    // Amenity rates
    val amenityRates: Map<String, VehicleAmenityPayload> = emptyMap(), // id -> payload with price
    val amenityPrices: Map<String, String> = emptyMap(), // label -> price string for UI

    // Prefill flags
    val isPrefilling: Boolean = false,

    // Vehicle ids
    val vehicleId: Int? = null
)

sealed class RateEvent {
    object OnNextClick : RateEvent()
    object OnBackClick : RateEvent()
    object OnSubmitClick : RateEvent()

    data class SetAdjustmentType(val type: AdjustmentType) : RateEvent()
    data class SetAdjustmentValue(val value: Float) : RateEvent()
    object ToggleDistanceUnit : RateEvent()

    object OpenCurrencySheet : RateEvent()
    object DismissCurrencySheet : RateEvent()
    data class SelectCurrency(val currency: String) : RateEvent()

    // Base rates
    data class SetPerMile(val value: String?) : RateEvent()
    data class SetUpToMiles(val value: String?) : RateEvent()
    data class SetAdditionalPerMile(val value: String?) : RateEvent()

    // Monetary fields
    data class SetField(val key: String, val value: String) : RateEvent()

    // Toggles
    data class SetTaxFlat(val key: String, val isFlat: Boolean) : RateEvent()
    data class SetAmenityPrice(val label: String, val price: String) : RateEvent()
}

@HiltViewModel
class VehicleRatesViewModel @Inject constructor(
    private val repo: DriverRegistrationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleRatesState())
    val uiState: StateFlow<VehicleRatesState> = _uiState.asStateFlow()

    init {
        loadPrefill()
    }

    private fun loadPrefill() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isPrefilling = true, error = null) }
            try {
                // Step 1: Check cached VehicleDetailsStepResponse first (iOS pattern)
                var cachedDetailsStep = tokenManager.getVehicleDetailsStepResponse()
                var vehicleId: Int? = cachedDetailsStep?.data?.vehicleId

                // Step 2: If vehicle_id is missing, fetch VehicleDetailsStep and cache it
                if (vehicleId == null) {
                    val detailsStepResponse = repo.getVehicleDetailsStep().getOrNull()
                    if (detailsStepResponse?.success == true && detailsStepResponse.data != null) {
                        // Cache the response
                        tokenManager.saveVehicleDetailsStepResponse(detailsStepResponse.data)
                        cachedDetailsStep = detailsStepResponse.data
                        vehicleId = detailsStepResponse.data.data?.vehicleId
                    }
                }

                // Step 3: Get vehicle_id string for API calls
                val vehicleIdString = vehicleId?.toString() ?: tokenManager.getSelectedVehicleId()
                vehicleIdString?.let { tokenManager.saveSelectedVehicleId(it) }

                // Step 4: Fetch vehicle info and rate settings in parallel (iOS pattern)
                val vehicleInfoResult = vehicleIdString?.let { id ->
                    repo.getVehicleInfo(id).getOrNull()?.data?.data
                }
                
                val rateStep = repo.getVehicleRateSettingsStep().getOrNull()?.data?.data

                // Step 5: Update UI state with vehicle info AND amenities from VehicleInfo API
                _uiState.update { state ->
                    // Get amenities from VehicleInfo (metadata)
                    val amenityMetadata = vehicleInfoResult?.amenities ?: emptyMap()
                    
                    // Convert VehicleAmenity to VehicleAmenityPayload for state
                    val amenityRatesMap = amenityMetadata.mapValues { (_, amenity) ->
                        VehicleAmenityPayload(
                            name = amenity.name ?: "",
                            label = amenity.label ?: amenity.id ?: "",
                            price = amenity.price?.toDoubleOrNull() ?: 0.0
                        )
                    }
                    
                    // Merge with prefill prices if available
                    val prefillAmenities = rateStep?.amenitiesRates ?: emptyMap()
                    val mergedAmenityPrices = mutableMapOf<String, String>()
                    
                    amenityRatesMap.forEach { (id, amenity) ->
                        val label = amenity.label ?: amenity.name
                        // Use prefill price if available, otherwise use metadata price
                        val price = prefillAmenities[label]?.price?.toString() 
                            ?: amenity.price.toString()
                        mergedAmenityPrices[label] = price
                    }
                    
                    state.copy(
                        isLoading = false,
                        isPrefilling = true,
                        vehicleId = vehicleId ?: rateStep?.vehicleId ?: state.vehicleId,
                        vehicleName = vehicleInfoResult?.vehicleType ?: state.vehicleName,
                        vehicleTags = listOfNotNull(
                            vehicleInfoResult?.vehicleColor,
                            vehicleInfoResult?.vehicleYear,
                            vehicleInfoResult?.vehicleMake,
                            vehicleInfoResult?.vehicleModel
                        ),
                        vehicleImageUrl = vehicleInfoResult?.vehicleImage ?: state.vehicleImageUrl,
                        amenityRates = amenityRatesMap, // Use metadata from VehicleInfo
                        amenityPrices = mergedAmenityPrices // Use merged prices
                    )
                }

                // Step 6: Apply prefill if rate settings are available
                if (rateStep != null) {
                    applyPrefill(rateStep, vehicleInfoResult)
                } else {
                    _uiState.update { it.copy(isPrefilling = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isPrefilling = false, error = e.message) }
            }
        }
    }

    private fun applyPrefill(prefill: VehicleRateSettingsStepPrefillData, vehicleInfoResult: VehicleInfoData? = null) {
        _uiState.update { state ->
            // Merge amenity prices: use prefill prices if available, otherwise keep existing (from VehicleInfo)
            val mergedAmenityPrices = state.amenityPrices.toMutableMap()
            val prefillAmenityRates = prefill.amenitiesRates ?: emptyMap()
            prefillAmenityRates.values.forEach { amenity ->
                val label = amenity.label ?: amenity.name
                mergedAmenityPrices[label] = amenity.price.toString()
            }

            state.copy(
                isPrefilling = true,
                selectedCurrency = currencyDisplayFromCode(prefill.currency) ?: state.selectedCurrency,
                distanceUnit = if ((prefill.kmMile ?: "mile").lowercase() == "kilometer") DistanceUnit.KILOMETER else DistanceUnit.MILE,
                perMile = prefill.milageRate?.let { fmt(it) },
                upToMiles = prefill.uptoMiles?.toString(),
                additionalPerMile = prefill.afterMileageRate?.let { fmt(it) },
                minAirportDep = prefill.minimumAirportDepartureRate?.let { fmtMoney(it) } ?: "",
                minAirportArr = prefill.minimumAirportArrivalRate?.let { fmtMoney(it) } ?: "",
                minCityIntercity = prefill.minimumCityRate?.let { fmtMoney(it) } ?: "",
                minCruiseArr = prefill.minimumCruisePortArrivalRate?.let { fmtMoney(it) } ?: "",
                minCruiseDep = prefill.minimumCruisePortDepartureRate?.let { fmtMoney(it) } ?: "",
                hourlyRate = prefill.hourlyRate?.let { fmtMoney(it) } ?: "",
                discountAfter5Hrs = prefill.hourlyRateAfterFiveHours?.let { fmtMoney(it) } ?: "",
                numHoursDayExt = prefill.hoursDayRate?.toString(),
                dayRateExt = prefill.dayRate?.let { fmtMoney(it) } ?: "",
                minCharterHrs = prefill.minimumCharterHours?.toString(),
                sharedRideRate = prefill.perPersonGroupRideRate?.toInt()?.toString(),
                earlyLateSurge = prefill.earlyLateCharges?.toInt()?.toString(),
                holidaySurge = prefill.holidayCharges?.toInt()?.toString(),
                friSatSurge = prefill.fridaySaturdayCharges?.toInt()?.toString(),
                extraStopSame = prefill.inTownExtraStop?.let { fmtMoney(it) } ?: "",
                extraStopDiff = prefill.outsideTownExtraStop?.let { fmtMoney(it) } ?: "",
                seaPortTax = prefill.seaPortTaxPerUs?.let { fmtMoney(it) } ?: "",
                cityCongestionTax = prefill.cityCongestionTaxPerUs?.let { fmtMoney(it) } ?: "",
                cityTax = prefill.cityTax?.let { fmtMoney(it) } ?: "",
                stateTax = prefill.stateTax?.let { fmtMoney(it) } ?: "",
                vat = prefill.vat?.let { fmtMoney(it) } ?: "",
                workmansComp = prefill.workmansComp?.let { fmtMoney(it) } ?: "",
                otherTransportTax = prefill.otherTransportationTax?.let { fmtMoney(it) } ?: "",
                cityTaxIsFlat = (prefill.cityTaxPercentFlat ?: "flat") == "flat",
                stateTaxIsFlat = (prefill.stateTaxPercentFlat ?: "flat") == "flat",
                vatIsFlat = (prefill.vatPercentFlat ?: "flat") == "flat",
                workmansCompIsFlat = (prefill.workmanCompPercentFlat ?: "flat") == "flat",
                otherTransportTaxIsFlat = (prefill.otherTransportationTaxPercentFlat ?: "flat") == "flat",
                // Keep amenityRates from VehicleInfo (already set in loadPrefill)
                // Only update prices with prefill values
                amenityPrices = mergedAmenityPrices
            )
        }
        _uiState.update { it.copy(isPrefilling = false) }
    }

    fun onEvent(event: RateEvent) {
        when (event) {
            is RateEvent.OnNextClick -> {
                _uiState.update { it.copy(currentStep = RateStep.AMENITIES_TAXES) }
            }
            is RateEvent.OnBackClick -> {
                if (_uiState.value.currentStep == RateStep.AMENITIES_TAXES) {
                    _uiState.update { it.copy(currentStep = RateStep.BASE_RATES) }
                }
            }
            is RateEvent.OnSubmitClick -> submit()

            is RateEvent.SetAdjustmentType -> _uiState.update { it.copy(adjustmentType = event.type) }
            is RateEvent.SetAdjustmentValue -> _uiState.update { it.copy(adjustmentValue = event.value) }
            is RateEvent.ToggleDistanceUnit -> _uiState.update {
                it.copy(distanceUnit = if (it.distanceUnit == DistanceUnit.MILE) DistanceUnit.KILOMETER else DistanceUnit.MILE)
            }
            is RateEvent.OpenCurrencySheet -> _uiState.update { it.copy(isCurrencySheetVisible = true) }
            is RateEvent.DismissCurrencySheet -> _uiState.update { it.copy(isCurrencySheetVisible = false) }
            is RateEvent.SelectCurrency -> _uiState.update { it.copy(selectedCurrency = event.currency, isCurrencySheetVisible = false) }

            is RateEvent.SetPerMile -> _uiState.update { it.copy(perMile = event.value) }
            is RateEvent.SetUpToMiles -> _uiState.update { it.copy(upToMiles = event.value) }
            is RateEvent.SetAdditionalPerMile -> _uiState.update { it.copy(additionalPerMile = event.value) }

            is RateEvent.SetField -> handleField(event.key, event.value)
            is RateEvent.SetTaxFlat -> handleTaxToggle(event.key, event.isFlat)
            is RateEvent.SetAmenityPrice -> handleAmenityPrice(event.label, event.price)
        }
    }

    private fun handleAmenityPrice(label: String, price: String) {
        _uiState.update {
            it.copy(amenityPrices = it.amenityPrices.toMutableMap().also { m -> m[label] = price })
        }
    }

    private fun handleTaxToggle(key: String, isFlat: Boolean) {
        _uiState.update {
            when (key) {
                "cityTax" -> it.copy(cityTaxIsFlat = isFlat)
                "stateTax" -> it.copy(stateTaxIsFlat = isFlat)
                "vat" -> it.copy(vatIsFlat = isFlat)
                "workmansComp" -> it.copy(workmansCompIsFlat = isFlat)
                "otherTransportTax" -> it.copy(otherTransportTaxIsFlat = isFlat)
                else -> it
            }
        }
    }

    private fun handleField(key: String, value: String) {
        _uiState.update {
            when (key) {
                "minAirportDep" -> it.copy(minAirportDep = value)
                "minAirportArr" -> it.copy(minAirportArr = value)
                "minCityIntercity" -> it.copy(minCityIntercity = value)
                "minCruiseArr" -> it.copy(minCruiseArr = value)
                "minCruiseDep" -> it.copy(minCruiseDep = value)
                "hourlyRate" -> it.copy(hourlyRate = value)
                "discountAfter5Hrs" -> it.copy(discountAfter5Hrs = value)
                "dayRateExt" -> it.copy(dayRateExt = value)
                "minCharterHrs" -> it.copy(minCharterHrs = value)
                "numHoursDayExt" -> it.copy(numHoursDayExt = value)
                "sharedRideRate" -> it.copy(sharedRideRate = value)
                "earlyLateSurge" -> it.copy(earlyLateSurge = value)
                "holidaySurge" -> it.copy(holidaySurge = value)
                "friSatSurge" -> it.copy(friSatSurge = value)
                "extraStopSame" -> it.copy(extraStopSame = value)
                "extraStopDiff" -> it.copy(extraStopDiff = value)
                "seaPortTax" -> it.copy(seaPortTax = value)
                "cityCongestionTax" -> it.copy(cityCongestionTax = value)
                "cityTax" -> it.copy(cityTax = value)
                "stateTax" -> it.copy(stateTax = value)
                "vat" -> it.copy(vat = value)
                "workmansComp" -> it.copy(workmansComp = value)
                "otherTransportTax" -> it.copy(otherTransportTax = value)
                "airportArrivalTax" -> it.copy(airportArrivalTax = value)
                "airportDepTax" -> it.copy(airportDepTax = value)
                else -> it
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        // basic validation
        if (state.perMile.isNullOrEmpty() || state.upToMiles.isNullOrEmpty() || state.additionalPerMile.isNullOrEmpty() || state.minCityIntercity.isEmpty()) {
            _uiState.update { it.copy(error = "Please fill required fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val req = buildRequest(_uiState.value)
                val result = repo.completeVehicleRateSettings(req)
                result.fold(
                    onSuccess = { resp ->
                        _uiState.update { it.copy(isLoading = false, success = true, nextStep = resp.data?.nextStep) }
                    },
                    onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun buildRequest(state: VehicleRatesState): VehicleRateSettingsRequest {
        val currencyCode = currencyCodeFromDisplay(state.selectedCurrency) ?: "USD"
        val isMile = state.distanceUnit == DistanceUnit.MILE
        val perMileVal = state.perMile?.toDoubleOrNull()
        val additionalVal = state.additionalPerMile?.toDoubleOrNull()
        val uptoVal = state.upToMiles?.toIntOrNull()

        val amenityPayload = state.amenityPrices.mapNotNull { (label, priceStr) ->
            val price = priceStr.toDoubleOrNull()
            val original = state.amenityRates.values.find { it.label == label || it.name == label }
            if (price != null && original != null) {
                original.label?.let { it to VehicleAmenityPayload(name = original.name, label = original.label, price = price) }
            } else null
        }.toMap()

        return VehicleRateSettingsRequest(
            vehicleId = state.vehicleId,
            currency = currencyCode,
            milageRate = perMileVal,
            uptoMiles = if (isMile) uptoVal else null,
            afterMileageRate = if (isMile) additionalVal else null,
            kilometerRate = if (!isMile) perMileVal else null,
            uptoKm = if (!isMile) uptoVal else null,
            afterKilometerRate = if (!isMile) additionalVal else null,
            minimumAirportArrivalRate = moneyToDouble(state.minAirportArr),
            minimumAirportDepartureRate = moneyToDouble(state.minAirportDep),
            minimumCityRate = moneyToDouble(state.minCityIntercity),
            minimumCruisePortDepartureRate = moneyToDouble(state.minCruiseDep),
            minimumCruisePortArrivalRate = moneyToDouble(state.minCruiseArr),
            hourlyRate = moneyToDouble(state.hourlyRate),
            hourlyRateAfterFiveHours = moneyToDouble(state.discountAfter5Hrs),
            hoursDayRate = state.numHoursDayExt?.toIntOrNull(),
            dayRate = moneyToDouble(state.dayRateExt),
            kmMile = if (isMile) "mile" else "kilometer",
            gratuity = 20.0,
            isGratuity = "yes",
            minimumOnDemandRate = 100.0,
            perPersonGroupRideRate = state.sharedRideRate?.toDoubleOrNull(),
            earlyLateCharges = state.earlyLateSurge?.toDoubleOrNull(),
            holidayCharges = state.holidaySurge?.toDoubleOrNull(),
            fridaySaturdayCharges = state.friSatSurge?.toDoubleOrNull(),
            airportArrivalTaxPerUs = moneyToDouble(state.airportArrivalTax),
            airportDepartureTaxPerUs = moneyToDouble(state.airportDepTax),
            seaPortTaxPerUs = moneyToDouble(state.seaPortTax),
            cityCongestionTaxPerUs = moneyToDouble(state.cityCongestionTax),
            cityTax = moneyToDouble(state.cityTax),
            cityTaxPercentFlat = if (state.cityTaxIsFlat) "flat" else "percent",
            stateTax = moneyToDouble(state.stateTax),
            stateTaxPercentFlat = if (state.stateTaxIsFlat) "flat" else "percent",
            vat = moneyToDouble(state.vat),
            vatPercentFlat = if (state.vatIsFlat) "flat" else "percent",
            workmansComp = moneyToDouble(state.workmansComp),
            workmanCompPercentFlat = if (state.workmansCompIsFlat) "flat" else "percent",
            otherTransportationTax = moneyToDouble(state.otherTransportTax),
            otherTransportationTaxPercentFlat = if (state.otherTransportTaxIsFlat) "flat" else "percent",
            rateRange = state.adjustmentValue.toDouble(),
            rateRangePercentFlat = if (state.adjustmentType == AdjustmentType.FLAT) "flat" else "percent",
            amenitiesRates = if (amenityPayload.isNotEmpty()) amenityPayload else null,
            inTownExtraStop = moneyToDouble(state.extraStopSame),
            outsideTownExtraStop = moneyToDouble(state.extraStopDiff),
            minimumCharterHours = state.minCharterHrs?.toIntOrNull()
        )
    }

    private fun moneyToDouble(raw: String): Double? {
        val cleaned = raw.replace("[^0-9.]", "").trim()
        return cleaned.toDoubleOrNull()
    }

    private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.2f", value)
    private fun fmtMoney(value: Double): String = fmt(value)

    private fun currencyDisplayFromCode(code: String?): String? {
        return when (code?.uppercase()) {
            "USD" -> "United States - $"
            "CAD" -> "Canada - CA$"
            "GBP" -> "United Kingdom - £"
            "EUR" -> "Euro - €"
            else -> null
        }
    }

    private fun currencyCodeFromDisplay(display: String): String? {
        return when {
            display.contains("United States") -> "USD"
            display.contains("Canada") -> "CAD"
            display.contains("United Kingdom") -> "GBP"
            display.contains("Euro") -> "EUR"
            else -> null
        }
    }
}