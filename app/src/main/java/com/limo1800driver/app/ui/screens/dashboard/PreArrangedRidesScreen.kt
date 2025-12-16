package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.components.*
import com.limo1800driver.app.ui.theme.LimoBlack
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.theme.LimoGrey
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.LimoRed
import com.limo1800driver.app.ui.viewmodel.PreArrangedRidesViewModel
import com.limo1800driver.app.ui.viewmodel.TimePeriod

// -- Constants --
private val ScreenBackgroundColor = Color.White
private val CardBackgroundColor = Color.White

// iOS Style Colors
private val IOSBlack = LimoBlack
private val IOSLightGray = Color(0xFFE6E6E6) // Light Gray for Summary
private val IOSPassengerBg = Color(0xFFF2F2F2) // Lighter Gray for Passenger
private val IOSOrange = LimoOrange
private val IOSGreen = LimoGreen
private val IOSTeal = Color(0xFF009688)

/**
 * Pre-arranged Rides Screen
 * Matches Android user app MyBookingsScreen design exactly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreArrangedRidesScreen(
    onBack: () -> Unit,
    onBookingClick: (Int) -> Unit = {},
    onNavigateToBookingPreview: (Int) -> Unit = {},
    onNavigateToFinalizeBooking: (bookingId: Int, source: String) -> Unit = { _, _ -> },
    onNavigateToFinalizeRates: (bookingId: Int, mode: String, source: String) -> Unit = { _, _, _ -> },
    onNavigateToEditBooking: (bookingId: Int, source: String) -> Unit = { _, _ -> },
    onNavigateToMap: (Int) -> Unit = {},
    refreshRequested: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    viewModel: PreArrangedRidesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(refreshRequested) {
        if (refreshRequested) {
            viewModel.refreshBookings()
            onRefreshConsumed()
        }
    }

    // Clear search when closing search bar
    LaunchedEffect(showSearchBar) {
        if (!showSearchBar) viewModel.clearSearch()
    }

    // Main Scaffold
    Scaffold(
        containerColor = ScreenBackgroundColor,
        topBar = {
            // Custom Header that handles Safe Area (Status Bar)
            LocalCommonHeaderWithSearch(
                title = "Pre-arranged Rides",
                onBackClick = onBack,
                onSearchClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) viewModel.clearSearch()
                },
                isSearching = showSearchBar
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Search Bar (Visible only when toggled)
            if (showSearchBar) {
                SearchBar(
                    searchText = uiState.searchText,
                    onSearchTextChange = { viewModel.setSearchText(it) },
                    onClearSearch = { viewModel.clearSearch() }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. Controls Section (Summary + Time Selector + Date Picker)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackgroundColor)
                            .padding(bottom = 16.dp)
                    ) {
                        SummaryCard(
                            dateRange = viewModel.getDateRangeString(),
                            // FIX: Call generic methods directly, removed helper functions
                            onPreviousPeriod = { viewModel.goToPreviousPeriod() },
                            onNextPeriod = { viewModel.goToNextPeriod() },
                            canGoPrevious = viewModel.canGoPrevious(),
                            canGoNext = viewModel.canGoNext()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TimePeriodSelector(
                            selectedTimePeriod = uiState.selectedTimePeriod,
                            onTimePeriodChange = viewModel::handleTimePeriodChange,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        if (uiState.selectedTimePeriod == TimePeriod.CUSTOM) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CustomDateRangePicker(
                                startDate = uiState.selectedWeekStart,
                                endDate = uiState.selectedWeekEnd,
                                onDateRangeSelected = { startDate, endDate ->
                                    viewModel.handleDateRangeSelection(startDate, endDate)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // 2. Bookings List
                when {
                    uiState.isLoading && uiState.bookings.isEmpty() -> {
                        items(3) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                BookingCardShimmer()
                            }
                        }
                    }
                    uiState.error != null && uiState.bookings.isEmpty() -> {
                        item {
                            ErrorView(
                                message = uiState.error,
                                onRetry = { viewModel.refreshBookings() }
                            )
                        }
                    }
                    uiState.bookings.isEmpty() -> {
                        item { EmptyBookingsView(hasSearchQuery = uiState.searchText.isNotBlank()) }
                    }
                    else -> {
                        items(uiState.bookings) { booking ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                DynamicBookingCard(
                                    booking = booking,
                                    onClick = { onBookingClick(booking.bookingId) },
                                    onEditClick = { onNavigateToEditBooking(booking.bookingId, "prearranged") },
                                    onFinalizeClick = {
                                        val isPending = booking.bookingStatus.equals("pending", ignoreCase = true)
                                        if (isPending) {
                                            onNavigateToBookingPreview(booking.bookingId)
                                        } else {
                                            // Finalize should go directly to rates screen (single-step flow)
                                            onNavigateToFinalizeRates(booking.bookingId, "finalizeOnly", "prearranged")
                                        }
                                    },
                                    onDriverEnRouteClick = { /* Handle En Route Action */ },
                                    onMapClick = { onNavigateToMap(booking.bookingId) },
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        // Load More Button
                        if (uiState.currentPage < uiState.totalPages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoading) {
                                        ShimmerBox(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Button(
                                            onClick = { viewModel.loadMoreBookings() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color.Gray),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Load More", color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Reused Components (Header, Search, etc.)

@Composable
private fun LocalCommonHeaderWithSearch(
    title: String,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    isSearching: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search bookings...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF0F0F0),
                unfocusedContainerColor = Color(0xFFF0F0F0),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = LimoOrange
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun SummaryCard(
    dateRange: String,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousPeriod,
                enabled = canGoPrevious,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    tint = if (canGoPrevious) Color.Black else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = dateRange,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            IconButton(
                onClick = onNextPeriod,
                enabled = canGoNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = if (canGoNext) Color.Black else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedTimePeriod: TimePeriod,
    onTimePeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TimePeriod.values().forEach { period ->
                val isSelected = selectedTimePeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onTimePeriodChange(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun BookingCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.height(16.dp).width(150.dp).clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier.height(20.dp).width(80.dp).clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(width = 50.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp)).shimmerEffect()
                )
                Box(
                    modifier = Modifier.size(width = 60.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp)).shimmerEffect()
                )
                Box(
                    modifier = Modifier.size(width = 70.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp)).shimmerEffect()
                )
            }
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0F0F0))
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.height(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).shimmerEffect())
                    Box(modifier = Modifier.width(2.dp).height(30.dp).shimmerEffect())
                    Box(
                        modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp))
                            .shimmerEffect()
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.9f).height(14.dp)
                            .clip(RoundedCornerShape(4.dp)).shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(0.7f).height(14.dp)
                            .clip(RoundedCornerShape(4.dp)).shimmerEffect()
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBookingsView(hasSearchQuery: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, start = 48.dp, end = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "No Bookings",
            modifier = Modifier.size(180.dp),
            tint = Color.Gray.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Bookings Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasSearchQuery) "There are no bookings matching your search." else "There are no bookings for the selected date range.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ErrorView(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, contentDescription = null, tint = LimoRed)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message ?: "Unknown Error")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}