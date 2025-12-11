package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.limo1800driver.app.data.model.Countries
import com.limo1800driver.app.data.model.Country
import com.limo1800driver.app.data.model.registration.CompanyInfoRequest
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.PhoneInputField
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.CompanyInfoViewModel
import java.util.regex.Pattern

@Composable
fun CompanyInfoScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: CompanyInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // --- Form State ---
    var companyName by remember { mutableStateOf("") }
    var dispatchEmail by remember { mutableStateOf("") }
    var doingBusinessAs by remember { mutableStateOf("") }
    var hasEditedDoingBusinessAs by remember { mutableStateOf(false) }

    // Phone fields
    var cellDispatch by remember { mutableStateOf("") }
    var businessPhone by remember { mutableStateOf("") }
    var fax by remember { mutableStateOf("") }

    // Country selections
    var selectedCountryCellDispatch by remember { mutableStateOf(Countries.list.firstOrNull { it.code == "+1" } ?: Countries.list[0]) }
    var selectedCountryBusinessPhone by remember { mutableStateOf(Countries.list.firstOrNull { it.code == "+1" } ?: Countries.list[0]) }
    var selectedCountryFax by remember { mutableStateOf(Countries.list.firstOrNull { it.code == "+1" } ?: Countries.list[0]) }

    // --- Prefill Logic ---
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.let { prefill ->
            if (companyName.isEmpty()) companyName = prefill.companyName ?: ""
            if (dispatchEmail.isEmpty()) dispatchEmail = prefill.dispatchEmail ?: ""
            if (doingBusinessAs.isEmpty()) doingBusinessAs = prefill.doingBusinessAs ?: ""
            if (cellDispatch.isEmpty()) cellDispatch = prefill.dispatchPhoneNumber ?: ""
            if (businessPhone.isEmpty()) businessPhone = prefill.businessTelephone ?: ""
            if (fax.isEmpty()) fax = prefill.faxNumber ?: ""

            // Set countries based on ISD codes
            prefill.dispatchIsd?.let { isd ->
                Countries.list.find { it.code == isd }?.let { selectedCountryCellDispatch = it }
            }
            prefill.businessTelephoneIsd?.let { isd ->
                Countries.list.find { it.code == isd }?.let { selectedCountryBusinessPhone = it }
            }
            prefill.faxIsd?.let { isd ->
                Countries.list.find { it.code == isd }?.let { selectedCountryFax = it }
            }
        }
    }

    // Fetch step data on load
    LaunchedEffect(Unit) {
        viewModel.fetchCompanyInfoStep()
    }

    // Handle success navigation
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onNext(uiState.nextStep)
        }
    }

    // --- Reset Logic ---
    fun onReset() {
        companyName = ""
        dispatchEmail = ""
        doingBusinessAs = ""
        hasEditedDoingBusinessAs = false
        cellDispatch = ""
        businessPhone = ""
        fax = ""
        // Optional: Reset countries to default if needed
        val defaultCountry = Countries.list.firstOrNull { it.code == "+1" } ?: Countries.list[0]
        selectedCountryCellDispatch = defaultCountry
        selectedCountryBusinessPhone = defaultCountry
        selectedCountryFax = defaultCountry
    }

    // --- Main Layout ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Company information",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.LimoBlack
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Let us know about your company details",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = AppColors.LimoBlack.copy(alpha = 0.6f)
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Scrollable Form
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            // Company Name
            CommonTextField(
                label = "YOUR COMPANY NAME",
                placeholder = "Eg.Toyota",
                text = companyName,
                onValueChange = {
                    companyName = it
                    if (!hasEditedDoingBusinessAs) {
                        doingBusinessAs = it
                    }
                },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Dispatch Email
            CommonTextField(
                label = "24 HR DISPATCH EMAIL",
                placeholder = "Eg.Toyota", // Matching the screenshot placeholder exactly
                text = dispatchEmail,
                onValueChange = { dispatchEmail = it },
                isRequired = true,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Doing Business As
            CommonTextField(
                label = "DOING BUSINESS AS?",
                placeholder = "-",
                text = doingBusinessAs,
                onValueChange = {
                    doingBusinessAs = it
                    hasEditedDoingBusinessAs = true
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 24 HR Cell Dispatch
            PhoneInputField(
                label = "24 HR CELL DISPATCH",
                phone = cellDispatch,
                onPhoneChange = { cellDispatch = it },
                selectedCountry = selectedCountryCellDispatch,
                onCountryChange = { selectedCountryCellDispatch = it },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Business Telephone (No Asterisk per screenshot)
            PhoneInputField(
                label = "BUSINESS TELEPHONE",
                phone = businessPhone,
                onPhoneChange = { businessPhone = it },
                selectedCountry = selectedCountryBusinessPhone,
                onCountryChange = { selectedCountryBusinessPhone = it },
                isRequired = false // Matches design
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Fax (Optional)
            PhoneInputField(
                label = "FAX",
                phone = fax,
                onPhoneChange = { fax = it },
                selectedCountry = selectedCountryFax,
                onCountryChange = { selectedCountryFax = it },
                isRequired = false
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- Bottom Action Bar ---
        BottomActionBar(
            showBackButton = true, // Flag to hide/show back button
            onBack = onBack,
            onReset = { onReset() },
            onNext = {
                // Validation
                if (companyName.isBlank()) return@BottomActionBar
                if (dispatchEmail.isBlank() || !isValidEmail(dispatchEmail)) return@BottomActionBar

                // Validate Dispatch Phone (Required)
                val dispatchDigits = cellDispatch.filter { it.isDigit() }
                if (dispatchDigits.length != selectedCountryCellDispatch.phoneLength) return@BottomActionBar

                // Validate Business Phone (If entered)
                if (businessPhone.isNotBlank()) {
                    val busDigits = businessPhone.filter { it.isDigit() }
                    if (busDigits.length != selectedCountryBusinessPhone.phoneLength) return@BottomActionBar
                }

                // Validate Fax (If entered)
                if (fax.isNotBlank()) {
                    val faxDigits = fax.filter { it.isDigit() }
                    if (faxDigits.length != selectedCountryFax.phoneLength) return@BottomActionBar
                }

                val request = CompanyInfoRequest(
                    companyName = companyName.trim(),
                    dispatchEmail = dispatchEmail.trim(),
                    doingBusinessAs = doingBusinessAs.trim(),
                    dispatchIsd = selectedCountryCellDispatch.code,
                    dispatchPhoneNumber = cellDispatch.trim(),
                    dispatchCountry = selectedCountryCellDispatch.shortCode,
                    businessTelephone = businessPhone.trim(),
                    businessTelephoneIsd = selectedCountryBusinessPhone.code,
                    businessTelephoneCountry = selectedCountryBusinessPhone.shortCode,
                    faxIsd = if (fax.isNotEmpty()) selectedCountryFax.code else null,
                    faxNumber = if (fax.isNotEmpty()) fax.trim() else null,
                    faxCountry = if (fax.isNotEmpty()) selectedCountryFax.shortCode else null
                )

                viewModel.completeCompanyInfo(request)
            },
            isLoading = uiState.isLoading
        )
    }
}

/**
 * Updated Bottom Action Bar
 * Supports hiding back button via [showBackButton] flag.
 * Layout: [Back (Circle)] [Reset (Pill)] [Next (Pill)]
 */
@Composable
fun BottomActionBar(
    showBackButton: Boolean = true,
    onBack: (() -> Unit)?,
    onReset: () -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Back Button (Conditional)
        if (showBackButton && onBack != null) {
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
            Spacer(modifier = Modifier.width(16.dp))
        }

        // 2. Reset Button
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

        // 3. Next Button
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

private fun isValidEmail(email: String): Boolean {
    val emailPattern = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    )
    return emailPattern.matcher(email).matches()
}