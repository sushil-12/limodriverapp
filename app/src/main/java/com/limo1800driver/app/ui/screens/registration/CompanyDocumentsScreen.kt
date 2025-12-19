package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.registration.CompanyDocumentsRequest
import com.limo1800driver.app.data.model.registration.Language
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.CustomMultiSelectDropdown
import com.limo1800driver.app.ui.components.camera.DocumentCameraScreen
import com.limo1800driver.app.ui.components.camera.DocumentSide
import com.limo1800driver.app.ui.components.camera.DocumentType
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.CompanyDocumentsViewModel
import kotlinx.coroutines.launch

@Composable
fun CompanyDocumentsScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: CompanyDocumentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // --- Form State ---
    var regNo by remember { mutableStateOf("") }

    // Multi-select Lists
    var selectedLanguages by remember { mutableStateOf<List<Language>>(emptyList()) }
    var selectedOrganisationTypes by remember { mutableStateOf<List<String>>(emptyList()) }

    // Error state variables
    var languagesError by remember { mutableStateOf<String?>(null) }
    var frontImageError by remember { mutableStateOf<String?>(null) }

    // Validation function
    fun validateField(fieldName: String, value: String): String? {
        // Transport Authority Reg No is optional, no validation needed
        return null
    }

    // Dropdown visibility toggles
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showOrganisationDropdown by remember { mutableStateOf(false) }

    // --- Image State ---
    var frontImage by remember { mutableStateOf<Bitmap?>(null) }
    var backImage by remember { mutableStateOf<Bitmap?>(null) }
    var permitImage by remember { mutableStateOf<Bitmap?>(null) }

    // IDs (Uploaded)
    var frontImageId by remember { mutableStateOf<Int?>(null) }
    var backImageId by remember { mutableStateOf<Int?>(null) }
    var permitImageId by remember { mutableStateOf<Int?>(null) }

    // Camera / Upload State
    var showFrontCamera by remember { mutableStateOf(false) }
    var showBackCamera by remember { mutableStateOf(false) }
    var showPermitCamera by remember { mutableStateOf(false) }
    var isUploadingFront by remember { mutableStateOf(false) }
    var isUploadingBack by remember { mutableStateOf(false) }
    var isUploadingPermit by remember { mutableStateOf(false) }

    // API Error state
    var apiError by remember { mutableStateOf<String?>(null) }

    // Fetch initial data
    LaunchedEffect(Unit) {
        viewModel.fetchCompanyDocumentsStep()
        viewModel.fetchLanguages()
        viewModel.fetchOrganisationTypes()
    }

    // Prefill logic
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.let { prefill ->
            if (regNo.isEmpty()) regNo = prefill.transportAuthorityRegNo ?: ""
        }
    }

    // Default English selection
    LaunchedEffect(uiState.languages) {
        if (uiState.languages.isNotEmpty() && selectedLanguages.isEmpty()) {
            // Find English language and select it by default
            val englishLanguage = uiState.languages.find { it.name.equals("English", ignoreCase = true) }
            englishLanguage?.let {
                selectedLanguages = listOf(it)
            }
        }
    }

    // Success Navigation - Only for API completion calls (when step wasn't already completed)
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onNext(uiState.nextStep)
        }
    }

    // Handle API Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            apiError = error
        }
    }

    // --- Reset Logic ---
    fun onReset() {
        regNo = ""
        selectedLanguages = emptyList()
        selectedOrganisationTypes = emptyList()
        frontImage = null
        backImage = null
        permitImage = null
        frontImageId = null
        backImageId = null
        permitImageId = null
        languagesError = null
        frontImageError = null
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
                text = "Company information",
                style = AppTextStyles.phoneEntryHeadline.copy(color = AppColors.LimoBlack)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Let us know about your company details",
                style = AppTextStyles.phoneEntryDescription.copy(color = AppColors.LimoBlack.copy(alpha = 0.6f))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Error Display
        apiError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        style = TextStyle(fontSize = 14.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Scrollable Form ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {

            // 1. Language Selection
            CustomMultiSelectDropdown(
                label = "CHOOSE LANGUAGE(S)",
                placeholder = "English",
                items = uiState.languages,
                selectedItems = selectedLanguages,
                itemLabel = { it.name },
                onItemSelected = { lang ->
                    if (!selectedLanguages.any { it.id == lang.id }) {
                        selectedLanguages = selectedLanguages + lang
                        languagesError = null
                        apiError = null
                    }
                },
                onItemRemoved = { lang ->
                    selectedLanguages = selectedLanguages.filter { it.id != lang.id }
                    languagesError = if (selectedLanguages.isEmpty()) "Please select at least one language" else null
                    apiError = null
                },
                isExpanded = showLanguageDropdown,
                onToggleExpand = { showLanguageDropdown = !showLanguageDropdown },
                isRequired = true,
                errorMessage = languagesError
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Transport Reg No
            CommonTextField(
                label = "TRANSPORTATION AUTHORITY REG. NO.",
                placeholder = "",
                text = regNo,
                onValueChange = {
                    regNo = it
                    apiError = null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))
            // 3. Organisation Types
            CustomMultiSelectDropdown(
                label = "BELONGS TO ANY PROFESSIONAL ORGANIZATIONS?",
                placeholder = "Select",
                items = uiState.organisationTypes,
                selectedItems = selectedOrganisationTypes.mapNotNull { id -> uiState.organisationTypes.find { it.id.toString() == id } },
                itemLabel = { it.name },
                onItemSelected = { org ->
                    val orgId = org.id.toString()
                    if (!selectedOrganisationTypes.contains(orgId)) {
                        selectedOrganisationTypes = selectedOrganisationTypes + orgId
                        apiError = null
                    }
                },
                onItemRemoved = { org ->
                    val orgId = org.id.toString()
                    selectedOrganisationTypes = selectedOrganisationTypes.filter { it != orgId }
                    apiError = null
                },
                isExpanded = showOrganisationDropdown,
                onToggleExpand = { showOrganisationDropdown = !showOrganisationDropdown }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Business Card Upload Section
            Text(
                text = "Upload Business Card",
                style = AppTextStyles.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = AppColors.LimoBlack
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Front Image
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FRONT SIDE",
                            style = AppTextStyles.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        )
                        Text(" *", color = Color.Red, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ImageUploadCard(
                        imageBitmap = frontImage,
                        placeholderResId = R.drawable.back_image,
                        isUploading = isUploadingFront,
                        onAddClick = { showFrontCamera = true }
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
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                }

                // Back Image
                Column(modifier = Modifier.weight(1f)) {
                    // Wrapped in Row to match Front Image structure for perfect alignment
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                        Text(
                            text = "BACK SIDE (OPTIONAL)",
                            style = AppTextStyles.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ImageUploadCard(
                        imageBitmap = backImage,
                        placeholderResId = R.drawable.back_image,
                        isUploading = isUploadingBack,
                        onAddClick = { showBackCamera = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Permit Image
            Column {
                // Header matching Business Card style
                Text(
                    text = "Upload Permit Image (Optional)",
                    style = AppTextStyles.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.LimoBlack
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Sub-label
                Text(
                    text = "PERMIT IMAGE",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                ImageUploadCard(
                    imageBitmap = permitImage,
                    placeholderResId = R.drawable.back_image,
                    isUploading = isUploadingPermit,
                    onAddClick = { showPermitCamera = true },
                    modifier = Modifier.width(160.dp) // Maintain consistent width
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- Bottom Bar ---
        BottomActionBar(
            showBackButton = true,
            onBack = onBack,
            onReset = { onReset() },
            onNext = {
                // DEBUG: Log that onNext was called
                android.util.Log.d("CompanyDocumentsScreen", "onNext called - isCompleted: ${uiState.isCompleted}")

                // Clear previous errors
                languagesError = null
                frontImageError = null
                apiError = null

                // Validation Logic
                var hasErrors = false

                // Validate Languages
                if (selectedLanguages.isEmpty()) {
                    languagesError = "Please select at least one language"
                    hasErrors = true
                }

                // Validate Front Image
                if (frontImageId == null) {
                    frontImageError = "Front side business card image is required"
                    hasErrors = true
                }

                // DEBUG: Log validation results
                android.util.Log.d("CompanyDocumentsScreen", "Validation - hasErrors: $hasErrors, languages: ${selectedLanguages.size}, frontImageId: $frontImageId")

                // If there are errors, don't proceed
                if (hasErrors) {
                    android.util.Log.d("CompanyDocumentsScreen", "Validation failed, not proceeding")
                    return@BottomActionBar
                }

                // Always make API call to save/update data, regardless of completion status
                // This ensures data is saved even if step was previously completed
                android.util.Log.d("CompanyDocumentsScreen", "Making API call to save company documents")

                val languageIds = selectedLanguages.map { it.id.toString() }
                val request = CompanyDocumentsRequest(
                    language = languageIds,
                    transportAuthorityRegNo = regNo.trim(),
                    businessFrontPhoto = frontImageId ?: 0,
                    businessBackPhoto = backImageId ?: 0,
                    permitImage = permitImageId ?: 0,
                    organisationType = selectedOrganisationTypes
                )

                viewModel.completeCompanyDocuments(request)
            },
            isLoading = uiState.isLoading || isUploadingFront || isUploadingBack || isUploadingPermit
        )
    }

    // --- Camera Screens ---
    if (showFrontCamera) {
        DocumentCameraScreen(
            documentType = DocumentType.BUSINESS_CARD,
            side = DocumentSide.FRONT,
            onImageCaptured = { bitmap ->
                bitmap?.let {
                    frontImage = it
                    isUploadingFront = true
                    scope.launch {
                        viewModel.uploadImage(it).fold(
                            onSuccess = { id ->
                                frontImageId = id
                                isUploadingFront = false
                                frontImageError = null  // Clear error on successful upload
                            },
                            onFailure = { isUploadingFront = false; frontImage = null }
                        )
                    }
                }
                showFrontCamera = false
            },
            onDismiss = { showFrontCamera = false }
        )
    }

    if (showBackCamera) {
        DocumentCameraScreen(
            documentType = DocumentType.BUSINESS_CARD,
            side = DocumentSide.BACK,
            onImageCaptured = { bitmap ->
                bitmap?.let {
                    backImage = it
                    isUploadingBack = true
                    scope.launch {
                        viewModel.uploadImage(it).fold(
                            onSuccess = { id -> backImageId = id; isUploadingBack = false },
                            onFailure = { isUploadingBack = false; backImage = null }
                        )
                    }
                }
                showBackCamera = false
            },
            onDismiss = { showBackCamera = false }
        )
    }

    if (showPermitCamera) {
        DocumentCameraScreen(
            documentType = DocumentType.VEHICLE_INSURANCE,
            side = DocumentSide.FRONT,
            onImageCaptured = { bitmap ->
                bitmap?.let {
                    permitImage = it
                    isUploadingPermit = true
                    scope.launch {
                        viewModel.uploadImage(it).fold(
                            onSuccess = { id -> permitImageId = id; isUploadingPermit = false },
                            onFailure = { isUploadingPermit = false; permitImage = null }
                        )
                    }
                }
                showPermitCamera = false
            },
            onDismiss = { showPermitCamera = false }
        )
    }
}

// --- Helper Components ---

@Composable
fun ImageUploadCard(
    imageBitmap: Bitmap?,
    placeholderResId: Int,
    isUploading: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3F4F6)) // Light Gray Background
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = AppColors.LimoBlack)
            } else if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap.asImageBitmap(),
                    contentDescription = "Captured Document",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = placeholderResId),
                    contentDescription = "Placeholder",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add Button (Black Pill)
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.LimoBlack),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

