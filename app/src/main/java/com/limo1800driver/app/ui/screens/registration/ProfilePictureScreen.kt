package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.limo1800driver.app.data.model.registration.ProfilePictureRequest
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.camera.ProfileCameraScreen
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.ProfilePictureViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfilePictureScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfilePictureViewModel = hiltViewModel()
) {
    val registrationNavigationState = remember { RegistrationNavigationState() }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // State variables
    var profileImage by remember { mutableStateOf<Bitmap?>(null) }
    var profileImageId by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchProfilePictureStep()
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            registrationNavigationState.setNextStep(uiState.nextStep)
            onNext(uiState.nextStep)
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
                    Button(
                        onClick = {
                            if (profileImageId == null) {
                                showCamera = true
                            } else {
                                if (uiState.isCompleted) {
                                    registrationNavigationState.setNextStep("vehicle_details")
                                    onNext("vehicle_details")
                                } else {
                                    val request = ProfilePictureRequest(profileImage = profileImageId!!)
                                    viewModel.completeProfilePicture(request)
                                }
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
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !uiState.isLoading && !isUploading
                    ) {
                        if (uiState.isLoading || isUploading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (profileImageId == null) "Take Photo" else "Submit",
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AppColors.LimoBlack
                        )
                    }
                    profileImage != null -> {
                        Image(
                            bitmap = profileImage!!.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.1f)),
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