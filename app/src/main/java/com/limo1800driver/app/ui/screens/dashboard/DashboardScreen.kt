package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.ui.viewmodel.DashboardStatsViewModel
import com.limo1800driver.app.ui.viewmodel.DriverBookingsViewModel
import com.limo1800driver.app.ui.viewmodel.DriverProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Dashboard Screen for Driver App
 * Displays stats, bookings, and navigation options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToBooking: (Int) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToMyActivity: () -> Unit = {},
    onLogout: () -> Unit = {},
    statsViewModel: DashboardStatsViewModel = hiltViewModel(),
    bookingsViewModel: DriverBookingsViewModel = hiltViewModel(),
    profileViewModel: DriverProfileViewModel = hiltViewModel()
) {
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()
    val bookingsState by bookingsViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    
    // Fetch profile on first load
    LaunchedEffect(Unit) {
        profileViewModel.fetchDriverProfile()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Text("Profile", fontSize = 14.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Section
            item {
                StatsSection(statsState = statsState)
            }
            
            // Bookings Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Bookings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    TextButton(onClick = onNavigateToMyActivity) {
                        Text("View All")
                    }
                }
            }
            
            // Bookings List
            if (bookingsState.isLoading && bookingsState.bookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (bookingsState.bookings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No bookings found",
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                items(bookingsState.bookings) { booking ->
                    BookingCard(
                        booking = booking,
                        onClick = { onNavigateToBooking(booking.bookingId) }
                    )
                }
                
                // Load more button
                if (bookingsState.canLoadMore) {
                    item {
                        Button(
                            onClick = { bookingsViewModel.loadMoreBookings() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !bookingsState.isLoading
                        ) {
                            if (bookingsState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Load More")
                            }
                        }
                    }
                }
            }
            
            // Error State
            bookingsState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
 * Stats Section Component
 */
@Composable
private fun StatsSection(
    statsState: com.limo1800driver.app.ui.viewmodel.DashboardStatsUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (statsState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                statsState.stats?.let { stats ->
                    // Monthly Earnings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Earnings",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.currencySymbol ?: "$"}${String.format("%.2f", stats.monthly?.earnings ?: 0.0)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // Today's Rides
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Today's Rides",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%02d", stats.today?.rides ?: 0),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    
                    // Weekly Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Weekly Earnings",
                            value = "${stats.currencySymbol ?: "$"}${String.format("%.2f", stats.weekly?.earnings ?: 0.0)}"
                        )
                        StatItem(
                            label = "Weekly Rides",
                            value = "${stats.weekly?.rides ?: 0}"
                        )
                    }
                } ?: run {
                    Text(
                        text = "No stats available",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Stat Item Component
 */
@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Booking Card Component
 */
@Composable
private fun BookingCard(
    booking: com.limo1800driver.app.data.model.dashboard.DriverBooking,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Booking ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Booking #${booking.bookingId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (booking.bookingStatus.lowercase()) {
                        "paid" -> Color(0xFF4CAF50)
                        "pending" -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    }.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = booking.bookingStatus.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (booking.bookingStatus.lowercase()) {
                            "paid" -> Color(0xFF4CAF50)
                            "pending" -> Color(0xFFFF9800)
                            else -> Color(0xFF9E9E9E)
                        }
                    )
                }
            }
            
            // Pickup and Dropoff
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFFFF9800),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = booking.pickupAddress,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFFF44336),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = booking.dropoffAddress,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Divider()
            
            // Date, Time, and Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${booking.pickupDate} ${booking.pickupTime}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (booking.passengerName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Passenger: ${booking.passengerName}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                if (booking.grandTotal != null) {
                    Text(
                        text = "${booking.currencySymbol}${String.format("%.2f", booking.grandTotal)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

