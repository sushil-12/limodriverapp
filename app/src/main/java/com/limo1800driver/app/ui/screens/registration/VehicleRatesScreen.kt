package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.VehicleRateSettingsRequest
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.VehicleRatesViewModel
import androidx.compose.runtime.remember
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.util.CurrencyOption
import com.limo1800driver.app.ui.util.loadCurrencyOptions

@Composable
fun VehicleRatesScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleRatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val registrationNavigationState = remember { RegistrationNavigationState() }
    
    var baseRate by remember { mutableStateOf("") }
    var perMileRate by remember { mutableStateOf("") }
    var perHourRate by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf<String?>(null) }
    var currencyOptions by remember { mutableStateOf<List<CurrencyOption>>(emptyList()) }
    val context = LocalContext.current
    val defaultKmMile = "mile"
    val defaultGratuity = 20.0
    val defaultIsGratuity = "yes"
    val defaultMinimumOnDemand = 100.0
    val defaultRateRangeFlat = "flat"
    
    // Fetch step data on load
    LaunchedEffect(context) {
        viewModel.fetchVehicleRateSettingsStep()
        currencyOptions = loadCurrencyOptions(context)
    }
    
    // Prefill data
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.let { prefill ->
            // Prefer expanded fields, fallback to legacy strings
            if (baseRate.isEmpty()) {
                baseRate = prefill.dayRate?.toString()
                    ?: prefill.baseRate
                    ?: ""
            }
            if (perMileRate.isEmpty()) {
                perMileRate = prefill.milageRate?.toString()
                    ?: prefill.perMileRate
                    ?: ""
            }
            if (perHourRate.isEmpty()) {
                perHourRate = prefill.hourlyRate?.toString()
                    ?: prefill.perHourRate
                    ?: ""
            }
            if (currency == null) currency = prefill.currency
        }
    }
    
    // Handle success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            registrationNavigationState.setNextStep(uiState.nextStep)
            onNext(uiState.nextStep)
        }
    }
    
    // Show error dialog
    var showErrorDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            showErrorDialog = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        RegistrationTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Edit your vehicle rates",
                style = AppTextStyles.phoneEntryHeadline.copy(
                    color = AppColors.LimoBlack,
                    fontSize = 24.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Enter an all-inclusive rate in each applicable bucket",
                style = AppTextStyles.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Currency
            CommonDropdown(
                label = "CURRENCY",
                placeholder = "Select currency",
                selectedValue = currencyOptions.find { it.code.equals(currency, ignoreCase = true) }?.display,
                options = currencyOptions.map { it.display },
                onValueSelected = { selected ->
                    currency = currencyOptions.find { it.display == selected }?.code
                },
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Base Rate
            CommonTextField(
                label = "BASE RATE",
                placeholder = "Enter base rate",
                text = baseRate,
                onValueChange = { baseRate = it }
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Per Mile Rate
            CommonTextField(
                label = "PER MILE RATE",
                placeholder = "Enter per mile rate",
                text = perMileRate,
                onValueChange = { perMileRate = it }
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Per Hour Rate
            CommonTextField(
                label = "PER HOUR RATE",
                placeholder = "Enter per hour rate",
                text = perHourRate,
                onValueChange = { perHourRate = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Bottom Bar
        BottomActionBar(
            isLoading = uiState.isLoading,
            onBack = null,
            onNext = {
                // Validation
                val currencyValue = currency ?: return@BottomActionBar
                
                val request = VehicleRateSettingsRequest(
                    vehicleId = null, // backend may infer from auth context
                    currency = currencyValue,
                    milageRate = perMileRate.toDoubleOrNull(),
                    hourlyRate = perHourRate.toDoubleOrNull(),
                    dayRate = baseRate.toDoubleOrNull(),
                    kmMile = defaultKmMile,
                    gratuity = defaultGratuity,
                    isGratuity = defaultIsGratuity,
                    minimumOnDemandRate = defaultMinimumOnDemand,
                    cityTaxPercentFlat = defaultRateRangeFlat,
                    stateTaxPercentFlat = defaultRateRangeFlat,
                    vatPercentFlat = defaultRateRangeFlat,
                    workmanCompPercentFlat = defaultRateRangeFlat,
                    otherTransportationTaxPercentFlat = defaultRateRangeFlat,
                    rateRangePercentFlat = defaultRateRangeFlat
                )
                
                viewModel.completeVehicleRateSettings(request)
            }
        )
    }
    
    // Error Dialog
    if (showErrorDialog && uiState.error != null) {
        AlertDialog(
            onDismissRequest = { 
                showErrorDialog = false
                viewModel.clearError()
            },
            title = { Text("Error") },
            text = { Text(uiState.error ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { 
                    showErrorDialog = false
                    viewModel.clearError()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

