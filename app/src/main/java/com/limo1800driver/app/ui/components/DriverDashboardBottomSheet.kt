package com.limo1800driver.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerText

/**
 * Bottom sheet for driver dashboard with recent bookings
 * Uber-style draggable bottom sheet - adapted from user app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardBottomSheet(
    isExpanded: Boolean,
    recentBookings: List<DriverBooking>,
    isLoading: Boolean,
    onToggleExpansion: () -> Unit,
    onRefresh: () -> Unit,
    onBookingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var sheetHeight by remember { mutableStateOf(0.25f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Update sheet height based on expansion state
    LaunchedEffect(isExpanded) {
        if (!isDragging) {
            sheetHeight = if (isExpanded) 0.7f else 0.25f
        }
    }
    
    val animatedHeight by animateFloatAsState(
        targetValue = sheetHeight,
        animationSpec = tween(300),
        label = "bottom_sheet_height"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(animatedHeight)
            .background(
                Color.White,
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { 
                        isDragging = false
                        // Snap to nearest position
                        sheetHeight = if (sheetHeight > 0.5f) 0.7f else 0.25f
                    }
                ) { _, dragAmount ->
                    val dragAmountDp = with(density) { dragAmount.y.toDp() }
                    val screenHeightDp = with(density) { size.height.toDp() }
                    val dragPercentage = -dragAmountDp / screenHeightDp
                    
                    sheetHeight = (sheetHeight + dragPercentage).coerceIn(0.15f, 0.8f)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Handle bar
            HandleBar(
                onToggleExpansion = onToggleExpansion,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sheetHeight > 0.5f) {
                // Expanded content
                ExpandedContent(
                    recentBookings = recentBookings,
                    isLoading = isLoading,
                    onRefresh = onRefresh,
                    onBookingClick = onBookingClick
                )
            } else {
                // Collapsed content
                CollapsedContent(
                    recentBookings = recentBookings
                )
            }
        }
    }
}

/**
 * Handle bar for dragging the bottom sheet
 */
@Composable
private fun HandleBar(
    onToggleExpansion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(4.dp)
            .background(
                Color.Gray.copy(alpha = 0.5f),
                RoundedCornerShape(2.dp)
            )
            .clickable { onToggleExpansion() }
    )
}

/**
 * Collapsed content showing quick overview
 */
@Composable
private fun CollapsedContent(
    recentBookings: List<DriverBooking>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quick info
        Column {
            Text(
                text = "Recent Bookings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${recentBookings.size} bookings",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * Expanded content with full booking list
 */
@Composable
private fun ExpandedContent(
    recentBookings: List<DriverBooking>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBookingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Bookings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Refresh button
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFFFF9800) // Orange color
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bookings list - Horizontal scroll
        if (isLoading) {
            LoadingState()
        } else if (recentBookings.isEmpty()) {
            EmptyState()
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(recentBookings) { booking ->
                    DriverBookingCardHorizontal(
                        booking = booking,
                        onClick = { onBookingClick(booking.bookingId) },
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

/**
 * Loading state - show shimmer booking cards
 */
@Composable
private fun LoadingState() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(2) {
            BookingCardShimmerHorizontal()
        }
    }
}

/**
 * Shimmer for horizontal booking card
 */
@Composable
private fun BookingCardShimmerHorizontal() {
    Card(
        modifier = Modifier
            .width(340.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Top Header shimmer
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            
            // Booking Summary shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE6E6E6))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerText(modifier = Modifier.width(60.dp), height = 14.dp)
                ShimmerText(modifier = Modifier.width(80.dp), height = 14.dp)
                ShimmerText(modifier = Modifier.width(100.dp), height = 14.dp)
            }
            
            // Route Details shimmer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBox(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.8f), height = 14.dp)
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 12.dp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBox(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.8f), height = 14.dp)
                        ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 12.dp)
                    }
                }
            }
        }
    }
}

/**
 * Empty state
 */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "No Bookings",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )
        
        Text(
            text = "No recent bookings",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        
        Text(
            text = "Your recent bookings will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

/**
 * Simplified horizontal booking card for bottom sheet
 */
@Composable
private fun DriverBookingCardHorizontal(
    booking: DriverBooking,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF424242))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${booking.pickupDate} ${booking.pickupTime}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = booking.serviceType ?: "Service",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${booking.bookingId}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                Text(
                    text = booking.bookingStatus.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (booking.bookingStatus.lowercase()) {
                            "paid" -> Color(0xFF4CAF50)
                            "pending" -> Color(0xFFFF9800)
                            else -> Color.Gray
                        }
                    )
                )
                
                booking.grandTotal?.let {
                    Text(
                        text = "$${String.format("%.2f", it)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            
            // Route
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = booking.pickupAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Pickup",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Red)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = booking.dropoffAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Dropoff",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

