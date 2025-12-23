package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.VehicleInsuranceRequest
import coil.compose.AsyncImage
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.DatePickerComponent
import com.limo1800driver.app.ui.components.DatePickerConfig
import com.limo1800driver.app.ui.components.DateStep
import com.limo1800driver.app.ui.components.camera.DocumentCameraScreen
import com.limo1800driver.app.ui.components.camera.DocumentSide
import com.limo1800driver.app.ui.components.camera.DocumentType
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.VehicleInsuranceViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
import java.util.Calendar

// Enum to track which step of the date selection is active
enum class DateStep { MONTH, DAY, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInsuranceScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleInsuranceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val registrationNavigationState = remember { RegistrationNavigationState() }

    var insuranceCompanyName by remember { mutableStateOf("") }
    var agencyName by remember { mutableStateOf("") }
    var policyNumber by remember { mutableStateOf("") }
    var agentPhone by remember { mutableStateOf("") }
    var agentEmail by remember { mutableStateOf("") }
    var insuranceLimit by remember { mutableStateOf<String?>(null) }
    var expiryMonth by remember { mutableStateOf<String?>(null) }
    var expiryDay by remember { mutableStateOf<String?>(null) }
    var expiryYear by remember { mutableStateOf<String?>(null) }


    var insuranceImage by remember { mutableStateOf<Bitmap?>(null) }
    var insuranceImageId by remember { mutableStateOf<Int?>(null) }
    var insuranceImageUrl by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // Error states
    var insuranceCompanyNameError by remember { mutableStateOf<String?>(null) }
    var policyNumberError by remember { mutableStateOf<String?>(null) }
    var agentPhoneError by remember { mutableStateOf<String?>(null) }
    var expiryDateError by remember { mutableStateOf<String?>(null) }
    var insuranceLimitError by remember { mutableStateOf<String?>(null) }
    var insuranceImageError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    val months = (1..12).map { String.format("%02d", it) }
    val days = (1..31).map { String.format("%02d", it) }
    val years = (2024..2035).map { it.toString() }
    val limits = listOf("$500,000", "$1,000,000", "$1,500,000", "$5,000,000")
    val defaultAgentIsd = "+1"
    val defaultAgentCountry = "US"

    // Fetch step data on load
    LaunchedEffect(Unit) {
        viewModel.fetchVehicleInsuranceStep()
    }

    // Prefill data
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.let { prefill ->
            if (insuranceCompanyName.isEmpty()) insuranceCompanyName = prefill.nameOfInsuranceCompany ?: ""
            if (agencyName.isEmpty()) agencyName = prefill.agencyName ?: ""
            if (policyNumber.isEmpty()) policyNumber = prefill.insurancePolicyNumber ?: ""
            if (agentPhone.isEmpty()) agentPhone = prefill.agentTelephoneNumber ?: ""
            if (agentEmail.isEmpty()) agentEmail = prefill.agentEmail ?: ""
            if (insuranceLimit == null) {
                fun normLimit(s: String?): String = s.orEmpty().replace(Regex("[^0-9]"), "")
                val raw = prefill.insuranceLimit
                insuranceLimit = limits.firstOrNull { normLimit(it) == normLimit(raw) } ?: raw
            }
            // Parse expiry date if present
            prefill.policyExpiryDate?.let { dateStr ->
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    expiryYear = parts[0]
                    expiryMonth = parts[1]
                    expiryDay = parts[2].trim().split(" ").firstOrNull()
                }
            }

