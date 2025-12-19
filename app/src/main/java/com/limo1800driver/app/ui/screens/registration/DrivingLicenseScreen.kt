package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.registration.DrivingLicenseRequest
import com.limo1800driver.app.data.model.registration.OptionalCertificationRequest
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.components.camera.DocumentCameraScreen
import com.limo1800driver.app.ui.components.camera.DocumentSide
import com.limo1800driver.app.ui.components.camera.DocumentType
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.viewmodel.DrivingLicenseViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Brand Colors ---
val BrandBlack = Color(0xFF1A1A1A)
val BrandOrange = Color(0xFFF28B2F)
val LightGrayBg = Color(0xFFF5F5F5)
val TextGray = Color(0xFF666666)

enum class CertificationType(val displayName: String) {
    VETERAN("Veteran"),
    DOD_CLEARANCE("DoD Clearance"),
    FOID_CARD("FOID Card"),
    CHILD_SCHOOL_BUS("Child/School Bus"),
    BACKGROUND_CERTIFIED("Background Certified"),
    EX_POLICE("Ex/Police")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverLicenseFormScreen(
    onBack: () -> Unit,
    onNext: (String?) -> Unit,
    viewModel: DrivingLicenseViewModel = hiltViewModel()
) {
    val registrationNavigationState = remember { RegistrationNavigationState() }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // --- State ---
    var licenseNumber by remember { mutableStateOf("") }

    // DOB State
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<String?>(null) }

    // Image State
    var frontImage by remember { mutableStateOf<Bitmap?>(null) }
    var backImage by remember { mutableStateOf<Bitmap?>(null) }
    var frontImageId by remember { mutableStateOf<Int?>(null) }
    var backImageId by remember { mutableStateOf<Int?>(null) }
    var isUploadingFront by remember { mutableStateOf(false) }
    var isUploadingBack by remember { mutableStateOf(false) }

    // Certification State
    val certificationImages = remember { mutableStateMapOf<CertificationType, Bitmap?>() }
    val certificationImageIds = remember { mutableStateMapOf<CertificationType, Int?>() }
    val certificationUploading = remember {
        mutableStateMapOf<CertificationType, Boolean>().apply {
            CertificationType.values().forEach { put(it, false) }
        }
    }
    val selectedCertifications = remember {
        mutableStateMapOf<CertificationType, Boolean>().apply {
            CertificationType.values().forEach { put(it, false) }
        }
    }

    // Camera State
    var showCamera by remember { mutableStateOf(false) }
    var activeSide by remember { mutableStateOf(DocumentSide.FRONT) }
    var activeDocumentType by remember { mutableStateOf(DocumentType.DRIVING_LICENSE) }
    var activeCertification by remember { mutableStateOf<CertificationType?>(null) }

    // Error handling
    var showErrorDialog by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    // Field-specific error states
    var licenseNumberError by remember { mutableStateOf<String?>(null) }
    var frontImageError by remember { mutableStateOf<String?>(null) }
    var certificationError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Validation function
    fun validateField(fieldName: String, value: String): String? {
        return when (fieldName) {
            "licenseNumber" -> when {
                value.isBlank() -> "License number is required"
                value.length < 5 -> "License number must be at least 5 characters"
                else -> null
            }
            else -> null
        }
    }

    // Prefill: fetch on load
    LaunchedEffect(Unit) {
        viewModel.fetchDrivingLicenseStep()
    }

