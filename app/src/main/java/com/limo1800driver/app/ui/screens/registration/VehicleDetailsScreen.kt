package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.limo1800driver.app.ui.components.CommonDropdown
import com.limo1800driver.app.ui.components.CommonTextField
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.theme.AppTextStyles
import com.limo1800driver.app.ui.util.noRippleClickable
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigateToVehicleSelection: () -> Unit = {},
    viewModel: VehicleDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Bottom sheet state for vehicle selection
    var showVehicleSelectionSheet by remember { mutableStateOf(false) }

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

    // Policies
    val policyOptions = listOf("24", "48", "72")
    val nonCharterPolicy by viewModel.nonCharterPolicy.collectAsState()
    val charterPolicy by viewModel.charterPolicy.collectAsState()

    // Error states
    var selectedVehicleTypeError by remember { mutableStateOf<String?>(null) }
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
            "numVehicles" -> if (value.isEmpty() || value.toIntOrNull() == null || value.toInt() <= 0) "Invalid number" else null
            "seats" -> if (value.isEmpty() || value.toIntOrNull() == null || value.toInt() <= 0) "Invalid seats" else null
            "luggage" -> if (value.isEmpty() || value.toIntOrNull() == null || value.toInt() < 0) "Invalid luggage" else null
            "licensePlate" -> if (value.trim().length < 2) "Min 2 chars required" else null
            else -> null
        }
    }

    // Handle API Errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            apiError = error
            selectedVehicleTypeError = null
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
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // <--- CRITICAL: Resizes the entire screen (pushing bottomBar up) when keyboard opens
        topBar = { RegistrationTopBar(onBack = onBack) },
        bottomBar = {
            // --- CUSTOM BOTTOM BAR (Docked at bottom) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 10.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            // Validation Logic
                            var hasErrors = false
                            apiError = null

                            if (selectedVehicleTypeId == null) { selectedVehicleTypeError = "Required"; hasErrors = true }
                            if (selectedMakeId == null) { selectedMakeError = "Required"; hasErrors = true }
                            if (selectedModelId == null) { selectedModelError = "Required"; hasErrors = true }
                            if (numVehicles.isEmpty()) { numVehiclesError = "Required"; hasErrors = true }
                            if (selectedYearId == null) { selectedYearError = "Required"; hasErrors = true }
                            if (selectedColorId == null) { selectedColorError = "Required"; hasErrors = true }
                            if (seats.isEmpty()) { seatsError = "Required"; hasErrors = true }
                            if (luggage.isEmpty()) { luggageError = "Required"; hasErrors = true }
                            if (nonCharterPolicy == null) { nonCharterPolicyError = "Required"; hasErrors = true }
                            if (charterPolicy == null) { charterPolicyError = "Required"; hasErrors = true }
                            if (licensePlate.trim().isEmpty()) { licensePlateError = "Required"; hasErrors = true }

                            if (!hasErrors) {
                                viewModel.saveStep1AndNavigate { onNext() }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE89148),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !uiState.isLoading,
                    ) {
                        if (uiState.isLoading) {
                            ShimmerCircle(size = 24.dp)
                        } else {
                            Text(text = "Next", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        // --- SCROLLABLE FORM CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Respects the Scaffold's top and bottom bars
                .verticalScroll(rememberScrollState())
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
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F2))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = error, color = Color(0xFFDC2626))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Service Types ---
            Text("CHOOSE ALL THAT APPLY", style = TextStyle(color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Local Service Only" to "local_service_only",
                    "Over The Road" to "over_the_road",
                    "Shuttle Service" to "shuttle_service"
                ).forEach { (label, apiValue) ->
                    val isSelected = selectedServices.contains(apiValue)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newList = if (isSelected) selectedServices - apiValue else selectedServices + apiValue
                            viewModel.selectedServiceTypes.value = newList
                        },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Black,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Vehicle Type ---
            Text(text = "VEHICLE TYPE *", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray))
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth().noRippleClickable { showVehicleSelectionSheet = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedVehicleTypeError != null) Color.Red else Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getVehicleTypeName(selectedVehicleTypeId).ifEmpty { "Select Vehicle Type" },
                        color = if (selectedVehicleTypeId != null) AppColors.LimoBlack else Color.Gray
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }
            }
            selectedVehicleTypeError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Make ---
            CommonDropdown(
                label = "MAKE",
                placeholder = "Select Make",
                selectedValue = getOptionName(selectedMakeId, uiState.makes),
                options = uiState.makes.map { it.name },
                onValueSelected = { name -> uiState.makes.find { it.name == name }?.let { viewModel.onMakeSelected(it.id) } },
                isRequired = true,
                errorMessage = selectedMakeError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Model ---
            if (selectedMakeId != null) {
                CommonDropdown(
                    label = "MODEL",
                    placeholder = if (uiState.isLoadingModels) "Loading..." else "Select Model",
                    selectedValue = getOptionName(selectedModelId, uiState.models),
                    options = uiState.models.map { it.name },
                    onValueSelected = { name -> uiState.models.find { it.name == name }?.let { viewModel.selectedModelId.value = it.id } },
                    enabled = !uiState.isLoadingModels,
                    isRequired = true,
                    errorMessage = selectedModelError
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CommonTextField(
                        label = "NUMBER OF VEHICLES",
                        placeholder = "1",
                        text = numVehicles,
                        onValueChange = { viewModel.numberOfVehicles.value = it },
                        keyboardType = KeyboardType.Number,
                        isRequired = true,
                        errorMessage = numVehiclesError
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CommonDropdown(
                        label = "YEAR",
                        placeholder = "Select",
                        selectedValue = getOptionName(selectedYearId, uiState.years),
                        options = uiState.years.map { it.name },
                        onValueSelected = { name -> uiState.years.find { it.name == name }?.let { viewModel.selectedYearId.value = it.id } },
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
                onValueSelected = { name -> uiState.colors.find { it.name == name }?.let { viewModel.selectedColorId.value = it.id } },
                isRequired = true,
                errorMessage = selectedColorError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Seats & Luggage ---
            CommonTextField(
                label = "NO. OF PAX SEATS",
                placeholder = "4",
                text = seats,
                onValueChange = { viewModel.seats.value = it },
                keyboardType = KeyboardType.Number,
                isRequired = true,
                errorMessage = seatsError
            )

            Spacer(modifier = Modifier.height(16.dp))

            CommonTextField(
                label = "NO. OF LARGE LUGGAGE",
                placeholder = "2",
                text = luggage,
                onValueChange = { viewModel.luggage.value = it },
                keyboardType = KeyboardType.Number,
                isRequired = true,
                errorMessage = luggageError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Policies ---
            CommonDropdown(
                label = "AIRPORT / CITY CANCEL POLICY",
                placeholder = "Select Hours",
                selectedValue = nonCharterPolicy?.toString(),
                options = policyOptions,
                onValueSelected = { viewModel.nonCharterPolicy.value = it.toIntOrNull() },
                isRequired = true,
                errorMessage = nonCharterPolicyError
            )

            Spacer(modifier = Modifier.height(16.dp))

            CommonDropdown(
                label = "CHARTER CANCEL POLICY",
                placeholder = "Select Hours",
                selectedValue = charterPolicy?.toString(),
                options = policyOptions,
                onValueSelected = { viewModel.charterPolicy.value = it.toIntOrNull() },
                isRequired = true,
                errorMessage = charterPolicyError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- License Plate (Last Item) ---
            CommonTextField(
                label = "LICENSE PLATE",
                placeholder = "ABC 123",
                text = licensePlate,
                onValueChange = { viewModel.licensePlate.value = it.uppercase() },
                isRequired = true,
                errorMessage = licensePlateError
            )

            // Small spacer just to give a little breathing room at end of list
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showVehicleSelectionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVehicleSelectionSheet = false },
            containerColor = Color.White
        ) {
            VehicleSelectionScreen(
                onNext = {}, onBack = null, viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                isBottomSheetMode = true,
                onVehicleSelected = { viewModel.selectedVehicleTypeId.value = it.idInt ?: it.id?.toIntOrNull(); showVehicleSelectionSheet = false },
                onDismiss = { showVehicleSelectionSheet = false }
            )
        }
    }
}