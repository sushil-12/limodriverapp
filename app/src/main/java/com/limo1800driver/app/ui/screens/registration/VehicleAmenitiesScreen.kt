package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.Amenity
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.VehicleAmenitiesViewModel
import androidx.compose.runtime.remember
import com.limo1800driver.app.ui.components.BottomActionBar

@Composable
fun VehicleAmenitiesScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleAmenitiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val registrationNavigationState = remember { RegistrationNavigationState() }
    
    var selectedChargeableAmenities by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedNonChargeableAmenities by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    // Fetch amenities on load
    LaunchedEffect(Unit) {
        viewModel.fetchAmenities()
    }
    
    // Prefill selected amenities
    LaunchedEffect(uiState.chargeableAmenities, uiState.nonChargeableAmenities) {
        // Prefill logic can be added here if needed
    }
    
    // Handle success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            registrationNavigationState.setNextStep(uiState.nextStep)
            onNext()
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Text("←", style = AppTextStyles.bodyLarge)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Choose vehicle amenities",
                style = AppTextStyles.phoneEntryHeadline.copy(
                    color = AppColors.LimoBlack,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chargeable Amenities
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHOOSE CHARGEABLE AMENITIES",
                        style = AppTextStyles.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text("*", color = Color.Red, fontSize = 13.sp)
                    Text(
                        text = " In $",
                        style = AppTextStyles.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.LimoOrange
                        )
                    )
                }
                Text(
                    text = "(Choose Each Chargeable Amenity You Supply Or Provide)",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 12.sp
                    )
                )
                
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(((uiState.chargeableAmenities.size / 2 + 1) * 50).dp)
                    ) {
                        items(uiState.chargeableAmenities) { amenity ->
                            AmenityChip(
                                name = amenity.name,
                                isSelected = selectedChargeableAmenities.contains(amenity.id),
                                onClick = {
                                    selectedChargeableAmenities = if (selectedChargeableAmenities.contains(amenity.id)) {
                                        selectedChargeableAmenities - amenity.id
                                    } else {
                                        selectedChargeableAmenities + amenity.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Non-Chargeable Amenities
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHOOSE NON CHARGEABLE AMENITIES",
                        style = AppTextStyles.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text("*", color = Color.Red, fontSize = 13.sp)
                }
                Text(
                    text = "(Choose Each Non Chargeable Amenity You Supply Or Provide)",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 12.sp
                    )
                )
                
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(((uiState.nonChargeableAmenities.size / 2 + 1) * 50).dp)
                    ) {
                        items(uiState.nonChargeableAmenities) { amenity ->
                            AmenityChip(
                                name = amenity.name,
                                isSelected = selectedNonChargeableAmenities.contains(amenity.id),
                                onClick = {
                                    selectedNonChargeableAmenities = if (selectedNonChargeableAmenities.contains(amenity.id)) {
                                        selectedNonChargeableAmenities - amenity.id
                                    } else {
                                        selectedNonChargeableAmenities + amenity.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Bottom Bar
        BottomActionBar(
            isLoading = uiState.isLoading,
            onBack = onBack,
            onNext = {
                // Validation
                if (selectedChargeableAmenities.isEmpty()) {
                    return@BottomActionBar
                }
                if (selectedNonChargeableAmenities.isEmpty()) {
                    return@BottomActionBar
                }
                
                val allAmenityIds = (selectedChargeableAmenities + selectedNonChargeableAmenities).toList()
                viewModel.saveSelectedAmenities(allAmenityIds)
            }
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

@Composable
fun AmenityChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) AppColors.LimoOrange.copy(alpha = 0.2f) else Color.White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Text(
            text = name,
            style = AppTextStyles.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) AppColors.LimoOrange else Color.Black
            ),
            modifier = Modifier.padding(12.dp)
        )
    }
}