    // Navigate on success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            registrationNavigationState.setNextStep(uiState.nextStep)
            onNext(uiState.nextStep)
        }
    }

    // Apply prefill data
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.let { prefill ->
            prefill.licenseNumber?.let { licenseNumber = it.uppercase() }
            prefill.licenceFrontPhoto?.let { photo ->
                frontImageId = photo.id
                photo.url?.let { url ->
                    loadBitmapFromUrl(url)?.let { frontImage = it }
                }
            }
            prefill.licenceBackPhoto?.let { photo ->
                backImageId = photo.id
                photo.url?.let { url ->
                    loadBitmapFromUrl(url)?.let { backImage = it }
                }
            }
            prefill.optionalCertification?.let { certs ->
                android.util.Log.d("DrivingLicenseScreen", "Prefilling certifications: $certs")
                suspend fun setCert(cert: CertificationType, imageData: com.limo1800driver.app.data.model.registration.ImageData?) {
                    if (imageData?.id != null) {
                        selectedCertifications[cert] = true
                        certificationImageIds[cert] = imageData.id
                        android.util.Log.d("DrivingLicenseScreen", "Prefilled ${cert.displayName} with ID: ${imageData.id}")
                        imageData.url?.let { url ->
                            loadBitmapFromUrl(url)?.let { certificationImages[cert] = it }
                        }
                    }
                }
                setCert(CertificationType.VETERAN, certs.veteran)
                setCert(CertificationType.DOD_CLEARANCE, certs.dodClearance)
                setCert(CertificationType.FOID_CARD, certs.foidCard)
                setCert(CertificationType.CHILD_SCHOOL_BUS, certs.childSchoolBus)
                setCert(CertificationType.BACKGROUND_CERTIFIED, certs.backgroundCertified)
                setCert(CertificationType.EX_POLICE, certs.exPolice)
            }
        }
    }

    // Handle API Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            apiError = error
            // Clear field-specific errors when we have an API error
            licenseNumberError = null
            frontImageError = null
            certificationError = null
        }
    }

    // --- Logic ---
    if (showCamera) {
        DocumentCameraScreen(
            documentType = DocumentType.DRIVING_LICENSE,
            side = activeSide,
            onImageCaptured = { bitmap ->
                bitmap?.let {
                    when (activeDocumentType) {
                        DocumentType.DRIVING_LICENSE -> {
                            if (activeSide == DocumentSide.FRONT) {
                                frontImage = it
                                isUploadingFront = true
                                scope.launch {
                                    viewModel.uploadImage(it).fold(
                                        onSuccess = { id -> frontImageId = id },
                                        onFailure = { _ -> frontImage = null }
                                    )
                                    isUploadingFront = false
                                }
                            } else {
                                backImage = it
                                isUploadingBack = true
                                scope.launch {
                                    viewModel.uploadImage(it).fold(
                                        onSuccess = { id -> backImageId = id },
                                        onFailure = { _ -> backImage = null }
                                    )
                                    isUploadingBack = false
                                }
                            }
                        }
                        DocumentType.CERTIFICATION -> {
                            activeCertification?.let { cert ->
                                certificationImages[cert] = it
                                certificationUploading[cert] = true
                                scope.launch {
                                    viewModel.uploadImage(it).fold(
                                        onSuccess = { id ->
                                            certificationImageIds[cert] = id
                                            android.util.Log.d("DrivingLicenseScreen", "Uploaded image for ${cert.displayName}, ID: $id")
                                        },
                                        onFailure = { _ ->
                                            certificationImages[cert] = null
                                            certificationImageIds[cert] = null
                                            android.util.Log.d("DrivingLicenseScreen", "Failed to upload image for ${cert.displayName}")
                                        }
                                    )
                                    certificationUploading[cert] = false
                                }
                            }
                        }
                        else -> Unit
                    }
                }
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
    } else {
        // --- REPLACED SCAFFOLD WITH COLUMN + WEIGHT + SURFACE ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .imePadding()
            // Do not apply windowInsetsPadding(NavigationBars) here
            // We want the white background to go behind the bottom nav bar
        ) {
            RegistrationTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // API Error Display
                apiError?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF2F2)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
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

                // Screen Title
                Text(
                    text = "Enter your license number and upload clear image",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlack,
                    lineHeight = 32.sp
                )

                // License Number Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelText(text = "LICENSE NUMBER *")
                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = {
                            licenseNumber = it.uppercase()
                            licenseNumberError = validateField("licenseNumber", it)
                            apiError = null
                        },
                        placeholder = { Text("DL 0000000000000", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = LightGrayBg,
                            focusedContainerColor = LightGrayBg,
                            disabledContainerColor = LightGrayBg,
                            cursorColor = BrandBlack,
                            focusedBorderColor = BrandOrange,
                            unfocusedBorderColor = if (licenseNumberError != null) Color(0xFFEF4444) else Color.Transparent,
                            errorBorderColor = Color(0xFFEF4444),
                        ),
                        isError = licenseNumberError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Characters
                        )
                    )

                    // License Number Error Message
                    licenseNumberError?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }

                // Upload Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Upload driver's license",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlack
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Front Side
                        UploadCard(
                            label = "FRONT SIDE *",
                            bitmap = frontImage,
                            isUploading = isUploadingFront,
                            isError = frontImageError != null,
                            modifier = Modifier.weight(1f),
                            onAddClick = {
                                activeDocumentType = DocumentType.DRIVING_LICENSE
                                activeSide = DocumentSide.FRONT
                                showCamera = true
                                apiError = null
                            }
                        )

                        // Front Image Error Message
                        frontImageError?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Normal
                            )
                        )
                        }

                        // Back Side
                        UploadCard(
                            label = "BACK SIDE (OPTIONAL)",
                            bitmap = backImage,
                            isUploading = isUploadingBack,
                            modifier = Modifier.weight(1f),
                            onAddClick = {
                                activeDocumentType = DocumentType.DRIVING_LICENSE
                                activeSide = DocumentSide.BACK
                                showCamera = true
                            }
                        )
                    }
                }

                // --- Additional Certifications Section ---
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Additional Certifications (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlack
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 0.dp, max = 220.dp)
                    ) {
                        items(CertificationType.values()) { cert ->
                            CertificationChip(
                                certification = cert,
                                isSelected = selectedCertifications[cert] == true,
                                onToggle = {
                                    val current = selectedCertifications[cert] ?: false
                                    selectedCertifications[cert] = !current
                                    android.util.Log.d("DrivingLicenseScreen", "Toggled ${cert.displayName}: $current -> ${!current}")
                                    if (current) {
                                        certificationImages.remove(cert)
                                        certificationImageIds.remove(cert)
                                        certificationUploading[cert] = false
                                        android.util.Log.d("DrivingLicenseScreen", "Cleared data for ${cert.displayName}")
                                    }
                                }
                            )
                        }
                    }

                    if (selectedCertifications.values.any { it }) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            selectedCertifications.filter { it.value }.keys.forEach { cert ->
                                UploadCard(
                                    label = "${cert.displayName.uppercase()} (OPTIONAL)",
                                    bitmap = certificationImages[cert],
                                    isUploading = certificationUploading[cert] == true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onAddClick = {
                                        activeDocumentType = DocumentType.CERTIFICATION
                                        activeCertification = cert
                                        activeSide = DocumentSide.FRONT
                                        showCamera = true
                                    }
                                )
                            }
                        }
                    }

                    // Certification Error Message
                    certificationError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
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
                }

                // Add padding at the bottom of scroll content so the last item isn't flush with the button
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- CUSTOM BOTTOM BAR ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 10.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .navigationBarsPadding() // Handles safe area inside the white surface
                ) {

                    Button(
                        onClick = {
                        // Clear previous errors
                        licenseNumberError = null
                        frontImageError = null
                        certificationError = null
                        apiError = null

                        // Validation Logic
                        var hasErrors = false

                        // Validate License Number
                        if (licenseNumber.isBlank()) {
                            licenseNumberError = "License number is required"
                            hasErrors = true
                        } else if (licenseNumber.length < 5) {
                            licenseNumberError = "License number must be at least 5 characters"
                            hasErrors = true
                        }

                        // Validate Front Image
                        if (frontImageId == null) {
                            frontImageError = "Front side license image is required"
                            hasErrors = true
                        }

                        // Validate Certifications - check if selected certs have images
                        android.util.Log.d("DrivingLicenseScreen", "Selected certifications: $selectedCertifications")
                        android.util.Log.d("DrivingLicenseScreen", "Certification image IDs: $certificationImageIds")

                        val selectedCertsWithoutImages = selectedCertifications.filter { (type, isSelected) ->
                            isSelected && certificationImageIds[type] == null
                        }

                        android.util.Log.d("DrivingLicenseScreen", "Selected certs without images: $selectedCertsWithoutImages")

                        if (selectedCertsWithoutImages.isNotEmpty()) {
                            val certNames = selectedCertsWithoutImages.keys.joinToString(", ") { it.displayName }
                            certificationError = "Please upload images for selected certifications: $certNames"
                            hasErrors = true
                            android.util.Log.d("DrivingLicenseScreen", "Setting certification error: $certificationError")
                        }

                        // Prevent API call if there are validation errors
                        if (hasErrors) {
                            android.util.Log.d("DrivingLicenseScreen", "Validation failed, not making API call")
                            return@Button
                        }

                        // Make API call to save/update data
                        android.util.Log.d("DrivingLicenseScreen", "Validation passed, making API call")

                            val expiryDate = if (!selectedDay.isNullOrBlank() && !selectedMonth.isNullOrBlank() && !selectedYear.isNullOrBlank()) {
                                "${selectedYear}-${selectedMonth}-${selectedDay}"
                            } else {
                                null
                            }

                            val optionalCerts = if (selectedCertifications.values.any { it }) {
                                OptionalCertificationRequest(
                                    veteran = certificationImageIds[CertificationType.VETERAN],
                                    dodClearance = certificationImageIds[CertificationType.DOD_CLEARANCE],
                                    foidCard = certificationImageIds[CertificationType.FOID_CARD],
                                    childSchoolBus = certificationImageIds[CertificationType.CHILD_SCHOOL_BUS],
                                    backgroundCertified = certificationImageIds[CertificationType.BACKGROUND_CERTIFIED],
                                    exPolice = certificationImageIds[CertificationType.EX_POLICE]
                                )
                            } else null

                            val request = DrivingLicenseRequest(
                                licenceFrontPhoto = frontImageId,
                                licenceBackPhoto = backImageId,
                                licenseNumber = licenseNumber.trim(),
                                expiryDate = expiryDate,
                                optionalCertification = optionalCerts
                            )
                            viewModel.completeDrivingLicense(request)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE89148), // Brand Orange
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !uiState.isLoading && !isUploadingFront && !isUploadingBack
                    ) {
                        if (uiState.isLoading || isUploadingFront || isUploadingBack) {
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
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                localErrorMessage = null
                viewModel.clearError()
            },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    localErrorMessage = null
                    viewModel.clearError()
                }) {
                    Text("OK")
                }
            },
            title = { Text("Error") },
            text = { Text(localErrorMessage ?: uiState.error ?: "Something went wrong") }
        )
    }
}

