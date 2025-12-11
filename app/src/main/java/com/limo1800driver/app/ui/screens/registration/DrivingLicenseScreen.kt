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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.registration.DrivingLicenseRequest
import com.limo1800driver.app.data.model.registration.OptionalCertificationRequest
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
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
                suspend fun setCert(cert: CertificationType, imageData: com.limo1800driver.app.data.model.registration.ImageData?) {
                    if (imageData?.id != null) {
                        selectedCertifications[cert] = true
                        certificationImageIds[cert] = imageData.id
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

    // Surface errors
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            showErrorDialog = true
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
                                        onSuccess = { id -> certificationImageIds[cert] = id },
                                        onFailure = { _ ->
                                            certificationImages[cert] = null
                                            certificationImageIds[cert] = null
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
        Scaffold(
            containerColor = Color.White,
            topBar = { RegistrationTopBar(onBack = onBack) },
            bottomBar = {
                BottomActionBar(
                    isLoading = uiState.isLoading || isUploadingFront || isUploadingBack,
                    onBack = null,
                    onNext = {
                        // Validation logic
                        localErrorMessage = when {
                            licenseNumber.isBlank() -> "Please enter your license number."
                            frontImageId == null -> "Please upload the front side of your license."
                            else -> null
                        }

                        // Ensure selected certifications have uploads
                        val missingCerts = selectedCertifications
                            .filter { it.value && (certificationImageIds[it.key] == null) }
                            .keys
                        if (missingCerts.isNotEmpty()) {
                            localErrorMessage = "Please upload images for: ${
                                missingCerts.joinToString { it.displayName }
                            }"
                        }

                        if (localErrorMessage != null) {
                            showErrorDialog = true
                            return@BottomActionBar
                        }

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
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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
                        onValueChange = { licenseNumber = it.uppercase() },
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
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Characters
                        )
                    )
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
                            modifier = Modifier.weight(1f),
                            onAddClick = {
                                activeDocumentType = DocumentType.DRIVING_LICENSE
                                activeSide = DocumentSide.FRONT
                                showCamera = true
                            }
                        )

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
                                    if (current) {
                                        certificationImages.remove(cert)
                                        certificationImageIds.remove(cert)
                                        certificationUploading[cert] = false
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
                }
                // Extra space at bottom to ensure content doesn't sit behind bottom bar
                Spacer(modifier = Modifier.height(20.dp))
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
                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = BrandBlack,
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
                // Placeholder graphic similar to screenshot
                Box(modifier = Modifier.fillMaxSize()) {
                    // In a real app, use: Image(painter = painterResource(R.drawable.sample_id_bg)...)
                    // Here we mimic the "Sample" text
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
            shape = CircleShape, // Fully rounded pill shape
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