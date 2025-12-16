package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.R
import com.limo1800driver.app.ui.navigation.NavRoutes
import com.limo1800driver.app.ui.theme.* // Ensure AppColors.LimoOrange is defined or use hex
import com.limo1800driver.app.ui.viewmodel.UserProfileDetailsViewModel

@Composable
fun UserProfileDetailsScreen(
    onNavigateToStep: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: UserProfileDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var userName by remember { mutableStateOf("") }
    var userLocation by remember { mutableStateOf("") }
    
    // Fetch data
    LaunchedEffect(Unit) {
        viewModel.fetchAllSteps()
        viewModel.ensureUserNameAndLocation()
    }
    
    LaunchedEffect(uiState.userName) {
        if (uiState.userName.isNotEmpty()) userName = uiState.userName
    }
    
    LaunchedEffect(uiState.userLocation) {
        if (uiState.userLocation.isNotEmpty()) userLocation = uiState.userLocation
    }
    
    val isDrivingLicenseCompleted = uiState.allSteps?.drivingLicense?.isCompleted ?: false
    val isBankDetailsCompleted = uiState.allSteps?.bankDetails?.isCompleted ?: false
    val isProfilePictureCompleted = uiState.allSteps?.profilePicture?.isCompleted ?: false
    
    // Logic: Can continue if all steps are done (or whatever your logic requires)
    val canContinue = isDrivingLicenseCompleted && isBankDetailsCompleted && isProfilePictureCompleted

    // Use Scaffold to handle Safe Area (Status bar & Navigation bar) automatically
    Scaffold(
        containerColor = Color.White,
        topBar = {
            RegistrationTopBar(onHelpClick = { /* Handle help */ })
        },
        bottomBar = {
            // MATCHING FIGMA: Custom Bottom Button Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
                    // Add padding for bottom navigation bar (home indicator)
                    .windowInsetsPadding(WindowInsets.navigationBars) 
                    .padding(bottom = 16.dp, top = 16.dp)
            ) {
                Button(
                    onClick = onContinue,
                    // If logic requires validation to enable:
                     enabled = canContinue,
                    shape = RoundedCornerShape(12.dp), // Matches Figma rounded corners
                    colors = ButtonDefaults.buttonColors(
                        // Use exact Orange color from screenshot or AppColors.LimoOrange
                        containerColor = Color(0xFFE89148), 
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE89148).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp) // Standard height matching screenshot
                ) {
                    Text(
                        text = "Continue",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // This prevents content from going under header/footer
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- Location Section ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Signing up for",
                    style = TextStyle(
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
                        style = TextStyle(
                            fontSize = 15.sp, // Slightly smaller to fit long addresses
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        ),
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = Color(0xFFEEEEEE)
            )

            // --- Welcome Title ---
            Text(
                text = "Welcome $userName!",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Progress Bars ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                @Composable
                fun ProgressBar(isCompleted: Boolean) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                // Green if done, light gray if not
                                color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(100.dp)
                            )
                    )
                }
                ProgressBar(isCompleted = isDrivingLicenseCompleted)
                ProgressBar(isCompleted = isBankDetailsCompleted)
                ProgressBar(isCompleted = isProfilePictureCompleted)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Steps List ---
            Column {
                // Driving License
                StepRow(
                    title = "Driving License",
                    isCompleted = isDrivingLicenseCompleted,
                    enabled = true,
                    onClick = { onNavigateToStep(NavRoutes.DrivingLicense) }
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Color(0xFFF5F5F5))

                // Bank Details
                StepRow(
                    title = "Bank Details",
                    isCompleted = isBankDetailsCompleted,
                    enabled = isDrivingLicenseCompleted,
                    onClick = { onNavigateToStep(NavRoutes.BankDetails) }
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Color(0xFFF5F5F5))

                // Profile Picture
                StepRow(
                    title = "Profile Picture",
                    isCompleted = isProfilePictureCompleted,
                    enabled = isBankDetailsCompleted,
                    onClick = { onNavigateToStep(NavRoutes.ProfilePicture) }
                )
            }
        }
    }
}

@Composable
fun RegistrationTopBar(
    onHelpClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    // Surface handles the background color for the status bar area
    Surface(
        color = Color(0xFF121212), // Black background
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                // This is the CRITICAL fix for Safe Area:
                .windowInsetsPadding(WindowInsets.statusBars) 
                .fillMaxWidth()
                .height(60.dp) // Height of the actual toolbar content
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.splashlogo), // Ensure this matches your XML filename exactly (lowercase/uppercase)
                    contentDescription = "Limo Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(40.dp)
                        .wrapContentWidth(align = Alignment.Start) 
                )
            }

            // Help Button
            Button(
                onClick = onHelpClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(50), // Fully rounded pill shape
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Help",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StepRow(
    title: String,
    isCompleted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 20.dp), // Spacious padding like screenshot
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            )
            
            // Status Text Logic based on screenshot ("In Review" is default for pending)
            val statusText = if (isCompleted) "Completed" else "Pending"
            val statusColor = if (isCompleted) Color(0xFF2E7D32) else Color.Gray
            
            Text(
                text = statusText,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = statusColor
                )
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
    }
}