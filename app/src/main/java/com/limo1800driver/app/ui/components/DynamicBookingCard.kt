package com.limo1800driver.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.theme.LimoBlack
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.LimoRed

// iOS Style Colors (kept here so the card is reusable across screens)
private val IOSBlack = LimoBlack
private val IOSLightGray = Color(0xFFE6E6E6) // Light Gray for Summary
private val IOSPassengerBg = Color(0xFFF2F2F2) // Lighter Gray for Passenger
private val IOSOrange = LimoOrange
private val IOSGreen = LimoGreen
private val IOSTeal = Color(0xFF009688)

/**
 * iOS-style booking card used across the app (ScheduledPickupsPager + PreArrangedRidesScreen).
 */
@Composable
fun DynamicBookingCard(
    booking: DriverBooking,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onFinalizeClick: () -> Unit,
    onDriverEnRouteClick: () -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Top Header (Black Background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSBlack)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = IOSOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatDate(booking.pickupDate)}, ${formatTime(booking.pickupTime)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Service Type Tag
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text(
                        text = "${booking.serviceType ?: "Service"} / ${booking.transferType ?: "Transfer"}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }

            // 2. Booking Summary (Light Gray Background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSLightGray)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${booking.bookingId}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color.Gray.copy(alpha = 0.4f))
                )

                Text(
                    text = booking.bookingStatus.replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = getStatusColor(booking.bookingStatus)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color.Gray.copy(alpha = 0.4f))
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = IOSOrange,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${booking.currencySymbol}${String.format("%.1f", booking.grandTotal ?: 0.0)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Route Details (White Background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Visual Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Black))
                    Box(modifier = Modifier.width(2.dp).height(32.dp).background(Color.Black))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.Black))
                }

                // Text Details
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = booking.pickupAddress,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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

            // 4. Passenger Information (Lighter Gray Background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSPassengerBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAX",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = booking.passengerName ?: "N/A",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color.Gray.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "${booking.passengerCellIsd ?: ""} ${booking.passengerCell ?: ""}".trim(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 5. Action Buttons (White Background)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isUnpaid = booking.paymentStatus?.lowercase() == "unpaid"
                    val isPending = booking.bookingStatus.lowercase() == "pending"

                    // Map Button
                    IOSActionButton(
                        text = "Map",
                        backgroundColor = IOSOrange,
                        textColor = Color.White,
                        leadingIcon = Icons.Default.LocationOn,
                        onClick = onMapClick,
                        modifier = Modifier.weight(if (isUnpaid) 0.3f else 1f)
                    )

                    if (isUnpaid) {
                        // Edit/Change Button
                        IOSActionButton(
                            text = "Edit/Change",
                            backgroundColor = Color.Black,
                            textColor = Color.White,
                            onClick = onEditClick,
                            modifier = Modifier.weight(1f)
                        )

                        // Finalize/Accept-Reject Button
                        IOSActionButton(
                            text = if (isPending) "Accept/Reject" else "Finalize",
                            backgroundColor = IOSGreen,
                            textColor = Color.White,
                            onClick = onFinalizeClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Driver En Route Button - Excludes "paid" and "paid_cash"
                if (shouldShowEnRouteButton(booking)) {
                    Button(
                        onClick = onDriverEnRouteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IOSTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Driver En Route",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IOSActionButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier.height(40.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// MARK: - Helpers (kept private to avoid leaking formatting rules across the app)

private fun formatDate(date: String?): String = date ?: ""
private fun formatTime(time: String?): String = time ?: ""

private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "pending" -> IOSOrange
        "paid", "completed", "finalized" -> IOSGreen
        "cancelled", "rejected" -> LimoRed
        else -> Color.Gray
    }
}

private fun shouldShowEnRouteButton(booking: DriverBooking): Boolean {
    val status = booking.bookingStatus.lowercase()
    val paymentStatus = booking.paymentStatus?.lowercase()

    val disallowedStatuses = listOf(
        "pending", "rejected", "cancelled", "ended",
        "complete", "completed"
    )

    // Explicitly check for paid statuses
    val isPaymentPaid = paymentStatus == "paid" || paymentStatus == "paid_cash"

    return status !in disallowedStatuses && !isPaymentPaid
}
/* ---------------- PREVIEW DATA ---------------- */

private fun previewBooking(
    bookingId: Int,
    bookingStatus: String,
    paymentStatus: String
) = DriverBooking(
    bookingId = bookingId,
    individualAccountId = 1,
    travelClientId = null,

    pickupDate = "2023-10-25",
    pickupTime = "14:30:00",

    dropoffLongitude = "40.6413",
    dropoffLatitude = "-73.7781",
    pickupLongitude = "40.7128",
    pickupLatitude = "-74.0060",

    changedFields = null,
    serviceType = "One Way",
    transferType = "City to Airport",

    paymentStatus = paymentStatus,
    createdByRole = "driver",
    paymentMethod = "card",
    bookingStatus = bookingStatus,

    affiliateId = 0,

    passengerName = "John Doe",
    passengerEmail = "john.doe@email.com",
    passengerCellIsd = "+1",
    passengerCellCountry = "US",
    passengerCell = "5550199",
    passengerImage = null,

    pickupAddress = "123 Main St, New York, NY 10001",
    accountType = "individual",
    dropoffAddress = "JFK Airport, Terminal 4",

    affiliateChargedAmount = null,
    vehicleCatName = "Sedan",
    affiliateType = null,
    companyName = "Limo Inc",

    affiliateDispatchIsd = "+1",
    affiliateDispatchNumber = "9998887777",
    dispatchEmail = "dispatch@limo.com",

    gigCellIsd = "+1",
    gigCellMobile = "8887776666",
    gigEmail = "driver@limo.com",

    reservationType = "scheduled",
    chargeObjectId = null,

    grandTotal = 150.50,
    currency = "USD",
    farmoutAffiliate = null,
    currencySymbol = "$",

    driverRating = 4.8,
    passengerRating = 5.0,
    showChangedFields = false
)


@Preview(showBackground = true)
@Composable
fun Preview_Unpaid() =
    DynamicBookingCard(previewBooking(1, "confirmed", "unpaid"), {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
fun Preview_Paid() =
    DynamicBookingCard(previewBooking(2, "completed", "paid"), {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
fun Preview_Pending() =
    DynamicBookingCard(previewBooking(3, "pending", "unpaid"), {}, {}, {}, {}, {})
