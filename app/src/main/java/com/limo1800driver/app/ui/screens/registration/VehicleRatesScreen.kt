package com.limo1800driver.app.ui.screens.registration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.limo1800driver.app.ui.util.noRippleClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.theme.AppTextStyles
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleRatesScreen(
    onNext: (String?) -> Unit,
    onBack: () -> Unit,
    viewModel: VehicleRatesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scroll = rememberScrollState()

    // Error states
    var currencyError by remember { mutableStateOf<String?>(null) }
    var perMileError by remember { mutableStateOf<String?>(null) }
    var upToMilesError by remember { mutableStateOf<String?>(null) }
    var additionalPerMileError by remember { mutableStateOf<String?>(null) }
    var minCityIntercityError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Helper: Validation
    fun validateField(fieldName: String, value: String): String? {
        return when (fieldName) {
            "currency" -> if (value.isBlank()) "Required" else null
            "perMile" -> if (value.isBlank()) "Required" else null
            "upToMiles" -> if (value.isBlank()) "Required" else null
            "additionalPerMile" -> if (value.isBlank()) "Required" else null
            "minCityIntercity" -> if (value.isBlank()) "Required" else null
            else -> null
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            apiError = error
            currencyError = null; perMileError = null; upToMilesError = null
            additionalPerMileError = null; minCityIntercityError = null
        }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            onNext(state.nextStep)
            viewModel.consumeSuccess()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            RegistrationTopBar(
                onBack = {
                    if (state.currentStep == RateStep.AMENITIES_TAXES) {
                        viewModel.onEvent(RateEvent.OnBackClick)
                    } else {
                        onBack()
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            currencyError = null; perMileError = null; upToMilesError = null
                            additionalPerMileError = null; minCityIntercityError = null
                            apiError = null

                            var hasErrors = false

                            if (state.currentStep == RateStep.BASE_RATES) {
                                if (state.selectedCurrency.isNullOrBlank()) { currencyError = "Required"; hasErrors = true }
                                if (state.perMile.isNullOrBlank()) { perMileError = "Required"; hasErrors = true }
                                if (state.upToMiles.isNullOrBlank()) { upToMilesError = "Required"; hasErrors = true }
                                if (state.additionalPerMile.isNullOrBlank()) { additionalPerMileError = "Required"; hasErrors = true }
                                if (state.minCityIntercity.isBlank()) { minCityIntercityError = "Required"; hasErrors = true }
                            }

                            if (!hasErrors) {
                                if (state.currentStep == RateStep.BASE_RATES) {
                                    viewModel.onEvent(RateEvent.OnNextClick)
                                } else {
                                    viewModel.onEvent(RateEvent.OnSubmitClick)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE89148),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !state.isLoading,
                    ) {
                        if (state.isLoading) {
                            ShimmerCircle(size = 24.dp)
                        } else {
                            Text(
                                text = if (state.currentStep == RateStep.BASE_RATES) "Next" else "Submit",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            HeaderCard(state)
            Spacer(modifier = Modifier.height(16.dp))

            VehicleInfoCard(state)
            Spacer(modifier = Modifier.height(24.dp))

            // API Error
            apiError?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F2)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, "Error", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = error, style = TextStyle(fontSize = 14.sp, color = Color(0xFFDC2626)))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.currentStep == RateStep.BASE_RATES) {
                BaseRatesStep(
                    state = state,
                    vm = viewModel,
                    currencyError = currencyError,
                    perMileError = perMileError,
                    upToMilesError = upToMilesError,
                    additionalPerMileError = additionalPerMileError,
                    minCityIntercityError = minCityIntercityError,
                    onCurrencyChange = {
                        viewModel.onEvent(RateEvent.SelectCurrency(it))
                        currencyError = validateField("currency", it)
                    },
                    onPerMileChange = {
                        viewModel.onEvent(RateEvent.SetPerMile(it))
                        perMileError = validateField("perMile", it)
                    },
                    onUpToMilesChange = {
                        viewModel.onEvent(RateEvent.SetUpToMiles(it))
                        upToMilesError = validateField("upToMiles", it)
                    },
                    onAdditionalPerMileChange = {
                        viewModel.onEvent(RateEvent.SetAdditionalPerMile(it))
                        additionalPerMileError = validateField("additionalPerMile", it)
                    },
                    onMinCityIntercityChange = {
                        viewModel.onEvent(RateEvent.SetField("minCityIntercity", it))
                        minCityIntercityError = validateField("minCityIntercity", it)
                    }
                )
            } else {
                AmenityTaxStep(state, viewModel)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeaderCard(state: VehicleRatesState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (state.currentStep == RateStep.BASE_RATES) "Edit your vehicle rates" else "Amenity Rates",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "(Enter an all-inclusive rate in each applicable bucket)",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleInfoCard(state: VehicleRatesState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.vehicleName.ifEmpty { "Vehicle Type" },
                    color = LimoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                val make = state.vehicleTags.getOrNull(2) ?: "Make"
                val model = state.vehicleTags.getOrNull(3) ?: "Model"
                val year = state.vehicleTags.getOrNull(1) ?: "Year"
                val color = state.vehicleTags.getOrNull(0) ?: "Color"

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VehicleTag(color)
                    VehicleTag(year)
                    VehicleTag(make)
                    VehicleTag(model)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (!state.vehicleImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = state.vehicleImageUrl,
                    contentDescription = "Vehicle",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(120.dp)
                        .height(70.dp)
                )
            } else {
                Image(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    alpha = 0.2f
                )
            }
        }
    }
}

@Composable
private fun VehicleTag(text: String) {
    Surface(
        color = LimoOrange.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BaseRatesStep(
    state: VehicleRatesState,
    vm: VehicleRatesViewModel,
    currencyError: String?,
    perMileError: String?,
    upToMilesError: String?,
    additionalPerMileError: String?,
    minCityIntercityError: String?,
    onCurrencyChange: (String) -> Unit,
    onPerMileChange: (String) -> Unit,
    onUpToMilesChange: (String) -> Unit,
    onAdditionalPerMileChange: (String) -> Unit,
    onMinCityIntercityChange: (String) -> Unit
) {
    Text(
        text = "Rate Manager (Adjust Your rates up or down)",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Color.Black
    )
    Text(
        text = "Click on the range value to reset.",
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier.padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.height(20.dp))

    CommonDropdown(
        label = "CURRENCY",
        placeholder = "Select Currency",
        selectedValue = state.selectedCurrency,
        options = state.currencyOptions,
        onValueSelected = onCurrencyChange,
        isRequired = true,
        errorMessage = currencyError
    )

    Spacer(modifier = Modifier.height(20.dp))

    CustomSegmentedControl(
        items = listOf("MILE", "KILOMETER"),
        selectedIndex = if (state.distanceUnit == DistanceUnit.MILE) 0 else 1,
        onIndexChanged = { if ((it == 0) != (state.distanceUnit == DistanceUnit.MILE)) vm.onEvent(RateEvent.ToggleDistanceUnit) },
        modifier = Modifier.fillMaxWidth().height(48.dp)
    )

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Select $ rate for the first x miles, then for additional miles.",
        color = LimoOrange,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommonDropdown(
            label = if (state.distanceUnit == DistanceUnit.MILE) "PER MILE" else "PER KM",
            placeholder = "Select",
            selectedValue = state.perMile,
            options = state.perMileOptions,
            onValueSelected = onPerMileChange,
            modifier = Modifier.weight(1f),
            isRequired = true,
            errorMessage = perMileError
        )

        CommonDropdown(
            label = if (state.distanceUnit == DistanceUnit.MILE) "UPTO X MILES" else "UPTO X KM",
            placeholder = "Select",
            selectedValue = state.upToMiles,
            options = state.uptoMilesOptions,
            onValueSelected = onUpToMilesChange,
            modifier = Modifier.weight(1f),
            isRequired = true,
            errorMessage = upToMilesError
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    CommonDropdown(
        label = if (state.distanceUnit == DistanceUnit.MILE) "ADDITIONAL PER MILE" else "ADDITIONAL PER KM",
        placeholder = "Select",
        selectedValue = state.additionalPerMile,
        options = state.perMileOptions,
        onValueSelected = onAdditionalPerMileChange,
        isRequired = true,
        errorMessage = additionalPerMileError
    )

    Spacer(modifier = Modifier.height(24.dp))

    MoneyRow("MIN AIRPORT RATE - DEP", state.minAirportDep) { vm.onEvent(RateEvent.SetField("minAirportDep", it)) }
    MoneyRow("MIN AIRPORT RATE - ARR", state.minAirportArr) { vm.onEvent(RateEvent.SetField("minAirportArr", it)) }

    MoneyRow(
        label = "MINIMUM CITY TO INTERCITY RATE",
        value = state.minCityIntercity,
        required = true,
        errorMessage = minCityIntercityError
    ) { onMinCityIntercityChange(it) }

    MoneyRow("MIN CRUISE PORT RATE - ARR", state.minCruiseArr) { vm.onEvent(RateEvent.SetField("minCruiseArr", it)) }
    MoneyRow("MIN CRUISE PORT RATE - DEP", state.minCruiseDep) { vm.onEvent(RateEvent.SetField("minCruiseDep", it)) }
    MoneyRow("HOURLY RATE", state.hourlyRate) { vm.onEvent(RateEvent.SetField("hourlyRate", it)) }
    MoneyRow("DISCOUNT AFTER 5 HRS?", state.discountAfter5Hrs) { vm.onEvent(RateEvent.SetField("discountAfter5Hrs", it)) }
    MoneyRow("DAY RATE EXTENSION", state.dayRateExt) { vm.onEvent(RateEvent.SetField("dayRateExt", it)) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommonDropdown(
            label = "MIN CHARTER HRS",
            placeholder = "Select",
            selectedValue = state.minCharterHrs,
            options = state.minCharterHoursOptions,
            onValueSelected = { vm.onEvent(RateEvent.SetField("minCharterHrs", it)) },
            modifier = Modifier.weight(1f),
            isRequired = false
        )
        CommonDropdown(
            label = "# HOURS = DAY RATE EXTENSION",
            placeholder = "Select",
            selectedValue = state.numHoursDayExt,
            options = state.hoursDayRateOptions,
            onValueSelected = { vm.onEvent(RateEvent.SetField("numHoursDayExt", it)) },
            modifier = Modifier.weight(1f),
            isRequired = false
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    CommonDropdown(
        label = "SHARED RIDE RATE - PER PERSON",
        placeholder = "Select",
        selectedValue = state.sharedRideRate,
        options = state.sharedRideOptions,
        onValueSelected = { vm.onEvent(RateEvent.SetField("sharedRideRate", it)) },
        isRequired = false
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text("You may add Early/Late rate between 11 PM and 5.30 AM.", fontSize = 12.sp, color = LimoOrange, style = AppTextStyles.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))

    CommonDropdown(
        label = "EARLY AM / LATE PM SURGE RATE",
        placeholder = "Select",
        selectedValue = state.earlyLateSurge,
        options = state.surgeOptions,
        onValueSelected = { vm.onEvent(RateEvent.SetField("earlyLateSurge", it)) },
        isRequired = false
    )

    Spacer(modifier = Modifier.height(16.dp))
    CommonDropdown(
        label = "HOLIDAY SURGE RATE",
        placeholder = "Select",
        selectedValue = state.holidaySurge,
        options = state.surgeOptions,
        onValueSelected = { vm.onEvent(RateEvent.SetField("holidaySurge", it)) },
        isRequired = false
    )

    Spacer(modifier = Modifier.height(16.dp))
    CommonDropdown(
        label = "FRIDAY / SATURDAY NIGHT SURGE RATE",
        placeholder = "Select",
        selectedValue = state.friSatSurge,
        options = state.surgeOptions,
        onValueSelected = { vm.onEvent(RateEvent.SetField("friSatSurge", it)) },
        isRequired = false
    )

    Spacer(modifier = Modifier.height(16.dp))

    MoneyRow("EXTRA STOP - SAME TOWN", state.extraStopSame) { vm.onEvent(RateEvent.SetField("extraStopSame", it)) }
    MoneyRow("EXTRA STOP - DIFF. TOWN", state.extraStopDiff) { vm.onEvent(RateEvent.SetField("extraStopDiff", it)) }
}

@Composable
private fun AmenityTaxStep(state: VehicleRatesState, vm: VehicleRatesViewModel) {
    Text("Amenity Rates", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(modifier = Modifier.height(12.dp))

    if (state.amenityRates.isEmpty()) {
        Text("No amenity metadata found.", color = Color.Gray, fontSize = 12.sp)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.amenityRates.values.forEach { amenity ->
                val label = amenity.label ?: amenity.name
                val price = state.amenityPrices[label] ?: (amenity.price?.toString() ?: "")
                MoneyRow(amenity.name, price) { vm.onEvent(RateEvent.SetAmenityPrice(label, it)) }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text("Applicable Taxes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommonTextField(
            label = "AIRPORT ARRIVAL TAX", placeholder = "0.00", text = state.airportArrivalTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("airportArrivalTax", it)) },
            keyboardType = KeyboardType.Decimal, isRequired = false, modifier = Modifier.weight(1f)
        )
        CommonTextField(
            label = "AIRPORT DEP. TAX", placeholder = "0.00", text = state.airportDepTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("airportDepTax", it)) },
            keyboardType = KeyboardType.Decimal, isRequired = false, modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommonTextField(
            label = "SEA PORT TAX", placeholder = "0.00", text = state.seaPortTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("seaPortTax", it)) },
            keyboardType = KeyboardType.Decimal, isRequired = false, modifier = Modifier.weight(1f)
        )
        CommonTextField(
            label = "CITY CONGESTION TAX", placeholder = "0.00", text = state.cityCongestionTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("cityCongestionTax", it)) },
            keyboardType = KeyboardType.Decimal, isRequired = false, modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))

    ToggleMoneyRow("CITY TAX", state.cityTax, state.cityTaxIsFlat, { vm.onEvent(RateEvent.SetField("cityTax", it)) }, { vm.onEvent(RateEvent.SetTaxFlat("cityTax", it)) })
    ToggleMoneyRow("STATE TAX", state.stateTax, state.stateTaxIsFlat, { vm.onEvent(RateEvent.SetField("stateTax", it)) }, { vm.onEvent(RateEvent.SetTaxFlat("stateTax", it)) })
    ToggleMoneyRow("VAT", state.vat, state.vatIsFlat, { vm.onEvent(RateEvent.SetField("vat", it)) }, { vm.onEvent(RateEvent.SetTaxFlat("vat", it)) })
    ToggleMoneyRow("WORKMAN'S COMP", state.workmansComp, state.workmansCompIsFlat, { vm.onEvent(RateEvent.SetField("workmansComp", it)) }, { vm.onEvent(RateEvent.SetTaxFlat("workmansComp", it)) })
    ToggleMoneyRow("OTHER TRANSPORTATION TAX", state.otherTransportTax, state.otherTransportTaxIsFlat, { vm.onEvent(RateEvent.SetField("otherTransportTax", it)) }, { vm.onEvent(RateEvent.SetTaxFlat("otherTransportTax", it)) })
}

@Composable
private fun MoneyRow(label: String, value: String, required: Boolean = false, errorMessage: String? = null, onChange: (String) -> Unit) {
    CommonTextField(
        label = label, placeholder = "0.00", text = value, onValueChange = onChange,
        keyboardType = KeyboardType.Decimal, isRequired = required, errorMessage = errorMessage, modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun ToggleMoneyRow(label: String, value: String, isFlat: Boolean, onChange: (String) -> Unit, onToggle: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
//        Text(
//            text = label,
//            style = AppTextStyles.bodyMedium.copy(
//                fontSize = 12.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = Color.Gray
//            )
//        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FIXED: Removed fixed height, removed padding issues
            CommonTextField(
                label = label,
                placeholder = "0.00",
                text = value,
                onValueChange = onChange,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
                isRequired = false
            )

            // FIXED: Reduced height to 48dp to look less bulky
            CustomSegmentedControl(
                items = listOf("FLAT ($)", "PERCENT"),
                selectedIndex = if (isFlat) 0 else 1,
                onIndexChanged = { onToggle(it == 0) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun CustomSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 8.dp

    Row(
        modifier = modifier
            .background(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, text ->
            val isSelected = index == selectedIndex
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) LimoOrange else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "segmentBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color(0xFF6B7280),
                label = "segmentText"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .shadow(
                        elevation = if (isSelected) 2.dp else 0.dp,
                        shape = RoundedCornerShape(cornerRadius - 2.dp),
                        clip = false
                    )
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(cornerRadius - 2.dp)
                    )
                    .clip(RoundedCornerShape(cornerRadius - 2.dp))
                    .noRippleClickable { onIndexChanged(index) }
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}