// Simple network bitmap loader for prefill images
private suspend fun loadBitmapFromUrl(url: String): Bitmap? {
    return withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val connection = java.net.URL(url).openConnection()
            connection.connect()
            val input = connection.getInputStream()
            android.graphics.BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextGray,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun DropdownSelector(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LightGrayBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (text.length > 2) BrandBlack else Color.Gray,
                fontSize = 16.sp
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun UploadCard(
    label: String,
    bitmap: Bitmap?,
    isUploading: Boolean = false,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        // Card Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.58f) // Standard ID Aspect Ratio
                .clip(RoundedCornerShape(12.dp))
                .background(LightGrayBg)
                .border(
                    width = 1.dp,
                    color = if (isError) Color(0xFFEF4444) else Color.LightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                ShimmerCircle(
                    size = 32.dp,
                    strokeWidth = 3.dp
                )
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured ID",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "SAMPLE",
                        color = Color.LightGray.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer(rotationZ = -15f)
                    )
                }
            }
        }

        // Black Pill "Add" Button
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlack),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (bitmap == null) "Add" else "Retake",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CertificationChip(
    certification: CertificationType,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandOrange.copy(alpha = 0.1f) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) BrandOrange else Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = certification.displayName,
                color = if (isSelected) BrandBlack else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(id = android.R.drawable.checkbox_on_background),
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}