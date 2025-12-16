package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.components.CommonMenuHeader
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerText
import com.limo1800driver.app.ui.components.WeeklySummaryCard
import com.limo1800driver.app.ui.viewmodel.MyActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * My Activity Screen
 * Shows weekly activity summary with earnings and rides
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActivityScreen(
    onBack: () -> Unit,
    viewModel: MyActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            CommonMenuHeader(
                title = "My Activity",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Weekly Summary Card
            item {
                if (uiState.isLoading && uiState.bookings.isEmpty()) {
                    WeeklySummaryCardShimmer()
                } else {
                    WeeklySummaryCard(
                        dateRange = viewModel.getDateRangeString(),
                        earnings = viewModel.getFormattedTotalEarnings(),
                        onlineTime = viewModel.getFormattedOnlineTime(),
                        rides = uiState.totalRides.toString(),
                        onPreviousWeek = { viewModel.goToPreviousWeek() },
                        onNextWeek = { viewModel.goToNextWeek() },
                        canGoPrevious = viewModel.canGoPrevious(),
                        canGoNext = viewModel.canGoNext(),
                        currencySymbol = uiState.currencySymbol,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
            }
            
            // Activity List
            if (uiState.isLoading && uiState.bookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading activities...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else if (uiState.bookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Activity.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Group bookings by date
                val groupedBookings = viewModel.getGroupedBookings()
                val sortedDates = groupedBookings.keys.sorted()
                
                items(sortedDates) { date ->
                    ActivityDateSection(
                        date = formatDateForDisplay(date),
                        dailyTotal = viewModel.getFormattedDailyTotal(date),
                        activities = groupedBookings[date] ?: emptyList(),
                        currencySymbol = uiState.currencySymbol
                    )
                }
            }
            
            // Error State
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
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
 * Format date for display
 */
private fun formatDateForDisplay(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("EEE, MMM dd", Locale.US)
        val date = inputFormat.parse(dateString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Activity Date Section Component
 */
@Composable
private fun ActivityDateSection(
    date: String,
    dailyTotal: String,
    activities: List<DriverBooking>,
    currencySymbol: String
) {
    Column {
        // Date Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF9800) // Orange
                )
                Text(
                    text = dailyTotal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = DividerDefaults.Thickness, color = Color.Gray.copy(alpha = 0.2f)
        )

        // Activity Items
        Column {
            activities.forEachIndexed { index, booking ->
                ActivityItemView(
                    booking = booking,
                    currencySymbol = currencySymbol
                )

                if (index < activities.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = DividerDefaults.Thickness, color = Color.Gray.copy(alpha = 0.2f)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = DividerDefaults.Thickness, color = Color.Gray.copy(alpha = 0.2f)
        )
    }
}

/**
 * Activity Item View Component
 */
@Composable
private fun ActivityItemView(
    booking: DriverBooking,
    currencySymbol: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Car icon
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Black
        )
        
        // Activity details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Ride to ${booking.dropoffAddress}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1
            )
            
            Text(
                text = "${booking.pickupDate} ${booking.pickupTime}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        // Earnings
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currencySymbol,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF9800) // Orange
            )
            Text(
                text = String.format("%.2f", booking.grandTotal ?: 0.0),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

/**
 * Shimmer for Weekly Summary Card
 */
@Composable
private fun WeeklySummaryCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            // Header shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape
                )
                ShimmerText(
                    modifier = Modifier.width(150.dp),
                    height = 16.dp
                )
                ShimmerBox(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness, color = Color.Gray.copy(alpha = 0.2f)
            )

            // Metrics shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShimmerText(
                            modifier = Modifier.width(60.dp),
                            height = 12.dp
                        )
                        ShimmerText(
                            modifier = Modifier.width(50.dp),
                            height = 16.dp
                        )
                    }
                }
            }
        }
    }
}
