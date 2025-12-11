package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.data.storage.TokenManager
import com.limo1800driver.app.ui.navigation.NavRoutes
import com.limo1800driver.app.ui.navigation.RegistrationNavigationState
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.VehicleDetailsStepViewModel
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets

@Composable
fun VehicleDetailsStepScreen(
    onNavigateToStep: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: VehicleDetailsStepViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val registrationNavigationState = remember { RegistrationNavigationState() }
    
    var userName by remember { mutableStateOf("") }
    var userLocation by remember { mutableStateOf("") }
    
    // Fetch all steps on load
    LaunchedEffect(Unit) {
        viewModel.fetchAllSteps()
        viewModel.ensureUserNameAndLocation()
    }
    
    // Update local state from ViewModel
    LaunchedEffect(uiState.userName) {
        if (uiState.userName.isNotEmpty()) {
            userName = uiState.userName
        }
    }
    
    LaunchedEffect(uiState.userLocation) {
        if (uiState.userLocation.isNotEmpty()) {
            userLocation = uiState.userLocation
        }
    }
    
    val isVehicleInsuranceCompleted = uiState.allSteps?.vehicleInsurance?.isCompleted ?: false
    val isVehicleDetailsCompleted = uiState.allSteps?.vehicleDetails?.isCompleted ?: false
    val isVehicleRatesCompleted = uiState.allSteps?.vehicleRateSettings?.isCompleted ?: false
    
    val canContinue = isVehicleInsuranceCompleted && isVehicleDetailsCompleted && isVehicleRatesCompleted
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        RegistrationTopBar(
            onHelpClick = { /* TODO: hook help */ },
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Location header
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Signing up for",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = userLocation.ifEmpty { "Select location" },
                        style = AppTextStyles.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle, // Use car icon if available
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Welcome message
            Text(
                text = "Welcome $userName!",
                style = AppTextStyles.phoneEntryHeadline.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            if (isVehicleInsuranceCompleted) AppColors.LimoOrange else Color(0xFFE5E5E5),
                            RoundedCornerShape(2.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            if (isVehicleDetailsCompleted) AppColors.LimoOrange else Color(0xFFE5E5E5),
                            RoundedCornerShape(2.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            if (isVehicleRatesCompleted) AppColors.LimoOrange else Color(0xFFE5E5E5),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Steps list
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Vehicle Insurance
                    StepRow(
                        title = "Vehicle Insurance",
                        isCompleted = isVehicleInsuranceCompleted,
                        enabled = true,
                        onClick = { onNavigateToStep(NavRoutes.VehicleInsurance) }
                    )
                    
                    Divider(color = Color(0xFF121212).copy(alpha = 0.05f), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Vehicle Details
                    StepRow(
                        title = "Vehicle Details",
                        isCompleted = isVehicleDetailsCompleted,
                        enabled = isVehicleInsuranceCompleted,
                        onClick = { onNavigateToStep(NavRoutes.VehicleDetails) }
                    )
                    
                    Divider(color = Color(0xFF121212).copy(alpha = 0.05f), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Vehicle Rate Settings
                    StepRow(
                        title = "Vehicle Rate Settings",
                        isCompleted = isVehicleRatesCompleted,
                        enabled = isVehicleDetailsCompleted,
                        onClick = { onNavigateToStep(NavRoutes.VehicleRates) }
                    )
                }
            }
        }
        
        // Bottom Button (match UserProfileDetails)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp, top = 16.dp)
        ) {
            Button(
                onClick = { if (canContinue) onContinue() },
                enabled = canContinue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE89148),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Continue",
                    style = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

