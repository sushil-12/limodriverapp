package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.R
import com.limo1800driver.app.ui.components.RegistrationTopBar
import com.limo1800driver.app.ui.navigation.NavRoutes
import com.limo1800driver.app.ui.theme.* import com.limo1800driver.app.ui.viewmodel.UserProfileDetailsViewModel

@Composable
fun UserProfileDetailsScreen(
    onNavigateToStep: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    onUpdateComplete: (() -> Unit)? = null,
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

    val canContinue = isDrivingLicenseCompleted && isBankDetailsCompleted && isProfilePictureCompleted

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            RegistrationTopBar()
        },
        bottomBar = {
            // MATCHING FIGMA: Custom Bottom Button Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp, top = 16.dp)
            ) {
                Button(
                    onClick = onContinue,
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
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- Location Section ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Signing up for",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = userLocation.ifEmpty { "Select location" },
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // --- Welcome Title ---
            Text(
                text = "Welcome $userName!",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                                color = if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
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

                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant)

                // Bank Details
                StepRow(
                    title = "Bank Details",
                    isCompleted = isBankDetailsCompleted,
                    enabled = isDrivingLicenseCompleted,
                    onClick = { onNavigateToStep(NavRoutes.BankDetails) }
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant)

                // Profile Picture
                StepRow(
                    title = "Profile Picture",
                    isCompleted = isProfilePictureCompleted,
                    enabled = isBankDetailsCompleted,
                    onClick = { onNavigateToStep(NavRoutes.ProfilePicture) }
                )
            }
        }

        // Bottom Action Bar (only in edit mode)
        if (isEditMode) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.Button(
                onClick = { onUpdateComplete?.invoke() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
            ) {
                Text(
                    text = "Update",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
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
    val interactionSource = remember { MutableInteractionSource() }

    // --- DESIGN LOGIC ---
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    val statusColor = if (enabled) {
        if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val statusText = if (isCompleted) "Completed" else "Pending"
    val iconTint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // NO RIPPLE
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 20.dp),
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
                    color = titleColor
                )
            )

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
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}