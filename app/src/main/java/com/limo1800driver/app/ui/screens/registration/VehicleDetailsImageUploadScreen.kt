package com.limo1800driver.app.ui.screens.registration

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.limo1800driver.app.ui.components.camera.VehicleCameraScreen
import com.limo1800driver.app.R
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsImageUploadViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleDetailsImageUploadScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    // Added: Pass the vehicle type here to determine slots/overlays
    vehicleTypeName: String = "Mid-Size Sedan",
    viewModel: VehicleDetailsImageUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showCamera by remember { mutableStateOf(false) }

    // CHANGED: Track the entire Slot object, not just index, so we can access the overlay ID
    var activeSlot by remember { mutableStateOf<VehicleImageSlot?>(null) }

    // Dynamic Configuration based on Vehicle Type
    val vehicleSlots = remember(vehicleTypeName) {
        VehicleImageConfig.getSlotsForVehicle(vehicleTypeName)
    }

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

                // ==========================================
                // --- SECTION 1: SPECIAL AMENITIES (KEPT) ---
                // ==========================================
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

                // ==========================================
                // --- SECTION 2: INTERIORS (KEPT) ---
                // ==========================================
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

                // ==========================================
                // --- SECTION 3: IMAGES (UPDATED) ---
                // ==========================================
                Text(
                    text = "Exterior and Interior",
                    style = AppTextStyles.headlineMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LimoBlack)
                )
                Text(
                    text = "Upload photos as per the requested angles for better quote accuracy.",
                    style = AppTextStyles.bodyMedium.copy(color = AppColors.LimoOrange, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Grid based on Configuration
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Step 2 to render pairs
                    for (i in vehicleSlots.indices step 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Left Column
                            val slot1 = vehicleSlots[i]
                            VehicleImageItem(
                                slot = slot1,
                                bitmap = uiState.displayedImages[slot1.index],
                                imageUrl = uiState.displayedImageUrls[slot1.index],
                                isUploaded = uiState.uploadedImageIds.containsKey(slot1.index),
                                modifier = Modifier.weight(1f),
                                onAddClick = {
                                    activeSlot = slot1
                                    showCamera = true
                                }
                            )

                            // Right Column (check existence)
                            if (i + 1 < vehicleSlots.size) {
                                val slot2 = vehicleSlots[i + 1]
                                VehicleImageItem(
                                    slot = slot2,
                                    bitmap = uiState.displayedImages[slot2.index],
                                    imageUrl = uiState.displayedImageUrls[slot2.index],
                                    isUploaded = uiState.uploadedImageIds.containsKey(slot2.index),
                                    modifier = Modifier.weight(1f),
                                    onAddClick = {
                                        activeSlot = slot2
                                        showCamera = true
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {

                Button(
                    onClick = {
                        // Optional: Validate mandatory image slots before submitting
                        val missingMandatory = vehicleSlots.any { slot ->
                            slot.isRequired &&
                                    !uiState.uploadedImageIds.containsKey(slot.index)
                        }

                        if (missingMandatory) {
                            // Use viewModel to show error state
                            // viewModel.showError("Please upload all required photos marked with *")
                            return@Button
                        }
                        viewModel.submitFinalDetails()
                    },
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

    if (showCamera && activeSlot != null) {
        VehicleCameraScreen(
            vehicleTypeTitle = vehicleTypeName, // "Mid-Size Sedan"
//            overlayResId = activeSlot!!.overlayResId ?: R.drawable.overlay_van_outline, // Fallback if null
            overlayResId = R.drawable.vehicle_van,
            onImageCaptured = { bitmap ->
                bitmap?.let { viewModel.uploadImage(activeSlot!!.index, it) }
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
    }
}

// --- Components ---

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
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .padding(16.dp),
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

// --- UPDATED ITEM COMPONENT TO ACCEPT SLOT CONFIG ---
@Composable
private fun VehicleImageItem(
    slot: VehicleImageSlot, // Changed from index to Slot Object
    bitmap: Bitmap?,
    imageUrl: String?,
    isUploaded: Boolean,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Label Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = slot.label.uppercase(),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LimoBlack),
                maxLines = 1
            )
            if (slot.isRequired) {
                Text(" *", color = Color.Red, fontSize = 11.sp)
            }
        }

        // Description
        Text(
            text = slot.description,
            style = TextStyle(fontSize = 10.sp, color = Color.Gray),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF9FAFB))
                .border(
                    width = 1.dp,
                    color = if(slot.isRequired && !isUploaded) Color.Red.copy(alpha=0.3f) else Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(12.dp)
                )
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
            Text(if (isUploaded) "Change" else "Add", fontSize = 12.sp)
        }
    }
}