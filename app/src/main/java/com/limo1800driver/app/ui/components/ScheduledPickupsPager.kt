package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.viewmodel.DriverBookingsViewModel

/**
 * Scheduled Pickups Pager Component
 * Displays scheduled bookings in a horizontal pager
 */
@Composable
fun ScheduledPickupsPager(
    onBookingSelected: (DriverBooking) -> Unit = {},
    onEditClick: (DriverBooking) -> Unit = {},
    onViewOnMapClick: (DriverBooking) -> Unit = {},
    onDriverEnRouteClick: (DriverBooking) -> Unit = {},
    onFinalizeClick: (DriverBooking) -> Unit = {},
    bookingsViewModel: DriverBookingsViewModel,
    modifier: Modifier = Modifier
) {
    val bookingsState by bookingsViewModel.uiState.collectAsStateWithLifecycle()
    
    // Fetch scheduled pickups for today - only if bookings are empty
    LaunchedEffect(Unit) {
        if (bookingsState.bookings.isEmpty() && !bookingsState.isLoading) {
            bookingsViewModel.fetchBookings(resetData = true)
        }
    }
    
    Column(modifier = modifier) {
        if (bookingsState.isLoading && bookingsState.bookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerCircle(
                        size = 24.dp,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading scheduled pickups...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        } else if (bookingsState.bookings.isEmpty()) {
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
                    Text(
                        text = "📅",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "No scheduled pickups",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "You don't have any scheduled pickups for today",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else if (bookingsState.bookings.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { bookingsState.bookings.size })
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    // DynamicBookingCard is fairly tall; 300dp can squeeze child layouts (buttons/text).
                    .height(360.dp)
            ) { page ->
                if (page < bookingsState.bookings.size) {
                    val booking = bookingsState.bookings[page]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Parent screens (e.g., Dashboard tab content) already apply horizontal padding.
                            // Adding more here makes the card noticeably narrower (and buttons look smaller).
                            .padding(horizontal = 0.dp)
                    ) {
                        DynamicBookingCard(
                            booking = booking,
                            onClick = { onBookingSelected(booking) },
                            onEditClick = { onEditClick(booking) },
                            onFinalizeClick = { onFinalizeClick(booking) },
                            onDriverEnRouteClick = { onDriverEnRouteClick(booking) },
                            onMapClick = { onViewOnMapClick(booking) },
                            modifier = Modifier
                                .fillMaxWidth()
                                // Small side padding so the card doesn't feel "stuck" to the edges while swiping.
                                .padding(horizontal = 8.dp)
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
            
            // Page Indicators
            if (bookingsState.bookings.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp), // Added slightly more top padding for separation
                    // FIX 1: Use spacedBy to add gap between circles
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bookingsState.bookings.indices.forEach { index ->
                        // Extract logic for readability
                        val isSelected = pagerState.currentPage == index

                        Box(
                            modifier = Modifier
                                // FIX 2: Remove the internal padding. The Row handles spacing now.
                                .size(if (isSelected) 10.dp else 8.dp)
                                .background(
                                    color = if (isSelected) Color.Black else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

