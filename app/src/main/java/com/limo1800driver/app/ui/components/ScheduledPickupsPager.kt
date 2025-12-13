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
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
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
                    .height(300.dp)
            ) { page ->
                if (page < bookingsState.bookings.size) {
                    val booking = bookingsState.bookings[page]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        DriverBookingCard(
                            booking = booking,
                            onEditClick = { onEditClick(booking) },
                            onViewOnMapClick = { onViewOnMapClick(booking) },
                            onDriverEnRouteClick = { onDriverEnRouteClick(booking) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Page Indicators
            if (bookingsState.bookings.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bookingsState.bookings.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .background(
                                    color = if (pagerState.currentPage == index) Color.Black else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

