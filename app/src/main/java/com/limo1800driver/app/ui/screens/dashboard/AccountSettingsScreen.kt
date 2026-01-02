package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.limo1800driver.app.ui.components.CommonMenuHeader
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerText
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.viewmodel.AccountSettingsViewModel

/**
 * Account Settings Screen
 * Shows profile, documents, vehicles, and logout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToProfilePicture: () -> Unit = {},
    onNavigateToCompanyInfo: () -> Unit = {},
    onNavigateToCompanyDocuments: () -> Unit = {},
    onNavigateToDrivingLicense: () -> Unit = {},
    onNavigateToBankDetails: () -> Unit = {},
    onNavigateToVehicleInsurance: () -> Unit = {},
    onNavigateToVehicleDetails: () -> Unit = {},
    onNavigateToVehicleRates: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Extract profile data reactively so screen recomposes when profile updates
    // Using remember with profile as key ensures recomposition when profile changes
    val profile = uiState.profile
    val driverName = remember(profile) {
        profile?.let { "${it.driverFirstName ?: ""} ${it.driverLastName ?: ""}".trim() }
            .takeIf { it?.isNotEmpty() == true } ?: "Driver"
    }
    val driverImageURL = remember(profile) { profile?.driverImage }

    // Interaction source for Logout button
    val logoutInteractionSource = remember { MutableInteractionSource() }

    Scaffold(
        topBar = {
            CommonMenuHeader(
                title = "Account Settings",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Profile Section
            item {
                ProfileSection(
                    driverName = driverName,
                    driverImageURL = driverImageURL,
                    isLoading = uiState.isLoading,
                    onViewProfile = onNavigateToProfile,
                    onEditProfilePicture = onNavigateToProfilePicture
                )
            }

            // Company Information & Documents Section
            item {
                Column {
                    // Section Header
                    SectionHeader(
                        icon = Icons.Default.Description,
                        title = "Company Information & Documents"
                    )

                    // Documents List
                    Column {
                        // Company Details (only if NOT gig_operator)
                        if (viewModel.shouldShowCompanyDetails()) {
                            DocumentSection(
                                title = "Company Details",
                                isVerified = true,
                                onEdit = onNavigateToCompanyInfo
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )

                            // Company Documents
                            DocumentSection(
                                title = "Company Documents",
                                isVerified = true,
                                onEdit = onNavigateToCompanyDocuments
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )
                        }

                        // Driving License
                        DocumentSection(
                            title = "Driving License",
                            isVerified = true,
                            onEdit = onNavigateToDrivingLicense
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )

                        // Bank Details
                        DocumentSection(
                            title = "Bank Details",
                            isVerified = true,
                            onEdit = onNavigateToBankDetails
                        )
                    }
                }
            }

            // Vehicles Section
            item {
                Column {
                    // Section Header
                    SectionHeader(
                        icon = Icons.Default.DirectionsCar,
                        title = "Vehicles"
                    )

                    // Vehicles List
                    Column {
                        // Vehicle Insurance (moved here to match iOS)
                        VehicleSection(
                            title = "Vehicle Insurance",
                            vehicleCount = null,
                            onEdit = onNavigateToVehicleInsurance
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                        // Vehicle Details
                        VehicleSection(
                            title = "Vehicle Details",
                            vehicleCount = uiState.vehicleCount,
                            onEdit = onNavigateToVehicleDetails
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )

                        // Vehicle Rate Settings
                        VehicleSection(
                            title = "Vehicle Rate Settings",
                            vehicleCount = null,
                            onEdit = onNavigateToVehicleRates
                        )
                    }
                }
            }

            // Logout Section
            item {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // NO RIPPLE
                            .clickable(
                                interactionSource = logoutInteractionSource,
                                indication = null
                            ) { onLogout() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFFF44336) // Red
                        )
                        Text(
                            text = "Logout",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF44336) // Red
                        )
                    }
                }
            }

            // Error State
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Profile Section Component
 */
@Composable
private fun ProfileSection(
    driverName: String,
    driverImageURL: String?,
    isLoading: Boolean,
    onViewProfile: () -> Unit,
    onEditProfilePicture: () -> Unit
) {
    // Interaction sources for ripple-free clicks
    val profileInteractionSource = remember { MutableInteractionSource() }
    val editInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(243, 147, 61, 26))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,

        ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                ShimmerText(
                    modifier = Modifier.width(150.dp),
                    height = 32.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(
                    modifier = Modifier.width(100.dp).height(20.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            } else {
                Text(
                    text = driverName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimoOrange,
                    lineHeight = 38.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = profileInteractionSource,
                        indication = null
                    ) { onViewProfile() }
                ) {
                    Text(
                        text = "VIEW PROFILE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = Color.Black
                    )
                }
            }
        }

        // Profile Image (+ edit pencil)
        if (isLoading) {
            ShimmerBox(
                modifier = Modifier.size(80.dp),
                shape = CircleShape
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clickable(
                        interactionSource = profileInteractionSource,
                        indication = null
                    ) { onViewProfile() },
                contentAlignment = Alignment.Center
            ) {
                if (!driverImageURL.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(driverImageURL)
                            .crossfade(true)
                            .memoryCacheKey(driverImageURL) // Force reload if URL changes
                            .build(),
                        contentDescription = "Driver Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFF3E0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Driver Profile",
                            modifier = Modifier.size(40.dp),
                            tint = LimoOrange
                        )
                    }
                }

                // Pencil icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(LimoOrange, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(
                            interactionSource = editInteractionSource,
                            indication = null
                        ) { onEditProfilePicture() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit profile picture",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Section Header Component
 */
@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Black
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

/**
 * Document Section Component
 */
@Composable
private fun DocumentSection(
    title: String,
    isVerified: Boolean,
    onEdit: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onEdit() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            if (isVerified) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF4CAF50) // Green
                    )
                    Text(
                        text = "Verified",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50) // Green
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.Gray
            )
        }
    }
}

/**
 * Vehicle Section Component
 */
@Composable
private fun VehicleSection(
    title: String,
    vehicleCount: Int?,
    onEdit: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onEdit() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.Gray
            )
        }
    }
}