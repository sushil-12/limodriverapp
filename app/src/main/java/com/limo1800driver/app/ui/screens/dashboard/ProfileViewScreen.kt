package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.limo1800driver.app.ui.components.CommonMenuHeader
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.util.noRippleClickable
import com.limo1800driver.app.ui.viewmodel.AccountSettingsViewModel
import com.limo1800driver.app.ui.viewmodel.DashboardStatsViewModel

/**
 * Profile View Screen
 * Matches the "Profile" screenshot design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewScreen(
    onBack: () -> Unit,
    onNavigateToBasicInfo: () -> Unit = {},
    viewModel: AccountSettingsViewModel = hiltViewModel(),
    statsViewModel: DashboardStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Ensure profile data is loaded when screen opens
    LaunchedEffect(key1 = uiState.profile, key2 = uiState.error) {
        // Only refresh if we don't have profile data yet or if there's an error
        if (uiState.profile == null || uiState.error != null) {
            viewModel.refreshProfileData()
        }
    }

    // Load stats data when screen opens
    LaunchedEffect(Unit) {
        statsViewModel.fetchDashboardStats()
    }

    // Get stats values - memoized for performance (with fallbacks for demo)
    val monthlyStats = remember(statsState.stats) { statsState.stats?.monthly }
    val acceptanceRate = remember { "85" } // TODO: Add to API response
    val cancellationRate = remember { "3" } // TODO: Add to API response
    val drivingInsightsScore = remember { "78" } // TODO: Add to API response
    val totalRides = remember(statsState.stats) { statsState.stats?.totalRides?.toString() ?: "0" }
    val journeyDuration = remember { "5y 3m" } // TODO: Calculate from join date

    // Get reactive values from uiState - memoized for performance
    val profile = uiState.profile
    val driverName = remember(profile) {
        profile?.let { "${it.driverFirstName ?: ""} ${it.driverLastName ?: ""}".trim() }
            .takeIf { it?.isNotEmpty() == true } ?: "Driver"
    }
    val driverRating = remember(profile) { profile?.driverRating ?: "0.0" }
    val driverImageUrl = remember(profile) { profile?.driverImage }

    Scaffold(
        topBar = {
            CommonMenuHeader(
                title = "Profile",
                onBackClick = onBack
            )
        },
        containerColor = Color(0xFFF9F9F9) // Light gray background for contrast
    ) { padding ->
        // Show loading overlay if profile data is being loaded initially
        if (uiState.isLoading && uiState.profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                ShimmerCircle(size = 48.dp)
            }
        } else if (uiState.error != null && uiState.profile == null) {
            // Show error state if loading failed and no cached data
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Failed to load profile data",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Black
                    )
                    Text(
                        text = uiState.error ?: "Unknown error occurred",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { 
                            scope.launch {
                                viewModel.refreshProfileData()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // 1. Profile Header Card
            item {
                ProfileHeaderCard(
                    name = driverName,
                    rating = driverRating,
                    imageUrl = driverImageUrl,
                    onEditClick = onNavigateToBasicInfo
                )
            }

            // 2. Rides Stats Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Rides",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // 2x2 Grid of Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = acceptanceRate,
                            suffix = "%",
                            label = "Acceptance Rate",
                            valueColor = Color.Black,
                            suffixColor = LimoOrange
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = cancellationRate,
                            suffix = "%",
                            label = "Cancellation Rate",
                            valueColor = Color.Black,
                            suffixColor = LimoOrange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = driverRating,
                            icon = Icons.Default.Star,
                            label = "Star Rating",
                            valueColor = Color.Black,
                            iconColor = LimoOrange
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = drivingInsightsScore,
                            label = "Driving Insights Score",
                            valueColor = Color.Black
                        )
                    }
                }
            }

            // 3. Lifetime Highlights Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Lifetime Highlights",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Trips
                        HighlightCard(
                            modifier = Modifier.weight(1f),
                            value = totalRides,
                            label = "Total Trips",
                            illustration = {
                                Icon(
                                    imageVector = Icons.Default.Map, // Placeholder for Map Illustration
                                    contentDescription = null,
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        )

                        // Journey Duration
                        HighlightCard(
                            modifier = Modifier.weight(1f),
                            value = journeyDuration,
                            label = "Journey With 1800limo",
                            illustration = {
                                Icon(
                                    imageVector = Icons.Default.Timeline, // Placeholder for Path Illustration
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

// --- COMPONENTS ---

@Composable
private fun ProfileHeaderCard(
    name: String,
    rating: String,
    imageUrl: String?,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFF121212).copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(121.dp) // Figma spec: 121px height
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            // Edit Button (Top Right of Card)
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = Color(0xFFF5F5F5),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .noRippleClickable { onEditClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W500,
                        color = Color(0xFF121212).copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF121212).copy(alpha = 0.8f)
                    )
                }
            }

            // Profile Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Profile Image with Rating and Edit Icon
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Profile Image
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFF5F5F5).copy(alpha = 0.96f))
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    // Orange Pencil Icon (Top Right of Image)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(LimoOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Rating Pill (Bottom Center of Image)
                    Surface(
                        shape = RoundedCornerShape(44.dp),
                        color = Color.White,
                        modifier = Modifier.offset(y = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = LimoOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = rating,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W500,
                                color = LimoOrange,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name (Right side of card)
                Text(
                    text = name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.W700,
                    color = Color(0xFF121212),
                    letterSpacing = (-0.23).sp,
                    lineHeight = 48.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    suffix: String? = null,
    icon: ImageVector? = null,
    label: String,
    valueColor: Color = Color.Black,
    suffixColor: Color = Color.Black,
    iconColor: Color = Color.Unspecified
) {
    Card(
        modifier = modifier
            .height(110.dp), // Fixed height for uniformity
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (suffix != null) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = suffix,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = suffixColor
                    )
                }
                if (icon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun HighlightCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    illustration: @Composable () -> Unit
) {
    // Transparent Card with content
    Column(modifier = modifier) {
        // Illustration Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF0F0F0)), // Light gray placeholder background
            contentAlignment = Alignment.Center
        ) {
            illustration()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Text Content
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}