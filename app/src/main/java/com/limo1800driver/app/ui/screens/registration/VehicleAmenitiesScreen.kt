package com.limo1800driver.app.ui.screens.registration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.Amenity
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.theme.AppTextStyles
import com.limo1800driver.app.ui.viewmodel.VehicleAmenitiesViewModel

@Composable
fun VehicleAmenitiesScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleAmenitiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Error Dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK", color = AppColors.LimoOrange) }
            }
        )
    }

    Scaffold(
        topBar = { RegistrationTopBar(onBack = onBack) },
        bottomBar = {
            // --- CUSTOM BOTTOM BAR ---
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
                        onClick = { viewModel.saveStep2AndNavigate(onSuccess = onNext) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE89148),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f),
                            disabledContentColor = Color.White
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
                                text = "Next",
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
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter your vehicle details",
                style = AppTextStyles.phoneEntryHeadline.copy(color = Color.Black, fontSize = 24.sp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- Chargeable ---
            SectionHeader(
                title = "CHOOSE CHARGEABLE AMENITIES",
                isRequired = true,
                extraText = " In $",
                extraColor = AppColors.LimoOrange
            )
            Text(
                text = "(Choose Each Chargeable Amenity You Supply Or Provide)",
                style = AppTextStyles.bodyMedium.copy(color = Color.Gray, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            BorderedGrid {
                if (uiState.chargeableAmenities.isEmpty() && uiState.isLoading) {
                    // Improved Shimmer Grid
                    AmenitiesShimmerGrid()
                } else {
                    AmenitiesGrid(
                        items = uiState.chargeableAmenities,
                        selectedIds = viewModel.selectedChargeableIds,
                        onToggle = { viewModel.toggleChargeable(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Non-Chargeable ---
            SectionHeader(title = "CHOOSE NON CHARGEABLE AMENITIES", isRequired = true)
            Text(
                text = "(Choose Each Non Chargeable Amenity You Supply Or Provide)",
                style = AppTextStyles.bodyMedium.copy(color = Color.Gray, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            BorderedGrid {
                if (uiState.nonChargeableAmenities.isEmpty() && uiState.isLoading) {
                    // Improved Shimmer Grid
                    AmenitiesShimmerGrid()
                } else {
                    AmenitiesGrid(
                        items = uiState.nonChargeableAmenities,
                        selectedIds = viewModel.selectedNonChargeableIds,
                        onToggle = { viewModel.toggleNonChargeable(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- Components (Private to prevent conflicts) ---

@Composable
private fun SectionHeader(title: String, isRequired: Boolean, extraText: String? = null, extraColor: Color = Color.Black) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = AppTextStyles.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
        if (isRequired) {
            Text(text = " *", style = AppTextStyles.bodyMedium.copy(color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp))
        }
        if (extraText != null) {
            Text(text = extraText, style = AppTextStyles.bodyMedium.copy(color = extraColor, fontWeight = FontWeight.Bold, fontSize = 13.sp))
        }
    }
}

@Composable
private fun BorderedGrid(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)) // Slightly rounder for modern look
            .padding(16.dp), // Increased padding
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesGrid(
    items: List<Amenity>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val identifier = item.getIdentifier()
            val isSelected = selectedIds.contains(identifier)

            // --- CLEANER CHIP DESIGN ---
            Surface(
                modifier = Modifier
                    .clickable { onToggle(identifier) },
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
                    // Modern selection indicator
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
                        text = item.name,
                        style = AppTextStyles.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) AppColors.LimoOrange else Color.Black
                    )
                }
            }
        }
    }
}

/**
 * Improved Shimmer Grid that looks like actual chips loading
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesShimmerGrid() {
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
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    // Simulate flow row of chips
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Create 8 fake items of varying sizes to look natural
        val sizes = listOf(100.dp, 80.dp, 120.dp, 90.dp, 110.dp, 70.dp, 130.dp, 85.dp)

        sizes.forEach { width ->
            Box(
                modifier = Modifier
                    .width(width)
                    .height(36.dp) // Match Chip Height
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
        }
    }
}