            // Prefill insurance document (existing uploaded image)
            if (insuranceImageId == null) {
                insuranceImageId =
                    prefill.insuranceFrontPhoto?.id?.toIntOrNull()
                        ?: prefill.insurancePolicyFrontPhoto?.toIntOrNull()
            }
            if (insuranceImageUrl.isNullOrBlank()) {
                insuranceImageUrl = prefill.insuranceFrontPhoto?.url
            }
        }
    }

    // Handle success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            registrationNavigationState.setNextStep(uiState.nextStep)
            onNext(uiState.nextStep)
            viewModel.consumeSuccess()
        }
    }

    // Validation function
    fun validateField(fieldName: String, value: String): String? {
        return when (fieldName) {
            "insuranceCompanyName" -> when {
                value.trim().isEmpty() -> "Insurance company name is required"
                value.trim().length < 2 -> "Insurance company name must be at least 2 characters"
                else -> null
            }
            "policyNumber" -> when {
                value.trim().isEmpty() -> "Policy number is required"
                value.trim().length < 3 -> "Policy number must be at least 3 characters"
                else -> null
            }
            "agentPhone" -> when {
                value.trim().isEmpty() -> "Agent phone number is required"
                !value.matches(Regex("^\\+?[0-9\\s\\-\\(\\)]{10,}$")) -> "Please enter a valid phone number"
                else -> null
            }
            else -> null
        }
    }

    // Handle API Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            apiError = error
            // Clear field-specific errors when we have an API error
            insuranceCompanyNameError = null
            policyNumberError = null
            agentPhoneError = null
            expiryDateError = null
            insuranceLimitError = null
            insuranceImageError = null
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
                text = "Enter your vehicle insurance details",
                style = AppTextStyles.phoneEntryHeadline.copy(
                    color = AppColors.LimoBlack,
                    fontSize = 24.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

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

            // Insurance Company Name
            CommonTextField(
                label = "NAME OF THE INSURANCE COMPANY",
                placeholder = "",
                text = insuranceCompanyName,
                onValueChange = {
                    insuranceCompanyName = it
                    insuranceCompanyNameError = validateField("insuranceCompanyName", it)
                    apiError = null
                },
                isRequired = true,
                errorMessage = insuranceCompanyNameError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Agency Name
            CommonTextField(
                label = "Agent NAME",
                placeholder = "",
                text = agencyName,
                onValueChange = { agencyName = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Policy Number
            CommonTextField(
                label = "POLICY NUMBER",
                placeholder = "",
                text = policyNumber,
                onValueChange = {
                    policyNumber = it
                    policyNumberError = validateField("policyNumber", it)
                    apiError = null
                },
                isRequired = true,
                errorMessage = policyNumberError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Agent Phone
            CommonTextField(
                label = "AGENCY/INSURANCE TELEPHONE",
                placeholder = "",
                text = agentPhone,
                onValueChange = {
                    agentPhone = it
                    agentPhoneError = validateField("agentPhone", it)
                    apiError = null
                },
                isRequired = true,
                errorMessage = agentPhoneError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Agent Email
            CommonTextField(
                label = "INSURANCE / AGENT EMAIL",
                placeholder = "",
                text = agentEmail,
                onValueChange = { agentEmail = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- POLICY EXPIRY DATE SECTION ---
            DatePickerComponent(
                label = "POLICY EXPIRY",
                selectedMonth = expiryMonth,
                selectedDay = expiryDay,
                selectedYear = expiryYear,
                onDateSelected = { month, day, year ->
                    expiryMonth = month
                    expiryDay = day
                    expiryYear = year
                    expiryDateError = if (month.isNotEmpty() && day.isNotEmpty() && year.isNotEmpty()) null else "Please select your complete policy expiry date"
                    apiError = null
                },
                config = DatePickerConfig(
                    allowFutureDates = true, // Policy expiry can be in future
                    allowPastDates = false, // But not in past
                    yearRange = Calendar.getInstance().get(Calendar.YEAR)..(Calendar.getInstance().get(Calendar.YEAR) + 10) // Next 10 years
                ),
                errorMessage = expiryDateError,
                isRequired = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Insurance Limit
            CommonDropdown(
                label = "INSURANCE LIMIT",
                placeholder = "Select",
                selectedValue = insuranceLimit,
                options = limits,
                onValueSelected = {
                    insuranceLimit = it
                    insuranceLimitError = if (it.isNullOrBlank()) "Insurance limit is required" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = insuranceLimitError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Insurance Document Image
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "INSURANCE DOCUMENT",
                        style = AppTextStyles.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text("*", color = Color.Red, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCamera = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail / Placeholder
                            Box(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    insuranceImage != null -> {
                                        Image(
                                            bitmap = insuranceImage!!.asImageBitmap(),
                                            contentDescription = "Insurance Document",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    !insuranceImageUrl.isNullOrBlank() -> {
                                        AsyncImage(
                                            model = insuranceImageUrl,
                                            contentDescription = "Insurance Document",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Insurance document",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                when {
                                    isUploading -> {
                                        Text(
                                            text = "Uploading…",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    insuranceImageId != null || !insuranceImageUrl.isNullOrBlank() -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Uploaded",
                                                fontSize = 12.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = "Add a photo of your insurance card (Camera or Gallery)",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            // Change action
                            TextButton(
                                onClick = { showCamera = true },
                                enabled = !isUploading
                            ) {
                                Text(
                                    text = if (insuranceImageId != null || !insuranceImageUrl.isNullOrBlank()) "Change" else "Add",
                                    color = AppColors.LimoOrange
                                )
                            }
                        }

                        if (isUploading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = AppColors.LimoOrange
                            )
                        }
                    }
                }
            }

            // Insurance Image Error Message
            insuranceImageError?.let {
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

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Bar (match BankDetails styling)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    // Clear previous errors
                    insuranceCompanyNameError = null
                    policyNumberError = null
                    agentPhoneError = null
                    expiryDateError = null
                    insuranceLimitError = null
                    insuranceImageError = null
                    apiError = null

                    // Validation Logic
                    var hasErrors = false

                    // Validate Insurance Company Name
                    if (insuranceCompanyName.trim().isEmpty()) {
                        insuranceCompanyNameError = "Insurance company name is required"
                        hasErrors = true
                    } else if (insuranceCompanyName.trim().length < 2) {
                        insuranceCompanyNameError = "Insurance company name must be at least 2 characters"
                        hasErrors = true
                    }

                    // Validate Policy Number
                    if (policyNumber.trim().isEmpty()) {
                        policyNumberError = "Policy number is required"
                        hasErrors = true
                    } else if (policyNumber.trim().length < 3) {
                        policyNumberError = "Policy number must be at least 3 characters"
                        hasErrors = true
                    }

                    // Validate Agent Phone
                    if (agentPhone.trim().isEmpty()) {
                        agentPhoneError = "Agent phone number is required"
                        hasErrors = true
                    } else if (!agentPhone.matches(Regex("^\\+?[0-9\\s\\-\\(\\)]{10,}$"))) {
                        agentPhoneError = "Please enter a valid phone number"
                        hasErrors = true
                    }

                    // Validate Expiry Date
                    if (expiryMonth.isNullOrBlank() || expiryDay.isNullOrBlank() || expiryYear.isNullOrBlank()) {
                        expiryDateError = "Please select your complete policy expiry date"
                        hasErrors = true
                    }

                    // Validate Insurance Limit
                    if (insuranceLimit.isNullOrBlank()) {
                        insuranceLimitError = "Insurance limit is required"
                        hasErrors = true
                    }

                    // Validate Insurance Image
                    if (insuranceImageId == null) {
                        insuranceImageError = "Insurance document is required"
                        hasErrors = true
                    }

                    // Only make API call if all validations pass
                    if (!hasErrors) {
                        val expiryDate = "$expiryYear-${expiryMonth}-${expiryDay ?: "01"} 00:00:00"

                        val request = VehicleInsuranceRequest(
                            nameOfInsuranceCompany = insuranceCompanyName.trim(),
                            agencyName = agencyName.trim().takeIf { it.isNotEmpty() },
                            insurancePolicyNumber = policyNumber.trim(),
                            agentTelephoneNumber = agentPhone.trim(),
                            agentTelephoneIsd = defaultAgentIsd,
                            agentTelephoneCountry = defaultAgentCountry,
                            agentEmail = agentEmail.trim().takeIf { it.isNotEmpty() },
                            insuranceLimit = insuranceLimit ?: "",
                            policyExpiryDate = expiryDate,
                            insurancePolicyFrontPhoto = insuranceImageId ?: 0
                        )

                        viewModel.completeVehicleInsurance(request)
                    }
                },
                enabled = !(uiState.isLoading || isUploading),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE89148),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (uiState.isLoading || isUploading) "Submitting..." else "Submit",
                    style = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }

    // Camera Screen
    if (showCamera) {
        DocumentCameraScreen(
            documentType = DocumentType.VEHICLE_INSURANCE,
            side = DocumentSide.FRONT,
            onImageCaptured = { bitmap ->
                bitmap?.let {
                    insuranceImage = it
                    isUploading = true
                    scope.launch {
                        val result = viewModel.uploadImage(it)
                        result.fold(
                            onSuccess = { imageId ->
                                insuranceImageId = imageId
                                isUploading = false
                            },
                            onFailure = { error ->
                                isUploading = false
                                insuranceImage = null
                            }
                        )
                    }
                }
                showCamera = false
            },
            onDismiss = { showCamera = false }
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


