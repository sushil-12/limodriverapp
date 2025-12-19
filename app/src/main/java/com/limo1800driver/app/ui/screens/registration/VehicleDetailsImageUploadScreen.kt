package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.components.camera.DocumentCameraScreen
import com.limo1800driver.app.ui.components.camera.DocumentSide
import com.limo1800driver.app.ui.components.camera.DocumentType
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsImageUploadViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleDetailsImageUploadScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleDetailsImageUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showCamera by remember { mutableStateOf(false) }
    var activeSlotIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onNext(uiState.nextStep)
            viewModel.consumeSuccess()
        }
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

    Box(modifier = Modifier.fillMaxSize().background(LimoWhite)) {
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
                    style = AppTextStyles.phoneEntryHeadline.copy(color = LimoBlack, fontSize = 24.sp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 1. Special Amenities ---
                ImageUploadSectionHeader(title = "CHOOSE SPECIAL AMENITIES", isRequired = true)
                Text(
                    text = "(Choose Each Special Amenity You Supply Or Provide)",
                    style = AppTextStyles.bodyMedium.copy(color = ComposeColor.Gray, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                ImageUploadBorderedGrid {
                    if (uiState.specialAmenitiesOptions.isEmpty() && uiState.isLoading) {
                        ImageUploadShimmerGrid()
                    } else if (uiState.specialAmenitiesOptions.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.specialAmenitiesOptions.forEach { item ->
                                val identifier = item.getIdentifier()
                                val isSelected = viewModel.selectedSpecialAmenities.contains(identifier)

                                CustomChipItem(
                                    text = item.name,
                                    isSelected = isSelected,
                                    onClick = { viewModel.toggleSpecialAmenity(identifier) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. Interiors ---
                ImageUploadSectionHeader(title = "CHOOSE INTERIORS", isRequired = true)
                Text(
                    text = "(Choose Each Interior Option For This Vehicle)",
                    style = AppTextStyles.bodyMedium.copy(color = ComposeColor.Gray, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                ImageUploadBorderedGrid {
                    if (uiState.interiorOptions.isEmpty() && uiState.isLoading) {
                        ImageUploadShimmerGrid()
                    } else if (uiState.interiorOptions.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.interiorOptions.forEach { item ->
                                val identifier = item.getIdentifier()
                                val isSelected = viewModel.selectedInteriors.contains(identifier)

                                CustomChipItem(
                                    text = item.name,
                                    isSelected = isSelected,
                                    onClick = { viewModel.toggleInterior(identifier) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- 3. Image Upload Grid ---
                Text(
                    text = "Exterior and Interior",
                    style = AppTextStyles.headlineMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LimoBlack)
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
                                imageUrl = uiState.displayedImageUrls[idx1],
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
                                imageUrl = uiState.displayedImageUrls[idx2],
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

        // --- CUSTOM BOTTOM BAR ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shadowElevation = 10.dp,
            color = LimoWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {

                Button(
                    onClick = { viewModel.submitFinalDetails() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE89148),
                        contentColor = LimoWhite,
                        disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f),
                        disabledContentColor = LimoWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
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

// --- Components (Private to avoid conflicts) ---

@Composable
private fun CustomChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) AppColors.LimoOrange.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) AppColors.LimoOrange else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AppColors.LimoOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text,
                style = AppTextStyles.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) AppColors.LimoOrange else Color.Black
            )
        }
    }
}

@Composable
private fun ImageUploadSectionHeader(title: String, isRequired: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = AppTextStyles.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
        if (isRequired) {
            Text(text = " *", style = AppTextStyles.bodyMedium.copy(color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp))
        }
    }
}

@Composable
private fun ImageUploadBorderedGrid(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)) // Cleaner, rounder border
            .padding(16.dp), // Increased padding
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageUploadShimmerGrid() {
    val shimmerColors = listOf(
        ComposeColor.LightGray.copy(alpha = 0.6f),
        ComposeColor.LightGray.copy(alpha = 0.2f),
        ComposeColor.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Random widths to simulate real chips loading
        val sizes = listOf(100.dp, 80.dp, 120.dp, 90.dp, 110.dp, 70.dp, 130.dp, 85.dp)

        sizes.forEach { width ->
            Box(
                modifier = Modifier
                    .width(width)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun VehicleImageItem(
    index: Int,
    bitmap: Bitmap?,
    imageUrl: String?,
    isUploaded: Boolean,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VEHICLE IMAGE ${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LimoBlack)
            if (index == 0) Text(" *", color = Color.Red, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF9FAFB)) // Very light gray background
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
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
                        tint = LimoWhite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(AppColors.LimoOrange, RoundedCornerShape(50))
                            .padding(2.dp)
                            .size(12.dp)
                    )
                } else {
                    ShimmerCircle(size = 24.dp)
                }
            } else if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isUploaded) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Uploaded",
                        tint = LimoWhite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(AppColors.LimoOrange, RoundedCornerShape(50))
                            .padding(2.dp)
                            .size(12.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ComposeColor.LightGray
                    )
                    Text("Add", fontSize = 12.sp, color = ComposeColor.LightGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = LimoBlack),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Text("Add", fontSize = 12.sp)
        }
    }
}