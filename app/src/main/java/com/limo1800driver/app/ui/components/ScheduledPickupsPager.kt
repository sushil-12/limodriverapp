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
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.R
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.screens.onboarding.AppColors.LimoOrange
import com.limo1800driver.app.ui.viewmodel.ScheduledPickupsViewModel

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
        scheduledPickupsViewModel: ScheduledPickupsViewModel,
        modifier: Modifier = Modifier
    ) {
        val scheduledPickupsState by scheduledPickupsViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            if (scheduledPickupsState.bookings.isEmpty() && !scheduledPickupsState.isLoading) {
                scheduledPickupsViewModel.fetchScheduledPickups(resetData = true)
            }
        }

        Column(modifier = modifier) {
            if (scheduledPickupsState.isLoading && scheduledPickupsState.bookings.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LimoOrange)
                }
            } else if (scheduledPickupsState.bookings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Icon(
                            painter = painterResource(id = R.drawable.pickup_no),
                            contentDescription = "No scheduled pickups",
                            modifier = Modifier.size(52.dp),
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )

                        Text(
                            text = "No Scheduled Pickups ",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Your scheduled pickups in the next 24 hours will appear here.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else {
                val pagerState = rememberPagerState(pageCount = { scheduledPickupsState.bookings.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp), // Centers card nicely
                    pageSpacing = 8.dp
                ) { page ->
                    val booking = scheduledPickupsState.bookings[page]
                    DynamicBookingCard(
                        booking = booking,
                        onClick = { onBookingSelected(booking) },
                        onEditClick = { onEditClick(booking) },
                        onFinalizeClick = { onFinalizeClick(booking) },
                        onDriverEnRouteClick = { onDriverEnRouteClick(booking) },
                        onMapClick = { onViewOnMapClick(booking) },
                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                    )
                }

                if (scheduledPickupsState.bookings.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        scheduledPickupsState.bookings.indices.forEach { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 10.dp else 8.dp)
                                    .background(
                                        color = if (isSelected) Color.Black else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }