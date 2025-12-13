package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    onNavigateToCompanyInfo: () -> Unit = {},
    onNavigateToDrivingLicense: () -> Unit = {},
    onNavigateToBankDetails: () -> Unit = {},
    onNavigateToVehicleInsurance: () -> Unit = {},
    onNavigateToVehicleDetails: () -> Unit = {},
    onNavigateToVehicleRates: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
                    driverName = viewModel.getFullName(),
                    driverImageURL = viewModel.getDriverImageURL(),
                    isLoading = uiState.isLoading,
                    onViewProfile = onNavigateToProfile
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
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        
                        // Driving License
                        DocumentSection(
                            title = "Driving License",
                            isVerified = true,
                            onEdit = onNavigateToDrivingLicense
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        // Bank Details
                        DocumentSection(
                            title = "Bank Details",
                            isVerified = true,
                            onEdit = onNavigateToBankDetails
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        // Vehicle Insurance
                        DocumentSection(
                            title = "Vehicle Insurance",
                            isVerified = true,
                            onEdit = onNavigateToVehicleInsurance
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
                        // Vehicle Details
                        VehicleSection(
                            title = "Vehicle Details",
                            vehicleCount = uiState.vehicleCount,
                            onEdit = onNavigateToVehicleDetails
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
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
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLogout() }
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
    onViewProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier.clickable { onViewProfile() }
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
        
        // Profile Image
        if (isLoading) {
            ShimmerBox(
                modifier = Modifier.size(80.dp),
                shape = CircleShape
            )
        } else if (!driverImageURL.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(driverImageURL)
                    .crossfade(true)
                    .build(),
                contentDescription = "Driver Profile",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF3E0), CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
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
            
            vehicleCount?.let { count ->
                Text(
                    text = "$count vehicle${if (count != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
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
