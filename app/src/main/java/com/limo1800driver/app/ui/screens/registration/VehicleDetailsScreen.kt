package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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

    // Error states
    var selectedMakeError by remember { mutableStateOf<String?>(null) }
    var selectedModelError by remember { mutableStateOf<String?>(null) }
    var numVehiclesError by remember { mutableStateOf<String?>(null) }
    var selectedYearError by remember { mutableStateOf<String?>(null) }
    var selectedColorError by remember { mutableStateOf<String?>(null) }
    var seatsError by remember { mutableStateOf<String?>(null) }
    var luggageError by remember { mutableStateOf<String?>(null) }
    var nonCharterPolicyError by remember { mutableStateOf<String?>(null) }
    var charterPolicyError by remember { mutableStateOf<String?>(null) }
    var licensePlateError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Helper to find name by ID
    fun getVehicleTypeName(id: Int?): String = uiState.vehicleTypes.find { (it.idInt ?: it.id?.toIntOrNull()) == id }?.vehicleName ?: ""
    fun getOptionName(id: Int?, list: List<com.limo1800driver.app.data.model.registration.VehicleOption>): String = list.find { it.id == id }?.name ?: ""

    // Validation function
    fun validateField(fieldName: String, value: String): String? {
        return when (fieldName) {
            "numVehicles" -> when {
                value.isEmpty() -> "Number of vehicles is required"
                value.toIntOrNull() == null || value.toInt() <= 0 -> "Please enter a valid number greater than 0"
                else -> null
            }
            "seats" -> when {
                value.isEmpty() -> "Number of seats is required"
                value.toIntOrNull() == null || value.toInt() <= 0 -> "Please enter a valid number greater than 0"
                else -> null
            }
            "luggage" -> when {
                value.isEmpty() -> "Number of luggage is required"
                value.toIntOrNull() == null || value.toInt() < 0 -> "Please enter a valid number"
                else -> null
            }
            "licensePlate" -> when {
                value.trim().isEmpty() -> "License plate is required"
                value.trim().length < 2 -> "License plate must be at least 2 characters"
                else -> null
            }
            else -> null
        }
    }

    // Handle API Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            apiError = error
            // Clear field-specific errors when we have an API error
            selectedMakeError = null
            selectedModelError = null
            numVehiclesError = null
            selectedYearError = null
            selectedColorError = null
            seatsError = null
            luggageError = null
            nonCharterPolicyError = null
            charterPolicyError = null
            licensePlateError = null
        }
    }

    Scaffold(
        topBar = { RegistrationTopBar(onBack = onBack) },
        bottomBar = {
            BottomActionBar(
                isLoading = uiState.isLoading,
                onBack = onBack,
                onNext = {
                    // Clear previous errors
                    selectedMakeError = null
                    selectedModelError = null
                    numVehiclesError = null
                    selectedYearError = null
                    selectedColorError = null
                    seatsError = null
                    luggageError = null
                    nonCharterPolicyError = null
                    charterPolicyError = null
                    licensePlateError = null
                    apiError = null

                    // Validation Logic
                    var hasErrors = false

                    // Validate Make
                    if (selectedMakeId == null) {
                        selectedMakeError = "Make is required"
                        hasErrors = true
                    }

                    // Validate Model
                    if (selectedModelId == null) {
                        selectedModelError = "Model is required"
                        hasErrors = true
                    }

                    // Validate Number of Vehicles
                    if (numVehicles.isEmpty()) {
                        numVehiclesError = "Number of vehicles is required"
                        hasErrors = true
                    } else if (numVehicles.toIntOrNull() == null || numVehicles.toInt() <= 0) {
                        numVehiclesError = "Please enter a valid number greater than 0"
                        hasErrors = true
                    }

                    // Validate Year
                    if (selectedYearId == null) {
                        selectedYearError = "Year is required"
                        hasErrors = true
                    }

                    // Validate Color
                    if (selectedColorId == null) {
                        selectedColorError = "Color is required"
                        hasErrors = true
                    }

                    // Validate Seats
                    if (seats.isEmpty()) {
                        seatsError = "Number of seats is required"
                        hasErrors = true
                    } else if (seats.toIntOrNull() == null || seats.toInt() <= 0) {
                        seatsError = "Please enter a valid number greater than 0"
                        hasErrors = true
                    }

                    // Validate Luggage
                    if (luggage.isEmpty()) {
                        luggageError = "Number of luggage is required"
                        hasErrors = true
                    } else if (luggage.toIntOrNull() == null || luggage.toInt() < 0) {
                        luggageError = "Please enter a valid number"
                        hasErrors = true
                    }

                    // Validate Non-charter Policy
                    if (nonCharterPolicy == null) {
                        nonCharterPolicyError = "Cancel policy is required"
                        hasErrors = true
                    }

                    // Validate Charter Policy
                    if (charterPolicy == null) {
                        charterPolicyError = "Charter cancel policy is required"
                        hasErrors = true
                    }

                    // Validate License Plate
                    if (licensePlate.trim().isEmpty()) {
                        licensePlateError = "License plate is required"
                        hasErrors = true
                    } else if (licensePlate.trim().length < 2) {
                        licensePlateError = "License plate must be at least 2 characters"
                        hasErrors = true
                    }

                    // Only make API call if all validations pass
                    if (!hasErrors) {
                        viewModel.saveStep1AndNavigate { onNext() }
                    }
                },
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
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enter your vehicle details",
                style = AppTextStyles.phoneEntryHeadline.copy(color = Color.Black, fontSize = 24.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // API Error Display
            apiError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF2F2)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Service Types ---
            Text(text = "CHOOSE ALL THAT APPLY", style = AppTextStyles.bodyMedium.copy(color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Backend values are snake_case (e.g. "over_the_road"), UI shows friendly labels.
                val serviceOptions = listOf(
                    "Local Service Only" to "local_service_only",
                    "Over The Road" to "over_the_road",
                    "Shuttle Service" to "shuttle_service"
                )
                serviceOptions.forEach { (label, apiValue) ->
                    val isSelected = selectedServices.contains(apiValue)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newList = if (isSelected) selectedServices - apiValue else selectedServices + apiValue
                            viewModel.selectedServiceTypes.value = newList
                        },
                        label = { Text(label) },
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
                    selectedMakeError = if (name.isBlank()) "Make is required" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = selectedMakeError
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
                    selectedModelError = if (name.isBlank()) "Model is required" else null
                    apiError = null
                },
                enabled = selectedMakeId != null && !uiState.isLoadingModels,
                isRequired = true,
                errorMessage = selectedModelError
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // --- Number of Vehicles ---
                Box(modifier = Modifier.weight(1f)) {
                    CommonTextField(
                        label = "NUMBER OF VEHICLES",
                        placeholder = "1",
                        text = numVehicles,
                        onValueChange = {
                            viewModel.numberOfVehicles.value = it
                            numVehiclesError = validateField("numVehicles", it)
                            apiError = null
                        },
                        keyboardType = KeyboardType.Number,
                        isRequired = true,
                        errorMessage = numVehiclesError
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
                            selectedYearError = if (name.isBlank()) "Year is required" else null
                            apiError = null
                        },
                        isRequired = true,
                        errorMessage = selectedYearError
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
                    selectedColorError = if (name.isBlank()) "Color is required" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = selectedColorError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Seats & Luggage ---
            CommonTextField(
                label = "NO. OF PAX SEATS/ SEAT BELTS",
                placeholder = "4",
                text = seats,
                onValueChange = {
                    viewModel.seats.value = it
                    seatsError = validateField("seats", it)
                    apiError = null
                },
                keyboardType = KeyboardType.Number,
                isRequired = true,
                errorMessage = seatsError
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CommonTextField(
                label = "NO. OF LARGE LUGGAGE",
                placeholder = "2",
                text = luggage,
                onValueChange = {
                    viewModel.luggage.value = it
                    luggageError = validateField("luggage", it)
                    apiError = null
                },
                keyboardType = KeyboardType.Number,
                isRequired = true,
                errorMessage = luggageError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Cancellation Policies ---
            CommonDropdown(
                label = "AIRPORT / CITY / CRUISE PORT CANCEL POLICY",
                placeholder = "Select Hours",
                selectedValue = nonCharterPolicy?.toString(),
                options = policyOptions,
                onValueSelected = {
                    viewModel.nonCharterPolicy.value = it.toIntOrNull()
                    nonCharterPolicyError = if (it.isBlank()) "Cancel policy is required" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = nonCharterPolicyError
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CommonDropdown(
                label = "CHARTER CANCEL POLICY",
                placeholder = "Select Hours",
                selectedValue = charterPolicy?.toString(),
                options = policyOptions,
                onValueSelected = {
                    viewModel.charterPolicy.value = it.toIntOrNull()
                    charterPolicyError = if (it.isBlank()) "Charter cancel policy is required" else null
                    apiError = null
                },
                isRequired = true,
                errorMessage = charterPolicyError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- License Plate ---
            CommonTextField(
                label = "LICENSE PLATE OR OTHER VEHICLE IDENTITY",
                placeholder = "ABC 123",
                text = licensePlate,
                onValueChange = {
                    viewModel.licensePlate.value = it.uppercase()
                    licensePlateError = validateField("licensePlate", it)
                    apiError = null
                },
                isRequired = true,
                errorMessage = licensePlateError
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}