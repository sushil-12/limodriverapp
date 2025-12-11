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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.camera.DocumentCameraScreen
import com.limo1800driver.app.ui.components.camera.DocumentSide
import com.limo1800driver.app.ui.components.camera.DocumentType
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsImageUploadViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleDetailsImageUploadScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleDetailsImageUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showCamera by remember { mutableStateOf(false) }
    var activeSlotIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(uiState.success) {
        if (uiState.success) onNext()
    }

    // Error Handling
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Leave space for bottom bar
        ) {
            RegistrationTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Enter your vehicle details",
                    style = AppTextStyles.phoneEntryHeadline.copy(color = Color.Black, fontSize = 24.sp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 1. Special Amenities ---
                SectionHeader(title = "CHOOSE SPECIAL AMENITIES")
                Text(
                    text = "(Choose Each Special Amenity You Supply Or Provide)",
                    style = AppTextStyles.bodyMedium.copy(color = Color.Gray, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                BorderedGrid {
                    if (uiState.specialAmenitiesOptions.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.specialAmenitiesOptions.forEach { item ->
                                val isSelected = viewModel.selectedSpecialAmenities.contains(item.id.toString())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleSpecialAmenity(item.id.toString()) },
                                    label = { Text(item.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.LimoOrange.copy(alpha = 0.2f),
                                        selectedLabelColor = AppColors.LimoOrange
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. Interiors ---
                SectionHeader(title = "CHOOSE INTERIORS", isRequired = true)
                Text(
                    text = "(Choose Each Interior Option For This Vehicle)",
                    style = AppTextStyles.bodyMedium.copy(color = Color.Gray, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                BorderedGrid {
                    if (uiState.interiorOptions.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.interiorOptions.forEach { item ->
                                val isSelected = viewModel.selectedInteriors.contains(item.id.toString())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleInterior(item.id.toString()) },
                                    label = { Text(item.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.LimoOrange.copy(alpha = 0.2f),
                                        selectedLabelColor = AppColors.LimoOrange
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- 3. Image Upload Grid ---
                Text(
                    text = "Exterior and Interior",
                    style = AppTextStyles.headlineMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
                Text(
                    text = "Upload the pictures of Interior and Exterior of the vehicle",
                    style = AppTextStyles.bodyMedium.copy(color = AppColors.LimoOrange, fontSize = 12.sp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (row in 0 until 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Left Column
                            val idx1 = row * 2
                            VehicleImageItem(
                                index = idx1,
                                bitmap = uiState.displayedImages[idx1],
                                isUploaded = uiState.uploadedImageIds.containsKey(idx1),
                                modifier = Modifier.weight(1f),
                                onAddClick = {
                                    activeSlotIndex = idx1
                                    showCamera = true
                                }
                            )
                            
                            // Right Column
                            val idx2 = row * 2 + 1
                            VehicleImageItem(
                                index = idx2,
                                bitmap = uiState.displayedImages[idx2],
                                isUploaded = uiState.uploadedImageIds.containsKey(idx2),
                                modifier = Modifier.weight(1f),
                                onAddClick = {
                                    activeSlotIndex = idx2
                                    showCamera = true
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Bottom Bar
        BottomActionBar(
            isLoading = uiState.isLoading,
            onBack = onBack,
            onNext = { viewModel.submitFinalDetails() },
            nextButtonText = "Submit",
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showCamera) {
        DocumentCameraScreen(
            documentType = DocumentType.VEHICLE_INSURANCE,
            side = DocumentSide.FRONT,
            onImageCaptured = { bitmap ->
                bitmap?.let { viewModel.uploadImage(activeSlotIndex, it) }
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
    }
}

@Composable
fun VehicleImageItem(
    index: Int,
    bitmap: Bitmap?,
    isUploaded: Boolean,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VEHICLE IMAGE ${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            if (index == 0) Text(" *", color = Color.Red, fontSize = 10.sp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3F4F6))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isUploaded) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Uploaded",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(AppColors.LimoOrange, RoundedCornerShape(50))
                            .padding(2.dp)
                            .size(12.dp)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppColors.LimoOrange)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                    Text("Add", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Text("Add", fontSize = 12.sp)
        }
    }
}