package com.limo1800driver.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.data.model.dashboard.AdminReservationRateArray
import com.limo1800driver.app.data.model.dashboard.AdminReservationRateItem
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.utils.RateCalculator

/**
 * Shared rate editor + calculator used by Edit/Finalize flows.
 * Mirrors the iOS driver DynamicRatesView behavior.
 */
@Composable
fun CommonRateCalculatorComponent(
    rateArray: AdminReservationRateArray,
    dynamicRates: SnapshotStateMap<String, String>,
    taxIsPercent: SnapshotStateMap<String, Boolean>,
    serviceType: String?,
    numberOfHours: MutableState<String>,
    numberOfVehicles: MutableState<String>,
    accountType: String?,
    createdBy: Int?,
    reservationType: String?,
    currencySymbol: String? = "$",
    isEditable: Boolean = true,
    showSummary: Boolean = true
) {
    // Initialize missing values from API baserates and tax toggle default.
    LaunchedEffect(rateArray) {
        fun initMap(map: Map<String, AdminReservationRateItem>, isTax: Boolean) {
            map.forEach { (key, item) ->
                if (!dynamicRates.containsKey(key)) {
                    dynamicRates[key] = String.format("%.2f", item.baserate ?: 0.0)
                }
                if (isTax && !taxIsPercent.containsKey(key)) {
                    taxIsPercent[key] = (item.type == "percent")
                }
            }
        }
        initMap(rateArray.allInclusiveRates, isTax = false)
        initMap(rateArray.taxes, isTax = true)
        initMap(rateArray.amenities, isTax = false)
        initMap(rateArray.misc, isTax = false)
    }

    val hoursInt = numberOfHours.value.toIntOrNull() ?: 0
    val vehiclesInt = numberOfVehicles.value.toIntOrNull() ?: 1
    val totals = RateCalculator.calculate(
        rateArray = rateArray,
        dynamicRates = dynamicRates,
        taxIsPercent = taxIsPercent,
        serviceType = serviceType,
        numberOfHours = hoursInt,
        numberOfVehicles = vehiclesInt,
        accountType = accountType,
        createdBy = createdBy,
        reservationType = reservationType
    )

    // iOS "orange amount" for tax percent rows uses base rates sum (all-inclusive, with Charter Base_Rate × hours).
    val baseRatesSumForPercentTaxes by remember(rateArray, serviceType) {
        derivedStateOf {
            calculateBaseRatesSum(
                rateArray = rateArray,
                dynamicRates = dynamicRates,
                serviceType = serviceType,
                numberOfHours = numberOfHours.value.toIntOrNull() ?: 0
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Rates",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = LimoOrange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. Base Rates (OPEN by default) ---
        RateSectionCard(
            title = "VEHICLE BASE RATES",
            items = orderedKeysFromApi(
                apiOrder = rateArray.allInclusiveRatesOrder,
                keys = rateArray.allInclusiveRates.keys,
                expectedFallback = expectedBaseOrder
            ),
            itemMap = rateArray.allInclusiveRates,
            dynamicRates = dynamicRates,
            taxIsPercent = taxIsPercent,
            isTaxSection = false,
            isEditable = isEditable,
            serviceType = serviceType,
            numberOfHours = numberOfHours,
            baseRatesSumForPercentTaxes = baseRatesSumForPercentTaxes,
            currencySymbol = currencySymbol,
            initiallyExpanded = true // <--- Changed here
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. Taxes (Closed) ---
        RateSectionCard(
            title = "TOLLS/TAXES (Airport, Sea, City, Others)",
            items = orderedKeysFromApi(
                apiOrder = rateArray.taxesOrder,
                keys = rateArray.taxes.keys,
                expectedFallback = expectedTaxesOrder
            ),
            itemMap = rateArray.taxes,
            dynamicRates = dynamicRates,
            taxIsPercent = taxIsPercent,
            isTaxSection = true,
            isEditable = isEditable,
            serviceType = serviceType,
            numberOfHours = numberOfHours,
            baseRatesSumForPercentTaxes = baseRatesSumForPercentTaxes,
            currencySymbol = currencySymbol,
            initiallyExpanded = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. Amenities (Closed) ---
        RateSectionCard(
            title = "EXTRA CHARGE AMENITIES",
            items = orderedKeysFromApi(
                apiOrder = rateArray.amenitiesOrder,
                keys = rateArray.amenities.keys,
                expectedFallback = expectedAmenitiesOrder
            ),
            itemMap = rateArray.amenities,
            dynamicRates = dynamicRates,
            taxIsPercent = taxIsPercent,
            isTaxSection = false,
            isEditable = isEditable,
            serviceType = serviceType,
            numberOfHours = numberOfHours,
            baseRatesSumForPercentTaxes = baseRatesSumForPercentTaxes,
            currencySymbol = currencySymbol,
            initiallyExpanded = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. Misc (Closed) ---
        RateSectionCard(
            title = "ADDITIONAL MISC. CHARGES",
            items = orderedKeysFromApi(
                apiOrder = rateArray.miscOrder,
                keys = rateArray.misc.keys,
                expectedFallback = expectedMiscOrder
            ),
            itemMap = rateArray.misc,
            dynamicRates = dynamicRates,
            taxIsPercent = taxIsPercent,
            isTaxSection = false,
            isEditable = isEditable,
            serviceType = serviceType,
            numberOfHours = numberOfHours,
            baseRatesSumForPercentTaxes = baseRatesSumForPercentTaxes,
            currencySymbol = currencySymbol,
            initiallyExpanded = false
        )

        if (showSummary) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                SummaryRowMoney(label = "Sub Total", amount = totals.subTotal, currencySymbol = currencySymbol)
                SummaryRowText(label = "Number of Vehicles", value = vehiclesInt.toString())
                SummaryRowMoney(label = "Booking Total", amount = totals.grandTotal, currencySymbol = currencySymbol, isTotal = true)
                SummaryRowMoney(label = "Affiliate Total", amount = totals.affiliatePayout, currencySymbol = currencySymbol)
            }
        }
    }
}

@Composable
private fun RateSectionCard(
    title: String,
    items: List<String>,
    itemMap: Map<String, AdminReservationRateItem>,
    dynamicRates: SnapshotStateMap<String, String>,
    taxIsPercent: SnapshotStateMap<String, Boolean>,
    isTaxSection: Boolean,
    isEditable: Boolean,
    serviceType: String?,
    numberOfHours: MutableState<String>,
    baseRatesSumForPercentTaxes: Double,
    currencySymbol: String?,
    initiallyExpanded: Boolean
) {
    val expanded = remember { mutableStateOf(initiallyExpanded) }
    // Add rotation animation for the arrow
    val rotationState by animateFloatAsState(targetValue = if (expanded.value) 180f else 0f, label = "arrowRotation")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = !expanded.value }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.rotate(rotationState) // Animated rotation
                )
            }

            if (expanded.value) {
                // Subtle divider between header and content
                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items.forEach { key ->
                        val item = itemMap[key] ?: return@forEach

                        val isCharterBaseRate = isCharterTour(serviceType) && key == "Base_Rate" && !isTaxSection
                        if (isCharterBaseRate) {
                            val base = (dynamicRates[key] ?: String.format("%.2f", item.baserate ?: 0.0))
                                .toDoubleOrNull() ?: (item.baserate ?: 0.0)
                            val hours = numberOfHours.value.toIntOrNull() ?: 0
                            DualFieldRow(
                                label = "BASE RATE & NUMBER OF HOURS",
                                baseValue = dynamicRates[key].orEmpty(),
                                onBaseChange = { if (isEditable) dynamicRates[key] = it },
                                hoursValue = numberOfHours.value,
                                onHoursChange = { if (isEditable) numberOfHours.value = it },
                                isEditable = isEditable,
                                calculatedValue = formatMoney(base * hours.toDouble(), currencySymbol)
                            )
                        } else {
                            val rawValue = dynamicRates[key].orEmpty()
                            val valueDouble = rawValue.toDoubleOrNull() ?: (item.baserate ?: 0.0)
                            val isPercent = if (isTaxSection) (taxIsPercent[key] ?: (item.type == "percent")) else null
                            val calculated = when {
                                isTaxSection && isPercent == true ->
                                    formatMoney((baseRatesSumForPercentTaxes * valueDouble) / 100.0, currencySymbol)
                                else -> formatMoney(valueDouble, currencySymbol)
                            }

                            RateItemRow(
                                label = item.rateLabel,
                                value = dynamicRates[key].orEmpty(),
                                onValueChange = { if (isEditable) dynamicRates[key] = it },
                                isEditable = isEditable,
                                isTaxItem = isTaxSection,
                                isPercent = isPercent,
                                onPercentToggle = if (isTaxSection) {
                                    { newValue -> taxIsPercent[key] = newValue }
                                } else null,
                                calculatedValue = calculated
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RateItemRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    isTaxItem: Boolean,
    isPercent: Boolean?,
    onPercentToggle: ((Boolean) -> Unit)?,
    calculatedValue: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallMoneyTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = isEditable,
                modifier = Modifier.width(90.dp)
            )

            if (isTaxItem && isPercent != null && onPercentToggle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                RateModeToggle(
                    isPercent = isPercent,
                    enabled = isEditable,
                    onToggle = { onPercentToggle(!isPercent) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = calculatedValue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LimoOrange
            )
        }
    }
}

@Composable
private fun DualFieldRow(
    label: String,
    baseValue: String,
    onBaseChange: (String) -> Unit,
    hoursValue: String,
    onHoursChange: (String) -> Unit,
    isEditable: Boolean,
    calculatedValue: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = "Rate", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                SmallMoneyTextField(
                    value = baseValue,
                    onValueChange = onBaseChange,
                    enabled = isEditable,
                    modifier = Modifier.width(80.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.Start) {
                Text(text = "Hours", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                SmallMoneyTextField(
                    value = hoursValue,
                    onValueChange = onHoursChange,
                    enabled = isEditable,
                    modifier = Modifier.width(80.dp),
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = calculatedValue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LimoOrange
            )
        }
    }
}

@Composable
private fun SummaryRowMoney(
    label: String,
    amount: Double,
    currencySymbol: String?,
    isTotal: Boolean = false
) {
    val isAffiliate = label.startsWith("Affiliate", ignoreCase = true)

    val labelColor = when {
        isTotal -> Color.Black
        isAffiliate -> LimoGreen // Green
        else -> Color.Gray
    }

    val amountColor = when {
        isTotal -> LimoOrange
        isAffiliate -> LimoGreen // Green
        else -> Color.Black
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = label,
            fontSize = if (isAffiliate) 16.sp else 14.sp,
            fontWeight = if (isAffiliate) FontWeight.SemiBold else FontWeight.Medium,
            color = labelColor
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currencySymbol ?: "$",
                fontSize = if (isAffiliate) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAffiliate) LimoGreen else LimoOrange
            )
            Text(
                text = " " + String.format("%.2f", amount),
                fontSize = if (isAffiliate) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}


@Composable
private fun SummaryRowText(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
}

// ... [Helper functions orderedKeys, formatMoney, etc. remain exactly the same as original] ...

private fun orderedKeys(keys: Set<String>, expected: List<String>): List<String> {
    val existing = keys.toSet()
    val ordered = expected.filter { existing.contains(it) }
    val remaining = (existing - ordered.toSet()).toList()
    return ordered + remaining
}

private fun orderedKeysFromApi(apiOrder: List<String>, keys: Set<String>, expectedFallback: List<String>): List<String> {
    if (apiOrder.isNotEmpty()) {
        val ordered = apiOrder.filter { keys.contains(it) }
        val remaining = (keys - ordered.toSet()).toList()
        return ordered + remaining
    }
    return orderedKeys(keys, expectedFallback)
}

private fun isCharterTour(serviceType: String?): Boolean {
    val s = serviceType?.lowercase() ?: return false
    return s.contains("charter") || s.contains("tour") || s.contains("charter/tour")
}

private val expectedBaseOrder = listOf("Base_Rate", "Stops", "Wait", "ELH_Charges")
private val expectedTaxesOrder = listOf(
    "Airport_Arrival_Tax_Per_US",
    "Airport_Departure_Tax_Per_US",
    "Sea_Port_Tax_Per_US",
    "City_Congestion_Tax_Per_US",
    "City_Tax",
    "State_Tax",
    "VAT_Tax",
    "Workman_Comp_Tax",
    "Other_Transportation_Tax",
    "Tolls"
)
private val expectedAmenitiesOrder = listOf(
    "Baby_Seat",
    "Booster_Seat",
    "Baggage_Meet_(Dom)",
    "Baggage_Meet_(Int)",
    "Bike_Rack",
    "Lei_Greeting_–_Hawaii",
    "Security_/_Guard",
    "Per_Diem",
    "Tour_Guide",
    "Luggage_Trailer",
    "Wedding_Package_(Decorations_and_Champaign)",
    "Red_Carpet",
    "Skis",
    "Golf_Bags"
)
private val expectedMiscOrder = listOf("Extra_Gratuity", "Parking", "Bar_Stock", "Misc_Charges")

private fun formatMoney(amount: Double, currencySymbol: String?): String {
    val sym = currencySymbol ?: "$"
    return "$sym " + String.format("%.2f", amount)
}

private fun calculateBaseRatesSum(
    rateArray: AdminReservationRateArray,
    dynamicRates: Map<String, String>,
    serviceType: String?,
    numberOfHours: Int
): Double {
    var sum = 0.0
    for ((key, item) in rateArray.allInclusiveRates) {
        val current = dynamicRates[key] ?: String.format("%.2f", item.baserate ?: 0.0)
        val base = current.toDoubleOrNull() ?: (item.baserate ?: 0.0)
        if (isCharterTour(serviceType) && key == "Base_Rate") {
            sum += base * numberOfHours.toDouble()
        } else {
            sum += base
        }
    }
    return sum
}

@Composable
private fun SmallMoneyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LimoOrange,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedContainerColor = Color(0xFFF9FAFB), // Slightly cleaner gray
            unfocusedContainerColor = Color(0xFFF9FAFB),
            disabledContainerColor = Color(0xFFF2F2F2),
            disabledBorderColor = Color.Gray.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(44.dp)
    )
}

@Composable
private fun RateModeToggle(
    isPercent: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    // Kept original toggle but made it slightly cleaner
    val capsuleColor = if (isPercent) LimoOrange.copy(alpha = 0.15f) else Color(0xFFF3F4F6)
    val knobColor = if (isPercent) LimoOrange else Color.Gray
    val alignment = if (isPercent) Alignment.CenterEnd else Alignment.CenterStart
    val textColor = if (isPercent) LimoOrange else Color.Gray

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = enabled) { onToggle() }
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(26.dp)
                .background(capsuleColor, RoundedCornerShape(50))
                .padding(3.dp),
            contentAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, RoundedCornerShape(50))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(knobColor, RoundedCornerShape(50))
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isPercent) "%" else "$",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}