package com.limo1800driver.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.res.painterResource
import android.content.res.Resources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.dashboard.DriverProfileData
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerText
import com.limo1800driver.app.ui.theme.LimoRed
import com.limo1800driver.app.ui.theme.LimoOrange

/**
 * Helper composable to safely load an icon with fallback
 * Handles cases where a drawable resource might not exist
 */
@Composable
private fun SafeIcon(
    painterResourceId: Int,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val resourceExists = remember(painterResourceId) {
        try {
            context.resources.getResourceName(painterResourceId)
            true
        } catch (e: Resources.NotFoundException) {
            false
        }
    }
    
    if (resourceExists) {
        Icon(
            painter = painterResource(id = painterResourceId),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = Color.Unspecified
        )
    } else {
        Icon(
            imageVector = fallbackIcon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

/**
 * Dashboard Drawer Menu Component
 * Matches Android user app NavigationDrawer design exactly
 */
@Composable
fun DashboardDrawerMenu(
    isPresented: Boolean,
    driverProfile: DriverProfileData?,
    isLoading: Boolean,
    vehicleName: String? = null,
    vehicleMakeModel: String? = null,
    vehicleYear: String? = null,
    vehicleColor: String? = null,
    vehicleImageUrl: String? = null,
    notificationBadge: String? = null,
    onClose: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPreArrangedRides: () -> Unit,
    onNavigateToMyActivity: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isPresented,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() }
        ) {
            // Drawer content - full screen from left (matching user app)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable(enabled = false) { /* Prevent closing when clicking drawer content */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(20.dp)
                ) {
                    // Driver Profile Section with close button
                    DriverProfileSectionWithClose(
                        driverProfile = driverProfile,
                        isLoading = isLoading,
                        onClose = onClose,
                        onProfileClick = {
                            onClose()
                            onNavigateToProfile()
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Vehicle Details Card
                    VehicleDetailsCard(
                        vehicleName = vehicleName,
                        vehicleMakeModel = vehicleMakeModel,
                        vehicleYear = vehicleYear,
                        vehicleColor = vehicleColor,
                        vehicleImageUrl = vehicleImageUrl,
                        isLoading = isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Menu Items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        DriverMenuOption(
                            title = "Pre-arranged Rides",
                            onClick = {
                                onClose()
                                onNavigateToPreArrangedRides()
                            }
                        )
                        
                        DriverMenuOption(
                            title = "My Activity",
                            onClick = {
                                onClose()
                                onNavigateToMyActivity()
                            }
                        )
                        
                        DriverMenuOption(
                            title = "Wallet",
                            onClick = {
                                onClose()
                                onNavigateToWallet()
                            }
                        )
                        
                        DriverMenuOption(
                            title = "Notifications",
                            badge = notificationBadge,
                            onClick = {
                                onClose()
                                onNavigateToNotifications()
                            }
                        )
                        
                        DriverMenuOption(
                            title = "Account Settings",
                            onClick = {
                                onClose()
                                onNavigateToAccountSettings()
                            }
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Logout option
                        DriverLogoutOption(
                            onClick = {
                                onClose()
                                onLogout()
                            }
                        )
                    }
                    
                    // App Version
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Driver Profile Section matching user app design
 */
@Composable
private fun DriverProfileSectionWithClose(
    driverProfile: DriverProfileData?,
    isLoading: Boolean,
    onClose: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clickable { onProfileClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // Profile Image - 48dp circle (matching user app)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5).copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (!driverProfile?.driverImage.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(driverProfile?.driverImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Image",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Gray
                )
            }
        }
        
        // Driver Info Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (isLoading) {
                // Shimmer loading placeholders
                ShimmerText(
                    modifier = Modifier.width(120.dp),
                    height = 18.dp
                )
                Spacer(modifier = Modifier.height(3.dp))
                ShimmerText(
                    modifier = Modifier.width(100.dp),
                    height = 12.dp
                )
            } else {
                // Driver Name - 18sp matching user app
                Text(
                    text = buildString {
                        driverProfile?.driverFirstName?.let { append(it) }
                        driverProfile?.driverLastName?.let {
                            if (isNotEmpty()) append(" ")
                            append(it)
                        }
                        if (isEmpty()) append("Driver")
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF121212),
                    lineHeight = 24.sp // 150% of 18sp = 24sp
                )
                
                // Phone Number - 12sp matching user app
                val displayPhone = remember(driverProfile?.driverPhone, driverProfile?.driverCellIsd, driverProfile?.driverCellNumber) {
                    driverProfile?.driverPhone?.trim()?.takeIf { it.isNotEmpty() }
                        ?: formatPhone(driverProfile?.driverCellIsd, driverProfile?.driverCellNumber)
                }
                Text(
                    text = displayPhone ?: "No phone number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF121212).copy(alpha = 0.4f),
                    lineHeight = 18.sp // 150% of 12sp = 18sp
                )
            }
        }
        
        // Close button - Red circle matching user app
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .background(LimoRed, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
    }
}

private fun formatPhone(isd: String?, number: String?): String? {
    val n = number?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val i = isd?.trim()?.takeIf { it.isNotEmpty() }
    return if (i == null) n else "${if (i.startsWith("+")) i else "+$i"} $n"
}

/**
 * Vehicle Details Card
 */
@Composable
private fun VehicleDetailsCard(
    vehicleName: String?,
    vehicleMakeModel: String?,
    vehicleYear: String?,
    vehicleColor: String?,
    vehicleImageUrl: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        height = 20.dp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(60.dp)
                                .height(24.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(50.dp)
                                .height(24.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(24.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                } else {
                    // Vehicle Name
                    Text(
                        text = vehicleName ?: "Vehicle",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LimoOrange
                    )
                    
                    // Vehicle Tags
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vehicleMakeModel?.let {
                            VehicleTag(text = it)
                        }
                        vehicleYear?.let {
                            VehicleTag(text = it)
                        }
                        vehicleColor?.let {
                            VehicleTag(text = it)
                        }
                    }
                }
            }
            
            // Vehicle Image
            if (isLoading) {
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            } else if (!vehicleImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vehicleImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Vehicle Image",
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Vehicle Tag Component
 */
@Composable
private fun VehicleTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = modifier
            .height(24.dp)
            .background(
                Color(0xFFFAF2E3), // Light cream/orange background
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        maxLines = 1
    )
}

/**
 * Menu Option Component matching user app design
 */
@Composable
private fun DriverMenuOption(
    title: String,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(54.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu item text with matching typography
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, // Font weight 590
                color = Color(0xFF121212),
                lineHeight = 30.sp, // 150% of 20sp = 30sp
                modifier = Modifier.weight(1f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                badge?.let { badgeText ->
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFFFF9800), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // Arrow icon - use custom icon with fallback to chevron
                SafeIcon(
                    painterResourceId = R.drawable.icon_left_menu_icon,
                    fallbackIcon = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFD9D9D9) // Gray matching user app
                )
            }
        }
    }
}

/**
 * Logout Option Component matching user app design
 */
@Composable
private fun DriverLogoutOption(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logout icon - use custom icon with fallback to default
        SafeIcon(
            painterResourceId = R.drawable.line_md_logout,
            fallbackIcon = Icons.Default.Logout,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFFCE0000) // Red matching user app
        )
        
        Text(
            text = "Logout",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFCE0000)
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}
