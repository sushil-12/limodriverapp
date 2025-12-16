package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.limo1800driver.app.data.model.dashboard.AdminBookingPreviewData
import com.limo1800driver.app.data.model.dashboard.AdminReservationRateArray
import com.limo1800driver.app.data.model.dashboard.AdminReservationRatesData
import com.limo1800driver.app.data.model.dashboard.CreditCardData
import com.limo1800driver.app.data.model.dashboard.CreditCardDetail
import com.limo1800driver.app.data.model.dashboard.CreditCardPaymentRequest
import com.limo1800driver.app.data.model.dashboard.FinalizeRateArray
import com.limo1800driver.app.data.model.dashboard.FinalizeRateItem
import com.limo1800driver.app.data.model.dashboard.FinalizeRatesRequest
import com.limo1800driver.app.data.model.dashboard.FinalizeShareArray
import com.limo1800driver.app.data.model.dashboard.PaymentProcessingRequest
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import com.limo1800driver.app.ui.utils.RateCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class FinalizeRatesUiState(
    val isLoading: Boolean = false,
    val preview: AdminBookingPreviewData? = null,
    val rates: AdminReservationRatesData? = null,
    val error: String? = null,
    val isFinalizing: Boolean = false,
    val finalizedSuccessfully: Boolean = false,
    val isProcessingCash: Boolean = false,
    val isProcessingCharge: Boolean = false,
    val creditCards: List<CreditCardData> = emptyList(),
    val selectedCard: CreditCardData? = null,
    val successMessage: String? = null
)

