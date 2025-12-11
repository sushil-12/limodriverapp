package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Required for the overlay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.BankDetailsRequest
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.LocationAutocomplete
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.util.CurrencyOption
import com.limo1800driver.app.ui.util.loadCurrencyOptions
import com.limo1800driver.app.ui.viewmodel.BankDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDetailsScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: BankDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val registrationNavigationState = remember { RegistrationNavigationState() }
    
    var accHolderFirstName by remember { mutableStateOf("") }
    var accHolderLastName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAddress by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var routingNumber by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf<String?>(null) }
    var currency by remember { mutableStateOf<String?>(null) }
    var currencySheetOpen by remember { mutableStateOf(false) }
    var currencySearch by remember { mutableStateOf("") }
    val currencySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currencyLoadTried by remember { mutableStateOf(false) }
    var ssn by remember { mutableStateOf("") }
    var ein by remember { mutableStateOf("") }
    var hasEIN by remember { mutableStateOf(true) }
    var badgeCity by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val accountTypeOptions = listOf("individual" to "Individual", "company" to "Business")
    var currencyOptions by remember { mutableStateOf<List<CurrencyOption>>(emptyList()) }
    
    // Prefill data
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.let { prefill ->
            if (accHolderFirstName.isEmpty()) accHolderFirstName = prefill.accountHolderName?.split(" ")?.firstOrNull() ?: ""
            if (accHolderLastName.isEmpty()) accHolderLastName = prefill.accountHolderName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
            if (bankName.isEmpty()) bankName = prefill.bankName ?: ""
            if (bankAccount.isEmpty()) bankAccount = prefill.accountNumber ?: ""
            if (routingNumber.isEmpty()) routingNumber = prefill.routingNumber ?: ""
            if (accountType == null) accountType = prefill.accountType
        }
    }
    
    // Fetch step data on load
    LaunchedEffect(Unit) {
        viewModel.fetchBankDetailsStep()

        if (!currencyLoadTried) {
            currencyLoadTried = true

            val loaded = loadCurrencyOptions(context)

            currencyOptions = if (loaded.isNotEmpty()) {
                loaded
            } else {
                listOf(
                    CurrencyOption("US", "United States", "USD", "$"),
                    CurrencyOption("CA", "Canada", "CAD", "CA$"),
                    CurrencyOption("GB", "United Kingdom", "GBP", "£"),
                    CurrencyOption("EU", "European Union", "EUR", "€")
                )
            }
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

    // --- MAIN LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .systemBarsPadding()
    ) {
        RegistrationTopBar(onBack = onBack)

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter your bank details",
                style = AppTextStyles.phoneEntryHeadline.copy(
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Account Holder Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommonTextField(
                    label = "ACC. HOLDER FIRST NAME *",
                    placeholder = "John",
                    text = accHolderFirstName,
                    onValueChange = { accHolderFirstName = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
                CommonTextField(
                    label = "ACC. HOLDER LAST NAME *",
                    placeholder = "Smith",
                    text = accHolderLastName,
                    onValueChange = { accHolderLastName = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Bank Name
            LocationAutocomplete(
                label = "BANK NAME *",
                value = bankName,
                onValueChange = { bankName = it },
                onLocationSelected = { fullAddress, name, _, _, displayText, _, _, countryCode, placeLabel ->
                    // Prefer explicit place name, fall back to Google display text
                    val selectedBankName = name
                        .takeIf { !it.isNullOrBlank() }
                        ?: placeLabel.takeIf { it.isNotBlank() }
                        ?: displayText.takeIf { it.isNotBlank() }
                        ?: bankName

                    // Use the full formatted address when available; fallback to display text
                    val baseAddress = fullAddress
                        .takeIf { it.isNotBlank() }
                        ?: displayText.takeIf { it.isNotBlank() }
                        ?: bankAddress

                    val resolvedAddress = if (!countryCode.isNullOrBlank() && !baseAddress.contains(countryCode, ignoreCase = true)) {
                        "$baseAddress, ${countryCode.uppercase()}"
                    } else {
                        baseAddress
                    }

                    bankName = selectedBankName
                    bankAddress = resolvedAddress
                },
                placeholder = "",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                typeFilter = com.google.android.libraries.places.api.model.TypeFilter.ESTABLISHMENT
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Bank Address
            LocationAutocomplete(
                label = "BANK ADDRESS *",
                value = bankAddress,
                onValueChange = { bankAddress = it },
                onLocationSelected = { fullAddress, _, _, _, displayText, _, _, countryCode, _ ->
                    // Always prefer the full formatted address so backend receives a complete value
                    val baseAddress = fullAddress.takeIf { it.isNotBlank() }
                        ?: displayText

                    bankAddress = if (!countryCode.isNullOrBlank() && !baseAddress.contains(countryCode, ignoreCase = true)) {
                        "$baseAddress, ${countryCode.uppercase()}"
                    } else {
                        baseAddress
                    }
                },
                placeholder = "",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                typeFilter = com.google.android.libraries.places.api.model.TypeFilter.ADDRESS
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Bank Account
            CommonTextField(
                label = "BANK ACCOUNT/IBAN *",
                placeholder = "",
                text = bankAccount,
                onValueChange = { bankAccount = it },
                isRequired = true,
                keyboardType = KeyboardType.Number
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Routing Number
            CommonTextField(
                label = "CHECKING ROUTING NUMBER *",
                placeholder = "",
                text = routingNumber,
                onValueChange = { routingNumber = it },
                isRequired = true,
                keyboardType = KeyboardType.Number
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Account Type
            CommonDropdown(
                label = "ACCOUNT TYPE *",
                placeholder = "Select",
                selectedValue = accountTypeOptions.find { it.first == accountType }?.second,
                options = accountTypeOptions.map { it.second },
                onValueSelected = { selected ->
                    accountType = accountTypeOptions.find { it.second == selected }?.first
                },
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // --- CURRENCY FIX START ---
            // We wrapper CommonTextField in a Box to intercept clicks
            Box(modifier = Modifier.fillMaxWidth()) {
                CommonTextField(
                    label = "CURRENCY *",
                    placeholder = "Select",
                    // Display the friendly name (e.g. "USD - $ - US Dollar")
                    text = currencyOptions.find { it.code.equals(currency, ignoreCase = true) }?.display ?: "",
                    onValueChange = { }, // Read-only, handled by overlay
                    isRequired = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // This overlay Box makes the entire text field area clickable
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { currencySheetOpen = true },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Add the dropdown arrow icon to match CommonDropdown style
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Currency",
                        tint = Color.Gray,
                        modifier = Modifier.padding(end = 12.dp, top = 8.dp) // Adjust padding to center visually in the input area
                    )
                }
            }
            // --- CURRENCY FIX END ---
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // SSN
            CommonTextField(
                label = "SSN (or Government ID)",
                placeholder = "",
                text = ssn,
                onValueChange = { ssn = it },
                isRequired = true,
                keyboardType = KeyboardType.Number
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // EIN Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Has EIN / Business ID?",
                    style = AppTextStyles.bodyMedium
                )
                Switch(
                    checked = hasEIN,
                    onCheckedChange = { hasEIN = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFE89148)
                    )
                )
            }
            
            if (hasEIN) {
                Spacer(modifier = Modifier.height(18.dp))
                CommonTextField(
                    label = "EIN / Business ID",
                    placeholder = "",
                    text = ein,
                    onValueChange = { ein = it },
                    isRequired = true
                )
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Badge City
            LocationAutocomplete(
                label = "Badge City",
                value = badgeCity,
                onValueChange = { badgeCity = it },
                onLocationSelected = { _, _, _, _, displayText, _, _, _, _ ->
                    badgeCity = displayText
                },
                placeholder = "Enter badge city",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                typeFilter = com.google.android.libraries.places.api.model.TypeFilter.CITIES
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // --- CUSTOM BOTTOM BAR ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    val first = accHolderFirstName.trim()
                    val last = accHolderLastName.trim()
                    val bankNm = bankName.trim()
                    val address = bankAddress.trim()
                    val accountNum = bankAccount.trim()
                    val routing = routingNumber.trim()
                    val acctType = accountType?.lowercase() ?: ""
                    val currencyCode = currency?.lowercase() ?: ""
                    val ssnValue = ssn.trim()
                    val badge = badgeCity.trim()
                    val businessId = if (hasEIN) ein.trim().ifEmpty { null } else null

                    // Validation Logic
                    if (first.isEmpty() || last.isEmpty()) return@Button
                    if (bankNm.isEmpty()) return@Button
                    if (address.isEmpty()) return@Button
                    if (accountNum.isEmpty()) return@Button
                    if (routing.isEmpty()) return@Button
                    if (acctType.isEmpty()) return@Button
                    if (currencyCode.isEmpty()) return@Button
                    if (ssnValue.isEmpty()) return@Button
                    if (badge.isEmpty()) return@Button

                    val request = BankDetailsRequest(
                        bankName = bankNm,
                        bankAddress = address,
                        accountHolderFirstName = first,
                        accountHolderLastName = last,
                        accountNumber = accountNum,
                        routingNumber = routing,
                        accountType = acctType,
                        currency = currencyCode,
                        socialSecurityNumber = ssnValue,
                        badgeCity = badge,
                        businessId = businessId
                    )

                    viewModel.completeBankDetails(request)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE89148),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Submit",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }

    if (currencySheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { currencySheetOpen = false },
            sheetState = currencySheetState,
            containerColor = Color.White // Ensure sheet background is white
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Currency",
                    style = AppTextStyles.phoneEntryHeadline.copy(
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                // Using OutlinedTextField for search to match app style
                OutlinedTextField(
                    value = currencySearch,
                    onValueChange = { currencySearch = it },
                    placeholder = { Text("Search currency") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFE89148),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
                val filtered = currencyOptions.filter {
                    currencySearch.isBlank() ||
                        it.display.contains(currencySearch, ignoreCase = true) ||
                        it.code.contains(currencySearch, ignoreCase = true)
                }
                
                // Scrollable list for currencies
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filtered.forEach { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currency = option.code
                                    currencySheetOpen = false
                                    currencySearch = ""
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8F8F8),
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 10.dp, horizontal = 14.dp)
                            ) {
                                Text(
                                    text = option.display,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = option.countryName,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            text = "No currency found",
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 16.dp),
                            fontSize = 13.sp
                        )
                    }
                }

            }
        }
    }

    // Error Dialog (unchanged)
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