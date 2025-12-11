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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.ProfilePictureRequest
import com.limo1800driver.app.ui.components.camera.ProfileCameraScreen
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.ProfilePictureViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar

@Composable
fun ProfilePictureScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfilePictureViewModel = hiltViewModel()
) {
    val registrationNavigationState = remember { RegistrationNavigationState() }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var profileImage by remember { mutableStateOf<Bitmap?>(null) }
    var profileImageId by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    
    // Fetch step data on load
    LaunchedEffect(Unit) {
        viewModel.fetchProfilePictureStep()
    }
    
    // Handle success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            registrationNavigationState.setNextStep(uiState.nextStep)
            onNext(uiState.nextStep)
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
            
            // Instructions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Use a clear, front-facing photo with good lighting and no sunglasses or masks.",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                Text(
                    text = "2. Upload high-quality images only (JPG, PNG formats; min 300x300 px recommended).",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                Text(
                    text = "3. Make sure your face is centered and fills most of the frame for easy recognition.",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Image Preview
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                    profileImage != null -> {
                        Image(
                            bitmap = profileImage!!.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No photo",
                                style = AppTextStyles.bodyMedium.copy(
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom Bar
        BottomActionBar(
            isLoading = uiState.isLoading || isUploading,
            onBack = null,
            onNext = {
                if (profileImageId == null) {
                    showCamera = true
                } else {
                    val request = ProfilePictureRequest(profileImage = profileImageId!!)
                    viewModel.completeProfilePicture(request)
                }
            },
            nextButtonText = "Submit"
        )
    }
    
    // Camera Screen
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
                            onFailure = { error ->
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

