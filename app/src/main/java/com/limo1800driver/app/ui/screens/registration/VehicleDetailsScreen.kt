package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.theme.AppTextStyles
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsViewModel

@Composable
fun VehicleDetailsScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Collect specific fields
    val selectedVehicleTypeId by viewModel.selectedVehicleTypeId.collectAsState()
    val selectedMakeId by viewModel.selectedMakeId.collectAsState()
    val selectedModelId by viewModel.selectedModelId.collectAsState()
    val selectedYearId by viewModel.selectedYearId.collectAsState()
    val selectedColorId by viewModel.selectedColorId.collectAsState()
    
    val licensePlate by viewModel.licensePlate.collectAsState()
    val seats by viewModel.seats.collectAsState()
    val luggage by viewModel.luggage.collectAsState()
    val numVehicles by viewModel.numberOfVehicles.collectAsState()
    
    // Service Types
    val selectedServices by viewModel.selectedServiceTypes.collectAsState()
    
    // Policies (Assuming simple dropdowns for 24/48/72 hrs)
    val policyOptions = listOf("24", "48", "72")
    val nonCharterPolicy by viewModel.nonCharterPolicy.collectAsState()
    val charterPolicy by viewModel.charterPolicy.collectAsState()

    // Helper to find name by ID
    fun getVehicleTypeName(id: Int?): String = uiState.vehicleTypes.find { (it.idInt ?: it.id?.toIntOrNull()) == id }?.vehicleName ?: ""
    fun getOptionName(id: Int?, list: List<com.limo1800driver.app.data.model.registration.VehicleOption>): String = list.find { it.id == id }?.name ?: ""

    Scaffold(
        topBar = { RegistrationTopBar(onBack = onBack) },
        bottomBar = {
            BottomActionBar(
                isLoading = uiState.isLoading,
                onBack = onBack,
                onNext = { viewModel.completeVehicleDetails { onNext() } },
                nextButtonText = "Next"
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enter your vehicle details",
                style = AppTextStyles.phoneEntryHeadline.copy(color = Color.Black, fontSize = 24.sp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // --- Service Types ---
            Text(text = "CHOOSE ALL THAT APPLY", style = AppTextStyles.bodyMedium.copy(color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Local Service Only", "Over The Road", "Shuttle Service").forEach { type ->
                    val isSelected = selectedServices.contains(type)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newList = if (isSelected) selectedServices - type else selectedServices + type
                            viewModel.selectedServiceTypes.value = newList
                        },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Black,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // --- Vehicle Type ---
            CommonDropdown(
                label = "VEHICLE TYPE",
                placeholder = "Select",
                selectedValue = getVehicleTypeName(selectedVehicleTypeId),
                options = uiState.vehicleTypes.map { it.vehicleName ?: "" },
                onValueSelected = { name ->
                    val type = uiState.vehicleTypes.find { it.vehicleName == name }
                    viewModel.selectedVehicleTypeId.value = type?.idInt ?: type?.id?.toIntOrNull()
                },
                isRequired = false
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- Make ---
            CommonDropdown(
                label = "MAKE",
                placeholder = "Select Make",
                selectedValue = getOptionName(selectedMakeId, uiState.makes),
                options = uiState.makes.map { it.name },
                onValueSelected = { name ->
                    uiState.makes.find { it.name == name }?.let { viewModel.onMakeSelected(it.id) }
                },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Model ---
            // Only enabled if Make is selected
            CommonDropdown(
                label = "MODEL",
                placeholder = if (uiState.isLoadingModels) "Loading..." else "Select Model",
                selectedValue = getOptionName(selectedModelId, uiState.models),
                options = uiState.models.map { it.name },
                onValueSelected = { name ->
                    uiState.models.find { it.name == name }?.let { viewModel.selectedModelId.value = it.id }
                },
                enabled = selectedMakeId != null && !uiState.isLoadingModels,
                isRequired = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // --- Number of Vehicles ---
                Box(modifier = Modifier.weight(1f)) {
                    CommonTextField(
                        label = "NUMBER OF VEHICLES",
                        placeholder = "1",
                        text = numVehicles,
                        onValueChange = { viewModel.numberOfVehicles.value = it },
                        keyboardType = KeyboardType.Number,
                        isRequired = true
                    )
                }
                
                // --- Year ---
                Box(modifier = Modifier.weight(1f)) {
                    CommonDropdown(
                        label = "YEAR",
                        placeholder = "Select",
                        selectedValue = getOptionName(selectedYearId, uiState.years),
                        options = uiState.years.map { it.name },
                        onValueSelected = { name ->
                            uiState.years.find { it.name == name }?.let { viewModel.selectedYearId.value = it.id }
                        },
                        isRequired = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Color ---
            CommonDropdown(
                label = "COLOR",
                placeholder = "Select Color",
                selectedValue = getOptionName(selectedColorId, uiState.colors),
                options = uiState.colors.map { it.name },
                onValueSelected = { name ->
                    uiState.colors.find { it.name == name }?.let { viewModel.selectedColorId.value = it.id }
                },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Seats & Luggage ---
            CommonTextField(
                label = "NO. OF PAX SEATS/ SEAT BELTS",
                placeholder = "4",
                text = seats,
                onValueChange = { viewModel.seats.value = it },
                keyboardType = KeyboardType.Number,
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CommonTextField(
                label = "NO. OF LARGE LUGGAGE",
                placeholder = "2",
                text = luggage,
                onValueChange = { viewModel.luggage.value = it },
                keyboardType = KeyboardType.Number,
                isRequired = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Cancellation Policies ---
            CommonDropdown(
                label = "AIRPORT / CITY / CRUISE PORT CANCEL POLICY",
                placeholder = "Select Hours",
                selectedValue = nonCharterPolicy?.toString(),
                options = policyOptions,
                onValueSelected = { viewModel.nonCharterPolicy.value = it.toIntOrNull() },
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CommonDropdown(
                label = "CHARTER CANCEL POLICY",
                placeholder = "Select Hours",
                selectedValue = charterPolicy?.toString(),
                options = policyOptions,
                onValueSelected = { viewModel.charterPolicy.value = it.toIntOrNull() },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- License Plate ---
            CommonTextField(
                label = "LICENSE PLATE OR OTHER VEHICLE IDENTITY",
                placeholder = "ABC 123",
                text = licensePlate,
                onValueChange = { viewModel.licensePlate.value = it.uppercase() },
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}