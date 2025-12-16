package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.screens.onboarding.AppColors.LimoOrange
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Driver Booking Card Component
 * Matches iOS DynamicBookingCard design
 */
@Composable
fun DriverBookingCard(
    booking: DriverBooking,
    onEditClick: () -> Unit = {},
    onViewOnMapClick: () -> Unit = {},
    onDriverEnRouteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 1. Top Header (Dark Grey/Black)
            TopHeaderView(booking = booking)
            
            // 2. Booking Summary (Light Grey)
            BookingSummaryView(booking = booking)
            
            // 3. Route Details (White)
            RouteDetailsView(booking = booking)
            
            // 4. Passenger Information (Light Grey)
            PassengerInfoView(booking = booking)
            
            // 5. Action Buttons (White)
            ActionButtonsView(
                booking = booking,
                onEditClick = onEditClick,
                onViewOnMapClick = onViewOnMapClick,
                onDriverEnRouteClick = onDriverEnRouteClick
            )
        }
    }
}

@Composable
private fun TopHeaderView(booking: DriverBooking) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = LimoOrange
            )
            Text(
                text = "${formatDate(booking.pickupDate)}, ${formatTime(booking.pickupTime)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.border(1.dp, Color.Black, RoundedCornerShape(6.dp))
        ) {
            Text(
                text = "${formatServiceType(booking.serviceType ?: "")}/${formatTransferType(booking.transferType)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun BookingSummaryView(booking: DriverBooking) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE6E6E6))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${booking.bookingId}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        
        Text(
            text = booking.bookingStatus.replaceFirstChar { it.uppercase() },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = getBookingStatusColor(booking.bookingStatus)
        )
        
        Text(
            text = "Total Cost ${formatCurrency(booking.grandTotal ?: 0.0, booking.currencySymbol ?: "$")}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

@Composable
private fun RouteDetailsView(booking: DriverBooking) {
    // Use IntrinsicSize.Min to properly align timeline - matches UserBookingCard
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .height(IntrinsicSize.Min)
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline Visual - matches UserBookingCard
        Column(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Pickup Circle
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
            
            // 2. Connecting Line (Fills remaining space)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .weight(1f)
                    .background(Color.LightGray)
            )
            
            // 3. Dropoff Square
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black)
            )
        }
        
        // Addresses - matches UserBookingCard layout
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Pickup Address (Top aligned with Circle)
            Text(
                text = booking.pickupAddress,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Dropoff Address (Bottom aligned with Square)
            Text(
                text = booking.dropoffAddress,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PassengerInfoView(booking: DriverBooking) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE6E6E6))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Passenger",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = booking.passengerName ?: "N/A",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
        
        if (booking.passengerCell != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "${booking.passengerCellIsd ?: ""}${booking.passengerCell}",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsView(
    booking: DriverBooking,
    onEditClick: () -> Unit,
    onViewOnMapClick: () -> Unit,
    onDriverEnRouteClick: () -> Unit
) {
    val isUnpaid = booking.paymentStatus?.lowercase() == "unpaid"
    val isPending = booking.bookingStatus.lowercase() == "pending"
    val shouldShowEnRouteButton = shouldShowDriverEnRouteButton(booking)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // First row: Map, Edit/Change, Accept/Reject or Finalize
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Map button - always visible (orange)
            Button(
                onClick = onViewOnMapClick,
                colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                modifier = Modifier.weight(if (isUnpaid) 1f else 1f)
            ) {
                Text(
                    "Map",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            // Edit/Change button - only if unpaid (black)
            if (isUnpaid) {
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Edit/Change",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            
            // Accept/Reject or Finalize button - only if unpaid (green)
            if (isUnpaid) {
                Button(
                    onClick = onEditClick, // TODO: Replace with actual finalize action
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008F43)), // finalizeGreen
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (isPending) "Accept/Reject" else "Finalize",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        
        // Driver En Route button - on separate line if needed (purple)
        if (shouldShowEnRouteButton) {
            Button(
                onClick = onDriverEnRouteClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6454AC)), // enRoutePuStatus
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Driver En Route",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// Helper function to determine if Driver En Route button should be shown
private fun shouldShowDriverEnRouteButton(booking: DriverBooking): Boolean {
    // Show if pickup time is within the next 2 hours (matching iOS logic)
    return try {
        val pickupDate = booking.pickupDate
        val pickupTime = booking.pickupTime
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val pickupDateTime = dateFormat.parse("$pickupDate $pickupTime")
        
        if (pickupDateTime != null) {
            val now = java.util.Date()
            val twoHoursFromNow = java.util.Date(now.time + 2 * 60 * 60 * 1000)
            pickupDateTime.after(now) && pickupDateTime.before(twoHoursFromNow)
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

// Helper functions
private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun formatTime(timeString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = inputFormat.parse(timeString)
        time?.let { outputFormat.format(it) } ?: timeString
    } catch (e: Exception) {
        timeString
    }
}

private fun formatServiceType(serviceType: String): String {
    return serviceType.replaceFirstChar { it.uppercase() }
}

private fun formatTransferType(transferType: String): String {
    return transferType.replaceFirstChar { it.uppercase() }
}

private fun formatCurrency(amount: Double, symbol: String): String {
    return "$symbol${String.format("%.2f", amount)}"
}

private fun getBookingStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "paid", "completed" -> Color(0xFF4CAF50)
        "pending" -> Color(0xFFFF9800)
        "cancelled" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
}