@HiltViewModel
class FinalizeRatesViewModel @Inject constructor(
    private val repository: DriverDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinalizeRatesUiState())
    val uiState: StateFlow<FinalizeRatesUiState> = _uiState.asStateFlow()

    fun load(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            val previewResult = repository.getBookingPreview(bookingId)
            val ratesResult = repository.getReservationRates(bookingId)

            var preview: AdminBookingPreviewData? = null
            var rates: AdminReservationRatesData? = null
            var error: String? = null

            previewResult
                .onSuccess { resp ->
                    if (resp.success) preview = resp.data else error = resp.message
                }
                .onFailure { e -> error = e.message }

            ratesResult
                .onSuccess { resp ->
                    if (resp.success) rates = resp.data else error = error ?: resp.message
                }
                .onFailure { e -> error = error ?: e.message }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                preview = preview,
                rates = rates,
                error = error
            )
        }
    }

    fun accept(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFinalizing = true, error = null, successMessage = null)
            repository.acceptBooking(bookingId)
                .onSuccess { resp ->
                    _uiState.value = _uiState.value.copy(isFinalizing = false, successMessage = resp.message)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isFinalizing = false, error = e.message)
                }
        }
    }

    fun reject(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFinalizing = true, error = null, successMessage = null)
            repository.rejectBooking(bookingId)
                .onSuccess { resp ->
                    _uiState.value = _uiState.value.copy(isFinalizing = false, successMessage = resp.message)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isFinalizing = false, error = e.message)
                }
        }
    }

    fun finalizeRates(
        bookingId: Int,
        dynamicRates: Map<String, String>,
        taxIsPercent: Map<String, Boolean>,
        numberOfHours: Int,
        numberOfVehicles: Int
    ) {
        val preview = _uiState.value.preview
        val ratesData = _uiState.value.rates
        if (preview == null || ratesData == null) {
            _uiState.value = _uiState.value.copy(error = "Missing booking preview or rates data")
            return
        }

        val totals = RateCalculator.calculate(
            rateArray = ratesData.rateArray,
            dynamicRates = dynamicRates,
            taxIsPercent = taxIsPercent,
            serviceType = preview.serviceType,
            numberOfHours = numberOfHours,
            numberOfVehicles = numberOfVehicles,
            accountType = preview.accountType,
            createdBy = preview.createdBy,
            reservationType = preview.reservationType
        )

        val request = FinalizeRatesRequest(
            reservationId = bookingId.toString(),
            rateArray = buildFinalizeRateArray(ratesData.rateArray, dynamicRates, taxIsPercent),
            subTotal = totals.subTotal,
            grandTotal = totals.grandTotal,
            numberOfHours = numberOfHours,
            shareArray = FinalizeShareArray(
                baseRate = totals.shareArray.baseRate,
                grandTotal = totals.shareArray.grandTotal,
                stripeFee = totals.shareArray.stripeFee,
                adminShare = totals.shareArray.adminShare,
                deductedAdminShare = totals.shareArray.deductedAdminShare,
                affiliateShare = totals.shareArray.affiliateShare,
                travelAgentShare = totals.shareArray.travelAgentShare,
                farmoutShare = totals.shareArray.farmoutShare
            ),
            waitingTimeInMins = 0
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFinalizing = true, error = null, successMessage = null)
            repository.finalizeRateEdit(request)
                .onSuccess { resp ->
                    _uiState.value = _uiState.value.copy(
                        isFinalizing = false,
                        finalizedSuccessfully = resp.success,
                        successMessage = resp.message
                    )
                }
                .onFailure { e ->
                    Timber.e(e, "Finalize rates failed")
                    _uiState.value = _uiState.value.copy(isFinalizing = false, error = e.message)
                }
        }
    }

    fun processCashPayment(reservationId: String, grandTotal: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingCash = true, error = null)
            repository.processPayment(
                PaymentProcessingRequest(
                    reservationId = reservationId,
                    grandTotal = grandTotal,
                    paymentMethod = "cash"
                )
            ).onSuccess { resp ->
                _uiState.value = _uiState.value.copy(isProcessingCash = false, successMessage = resp.message)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isProcessingCash = false, error = e.message)
            }
        }
    }

    fun loadCreditCards(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingCharge = true, error = null)
            repository.getCreditCardDetails(bookingId)
                .onSuccess { resp ->
                    if (!resp.success || resp.data == null) {
                        _uiState.value = _uiState.value.copy(isProcessingCharge = false, error = resp.message)
                        return@onSuccess
                    }
                    val cards = extractCards(resp.data)
                    val primary = cards.firstOrNull { (it.ccPriority ?: "").equals("primary", ignoreCase = true) }
                        ?: cards.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        isProcessingCharge = false,
                        creditCards = cards,
                        selectedCard = primary
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isProcessingCharge = false, error = e.message)
                }
        }
    }

    fun processChargePayment(reservationId: String, grandTotal: Double) {
        val cardId = _uiState.value.selectedCard?.ID ?: _uiState.value.selectedCard?.id
        if (cardId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "No credit card selected")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingCharge = true, error = null)
            repository.processCreditCardPayment(
                CreditCardPaymentRequest(
                    isExistingCard = true,
                    paymentMethod = "credit_card",
                    creditCardsDetail = CreditCardDetail(cardId = cardId),
                    reservationId = reservationId,
                    grandTotal = grandTotal
                )
            ).onSuccess { resp ->
                _uiState.value = _uiState.value.copy(isProcessingCharge = false, successMessage = resp.message)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isProcessingCharge = false, error = e.message)
            }
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    private fun buildFinalizeRateArray(
        rateArray: AdminReservationRateArray,
        dynamicRates: Map<String, String>,
        taxIsPercent: Map<String, Boolean>
    ): FinalizeRateArray {
        fun convert(map: Map<String, com.limo1800driver.app.data.model.dashboard.AdminReservationRateItem>, isTax: Boolean): Map<String, FinalizeRateItem> {
            return map.mapValues { (key, item) ->
                val value = dynamicRates[key]?.toDoubleOrNull() ?: (item.baserate ?: 0.0)
                val type = if (isTax) {
                    if (taxIsPercent[key] == true) "percent" else "flat"
                } else item.type
                FinalizeRateItem(
                    rateLabel = item.rateLabel,
                    baserate = value,
                    multiple = item.multiple,
                    percentage = item.percentage,
                    amount = item.amount,
                    type = type
                )
            }
        }

        return FinalizeRateArray(
            allInclusiveRates = convert(rateArray.allInclusiveRates, isTax = false),
            taxes = convert(rateArray.taxes, isTax = true),
            amenities = convert(rateArray.amenities, isTax = false),
            misc = convert(rateArray.misc, isTax = false)
        )
    }

    private fun extractCards(element: JsonElement): List<CreditCardData> {
        // Response can be an array or a nested object; search for arrays of objects with last4/brand keys.
        val results = mutableListOf<CreditCardData>()

        fun visit(e: JsonElement) {
            when {
                e.isJsonArray -> {
                    val arr = e.asJsonArray
                    if (looksLikeCardsArray(arr)) {
                        arr.forEach { item ->
                            runCatching {
                                val obj = item.asJsonObject
                                results.add(
                                    CreditCardData(
                                        id = obj.getAsStringOrNull("id"),
                                        ID = obj.getAsStringOrNull("ID") ?: obj.getAsStringOrNull("Id") ?: obj.getAsStringOrNull("cardID"),
                                        name = obj.getAsStringOrNull("name"),
                                        brand = obj.getAsStringOrNull("brand"),
                                        expMonth = obj.getAsIntOrNull("exp_month"),
                                        expYear = obj.getAsIntOrNull("exp_year"),
                                        last4 = obj.getAsStringOrNull("last4"),
                                        cardType = obj.getAsStringOrNull("card_type") ?: obj.getAsStringOrNull("cardType"),
                                        ccPriority = obj.getAsStringOrNull("cc_prority") ?: obj.getAsStringOrNull("cc_priority") ?: obj.getAsStringOrNull("ccPriority")
                                    )
                                )
                            }
                        }
                    } else {
                        arr.forEach { visit(it) }
                    }
                }
                e.isJsonObject -> {
                    val obj = e.asJsonObject
                    obj.entrySet().forEach { visit(it.value) }
                }
            }
        }

        visit(element)
        return results.distinctBy { it.ID ?: it.id ?: "${it.brand}:${it.last4}" }
    }

    private fun looksLikeCardsArray(arr: JsonArray): Boolean {
        val first = arr.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        return first.has("last4") || first.has("brand") || first.has("exp_month") || first.has("cc_prority") || first.has("cc_priority")
    }

    private fun JsonObject.getAsStringOrNull(key: String): String? =
        if (has(key) && get(key).isJsonPrimitive) get(key).asString else null

    private fun JsonObject.getAsIntOrNull(key: String): Int? =
        if (has(key) && get(key).isJsonPrimitive) runCatching { get(key).asInt }.getOrNull() else null
}


