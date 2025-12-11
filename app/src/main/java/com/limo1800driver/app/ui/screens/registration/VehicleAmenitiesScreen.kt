package com.limo1800driver.app.ui.screens.registration

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.Amenity
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.RegistrationTopBar
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
            BottomActionBar(
                isLoading = uiState.isLoading,
                onBack = onBack,
                onNext = { viewModel.saveAmenities(onSuccess = onNext) },
                nextButtonText = "Next"
            )
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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

// --- Components ---

@Composable
fun SectionHeader(title: String, isRequired: Boolean, extraText: String? = null, extraColor: Color = Color.Black) {
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
fun BorderedGrid(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(12.dp),
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmenitiesGrid(
    items: List<Amenity>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = selectedIds.contains(item.id.toString())
            val bgColor = if (isSelected) AppColors.LimoOrange.copy(alpha = 0.15f) else AppColors.LimoOrange.copy(alpha = 0.05f)
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable { onToggle(item.id.toString()) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom Checkbox appearance
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Check, // Or use a square outline for unselected
                    contentDescription = null,
                    tint = if (isSelected) AppColors.LimoOrange else Color.Transparent, // Hide check if not selected
                    modifier = Modifier.size(16.dp).border(1.dp, if(isSelected) AppColors.LimoOrange else Color.Gray, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.name, 
                    style = AppTextStyles.bodyMedium.copy(fontSize = 13.sp),
                    color = Color.Black
                )
            }
        }
    }
}