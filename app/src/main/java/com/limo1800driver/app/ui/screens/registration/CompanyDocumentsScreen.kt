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
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.camera.DocumentCameraScreen
import com.limo1800driver.app.ui.components.FullScreenImagePreview
import com.limo1800driver.app.ui.components.camera.DocumentSide
import com.limo1800driver.app.ui.components.camera.DocumentType
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.CompanyDocumentsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Debug

@Composable
fun CompanyDocumentsScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    onUpdateComplete: (() -> Unit)? = null,
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

    // Image Preview State
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // API Error state
    var apiError by remember { mutableStateOf<String?>(null) }

    // Initial Data Fetch
    LaunchedEffect(Unit) {
        // Reset success state when screen loads (important for back navigation)
        viewModel.resetSuccessState()
        viewModel.fetchCompanyDocumentsStep()
        viewModel.fetchLanguages()
        viewModel.fetchOrganisationTypes()
    }

    // Prefill logic - runs when prefill data is available
    LaunchedEffect(uiState.prefillData) {
        android.util.Log.d("CompanyDocumentsScreen", "LaunchedEffect triggered - prefillData: ${uiState.prefillData != null}")
        uiState.prefillData?.let { prefill ->
            android.util.Log.d("CompanyDocumentsScreen", "Prefill data received: $prefill")

            // Transport authority registration number
            if (regNo.isEmpty()) regNo = prefill.transportAuthorityRegNo ?: ""

            // Languages - parse JSON array string (only if languages are loaded)
            if (uiState.languages.isNotEmpty()) {
            prefill.language?.let { languageJson ->
                    android.util.Log.d("CompanyDocumentsScreen", "Processing language JSON: $languageJson")
                try {
                    // Parse JSON array like "[\"1\"]" to get IDs
                    val languageIds = languageJson
                        .removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() }

                        android.util.Log.d("CompanyDocumentsScreen", "Parsed language IDs: $languageIds")

                    // Only set if we don't already have selections (to avoid overriding user changes)
                        if (selectedLanguages.isEmpty()) {
                        val prefillLanguages = uiState.languages.filter { lang ->
                            languageIds.contains(lang.id.toString())
                        }
                            android.util.Log.d("CompanyDocumentsScreen", "Found matching languages: ${prefillLanguages.map { it.id }}")
                        if (prefillLanguages.isNotEmpty()) {
                            selectedLanguages = prefillLanguages
                                android.util.Log.d("CompanyDocumentsScreen", "Set selectedLanguages to: ${selectedLanguages.map { it.id }}")
                            }
                        } else {
                            android.util.Log.d("CompanyDocumentsScreen", "Skipping language prefill - selectedLanguages already set")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CompanyDocumentsScreen", "Failed to parse language JSON: $languageJson", e)
                    }
                }
            } else {
                android.util.Log.d("CompanyDocumentsScreen", "Languages not loaded yet, will retry when available")
            }

            // Organization types - parse JSON array string
            prefill.organisationType?.let { orgJson ->
                try {
                    // Parse JSON array like "[\"1\",\"2\",\"12\"]" to get IDs
                    val orgIds = orgJson
                        .removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() }

                    // Only set if we don't already have selections
                    if (selectedOrganisationTypes.isEmpty()) {
                        selectedOrganisationTypes = orgIds
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CompanyDocumentsScreen", "Failed to parse organisation JSON: $orgJson", e)
                }
            }

            // Photo IDs and load images
            prefill.businessFrontPhoto?.let { photo ->
                android.util.Log.d("CompanyDocumentsScreen", "Loading front image: id=${photo.id}, url=${photo.url}")
                frontImageId = photo.id
                photo.url?.let { url ->
                    loadBitmapFromUrl(url)?.let { bitmap ->
                        frontImage = bitmap
                        android.util.Log.d("CompanyDocumentsScreen", "Front image loaded successfully")
                    } ?: android.util.Log.e("CompanyDocumentsScreen", "Failed to load front image from $url")
                }
            }
            prefill.businessBackPhoto?.let { photo ->
                android.util.Log.d("CompanyDocumentsScreen", "Loading back image: id=${photo.id}, url=${photo.url}")
                backImageId = photo.id
                photo.url?.let { url ->
                    loadBitmapFromUrl(url)?.let { bitmap ->
                        backImage = bitmap
                        android.util.Log.d("CompanyDocumentsScreen", "Back image loaded successfully")
                    } ?: android.util.Log.e("CompanyDocumentsScreen", "Failed to load back image from $url")
                }
            }
            prefill.permitImage?.let { photo ->
                android.util.Log.d("CompanyDocumentsScreen", "Loading permit image: id=${photo.id}, url=${photo.url}")
                permitImageId = photo.id
                photo.url?.let { url ->
                    loadBitmapFromUrl(url)?.let { bitmap ->
                        permitImage = bitmap
                        android.util.Log.d("CompanyDocumentsScreen", "Permit image loaded successfully")
                    } ?: android.util.Log.e("CompanyDocumentsScreen", "Failed to load permit image from $url")
                }
            }
        }
    }

    // Handle language prefill when languages become available (if prefill data was loaded first)
    LaunchedEffect(uiState.languages, uiState.prefillData) {
        if (uiState.languages.isNotEmpty() && uiState.prefillData != null && selectedLanguages.isEmpty()) {
            val prefill = uiState.prefillData!!
            prefill.language?.let { languageJson ->
                android.util.Log.d("CompanyDocumentsScreen", "Processing language JSON (delayed): $languageJson")
                try {
                    // Parse JSON array like "[\"1\"]" to get IDs
                    val languageIds = languageJson
                        .removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() }

                    android.util.Log.d("CompanyDocumentsScreen", "Parsed language IDs (delayed): $languageIds")

                    val prefillLanguages = uiState.languages.filter { lang ->
                        languageIds.contains(lang.id.toString())
                    }
                    android.util.Log.d("CompanyDocumentsScreen", "Found matching languages (delayed): ${prefillLanguages.map { it.id }}")
                    if (prefillLanguages.isNotEmpty()) {
                        selectedLanguages = prefillLanguages
                        android.util.Log.d("CompanyDocumentsScreen", "Set selectedLanguages to (delayed): ${selectedLanguages.map { it.id }}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CompanyDocumentsScreen", "Failed to parse language JSON (delayed): $languageJson", e)
                }
            }
        }
    }

    // Default English selection (only if no prefill or user selection)
    LaunchedEffect(uiState.languages) {
        if (uiState.languages.isNotEmpty() && selectedLanguages.isEmpty() && uiState.prefillData?.language.isNullOrEmpty()) {
            // Find English language and select it by default (only if no prefill data)
            val englishLanguage = uiState.languages.find { it.name.equals("English", ignoreCase = true) }
            englishLanguage?.let {
                selectedLanguages = listOf(it)
            }
        }
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
                    if (selectedLanguages.any { it.id == lang.id }) {
                        // Item is already selected, remove it (deselect)
                        selectedLanguages = selectedLanguages.filter { it.id != lang.id }
                        languagesError = if (selectedLanguages.isEmpty()) "Please select at least one language" else null
                    } else {
                        // Item is not selected, add it (select)
                        selectedLanguages = selectedLanguages + lang
                        languagesError = null
                    }
                    apiError = null
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
                    if (selectedOrganisationTypes.contains(orgId)) {
                        // Item is already selected, remove it (deselect)
                        selectedOrganisationTypes = selectedOrganisationTypes.filter { it != orgId }
                    } else {
                        // Item is not selected, add it (select)
                        selectedOrganisationTypes = selectedOrganisationTypes + orgId
                    }
                    apiError = null
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
                        onAddClick = { showFrontCamera = true },
                        onImageClick = if (frontImage != null) {
                            {
                                previewImageBitmap = frontImage
                                previewImageUrl = null
                                showImagePreview = true
                            }
                        } else null
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
                        onAddClick = { showBackCamera = true },
                        onImageClick = if (backImage != null) {
                            {
                                previewImageBitmap = backImage
                                previewImageUrl = null
                                showImagePreview = true
                            }
                        } else null
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
                    modifier = Modifier.width(160.dp), // Maintain consistent width
                    onImageClick = if (permitImage != null) {
                        {
                            previewImageBitmap = permitImage
                            previewImageUrl = null
                            showImagePreview = true
                        }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- Bottom Bar ---
        BottomActionBar(
            showBackButton = !isEditMode, // Hide back button in edit mode
            onBack = onBack,
            onReset = { onReset() },
            onNext = {
                // DEBUG: Log that onNext was called
                android.util.Log.d("CompanyDocumentsScreen", "onNext called - isCompleted: ${uiState.isCompleted}, isEditMode: $isEditMode")

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

                // Always make API call to save/update data, regardless of completion status or edit mode
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
                
                // In edit mode, navigation is handled by onUpdateComplete callback in LaunchedEffect
                // Don't navigate automatically - let the success handler manage it
            },
            isLoading = uiState.isLoading || isUploadingFront || isUploadingBack || isUploadingPermit,
            isEditMode = isEditMode
        )
    }

    // --- Camera Screens ---
    // Full Screen Image Preview
    FullScreenImagePreview(
        isVisible = showImagePreview,
        onDismiss = {
            showImagePreview = false
            previewImageBitmap = null
            previewImageUrl = null
        },
        imageBitmap = previewImageBitmap,
        imageUrl = previewImageUrl,
        contentDescription = "Full screen document image preview"
    )

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
    modifier: Modifier = Modifier,
    onImageClick: (() -> Unit)? = null
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
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                .then(
                    if (imageBitmap != null && onImageClick != null) {
                        Modifier.clickable { onImageClick() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                ShimmerBox(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp)
                )
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
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.LimoBlack,
                contentColor = Color.White,
                disabledContainerColor = AppColors.LimoBlack.copy(alpha = 0.5f),
                disabledContentColor = Color.White
            ),
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
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

// Simple network bitmap loader for prefill images
private suspend fun loadBitmapFromUrl(url: String): Bitmap? {
    return withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            android.util.Log.d("CompanyDocumentsScreen", "Loading image from URL: $url")
            val connection = java.net.URL(url).openConnection()
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000    // 10 seconds
            connection.connect()
            val input = connection.getInputStream()
            val bitmap = android.graphics.BitmapFactory.decodeStream(input)
            input.close()
            android.util.Log.d("CompanyDocumentsScreen", "Image loaded successfully from $url")
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("CompanyDocumentsScreen", "Failed to load image from $url", e)
            null
        }
    }
}

