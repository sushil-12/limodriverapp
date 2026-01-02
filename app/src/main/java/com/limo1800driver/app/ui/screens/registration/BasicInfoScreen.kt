package com.limo1800driver.app.ui.screens.registration

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.BasicInfoRequest
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.DatePickerComponent
import com.limo1800driver.app.ui.components.DatePickerConfig
import com.limo1800driver.app.ui.components.LocationAutocomplete
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.BasicInfoViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    onUpdateComplete: (() -> Unit)? = null,
    viewModel: BasicInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // Focus requesters
    val affiliateFocusRequester = remember { FocusRequester() }
    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }
    val genderFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val dobMonthFocusRequester = remember { FocusRequester() } // Kept for error focus
    val driverYearFocusRequester = remember { FocusRequester() }
    val locationFocusRequester = remember { FocusRequester() }

    // Form fields State
    var affiliateType by remember { mutableStateOf<String?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }

    // DOB State
    var dobMonth by remember { mutableStateOf<String?>(null) }
    var dobDay by remember { mutableStateOf<String?>(null) }
    var dobYear by remember { mutableStateOf<String?>(null) }


    var driverYear by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf("") }

    // Error state variables
    var affiliateTypeError by remember { mutableStateOf<String?>(null) }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var driverYearError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Validation function
    fun validateField(fieldName: String, value: String): String? {
        return when (fieldName) {
            "firstName" -> when {
                value.isBlank() -> "First name is required"
                value.length < 2 -> "First name must be at least 2 characters"
                else -> null
            }
            "lastName" -> when {
                value.isBlank() -> "Last name is required"
                value.length < 2 -> "Last name must be at least 2 characters"
                else -> null
            }
            "email" -> when {
                value.isBlank() -> "Email is required"
                !Patterns.EMAIL_ADDRESS.matcher(value).matches() -> "Please enter a valid email address"
                else -> null
            }
            "location" -> if (value.isBlank()) "Location is required" else null
            else -> null
        }
    }

    // Location details state
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var zipCode by remember { mutableStateOf<String?>(null) }
    var country by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<String?>(null) }
    var city by remember { mutableStateOf<String?>(null) }

    // Data Options
    val affiliateOptions = listOf(
        "Black Car / Limo / Bus / Motor Coach",
        "5-Star Uber Black / X / XL / XXL",
        "Taxi Operator"
    )
    val affiliateApiValues = mapOf(
        "Black Car / Limo / Bus / Motor Coach" to "black_limo_operator",
        "5-Star Uber Black / X / XL / XXL" to "gig_operator",
        "Taxi Operator" to "taxi_operator"
    )

    val genderOptions = listOf("Male", "Female", "X(LGBTQ+)")
    val driverYearOptions = (1980..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }.reversed()

    // Function to parse address and extract location components
    fun parseAddressForPrefill(address: String) {
        // Expected format: "Street Address, City, State, Country" or "Street Address, City, State, ZIP, Country"
        val parts = address.split(",").map { it.trim() }
        when (parts.size) {
            4 -> {
                // Format: "Street, City, State, Country"
                city = parts[1]
                state = parts[2]
                country = parts[3]
            }
            5 -> {
                // Format: "Street, City, State, ZIP, Country"
                city = parts[1]
                state = parts[2]
                zipCode = parts[3]
                country = parts[4]
            }
            else -> {
                // Fallback: try to extract what we can
                if (parts.size >= 3) {
                    city = parts.getOrNull(1)
                    state = parts.getOrNull(2)
                    country = parts.getOrNull(3)
                }
            }
        }
    }

    // Prefill Logic
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.let { prefill ->
            if (affiliateType == null && !prefill.affiliateType.isNullOrEmpty()) {
                affiliateApiValues.entries.find { it.value == prefill.affiliateType }?.let {
                    affiliateType = it.key
                }
            }
            if (firstName.isEmpty()) firstName = prefill.firstName ?: ""
            if (lastName.isEmpty()) lastName = prefill.lastName ?: ""
            if (gender == null && !prefill.gender.isNullOrEmpty()) {
                gender = when (prefill.gender.lowercase()) {
                    "other" -> "X(LGBTQ+)"
                    else -> genderOptions.find { it.lowercase() == prefill.gender.lowercase() } ?: prefill.gender
                }
            }
            if (email.isEmpty()) email = prefill.email ?: ""
            if (location.isEmpty()) {
                location = prefill.address ?: ""
                // Parse the address to extract location components for API
                if (!location.isEmpty()) {
                    parseAddressForPrefill(location)
                }
            }
            if (dobMonth == null || dobDay == null || dobYear == null) {
                prefill.dob?.let { dob ->
                    val parts = dob.split("-")
                    if (parts.size == 3) {
                        dobYear = parts[0]
                        dobMonth = parts[1]
                        dobDay = parts[2]
                    }
                }
            }
            if (driverYear == null && !prefill.firstYearBusiness.isNullOrEmpty()) {
                driverYear = prefill.firstYearBusiness
            }
        }
    }

    // Initial Data Fetch
    LaunchedEffect(Unit) {  
        // Reset success state when screen loads (important for back navigation)
        viewModel.resetSuccessState()
        viewModel.fetchBasicInfoStep()
    }

    // Success Navigation - Only for API completion calls (when step wasn't already completed)
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            if (isEditMode) {
                // In edit mode, call onUpdateComplete callback to refresh and navigate back
                onUpdateComplete?.invoke()
            } else if (uiState.nextStep != null) {
                onNext(uiState.nextStep)
            }
        }
    }

    // Handle API Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            apiError = error
            // Clear field-specific errors when we have an API error
            affiliateTypeError = null
            firstNameError = null
            lastNameError = null
            genderError = null
            emailError = null
            dobError = null
            driverYearError = null
            locationError = null
        }
    }

    // Reset Function
    fun onReset() {
        affiliateType = null
        firstName = ""
        lastName = ""
        gender = null
        email = ""
        dobMonth = null
        dobDay = null
        dobYear = null
        driverYear = null
        location = ""
        latitude = null
        longitude = null
        zipCode = null
        country = null
        state = null
        city = null

        affiliateTypeError = null
        firstNameError = null
        lastNameError = null
        genderError = null
        emailError = null
        dobError = null
        driverYearError = null
        locationError = null
        apiError = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // --- Header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Enter your basic information",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.LimoBlack
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Let us know how to properly address you",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = AppColors.LimoBlack.copy(alpha = 0.6f)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Error Display
        apiError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF2F2)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
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

        // --- Scrollable Form ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            // Affiliate Type
            Text(
                text = "CHOOSE ONE",
                style = AppTextStyles.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.LimoBlack.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            DarkDropdown(
                placeholder = "Select Affiliate Type",
                selectedValue = affiliateType,
                options = affiliateOptions,
                onValueSelected = {
                    affiliateType = it
                    affiliateTypeError = if (it.isNullOrBlank()) "Please select an affiliate type" else null
                    apiError = null
                },
                errorMessage = affiliateTypeError,
                onDropdownOpened = { focusManager.clearFocus() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommonTextField(
                    label = "First name",
                    placeholder = "John",
                    text = firstName,
                    onValueChange = {
                        firstName = it
                        firstNameError = validateField("firstName", it)
                        apiError = null
                    },
                    modifier = Modifier.weight(1f),
                    isRequired = true,
                    errorMessage = firstNameError,
                    focusRequester = firstNameFocusRequester
                )
                CommonTextField(
                    label = "Last name",
                    placeholder = "Smith",
                    text = lastName,
                    onValueChange = {
                        lastName = it
                        lastNameError = validateField("lastName", it)
                        apiError = null
                    },
                    modifier = Modifier.weight(1f),
                    isRequired = true,
                    errorMessage = lastNameError,
                    focusRequester = lastNameFocusRequester
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Gender
            CommonDropdown(
                label = "GENDER",
                placeholder = "Select",
                selectedValue = gender,
                options = genderOptions,
                onValueSelected = {
                    gender = it
                    genderError = if (it.isNullOrBlank()) "Please select your gender" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = genderError,
                onDropdownOpened = { focusManager.clearFocus() }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Email
            CommonTextField(
                label = "EMAIL",
                placeholder = "abc@gmail.com",
                text = email,
                onValueChange = {
                    email = it
                    emailError = validateField("email", it)
                    apiError = null
                },
                isRequired = true,
                keyboardType = KeyboardType.Email,
                errorMessage = emailError,
                focusRequester = emailFocusRequester
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- DATE OF BIRTH SECTION ---
            DatePickerComponent(
                label = "DATE OF BIRTH",
                selectedMonth = dobMonth,
                selectedDay = dobDay,
                selectedYear = dobYear,
                onDateSelected = { month, day, year ->
                    dobMonth = month
                    dobDay = day
                    dobYear = year
                    dobError = if (month.isNotEmpty() && day.isNotEmpty() && year.isNotEmpty()) null else "Please select your complete date of birth"
                    apiError = null
                },
                config = DatePickerConfig(
                    allowFutureDates = false, // DOB can't be in future
                    allowPastDates = true,
                    minAgeYears = 18, // Must be at least 18 years old
                    yearRange = 1900..Calendar.getInstance().get(Calendar.YEAR) // Reasonable range for DOB
                ),
                errorMessage = dobError,
                isRequired = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // First Year Driver
            CommonDropdown(
                label = "FIRST YEAR PROFESSIONAL DRIVER",
                placeholder = "Select",
                selectedValue = driverYear,
                options = driverYearOptions,
                onValueSelected = {
                    driverYear = it
                    driverYearError = if (it.isNullOrBlank()) "Please select the year you started driving professionally" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = driverYearError,
                onDropdownOpened = { focusManager.clearFocus() }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Location Autocomplete
            LocationAutocomplete(
                label = "ENTER LOCATION",
                value = location,
                onValueChange = {
                    location = it
                    locationError = validateField("location", it)
                    apiError = null
                },
                onLocationSelected = { _, cityValue, stateValue, zipCodeValue, displayText, lat, lng, countryValue, _ ->
                    location = displayText
                    city = cityValue
                    state = stateValue
                    zipCode = zipCodeValue
                    country = countryValue ?: ""
                    latitude = lat
                    longitude = lng
                    locationError = null
                },
                placeholder = "Enter your location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth(),
                errorMessage = locationError
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- Bottom Action Bar ---
        BottomNavigationRow(
            onBack = onBack,
            onReset = { onReset() },
            onNext = {
                // Clear previous errors
                affiliateTypeError = null
                firstNameError = null
                lastNameError = null
                genderError = null
                emailError = null
                dobError = null
                driverYearError = null
                locationError = null
                apiError = null

                var isValid = true
                var firstInvalidField: FocusRequester? = null

                if (affiliateType.isNullOrBlank()) {
                    affiliateTypeError = "Please select an affiliate type"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = affiliateFocusRequester
                }

                if (firstName.isBlank()) {
                    firstNameError = "First name is required"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = firstNameFocusRequester
                } else if (firstName.length < 2) {
                    firstNameError = "First name must be at least 2 characters"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = firstNameFocusRequester
                }

                if (lastName.isBlank()) {
                    lastNameError = "Last name is required"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = lastNameFocusRequester
                } else if (lastName.length < 2) {
                    lastNameError = "Last name must be at least 2 characters"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = lastNameFocusRequester
                }

                if (gender.isNullOrBlank()) {
                    genderError = "Please select your gender"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = genderFocusRequester
                }

                if (email.isBlank()) {
                    emailError = "Email is required"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = emailFocusRequester
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailError = "Please enter a valid email address"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = emailFocusRequester
                }

                if (dobMonth.isNullOrBlank() || dobDay.isNullOrBlank() || dobYear.isNullOrBlank()) {
                    dobError = "Please select your complete date of birth"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = dobMonthFocusRequester
                }

                if (driverYear.isNullOrBlank()) {
                    driverYearError = "Please select the year you started driving professionally"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = driverYearFocusRequester
                }

                if (location.isBlank()) {
                    locationError = "Location is required"
                    isValid = false
                    if (firstInvalidField == null) firstInvalidField = locationFocusRequester
                }

                if (isValid) {
                    // Always make API call to save/update data, regardless of completion status
                    // This ensures data is saved even if step was previously completed
                    android.util.Log.d("BasicInfoScreen", "Making API call to save basic info")

                    // Make API call to complete/update the step
                        val apiAffiliateType = affiliateApiValues[affiliateType]
                        val dob = "$dobYear-$dobMonth-$dobDay"
                        val apiGender = if(gender == "X(LGBTQ+)") "other" else gender!!.lowercase()

                        val request = BasicInfoRequest(
                            affiliateType = apiAffiliateType!!,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            gender = apiGender,
                            email = email.trim(),
                            dob = dob,
                            firstYearBusiness = driverYear ?: "",
                            address = location.trim(),
                            latitude = latitude,
                            longitude = longitude,
                            zipCode = zipCode,
                            country = country,
                            state = state,
                            city = city
                        )
                        viewModel.completeBasicInfo(request)
                } else {
                    firstInvalidField?.requestFocus()
                }
            },
            isLoading = uiState.isLoading,
            isEditMode = isEditMode
        )

    }
}


@Composable
fun DarkDropdown(
    placeholder: String,
    selectedValue: String?,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    // 1. Add modifier parameter with default value
    modifier: Modifier = Modifier, 
    errorMessage: String? = null,
    onDropdownOpened: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    // State to store the width of the trigger button
    var rowSize by remember { mutableStateOf(Size.Zero) }

    // 2. Apply the passed modifier here instead of hardcoding fillMaxWidth
    Column(modifier = modifier) { 
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth() // This fills the Column (which is controlled by the param above)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (errorMessage != null) Color(0xFF7F1D1D) else Color(0xFF121212))
                    // 3. Capture the width of the button
                    .onGloballyPositioned { coordinates ->
                        rowSize = coordinates.size.toSize()
                    }
                    .clickable {
                        expanded = true
                        onDropdownOpened?.invoke()
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedValue ?: placeholder,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    // 4. Set the menu width to match the button width exactly
                    .width(with(LocalDensity.current) { rowSize.width.toDp() })
                    .background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.Black) },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun BottomNavigationRow(
    onBack: (() -> Unit)?,
    onReset: () -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean,
    isEditMode: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBack != null && !isEditMode) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.LimoBlack
                )
            }
        } else {
            Spacer(modifier = Modifier.size(50.dp))
        }

        if (!isEditMode) {
            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF3F4F6),
                    contentColor = AppColors.LimoBlack
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Reset",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
        }

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.LimoOrange,
                disabledContainerColor = AppColors.LimoOrange.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                ShimmerCircle(size = 24.dp)
            } else {
                if (isEditMode) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Next",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}