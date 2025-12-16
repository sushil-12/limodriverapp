package com.limo1800driver.app.ui.utils

import com.limo1800driver.app.data.model.dashboard.AdminReservationRateArray
import kotlin.math.abs

data class ShareArray(
    val baseRate: Double,
    val grandTotal: Double,
    val stripeFee: Double,
    val adminShare: Double,
    val deductedAdminShare: Double,
    val affiliateShare: Double,
    val travelAgentShare: Double? = null,
    val farmoutShare: Double? = null
)

data class TotalsResult(
    val subTotal: Double,
    val grandTotal: Double,
    val adminShare: Double,
    val affiliatePayout: Double,
    val shareArray: ShareArray
)

object RateCalculator {

    fun calculate(
        rateArray: AdminReservationRateArray,
        dynamicRates: Map<String, String>,
        taxIsPercent: Map<String, Boolean>,
        serviceType: String?,
        numberOfHours: Int,
        numberOfVehicles: Int,
        accountType: String?,
        createdBy: Int?,
        reservationType: String?
    ): TotalsResult {
        val baseRatesSum = calculateBaseRatesSum(rateArray, dynamicRates, serviceType, numberOfHours)

        val totalBaserates = calculateTotalBaserates(
            rateArray = rateArray,
            dynamicRates = dynamicRates,
            taxIsPercent = taxIsPercent,
            baseRatesSum = baseRatesSum,
            serviceType = serviceType,
            numberOfHours = numberOfHours
        )

        val specialCase = isTravelPlannerSpecialCase(accountType, createdBy)
        val farmoutCase = isFarmoutCase(reservationType)
        val adminSharePct = if (specialCase || farmoutCase) 0.15 else 0.25

        val adminShareBaserates = baseRatesSum
        val adminAmount = adminShareBaserates * adminSharePct

        val travelAgentShare = if (specialCase) adminShareBaserates * 0.10 else 0.0
        val farmoutShare = if (farmoutCase) adminShareBaserates * 0.10 else 0.0

        val subTotal = totalBaserates + adminAmount + travelAgentShare + farmoutShare
        val grandTotal = subTotal * numberOfVehicles.toDouble()

        // iOS driver uses Extra_Gratuity amount from API (not the edited field) and takes 25%.
        val extraGratuityAmount = rateArray.misc["Extra_Gratuity"]?.amount ?: 0.0
        val extraGratuityShare = extraGratuityAmount * 0.25

        val affiliateAmount = when {
            farmoutCase -> grandTotal - (adminAmount + extraGratuityShare) - farmoutShare
            specialCase -> grandTotal - (adminAmount + extraGratuityShare) - travelAgentShare
            else -> grandTotal - (adminAmount + extraGratuityShare)
        }

        // iOS driver uses 5.1% (0.051) for stripe fee in FinalizeRatesView.calculateShareArray
        val stripeFee = grandTotal * 0.051
        // iOS driver uses adminAmount * 0.75 as deducted admin share
        val deductedAdminShare = adminAmount * 0.75

        val shareArray = ShareArray(
            baseRate = adminShareBaserates,
            grandTotal = grandTotal,
            stripeFee = stripeFee,
            adminShare = adminAmount,
            deductedAdminShare = deductedAdminShare,
            affiliateShare = affiliateAmount,
            travelAgentShare = if (specialCase) travelAgentShare else null,
            farmoutShare = if (farmoutCase) farmoutShare else null
        )

        return TotalsResult(
            subTotal = subTotal,
            grandTotal = grandTotal,
            adminShare = adminAmount,
            affiliatePayout = affiliateAmount,
            shareArray = shareArray
        )
    }

    fun areTotalsDifferent(a: Double, b: Double): Boolean = abs(a - b) > 0.01

    private fun calculateBaseRatesSum(
        rateArray: AdminReservationRateArray,
        dynamicRates: Map<String, String>,
        serviceType: String?,
        numberOfHours: Int
    ): Double {
        var sum = 0.0
        for ((key, item) in rateArray.allInclusiveRates) {
            val current = dynamicRates[key] ?: item.baserate?.toString().orEmpty()
            val base = current.toDoubleOrNull() ?: (item.baserate ?: 0.0)
            if (isCharterTour(serviceType) && key == "Base_Rate") {
                sum += base * numberOfHours.toDouble()
            } else {
                sum += base
            }
        }
        return sum
    }

    private fun calculateTotalBaserates(
        rateArray: AdminReservationRateArray,
        dynamicRates: Map<String, String>,
        taxIsPercent: Map<String, Boolean>,
        baseRatesSum: Double,
        serviceType: String?,
        numberOfHours: Int
    ): Double {
        var total = 0.0

        // Base rates (all inclusive)
        for ((key, item) in rateArray.allInclusiveRates) {
            val current = dynamicRates[key] ?: item.baserate?.toString().orEmpty()
            val base = current.toDoubleOrNull() ?: (item.baserate ?: 0.0)
            if (isCharterTour(serviceType) && key == "Base_Rate") {
                total += base * numberOfHours.toDouble()
            } else {
                total += base
            }
        }

        // Taxes: percent or flat
        for ((key, item) in rateArray.taxes) {
            val current = dynamicRates[key] ?: item.baserate?.toString().orEmpty()
            val value = current.toDoubleOrNull() ?: (item.baserate ?: 0.0)
            val isPercent = taxIsPercent[key] ?: (item.type == "percent")
            total += if (isPercent) (baseRatesSum * value) / 100.0 else value
        }

        // Amenities
        for ((key, item) in rateArray.amenities) {
            val current = dynamicRates[key] ?: item.baserate?.toString().orEmpty()
            val value = current.toDoubleOrNull() ?: (item.baserate ?: 0.0)
            total += value
        }

        // Misc
        for ((key, item) in rateArray.misc) {
            val current = dynamicRates[key] ?: item.baserate?.toString().orEmpty()
            val value = current.toDoubleOrNull() ?: (item.baserate ?: 0.0)
            total += value
        }

        return total
    }

    private fun isCharterTour(serviceType: String?): Boolean {
        val s = serviceType?.lowercase() ?: return false
        return s.contains("charter") || s.contains("tour") || s.contains("charter/tour")
    }

    private fun isTravelPlannerSpecialCase(accountType: String?, createdBy: Int?): Boolean {
        return (accountType ?: "").lowercase() == "travel_planner" && (createdBy ?: 1) != 1
    }

    private fun isFarmoutCase(reservationType: String?): Boolean {
        return (reservationType ?: "").lowercase() == "farmout"
    }
}


