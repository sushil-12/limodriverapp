package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.BasicInfoRequest
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.LocationAutocomplete
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.BasicInfoViewModel
import java.util.Calendar

@Composable
fun BasicInfoScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: BasicInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Form fields State
    var affiliateType by remember { mutableStateOf<String?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var dobMonth by remember { mutableStateOf<String?>(null) }
    var dobDay by remember { mutableStateOf<String?>(null) }
    var dobYear by remember { mutableStateOf<String?>(null) }
    var driverYear by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf("") }

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
    val monthOptions = (1..12).map { String.format("%02d", it) }
    val dayOptions = (1..31).map { String.format("%02d", it) }
    val yearOptions = (1950..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }.reversed()
    val driverYearOptions = (1980..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }.reversed()

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
            if (location.isEmpty()) location = prefill.address ?: ""
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
        viewModel.fetchBasicInfoStep()
    }

    // Success Navigation
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onNext(uiState.nextStep)
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

        // --- Scrollable Form ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            // Custom Black Dropdown for Affiliate Type
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
                onValueSelected = { affiliateType = it }
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
                    onValueChange = { firstName = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
                CommonTextField(
                    label = "Last name",
                    placeholder = "Smith",
                    text = lastName,
                    onValueChange = { lastName = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Gender
            CommonDropdown(
                label = "GENDER",
                placeholder = "Select",
                selectedValue = gender,
                options = genderOptions,
                onValueSelected = { gender = it },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Email
            CommonTextField(
                label = "EMAIL",
                placeholder = "abc@gmail.com",
                text = email,
                onValueChange = { email = it },
                isRequired = true,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Date of Birth (Order: DD - MM - YYYY)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommonDropdown(
                    label = "DATE OF BIRTH",
                    placeholder = "DD",
                    selectedValue = dobDay,
                    options = dayOptions,
                    onValueSelected = { dobDay = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
                CommonDropdown(
                    label = "",
                    placeholder = "MM",
                    selectedValue = dobMonth,
                    options = monthOptions,
                    onValueSelected = { dobMonth = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
                CommonDropdown(
                    label = "",
                    placeholder = "YYYY",
                    selectedValue = dobYear,
                    options = yearOptions,
                    onValueSelected = { dobYear = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // First Year Driver
            CommonDropdown(
                label = "FIRST YEAR PROFESSIONAL DRIVER",
                placeholder = "Select",
                selectedValue = driverYear,
                options = driverYearOptions,
                onValueSelected = { driverYear = it },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Location Autocomplete
            LocationAutocomplete(
                label = "ENTER LOCATION",
                value = location,
                onValueChange = { location = it },
                onLocationSelected = { _, cityValue, stateValue, zipCodeValue, displayText, lat, lng, countryValue, _ ->
                    location = displayText
                    city = cityValue
                    state = stateValue
                    zipCode = zipCodeValue
                    country = countryValue ?: ""
                    latitude = lat
                    longitude = lng
                },
                placeholder = "Enter your location",
                isRequired = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- Bottom Action Bar ---
        BottomNavigationRow(
            onBack = onBack,
            onReset = { onReset() },
            onNext = {
                // Validation Logic
                val apiAffiliateType = affiliateApiValues[affiliateType]
                if (apiAffiliateType != null && firstName.isNotBlank() && lastName.isNotBlank() &&
                    gender != null && email.isNotBlank() && dobMonth != null &&
                    dobDay != null && dobYear != null && driverYear != null &&
                    location.isNotBlank()) {

                    val dob = "$dobYear-$dobMonth-$dobDay"
                    val apiGender = if(gender == "X(LGBTQ+)") "other" else gender!!.lowercase()

                    val request = BasicInfoRequest(
                        affiliateType = apiAffiliateType,
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
                    // Trigger UI error state if needed
                }
            },
            isLoading = uiState.isLoading
        )
    }
}

/**
 * Custom Dark Dropdown Component for the "Choose One" field
 */
@Composable
fun DarkDropdown(
    placeholder: String,
    selectedValue: String?,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF121212)) // Dark Background
                .clickable { expanded = true }
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
            modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = AppColors.LimoBlack) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Custom Bottom Navigation Row matching the screenshot design
 * [Back Icon]  [Reset Pill]  [Next Pill]
 */
@Composable
fun BottomNavigationRow(
    onBack: (() -> Unit)?,
    onReset: () -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 16.dp), // Safe area padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Back Button (Circle Icon)
        if (onBack != null) {
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
            // Spacer to keep layout balanced if back is null,
            // though prompt implies visual match is needed.
            Spacer(modifier = Modifier.size(50.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Reset Button (Gray Pill)
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

        // 3. Next Button (Orange Pill with Arrow)
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
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
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