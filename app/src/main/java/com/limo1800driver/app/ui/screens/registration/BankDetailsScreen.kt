package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Required for the overlay
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
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
import com.limo1800driver.app.ui.components.ShimmerCircle
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
    // Needed because multiple countries share the same currency code (e.g., USD for US, AS, etc.)
    var currencyCountryCode by remember { mutableStateOf<String?>(null) }
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
            // Prefer explicit first/last names from API; fall back to combined name when needed.
            if (accHolderFirstName.isEmpty()) {
                accHolderFirstName = prefill.accountHolderFirstName
                    ?: prefill.accountHolderName?.trim()?.split(" ")?.firstOrNull()
                            ?: ""
            }
            if (accHolderLastName.isEmpty()) {
                accHolderLastName = prefill.accountHolderLastName
                    ?: prefill.accountHolderName?.trim()?.split(" ")?.drop(1)?.joinToString(" ")
                            ?: ""
            }
            if (bankName.isEmpty()) bankName = prefill.bankName ?: ""
            if (bankAddress.isEmpty()) bankAddress = prefill.bankAddress ?: ""
            if (bankAccount.isEmpty()) bankAccount = prefill.accountNumber ?: ""
            if (routingNumber.isEmpty()) routingNumber = prefill.routingNumber ?: ""
            if (accountType == null) accountType = prefill.accountType
            if (currency == null) currency = prefill.currency
            if (currencyCountryCode == null) currencyCountryCode = prefill.country
            if (ssn.isEmpty()) ssn = prefill.socialSecurityNumber ?: ""
            if (badgeCity.isEmpty()) badgeCity = prefill.badgeCity ?: ""

            val prefillBusinessId = prefill.businessId?.trim().orEmpty()
            if (prefillBusinessId.isNotEmpty()) {
                hasEIN = true
                if (ein.isEmpty()) ein = prefillBusinessId
            }
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


    // Handle success - Only for API completion calls (when step wasn't already completed)
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            registrationNavigationState.setNextStep(uiState.nextStep)
            onNext(uiState.nextStep)
        }
    }

    // Error handling

    // Field-specific error states
    var accHolderFirstNameError by remember { mutableStateOf<String?>(null) }
    var accHolderLastNameError by remember { mutableStateOf<String?>(null) }
    var bankNameError by remember { mutableStateOf<String?>(null) }
    var bankAddressError by remember { mutableStateOf<String?>(null) }
    var bankAccountError by remember { mutableStateOf<String?>(null) }
    var routingNumberError by remember { mutableStateOf<String?>(null) }
    var accountTypeError by remember { mutableStateOf<String?>(null) }
    var currencyError by remember { mutableStateOf<String?>(null) }
    var ssnError by remember { mutableStateOf<String?>(null) }
    var einError by remember { mutableStateOf<String?>(null) }
    var badgeCityError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Validation function
    fun validateField(fieldName: String, value: String): String? {
        return when (fieldName) {
            "accHolderFirstName" -> when {
                value.isBlank() -> "Account holder first name is required"
                value.length < 2 -> "First name must be at least 2 characters"
                else -> null
            }
            "accHolderLastName" -> when {
                value.isBlank() -> "Account holder last name is required"
                value.length < 2 -> "Last name must be at least 2 characters"
                else -> null
            }
            "bankName" -> if (value.isBlank()) "Bank name is required" else null
            "bankAddress" -> if (value.isBlank()) "Bank address is required" else null
            "bankAccount" -> when {
                value.isBlank() -> "Account number is required"
                value.length < 8 -> "Account number must be at least 8 characters"
                else -> null
            }
            "routingNumber" -> when {
                value.isBlank() -> "Routing number is required"
                !value.matches(Regex("^\\d{9}$")) -> "Routing number must be exactly 9 digits"
                else -> null
            }
            "badgeCity" -> if (value.isBlank()) "Badge city is required" else null
            else -> null
        }
    }

    // Handle API Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            apiError = error
            // Clear field-specific errors when we have an API error
            accHolderFirstNameError = null
            accHolderLastNameError = null
            bankNameError = null
            bankAddressError = null
            bankAccountError = null
            routingNumberError = null
            accountTypeError = null
            currencyError = null
            ssnError = null
            einError = null
            badgeCityError = null
        }
    }

    // --- MAIN LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
        // REMOVED: .windowInsetsPadding(WindowInsets.navigationBars)
        // We want the content to flow behind the bottom bar, and the bottom bar
        // to handle its own padding to ensure the white background goes edge-to-edge.
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

            // API Error Display
            apiError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF2F2)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Account Holder Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommonTextField(
                    label = "ACC. HOLDER FIRST NAME",
                    placeholder = "John",
                    text = accHolderFirstName,
                    onValueChange = {
                        accHolderFirstName = it
                        accHolderFirstNameError = validateField("accHolderFirstName", it)
                        apiError = null
                    },
                    modifier = Modifier.weight(1f),
                    isRequired = true,
                    errorMessage = accHolderFirstNameError
                )
                CommonTextField(
                    label = "ACC. HOLDER LAST NAME",
                    placeholder = "Smith",
                    text = accHolderLastName,
                    onValueChange = {
                        accHolderLastName = it
                        accHolderLastNameError = validateField("accHolderLastName", it)
                        apiError = null
                    },
                    modifier = Modifier.weight(1f),
                    isRequired = true,
                    errorMessage = accHolderLastNameError
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bank Name
            LocationAutocomplete(
                label = "BANK NAME",
                value = bankName,
                onValueChange = {
                    bankName = it
                    bankNameError = validateField("bankName", it)
                    bankAddressError = null // Clear bank address error when bank name changes
                    apiError = null
                },
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
                    bankNameError = null
                    apiError = null
                },
                placeholder = "",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                typeFilter = com.google.android.libraries.places.api.model.TypeFilter.ESTABLISHMENT
            )

            // Bank Name Error Message
            bankNameError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bank Address
            LocationAutocomplete(
                label = "BANK ADDRESS",
                value = bankAddress,
                onValueChange = {
                    bankAddress = it
                    bankAddressError = validateField("bankAddress", it)
                    apiError = null
                },
                onLocationSelected = { fullAddress, _, _, _, displayText, _, _, countryCode, _ ->
                    // Always prefer the full formatted address so backend receives a complete value
                    val baseAddress = fullAddress.takeIf { it.isNotBlank() }
                        ?: displayText

                    bankAddress = if (!countryCode.isNullOrBlank() && !baseAddress.contains(countryCode, ignoreCase = true)) {
                        "$baseAddress, ${countryCode.uppercase()}"
                    } else {
                        baseAddress
                    }
                    bankAddressError = null
                    apiError = null
                },
                placeholder = "",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                typeFilter = com.google.android.libraries.places.api.model.TypeFilter.ADDRESS
            )

            // Bank Address Error Message
            bankAddressError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bank Account
            CommonTextField(
                label = "BANK ACCOUNT/IBAN",
                placeholder = "",
                text = bankAccount,
                onValueChange = {
                    bankAccount = it
                    bankAccountError = validateField("bankAccount", it)
                    apiError = null
                },
                isRequired = true,
                keyboardType = KeyboardType.Number,
                errorMessage = bankAccountError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Routing Number
            CommonTextField(
                label = "CHECKING ROUTING NUMBER",
                placeholder = "",
                text = routingNumber,
                onValueChange = {
                    routingNumber = it
                    routingNumberError = validateField("routingNumber", it)
                    apiError = null
                },
                isRequired = true,
                keyboardType = KeyboardType.Number,
                errorMessage = routingNumberError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Account Type
            CommonDropdown(
                label = "ACCOUNT TYPE",
                placeholder = "Select",
                selectedValue = accountTypeOptions.find { it.first == accountType }?.second,
                options = accountTypeOptions.map { it.second },
                onValueSelected = { selected ->
                    accountType = accountTypeOptions.find { it.second == selected }?.first
                    accountTypeError = if (accountType.isNullOrBlank()) "Please select account type" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = accountTypeError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- CURRENCY FIX START ---
            // We wrapper CommonTextField in a Box to intercept clicks
            Box(modifier = Modifier.fillMaxWidth()) {
                CommonTextField(
                    label = "CURRENCY",
                    placeholder = "Select",
                    // Display the friendly name (e.g. "USD - $ - US Dollar")
                    text = (
                            currencyOptions.firstOrNull {
                                it.code.equals(currency, ignoreCase = true) &&
                                        it.countryCode.equals(currencyCountryCode, ignoreCase = true)
                            } ?: currencyOptions.firstOrNull {
                                it.code.equals(currency, ignoreCase = true)
                            }
                            )?.display ?: "",
                    onValueChange = { }, // Read-only, handled by overlay
                    isRequired = true,
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = currencyError
                )

                // This overlay Box makes the entire text field area clickable
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null, // <--- This removes the gray ripple/shadow effect
                            onClick = { currencySheetOpen = true }
                        ),
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
                onValueChange = {
                    ssn = it
                    ssnError = null
                    apiError = null
                },
                isRequired = true,
                keyboardType = KeyboardType.Number,
                errorMessage = ssnError
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
                    onValueChange = {
                        ein = it
                        einError = null
                        apiError = null
                    },
                    isRequired = true,
                    errorMessage = einError
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Badge City
            LocationAutocomplete(
                label = "Badge City",
                value = badgeCity,
                onValueChange = {
                    badgeCity = it
                    badgeCityError = validateField("badgeCity", it)
                    apiError = null
                },
                onLocationSelected = { _, _, _, _, displayText, _, _, _, _ ->
                    badgeCity = displayText
                    badgeCityError = null
                    apiError = null
                },
                placeholder = "Enter badge city",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                typeFilter = com.google.android.libraries.places.api.model.TypeFilter.CITIES,
                errorMessage = badgeCityError
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- CUSTOM BOTTOM BAR ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            // High elevation for a nice drop shadow pointing UP
            shadowElevation = 10.dp,
            // Ensures the bar is opaque white all the way to bottom edge
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .navigationBarsPadding()
            ) {

                Button(
                    onClick = {
                        // Clear previous errors
                        accHolderFirstNameError = null
                        accHolderLastNameError = null
                        bankNameError = null
                        bankAddressError = null
                        bankAccountError = null
                        routingNumberError = null
                        accountTypeError = null
                        currencyError = null
                        ssnError = null
                        einError = null
                        badgeCityError = null
                        apiError = null

                        // Validation Logic
                        var hasErrors = false

                        // Validate Account Holder First Name
                        if (accHolderFirstName.isBlank()) {
                            accHolderFirstNameError = "Account holder first name is required"
                            hasErrors = true
                        } else if (accHolderFirstName.length < 2) {
                            accHolderFirstNameError = "First name must be at least 2 characters"
                            hasErrors = true
                        }

                        // Validate Account Holder Last Name
                        if (accHolderLastName.isBlank()) {
                            accHolderLastNameError = "Account holder last name is required"
                            hasErrors = true
                        } else if (accHolderLastName.length < 2) {
                            accHolderLastNameError = "Last name must be at least 2 characters"
                            hasErrors = true
                        }

                        // Validate Bank Name
                        if (bankName.isBlank()) {
                            bankNameError = "Bank name is required"
                            hasErrors = true
                        }

                        // Validate Bank Address
                        if (bankAddress.isBlank()) {
                            bankAddressError = "Bank address is required"
                            hasErrors = true
                        }

                        // Validate Bank Account
                        if (bankAccount.isBlank()) {
                            bankAccountError = "Account number is required"
                            hasErrors = true
                        } else if (bankAccount.length < 8) {
                            bankAccountError = "Account number must be at least 8 characters"
                            hasErrors = true
                        }

                        // Validate Routing Number
                        if (routingNumber.isBlank()) {
                            routingNumberError = "Routing number is required"
                            hasErrors = true
                        } else if (!routingNumber.matches(Regex("^\\d{9}$"))) {
                            routingNumberError = "Routing number must be exactly 9 digits"
                            hasErrors = true
                        }

                        // Validate Account Type
                        if (accountType.isNullOrBlank()) {
                            accountTypeError = "Please select account type"
                            hasErrors = true
                        }

                        // Validate Currency
                        if (currency.isNullOrBlank()) {
                            currencyError = "Please select currency"
                            hasErrors = true
                        }

                        // Validate SSN
                        if (ssn.isBlank()) {
                            ssnError = "SSN or Government ID is required"
                            hasErrors = true
                        }

                        // Validate EIN if hasEIN is enabled
                        if (hasEIN && ein.isBlank()) {
                            einError = "EIN / Business ID is required"
                            hasErrors = true
                        }

                        // Validate Badge City
                        if (badgeCity.isBlank()) {
                            badgeCityError = "Badge city is required"
                            hasErrors = true
                        }

                        // Only make API call if all validations pass
                        if (!hasErrors) {
                            android.util.Log.d("BankDetailsScreen", "Making API call to save bank details")

                            val first = accHolderFirstName.trim()
                            val last = accHolderLastName.trim()
                            val bankNm = bankName.trim()
                            val address = bankAddress.trim()
                            val accountNum = bankAccount.trim()
                            val routing = routingNumber.trim()
                            val acctType = accountType?.lowercase() ?: ""
                            val currencyCode = currency?.uppercase() ?: ""
                            val ssnValue = ssn.trim()
                            val badge = badgeCity.trim()
                            val businessId = if (hasEIN) ein.trim().ifEmpty { null } else null

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
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE89148),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        ShimmerCircle(
                            size = 24.dp,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Submit",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }

    if (currencySheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { currencySheetOpen = false },
            sheetState = currencySheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Header ---
                Text(
                    text = "Select Currency",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.LimoBlack
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )

                // --- Search Bar ---
                OutlinedTextField(
                    value = currencySearch,
                    onValueChange = { currencySearch = it },
                    placeholder = {
                        Text(
                            "Search (e.g. USD, Euro)",
                            style = TextStyle(color = Color.Gray, fontSize = 14.sp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = AppColors.LimoOrange,
                        unfocusedBorderColor = Color(0xFFE5E7EB), // Light gray
                        cursorColor = AppColors.LimoOrange
                    )
                )

                val filtered = remember(currencyOptions, currencySearch) {
                    currencyOptions.filter {
                        currencySearch.isBlank() ||
                                it.display.contains(currencySearch, ignoreCase = true) ||
                                it.code.contains(currencySearch, ignoreCase = true)
                    }
                }

                // --- Scrollable List (Optimized) ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp) // Space for navigation bar
                        .weight(1f, fill = false), // Takes available space but doesn't force full height
                    verticalArrangement = Arrangement.Top
                ) {
                    items(filtered) { option ->
                        val isSelected = currency == option.code

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currency = option.code
                                    currencyCountryCode = option.countryCode
                                    currencySheetOpen = false
                                    currencySearch = ""
                                    currencyError = null
                                    apiError = null
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp), // Comfortable touch target
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Currency Code Badge
                                    Surface(
                                        color = if (isSelected) AppColors.LimoOrange.copy(alpha = 0.1f) else Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = option.code,
                                            style = TextStyle(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) AppColors.LimoOrange else Color.Black
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Currency Name
                                    Text(
                                        text = option.display,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                            color = if (isSelected) AppColors.LimoOrange else AppColors.LimoBlack
                                        )
                                    )
                                }

                                // Country Name (Secondary Text)
                                Text(
                                    text = option.countryName,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    ),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // Selection Checkmark
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AppColors.LimoOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Clean Divider
                        HorizontalDivider(
                            color = Color(0xFFF3F4F6),
                            thickness = 1.dp
                        )
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No currency found",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}