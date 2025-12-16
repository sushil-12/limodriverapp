package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
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
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
import java.util.Calendar

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
            
            // Insurance Company Name
            CommonTextField(
                label = "NAME OF THE INSURANCE COMPANY",
                placeholder = "",
                text = insuranceCompanyName,
                onValueChange = { insuranceCompanyName = it },
                isRequired = true
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
                onValueChange = { policyNumber = it },
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Agent Phone
            CommonTextField(
                label = "AGENCY/INSURANCE TELEPHONE",
                placeholder = "",
                text = agentPhone,
                onValueChange = { agentPhone = it },
                isRequired = true
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
            
            // Policy Expiry Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommonDropdown(
                    label = "POLICY EXPIRY",
                    placeholder = "MM",
                    selectedValue = expiryMonth,
                    options = months,
                    onValueSelected = { expiryMonth = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
                CommonDropdown(
                    label = "",
                    placeholder = "DD",
                    selectedValue = expiryDay,
                    options = days,
                    onValueSelected = { expiryDay = it },
                    modifier = Modifier.weight(1f)
                )
                CommonDropdown(
                    label = "",
                    placeholder = "YYYY",
                    selectedValue = expiryYear,
                    options = years,
                    onValueSelected = { expiryYear = it },
                    modifier = Modifier.weight(1f),
                    isRequired = true
                )
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Insurance Limit
            CommonDropdown(
                label = "INSURANCE LIMIT",
                placeholder = "Select",
                selectedValue = insuranceLimit,
                options = limits,
                onValueSelected = { insuranceLimit = it },
                isRequired = true
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
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Bottom Bar (match BankDetails styling)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    // Validation
                    if (insuranceCompanyName.trim().isEmpty()) return@Button
                    if (policyNumber.trim().isEmpty()) return@Button
                    if (agentPhone.trim().isEmpty()) return@Button
                    if (expiryMonth == null || expiryYear == null) return@Button
                    if (insuranceLimit == null) return@Button
                    if (insuranceImageId == null) return@Button
                    
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

