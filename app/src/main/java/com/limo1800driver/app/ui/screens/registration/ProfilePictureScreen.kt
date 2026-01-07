package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.limo1800driver.app.data.model.registration.ProfilePictureRequest
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.components.camera.ProfileCameraScreen
import com.limo1800driver.app.ui.components.FullScreenImagePreview
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.ProfilePictureViewModel
import com.limo1800driver.app.ui.components.ErrorAlertDialog
import kotlinx.coroutines.launch

@Composable
fun ProfilePictureScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    onUpdateComplete: (() -> Unit)? = null,
    viewModel: ProfilePictureViewModel = hiltViewModel()
) {
    val registrationNavigationState = remember { RegistrationNavigationState() }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // State variables
    var profileImage by remember { mutableStateOf<Bitmap?>(null) }
    var profileImageId by remember { mutableStateOf<String?>(null) }
    var existingProfileImageUrl by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Image Preview State
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchProfilePictureStep()
    }

    // Extract existing profile image URL when prefill data is loaded
    LaunchedEffect(uiState.prefillData) {
        uiState.prefillData?.profileImage?.let { profileImageData ->
            existingProfileImageUrl = profileImageData.url
            profileImageId = profileImageData.id
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            if (isEditMode) {
                // In edit mode, just call the update complete callback
                onUpdateComplete?.invoke()
            } else {
                registrationNavigationState.setNextStep(uiState.nextStep)
                onNext(uiState.nextStep)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            showErrorDialog = true
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = { RegistrationTopBar(onBack = onBack) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 10.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                ) {
                    if (profileImageId == null) {
                        // Single button when no image is captured
                        Button(
                            onClick = { showCamera = true },
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
                            enabled = !uiState.isLoading && !isUploading
                        ) {
                            if (uiState.isLoading || isUploading) {
                                ShimmerCircle(size = 24.dp)
                            } else {
                                Text(
                                    text = "Take Photo",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    } else {
                        // Two buttons when image is captured
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Retake button
                            OutlinedButton(
                                onClick = {
                                    profileImage = null
                                    profileImageId = null
                                    existingProfileImageUrl = null
                                    showCamera = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFE89148),
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = Color(0xFFE89148).copy(alpha = 0.5f)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = 1.5.dp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                enabled = !uiState.isLoading && !isUploading
                            ) {
                                Text(
                                    text = "Retake",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE89148)
                                    )
                                )
                            }

                            // Submit button
                            Button(
                                onClick = {
                                    if (isEditMode) {
                                        // In edit mode, always update the profile picture
                                        val request = ProfilePictureRequest(profileImage = profileImageId!!)
                                        viewModel.completeProfilePicture(request)
                                    } else if (uiState.isCompleted) {
                                        registrationNavigationState.setNextStep("vehicle_details")
                                        onNext("vehicle_details")
                                    } else {
                                        val request = ProfilePictureRequest(profileImage = profileImageId!!)
                                        viewModel.completeProfilePicture(request)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE89148), // Brand Orange
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f),
                                    disabledContentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                enabled = !uiState.isLoading && !isUploading
                            ) {
                                if (uiState.isLoading) {
                                    ShimmerCircle(size = 24.dp)
                                } else {
                                    Text(
                                        text = "Submit",
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Take your profile photo",
                style = AppTextStyles.phoneEntryHeadline.copy(
                    color = AppColors.LimoBlack,
                    fontSize = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your profile photo helps people recognize you. Please note that once you submit your profile photo it can only be changed in limited circumstances.",
                style = AppTextStyles.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InstructionText(text = "1. Use a clear, front-facing photo with good lighting and no sunglasses or masks.")
            InstructionText(text = "2. Upload high-quality images only (JPG, PNG formats; min 300x300 px recommended).")
            InstructionText(text = "3. Make sure your face is centered and fills most of the frame for easy recognition.")

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> {
                        ShimmerCircle(size = 32.dp)
                    }
                    profileImage != null -> {
                        Image(
                            bitmap = profileImage!!.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.1f))
                                .clickable {
                                    previewImageBitmap = profileImage
                                    previewImageUrl = null
                                    showImagePreview = true
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                    existingProfileImageUrl != null -> {
                        AsyncImage(
                            model = existingProfileImageUrl,
                            contentDescription = "Existing Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.1f))
                                .clickable {
                                    previewImageBitmap = null
                                    previewImageUrl = existingProfileImageUrl
                                    showImagePreview = true
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No photo",
                                style = AppTextStyles.bodyMedium.copy(color = Color.Gray)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCamera) {
        ProfileCameraScreen(
            onImageCaptured = { bitmap ->
                bitmap?.let {
                    profileImage = it
                    isUploading = true
                    scope.launch {
                        val result = viewModel.uploadImage(it)
                        result.fold(
                            onSuccess = { imageUrl ->
                                profileImageId = imageUrl
                                isUploading = false
                            },
                            onFailure = {
                                isUploading = false
                                profileImage = null
                            }
                        )
                    }
                }
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
    }

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
        contentDescription = "Full screen profile picture preview"
    )

    // Error Dialog
    ErrorAlertDialog(
        isVisible = showErrorDialog && uiState.error != null,
        onDismiss = {
            showErrorDialog = false
            viewModel.clearError()
        },
        title = "Error",
        message = uiState.error ?: "Unknown error"
    )
}

@Composable
private fun InstructionText(text: String) {
    Text(
        text = text,
        style = AppTextStyles.bodyMedium.copy(
            fontSize = 14.sp,
            color = Color.Gray
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}