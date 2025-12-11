package com.limo1800driver.app.ui.screens.registration

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.theme.AppColors // Ensure defined
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsImageUploadViewModel
import com.limo1800driver.app.ui.viewmodel.VehicleImageState
import com.limo1800driver.app.util.ImageUtils // Helper to get bytes from Uri

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleDetailsImageUploadScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleDetailsImageUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Image Picking Logic
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var activeSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && activeSlotIndex != null && tempImageUri != null) {
            val bytes = ImageUtils.uriToBytes(context, tempImageUri!!)
            if (bytes != null) {
                viewModel.uploadImage(activeSlotIndex!!, tempImageUri!!, bytes)
            }
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && activeSlotIndex != null) {
            val bytes = ImageUtils.uriToBytes(context, uri)
            if (bytes != null) {
                viewModel.uploadImage(activeSlotIndex!!, uri, bytes)
            }
        }
    }

    Scaffold(
        topBar = { RegistrationTopBar(onBack = onBack) },
        bottomBar = {
            BottomActionBar(
                isLoading = uiState.isSubmitting,
                onBack = onBack,
                onNext = { viewModel.submit(onSuccess = onNext) },
                nextButtonText = if (uiState.isSubmitting) "Submitting..." else "Submit"
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // --- Header ---
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Enter your vehicle details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Special Amenities ---
            SectionHeader(title = "CHOOSE SPECIAL AMENITIES", isRequired = true)
            Text(
                text = "(Choose Each Special Amenity You Supply Or Provide)",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )

            BorderedGridContainer {
                if (uiState.isLoadingConfig) {
                    ShimmerLoadingGrid()
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.specialAmenitiesOptions.forEach { option ->
                            val isSelected = viewModel.selectedSpecialAmenities.contains(option.name)
                            AmenitiesChip(
                                text = option.name,
                                isSelected = isSelected,
                                onClick = { viewModel.toggleSpecialAmenity(option.name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Vehicle Interiors ---
            SectionHeader(title = "CHOOSE INTERIORS", isRequired = true)
            Text(
                text = "(Choose Each Interior Option For This Vehicle)",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )

            BorderedGridContainer {
                if (uiState.isLoadingConfig) {
                    ShimmerLoadingGrid()
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.interiorOptions.forEach { option ->
                            val isSelected = viewModel.selectedInteriors.contains(option.name)
                            AmenitiesChip(
                                text = option.name,
                                isSelected = isSelected,
                                onClick = { viewModel.toggleInterior(option.name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Image Instructions ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Exterior and Interior",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "(Exterior And Interior –  Photo Tip – Take Interior Pix On Cloudy Days)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Upload the pictures of Interior and Exterior of the vehicle",
                    fontSize = 13.sp,
                    color = Color(0xFFF57F20) // Limo Orange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "These are sample images as a guide to good picture taking. Sell your unique features that travelers and party goers want!",
                    fontSize = 12.sp,
                    color = Color(0xFFF57F20)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Image Grid ---
            // Manual 2-column layout to avoid nested scroll issues
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val imageSlots = viewModel.imageSlots
                for (i in 0 until 6 step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column
                        VehicleImageCard(
                            index = i,
                            state = imageSlots[i],
                            modifier = Modifier.weight(1f),
                            onTap = {
                                activeSlotIndex = i
                                showSourceDialog = true
                            }
                        )
                        // Right Column
                        VehicleImageCard(
                            index = i + 1,
                            state = imageSlots[i+1],
                            modifier = Modifier.weight(1f),
                            onTap = {
                                activeSlotIndex = i + 1
                                showSourceDialog = true
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Source Selection Dialog (Action Sheet equivalent)
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Select Image Source") },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Cancel") }
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showSourceDialog = false
                            tempImageUri = ImageUtils.createTempUri(context)
                            cameraLauncher.launch(tempImageUri!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Camera", color = Color.Black)
                    }
                    Divider()
                    TextButton(
                        onClick = {
                            showSourceDialog = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gallery", color = Color.Black)
                    }
                }
            }
        )
    }

    // Error Handling
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error ?: "Unknown Error") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }
}

// --- Helper Components ---

@Composable
fun SectionHeader(title: String, isRequired: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        if (isRequired) {
            Text(
                text = " *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Red
            )
        }
    }
}

@Composable
fun BorderedGridContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        content()
    }
}

@Composable
fun AmenitiesChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) Color.Black else Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

@Composable
fun VehicleImageCard(
    index: Int,
    state: VehicleImageState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Label
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("VEHICLE IMAGE ${index + 1}", fontSize = 10.sp, color = Color.Black)
            if (index == 0) {
                Text(" *", fontSize = 10.sp, color = Color.Red)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Image Slot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F7)) // System Gray 6 equivalent
                .clickable { onTap() }
        ) {
            if (state.uri != null) {
                AsyncImage(
                    model = state.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Uploading Overlay
                if (state.isUploading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else {
                // Placeholder Image
                // Replace "R.drawable.vehicle_placeholder" with your actual resource
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Icon(
                         painter = painterResource(android.R.drawable.ic_menu_camera), 
                         contentDescription = null,
                         tint = Color.Gray
                     )
                 }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Add Button (Pill shape)
        Button(
            onClick = onTap,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add", fontSize = 12.sp, color = Color.White)
        }
    }
}

// Simple Shimmer Effect for Loading
@Composable
fun ShimmerLoadingGrid() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )
    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(120.dp, 26.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                Box(modifier = Modifier.size(120.dp, 26.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            }
        }
    }
}