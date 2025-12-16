package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
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

    LaunchedEffect(state.success) {
        if (state.success) {
            onNext(state.nextStep)
            viewModel.consumeSuccess()
        }
    }

    Scaffold(
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
            BottomActionBar(
                isLoading = state.isLoading,
                onBack = null,
                onNext = {
                    if (state.currentStep == RateStep.BASE_RATES) {
                        viewModel.onEvent(RateEvent.OnNextClick)
                    } else {
                        viewModel.onEvent(RateEvent.OnSubmitClick)
                    }
                },
                nextButtonText = if (state.currentStep == RateStep.BASE_RATES) "Next" else "Submit"
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scroll)
                .imePadding()
                .padding(16.dp)
        ) {
            HeaderCard(state)
            Spacer(modifier = Modifier.height(16.dp))
            if (state.currentStep == RateStep.BASE_RATES) {
                BaseRatesStep(state, viewModel)
            } else {
                AmenityTaxStep(state, viewModel)
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HeaderCard(state: VehicleRatesState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (state.currentStep == RateStep.BASE_RATES) "Edit your vehicle rates" else "Amenity Rates",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "(Enter an all-inclusive rate in each applicable bucket)",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun BaseRatesStep(state: VehicleRatesState, vm: VehicleRatesViewModel) {
    // Vehicle details card (match reference design)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.vehicleName.ifEmpty { "Vehicle Type" },
                color = LimoOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.vehicleTags.take(4).forEach { tag ->
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier
                            .background(Color(0xFFFFF2E5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        // Vehicle image on the right (bigger like reference)
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(54.dp)
                // Order matters: draw border, then clip content so border is visible
                .border(1.dp, Color.Gray.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (!state.vehicleImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = state.vehicleImageUrl,
                    contentDescription = "Vehicle Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder when no image - matching iOS design
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    CommonDropdown(
        label = "CURRENCY",
        placeholder = "Select Currency",
        selectedValue = state.selectedCurrency,
        options = state.currencyOptions,
        onValueSelected = { vm.onEvent(RateEvent.SelectCurrency(it)) },
        isRequired = true
    )

    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("MILE", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        Switch(
            checked = state.distanceUnit == DistanceUnit.KILOMETER,
            onCheckedChange = { vm.onEvent(RateEvent.ToggleDistanceUnit) },
            colors = SwitchDefaults.colors(checkedThumbColor = LimoOrange, uncheckedThumbColor = LimoOrange)
        )
        Text("KILOMETER", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Select \$ rate for the first x miles, then for additional miles.", color = LimoOrange, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(6.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommonDropdown(
            label = if (state.distanceUnit == DistanceUnit.MILE) "PER MILE" else "PER KM",
            placeholder = "Select",
            selectedValue = state.perMile,
            options = state.perMileOptions,
            onValueSelected = { vm.onEvent(RateEvent.SetPerMile(it)) },
            modifier = Modifier.weight(1f),
            isRequired = true
        )

        CommonDropdown(
            label = if (state.distanceUnit == DistanceUnit.MILE) "UPTO X MILES" else "UPTO X KM",
            placeholder = "Select",
            selectedValue = state.upToMiles,
            options = state.uptoMilesOptions,
            onValueSelected = { vm.onEvent(RateEvent.SetUpToMiles(it)) },
            modifier = Modifier.weight(1f),
            isRequired = true
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    CommonDropdown(
        label = if (state.distanceUnit == DistanceUnit.MILE) "ADDITIONAL PER MILE" else "ADDITIONAL PER KM",
        placeholder = "Select",
        selectedValue = state.additionalPerMile,
        options = state.perMileOptions,
        onValueSelected = { vm.onEvent(RateEvent.SetAdditionalPerMile(it)) },
        isRequired = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    MoneyRow("MIN AIRPORT RATE - DEP", state.minAirportDep) { vm.onEvent(RateEvent.SetField("minAirportDep", it)) }
    MoneyRow("MIN AIRPORT RATE - ARR", state.minAirportArr) { vm.onEvent(RateEvent.SetField("minAirportArr", it)) }
    MoneyRow("MINIMUM CITY TO INTERCITY RATE", state.minCityIntercity, true) { vm.onEvent(RateEvent.SetField("minCityIntercity", it)) }
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
    
    Text(
        "You may add Early/Late rate between 11 PM and 5.30 AM.",
        fontSize = 12.sp,
        color = LimoOrange,
        style = AppTextStyles.bodyMedium
    )
    
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

    MoneyRow("EXTRA STOP - SAME TOWN", state.extraStopSame) { vm.onEvent(RateEvent.SetField("extraStopSame", it)) }
    MoneyRow("EXTRA STOP - DIFF. TOWN", state.extraStopDiff) { vm.onEvent(RateEvent.SetField("extraStopDiff", it)) }
}

@Composable
private fun AmenityTaxStep(state: VehicleRatesState, vm: VehicleRatesViewModel) {
    Text(
        "Amenity Rates",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        style = AppTextStyles.bodyMedium
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (state.amenityRates.isEmpty()) {
        Text(
            "No amenity metadata found.",
            color = Color.Gray,
            fontSize = 12.sp,
            style = AppTextStyles.bodyMedium
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.amenityRates.values.forEach { amenity ->
                val label = amenity.label ?: amenity.name
                val price = state.amenityPrices[label] ?: (amenity.price?.toString() ?: "")
                MoneyRow(amenity.name, price) {
                    vm.onEvent(RateEvent.SetAmenityPrice(label, it))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "Applicable Taxes",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        style = AppTextStyles.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Tax fields side-by-side (matching iOS design)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommonTextField(
            label = "AIRPORT ARRIVAL TAX",
            placeholder = "0.00",
            text = state.airportArrivalTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("airportArrivalTax", it)) },
            keyboardType = KeyboardType.Decimal,
            isRequired = false,
            modifier = Modifier.weight(1f)
        )
        CommonTextField(
            label = "AIRPORT DEP. TAX",
            placeholder = "0.00",
            text = state.airportDepTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("airportDepTax", it)) },
            keyboardType = KeyboardType.Decimal,
            isRequired = false,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommonTextField(
            label = "SEA PORT TAX",
            placeholder = "0.00",
            text = state.seaPortTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("seaPortTax", it)) },
            keyboardType = KeyboardType.Decimal,
            isRequired = false,
            modifier = Modifier.weight(1f)
        )
        CommonTextField(
            label = "CITY CONGESTION TAX",
            placeholder = "0.00",
            text = state.cityCongestionTax,
            onValueChange = { vm.onEvent(RateEvent.SetField("cityCongestionTax", it)) },
            keyboardType = KeyboardType.Decimal,
            isRequired = false,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    ToggleMoneyRow("CITY TAX", state.cityTax, state.cityTaxIsFlat,
        { vm.onEvent(RateEvent.SetField("cityTax", it)) },
        { vm.onEvent(RateEvent.SetTaxFlat("cityTax", it)) }
    )
    ToggleMoneyRow("STATE TAX", state.stateTax, state.stateTaxIsFlat,
        { vm.onEvent(RateEvent.SetField("stateTax", it)) },
        { vm.onEvent(RateEvent.SetTaxFlat("stateTax", it)) }
    )
    ToggleMoneyRow("VAT", state.vat, state.vatIsFlat,
        { vm.onEvent(RateEvent.SetField("vat", it)) },
        { vm.onEvent(RateEvent.SetTaxFlat("vat", it)) }
    )
    ToggleMoneyRow("WORKMAN'S COMP", state.workmansComp, state.workmansCompIsFlat,
        { vm.onEvent(RateEvent.SetField("workmansComp", it)) },
        { vm.onEvent(RateEvent.SetTaxFlat("workmansComp", it)) }
    )
    ToggleMoneyRow("OTHER TRANSPORTATION TAX", state.otherTransportTax, state.otherTransportTaxIsFlat,
        { vm.onEvent(RateEvent.SetField("otherTransportTax", it)) },
        { vm.onEvent(RateEvent.SetTaxFlat("otherTransportTax", it)) }
    )
}

@Composable
private fun MoneyRow(label: String, value: String, required: Boolean = false, onChange: (String) -> Unit) {
    CommonTextField(
        label = label,
        placeholder = "0.00",
        text = value,
        onValueChange = onChange,
        keyboardType = KeyboardType.Decimal,
        isRequired = required,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ToggleMoneyRow(
    label: String,
    value: String,
    isFlat: Boolean,
    onChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommonTextField(
            label = label,
            placeholder = "0.00",
            text = value,
            onValueChange = onChange,
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
            isRequired = false
        )
        Spacer(modifier = Modifier.width(8.dp))
        CustomSegmentedControl(
            items = listOf("FLAT ($)", "PERCENT"),
            selectedIndex = if (isFlat) 0 else 1,
            onIndexChanged = { onToggle(it == 0) },
            // Prevent the control from taking the entire row width (which squeezes the text field)
            modifier = Modifier.widthIn(min = 150.dp, max = 180.dp)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun CustomSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
            .padding(4.dp)
            .height(32.dp)
    ) {
        items.forEachIndexed { index, text ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (index == selectedIndex) LimoOrange else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onIndexChanged(index) }
            ) {
                Text(text, color = if (index == selectedIndex) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CenterText(text: String, color: Color = Color.Black, bold: Boolean = false) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = AppTextStyles.bodyMedium
        )
    }
}