package com.limo1800driver.app.ui.screens.registration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.limo1800driver.app.ui.util.noRippleClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.limo1800driver.app.data.model.registration.VehicleType
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.theme.* // Ensure you have your AppColors/AppTextStyles here
import com.limo1800driver.app.ui.viewmodel.VehicleSelectionUiState
import com.limo1800driver.app.ui.viewmodel.VehicleSelectionViewModel
import kotlinx.coroutines.launch

// Placeholder for your TopBar if not imported.
// Assuming RegistrationTopBar is available in your project as per your snippet.
// import com.limo1800driver.app.ui.components.RegistrationTopBar

@Composable
fun VehicleSelectionScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleSelectionViewModel = hiltViewModel(),
    isBottomSheetMode: Boolean = false,
    onVehicleSelected: ((VehicleType) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Track selection
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Fetch vehicle types on load
    LaunchedEffect(Unit) {
        viewModel.fetchVehicleTypes()
    }

    // Handle success navigation (only for full screen mode)
    LaunchedEffect(uiState.success) {
        if (uiState.success && !isBottomSheetMode) {
            onNext()
        }
    }

    // Error Handling State
    var showErrorDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            showErrorDialog = true
        }
    }

    if (isBottomSheetMode) {
        // Bottom sheet mode - just return the content
        VehicleSelectionContent(
            uiState = uiState,
            selectedIndex = selectedIndex,
            onSelectVehicle = { index -> selectedIndex = index },
            onVehicleSelected = { vehicle ->
                onVehicleSelected?.invoke(vehicle)
            },
            showErrorDialog = showErrorDialog
        )
    } else {
        // Full screen mode - use Scaffold
        Scaffold(
            topBar = {
                // Assuming this component exists in your project as per your imports
                 RegistrationTopBar( onBack = onBack)
            },
            bottomBar = {
                Button(
                    onClick = {
                        if (selectedIndex != null && selectedIndex!! < uiState.vehicles.size) {
                            val selectedVehicle = uiState.vehicles[selectedIndex!!]
                            scope.launch {
                                viewModel.saveSelectedVehicle(selectedVehicle)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF57F20) // Limo Orange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedIndex != null && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        ShimmerCircle(size = 24.dp)
                    } else {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            },
            containerColor = Color.White
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {

                // --- HEADER (Updated to match Figma) ---
                Text(
                    text = "Choose how you want to earn with 1800limo",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .fillMaxWidth()
                )

                // --- LIST ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        uiState.isLoading && uiState.vehicles.isEmpty() -> {
                            // Show shimmer loading items for full screen
                            items(8) { // Show 8 shimmer items for full screen
                                VehicleRowItemShimmer()
                            }
                        }
                        uiState.error != null && !showErrorDialog -> {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("Unable to load vehicles.", color = Color.Gray)
                                }
                            }
                        }
                        else -> {
                            // Show actual vehicle items
                            itemsIndexed(uiState.vehicles) { index, vehicle ->
                                VehicleRowItem(
                                    vehicle = vehicle,
                                    isSelected = selectedIndex == index,
                                    onSelect = { selectedIndex = index }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Error Dialog
    if (showErrorDialog && uiState.error != null) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearError()
            },
            title = { Text("Error") },
            text = { Text(uiState.error ?: "Unknown error occurred") },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    viewModel.clearError()
                }) {
                    Text("OK", color = Color(0xFFF57F20))
                }
            }
        )
    }
}

/**
 * Reusable Vehicle Selection Content (without Scaffold) for bottom sheet usage
 */
@Composable
fun VehicleSelectionContent(
    uiState: VehicleSelectionUiState,
    selectedIndex: Int?,
    onSelectVehicle: (Int) -> Unit,
    onVehicleSelected: (VehicleType) -> Unit,
    showErrorDialog: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        // --- HEADER ---
        Text(
            text = "Choose how you want to earn with 1800limo",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // --- LIST ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp), // Limit height for bottom sheet
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading && uiState.vehicles.isEmpty() -> {
                    // Show shimmer loading items
                    items(6) { // Show 6 shimmer items
                        VehicleRowItemShimmer()
                    }
                }
                uiState.error != null && !showErrorDialog -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Unable to load vehicles.", color = Color.Gray)
                        }
                    }
                }
                else -> {
                    // Show actual vehicle items
                    itemsIndexed(uiState.vehicles) { index, vehicle ->
                        VehicleRowItem(
                            vehicle = vehicle,
                            isSelected = selectedIndex == index,
                            onSelect = {
                                onSelectVehicle(index)
                                onVehicleSelected(vehicle) // Auto-select in bottom sheet mode
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleRowItem(
    vehicle: VehicleType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    // Colors matching the screenshot style
    val borderColor = if (isSelected) Color.Black else Color(0xFFE0E0E0)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val badgeBgColor = Color(0xFFFFF0E0) // Very light orange
    val badgeContentColor = Color(0xFFF57F20) // Limo Orange

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable { onSelect() }
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // Left Content: Badge + Texts
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Rides Badge
                Surface(
                    color = badgeBgColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = badgeContentColor
                        )
                        Text(
                            text = "Rides",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeContentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Vehicle Name
                Text(
                    text = vehicle.vehicleName ?: "Unknown Vehicle",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )

                // Description
                Text(
                    text = "Vehicle: You have a car that you wish to drive",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                )
            }

            // Right Content: Image
            Spacer(modifier = Modifier.width(12.dp))

            if (!vehicle.vehicleImage.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(vehicle.vehicleImage),
                    contentDescription = vehicle.vehicleName,
                    modifier = Modifier
                        .width(90.dp)
                        .height(55.dp), // Fixed height to align rows nicely
                    contentScale = ContentScale.Fit
                )
            } else {
                // Fallback placeholder if image is missing
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(55.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Img", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

/**
 * Shimmer version of VehicleRowItem for loading states
 */
@Composable
fun VehicleRowItemShimmer() {
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

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Content: Badge + Texts
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Rides Badge Shimmer
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )

                // Vehicle Name Shimmer
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(18.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )

                // Description Shimmer
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(14.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
            }

            // Right Content: Image Shimmer
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(55.dp)
                    .background(brush, RoundedCornerShape(8.dp))
            )
        }
    }
}