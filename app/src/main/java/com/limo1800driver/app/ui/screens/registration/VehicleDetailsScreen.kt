package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.model.registration.VehicleOption
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsViewModel
import com.limo1800driver.app.ui.components.RegistrationTopBar // Assumed existing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // State Collection
    val serviceTypes by viewModel.selectedServiceTypes.collectAsState()
    val selectedVehicleType by viewModel.selectedVehicleType.collectAsState()
    val selectedMake by viewModel.selectedMake.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val seats by viewModel.seats.collectAsState()
    val luggage by viewModel.luggage.collectAsState()
    val licensePlate by viewModel.licensePlate.collectAsState()
    val nonCharterPolicy by viewModel.nonCharterPolicy.collectAsState()
    val charterPolicy by viewModel.charterPolicy.collectAsState()

    // Sheet States
    var showVehicleTypeSheet by remember { mutableStateOf(false) }
    var showMakeSheet by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showYearSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showNonCharterSheet by remember { mutableStateOf(false) }
    var showCharterSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { RegistrationTopBar(onBack = onBack) },
        bottomBar = {
            BottomActionBar(
                isLoading = uiState.isLoading,
                onBack = onBack,
                onNext = { viewModel.submitVehicleDetails(onSuccess = onNext) },
                nextButtonText = if(uiState.isLoading) "Loading..." else "Next"
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
            // --- HEADER ---
            // HeaderCard(title: "Enter your vehicle details") logic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                 Text(
                    text = "Enter your vehicle details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // --- SERVICE TYPES ---
            Text(
                text = "CHOOSE ALL THAT APPLY",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val options = listOf("Local Service Only", "Over The Road", "Shuttle Service")
                options.forEach { option ->
                    val isSelected = serviceTypes.contains(option)
                    ServiceTypeButton(
                        text = option,
                        isSelected = isSelected,
                        onClick = {
                            val currentList = serviceTypes.toMutableList()
                            if (isSelected) currentList.remove(option) else currentList.add(option)
                            viewModel.selectedServiceTypes.value = currentList
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FORM FIELDS ---
            
            // 1. Vehicle Type
            ReadOnlyDropdownField(
                label = "VEHICLE TYPE",
                value = selectedVehicleType?.name ?: "Select",
                isRequired = false, // iOS UI doesn't show red star on this specific label in code provided
                onClick = { showVehicleTypeSheet = true }
            )

            // 2. Make
            ReadOnlyDropdownField(
                label = "MAKE",
                value = selectedMake?.name ?: "Select",
                isRequired = true,
                onClick = { showMakeSheet = true }
            )

            // 3. Model
            ReadOnlyDropdownField(
                label = "MODEL",
                value = selectedModel?.name ?: "Select",
                isRequired = true,
                onClick = { 
                    if (selectedMake != null) showModelSheet = true 
                }
            )

            // 4. Year
            ReadOnlyDropdownField(
                label = "YEAR",
                value = selectedYear?.name ?: "Select",
                isRequired = true,
                onClick = { showYearSheet = true }
            )

            // 5. Color
            ReadOnlyDropdownField(
                label = "COLOR",
                value = selectedColor?.name ?: "Select",
                isRequired = true,
                onClick = { showColorSheet = true }
            )

            // 6. Seats
            StandardTextField(
                label = "NO. OF PAX SEATS/ SEAT BELTS",
                value = seats,
                onValueChange = { viewModel.seats.value = it },
                isRequired = true,
                keyboardType = KeyboardType.Number
            )

            // 7. Luggage
            StandardTextField(
                label = "NO. OF Large Luggage (2 Carry On + 1 Large Suitcase)",
                value = luggage,
                onValueChange = { viewModel.luggage.value = it },
                isRequired = true,
                keyboardType = KeyboardType.Number
            )

            // 8. Non-Charter Policy
            ReadOnlyDropdownField(
                label = "AIRPORT / CITY/ CRUISE PORT CANCEL POLICY",
                value = nonCharterPolicy?.name ?: "Select",
                isRequired = true,
                onClick = { showNonCharterSheet = true }
            )

            // 9. Charter Policy
            ReadOnlyDropdownField(
                label = "CHARTER CANCEL POLICY",
                value = charterPolicy?.name ?: "Select",
                isRequired = true,
                onClick = { showCharterSheet = true }
            )

            // 10. License Plate
            StandardTextField(
                label = "LICENSE PLATE OR OTHER VEHICLE IDENTITY",
                value = licensePlate,
                onValueChange = { viewModel.licensePlate.value = it.uppercase() },
                isRequired = false, // Matches iOS code
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters
            )
            
            Spacer(modifier = Modifier.height(30.dp))
        }

        // --- BOTTOM SHEETS ---
        
        if (showVehicleTypeSheet) {
            SelectionBottomSheet(
                title = "Select Vehicle Type",
                options = uiState.vehicleTypes,
                onDismiss = { showVehicleTypeSheet = false },
                onSelected = { viewModel.selectedVehicleType.value = it }
            )
        }
        
        if (showMakeSheet) {
            SelectionBottomSheet(
                title = "Select Make",
                options = uiState.makeOptions,
                onDismiss = { showMakeSheet = false },
                onSelected = { 
                    viewModel.selectedMake.value = it
                    viewModel.selectedModel.value = null // Reset model on make change
                    viewModel.loadModelsForMake(it.id)
                }
            )
        }

        if (showModelSheet) {
            SelectionBottomSheet(
                title = "Select Model",
                options = uiState.modelOptions,
                onDismiss = { showModelSheet = false },
                onSelected = { viewModel.selectedModel.value = it }
            )
        }
        
        if (showYearSheet) {
            SelectionBottomSheet(
                title = "Select Year",
                options = uiState.yearOptions,
                onDismiss = { showYearSheet = false },
                onSelected = { viewModel.selectedYear.value = it }
            )
        }
        
        if (showColorSheet) {
            SelectionBottomSheet(
                title = "Select Color",
                options = uiState.colorOptions,
                onDismiss = { showColorSheet = false },
                onSelected = { viewModel.selectedColor.value = it }
            )
        }
        
        if (showNonCharterSheet) {
            SelectionBottomSheet(
                title = "Select Non-Charter Policy",
                options = uiState.cancelPolicyOptions,
                onDismiss = { showNonCharterSheet = false },
                onSelected = { 
                    viewModel.nonCharterPolicy.value = it 
                    // iOS logic syncs charter policy if non-charter is selected? 
                    // Code says: viewModel.charterCancelPolicy = option.value (inside nonCharter setter)
                    viewModel.charterPolicy.value = it
                }
            )
        }

        if (showCharterSheet) {
            SelectionBottomSheet(
                title = "Select Charter Policy",
                options = uiState.cancelPolicyOptions,
                onDismiss = { showCharterSheet = false },
                onSelected = { viewModel.charterPolicy.value = it }
            )
        }
    }
    
    // Error Handling
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }
}

// --- COMPONENT HELPERS TO MATCH IOS STYLE ---

@Composable
fun ServiceTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(35.dp)
            .background(
                color = if (isSelected) Color.Black else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color.Black,
            maxLines = 1,
            lineHeight = 10.sp
        )
    }
}

@Composable
fun ReadOnlyDropdownField(
    label: String,
    value: String,
    isRequired: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            if (isRequired) {
                Text(text = "*", fontSize = 12.sp, color = Color.Red)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)) // AppColors.textfieldbackgroundcolore
                .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StandardTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isRequired: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Label logic handling complex labels like Luggage
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            if (isRequired) {
                Text(text = "*", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization
            ),
            singleLine = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionBottomSheet(
    title: String,
    options: List<VehicleOption>,
    onDismiss: () -> Unit,
    onSelected: (VehicleOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            // Header
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // List
            val filteredOptions = options.filter { 
                it.name.contains(searchQuery, ignoreCase = true) 
            }
            
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(filteredOptions.size) { index ->
                    val option = filteredOptions[index]
                    TextButton(
                        onClick = {
                            onSelected(option)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            text = option.name,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            color = Color.Black
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}