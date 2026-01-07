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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.data.model.dashboard.DriverBooking
import com.limo1800driver.app.ui.theme.* // Assuming your theme imports
import java.text.SimpleDateFormat
import java.util.*

// --- COLORS (Kept locally for portability) ---
private val IOSBlack = Color(0xFF000000)

private val Blue = Color(0xFF0474CF)
private val IOSLightGray = Color(0xFFE6E6E6)
private val IOSPassengerBg = Color(0xFFF2F2F2)
private val IOSOrange = Color(0xFFFF9800) // Replace with LimoOrange if available
private val IOSGreen = LimoGreen

private val IOSYellow = Color(0xFFD0B001)

private val IOSFINALIZE = Color(0xFF7EC8E3)

private val IOSTeal = Color(0xFF009688)
private val LimoRed = Color(0xFFF44336)

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
    // Responsive scaling based on screen width
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val scale = (screenWidth / 375f).coerceIn(0.85f, 1.1f) // Gentle scaling: 85% on small screens, up to 110% on large

    // Base sizes (optimized for readability without being too large)
    val headerDateTimeSize = (13f * scale).sp
    val serviceBadgeSize = (12f * scale).sp
    val summaryTextSize = (14f * scale).sp
    val addressTextSize = (14f * scale).sp
    val paxLabelSize = (12f * scale).sp
    val paxInfoSize = (14f * scale).sp
    val buttonTextSize = (13f * scale).sp

    // Padding & spacing scaling
    val defaultPadding = (16f * scale).dp
    val smallPadding = (12f * scale).dp
    val tinyPadding = (8f * scale).dp

    // Format data
    val formattedDate = remember(booking.pickupDate) { formatToDisplayDate(booking.pickupDate) }
    val formattedTime = remember(booking.pickupTime) { formatToDisplayTime(booking.pickupTime) }
    val formattedTransferType = remember(booking.transferType) { cleanText(booking.transferType) }
    val formattedServiceType = remember(booking.serviceType) { cleanText(booking.serviceType) }

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

            // --- 1. TOP HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSBlack)
                    .padding(horizontal = defaultPadding, vertical = smallPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // 1. Vertically centers the "Left Group" and "Right Badge"
            ) {
                // Left Group: Icon + Text Column
                Row(
                    verticalAlignment = Alignment.CenterVertically, // 2. Vertically centers the Icon against the Text Column
                    horizontalArrangement = Arrangement.spacedBy(tinyPadding)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = IOSOrange,
                        modifier = Modifier.size((16f * scale).dp)
                    )
                    Column(
                        // 3. Ensures the text stack is packed tightly
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formattedDate,
                            color = Color.White,
                            fontSize = headerDateTimeSize,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Use 'Modifier' (capital M) to avoid inheriting parent padding
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = formattedTime,
                            color = Color.White,
                            fontSize = headerDateTimeSize,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Group: Badge
                Surface(
                    color = Color(0xFFE5E5EA), // Changed to iOS Light Gray to match screenshot
                    shape = RoundedCornerShape(6.dp)
                    // Removed BorderStroke to match the filled style in screenshot
                ) {
                    Text(
                        text = "$formattedServiceType/ $formattedTransferType",
                        modifier = Modifier.padding(horizontal = smallPadding, vertical = tinyPadding),
                        fontSize = serviceBadgeSize,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- 2. BOOKING SUMMARY ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSLightGray)
                    .padding(horizontal = smallPadding, vertical = (10f * scale).dp),
                horizontalArrangement = Arrangement.spacedBy(smallPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${booking.bookingId}",
                    fontSize = summaryTextSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = booking.bookingStatus.replaceFirstChar { it.uppercase() },
                        fontSize = summaryTextSize,
                        fontWeight = FontWeight.SemiBold,
                        color = getBookingStatusColor(booking.bookingStatus),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(tinyPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        fontSize = summaryTextSize,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Surface(color = IOSOrange, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = formatCurrency(booking.grandTotal ?: 0.0, booking.currencySymbol ?: "$"),
                            modifier = Modifier.padding(horizontal = (10f * scale).dp, vertical = (6f * scale).dp),
                            fontSize = summaryTextSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // --- 3. ROUTE DETAILS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = defaultPadding, vertical = smallPadding),
                horizontalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size((12f * scale).dp).clip(CircleShape).background(Color.Black))
                    Box(modifier = Modifier.width(2.dp).height((20f * scale).dp).background(Color.Black))
                    Box(modifier = Modifier.size((12f * scale).dp).clip(RoundedCornerShape(2.dp)).background(Color.Black))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(smallPadding)
                ) {
                    Text(
                        text = booking.pickupAddress,
                        fontSize = addressTextSize,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = booking.dropoffAddress,
                        fontSize = addressTextSize,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- 4. PASSENGER INFO ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSPassengerBg)
                    .padding(horizontal = defaultPadding, vertical = smallPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                Text(
                    text = "PAX",
                    fontSize = paxLabelSize,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = smallPadding, vertical = tinyPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((10f * scale).dp)
                    ) {
                        Text(
                            text = booking.passengerName ?: "N/A",
                            fontSize = paxInfoSize,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height((18f * scale).dp)
                                .background(Color.Gray.copy(alpha = 0.3f))
                        )
                        Text(
                            text = "${booking.passengerCellIsd ?: ""} ${booking.passengerCell ?: ""}".trim(),
                            fontSize = paxInfoSize,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // --- 5. ACTION BUTTONS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = defaultPadding, vertical = (10f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((10f * scale).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(smallPadding)
                ) {
                    val isUnpaid = booking.paymentStatus?.lowercase() == "unpaid"
                    val isPending = booking.bookingStatus.lowercase() == "pending"

                    Button(
                        onClick = onMapClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IOSOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = (10f * scale).dp),
                        modifier = Modifier.height((40f * scale).dp)
                    ) {
                        Text("Map", fontSize = buttonTextSize, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    if (isUnpaid) {
                        Button(
                            onClick = onEditClick,
                            colors = ButtonDefaults.buttonColors(containerColor = IOSBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height((40f * scale).dp)
                        ) {
                            Text("Edit/Change", fontSize = buttonTextSize, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }

                        Button(
                            onClick = onFinalizeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = getFinalizeGreenColor()),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = (10f * scale).dp),
                            modifier = Modifier.weight(1f).height((40f * scale).dp)
                        ) {
                            Text(
                                text = if (isPending) "Accept/Reject" else "Finalize",
                                fontSize = buttonTextSize,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (shouldShowEnRouteButton(booking, onDriverEnRouteClick)) {
                    Button(
                        onClick = onDriverEnRouteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IOSTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height((40f * scale).dp)
                    ) {
                        Text("Driver En Route", fontSize = buttonTextSize, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// --- HELPERS (unchanged except minor scaling) ---
@Composable
fun BookingStatusBadge(status: String, modifier: Modifier = Modifier) {
    val scale = (LocalConfiguration.current.screenWidthDp / 375f).coerceIn(0.85f, 1.1f)
    val fontSize = (13f * scale).sp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(getStatusColor(status))
            .padding(horizontal = (12f * scale).dp, vertical = (6f * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Rest of helpers remain the same (formatting, colors, logic)
private fun formatToDisplayDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "Invalid Date"
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val output = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val date = input.parse(dateStr)
        output.format(date!!)
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatToDisplayTime(timeStr: String?): String {
    if (timeStr.isNullOrEmpty()) return ""
    return try {
        val clean = if (timeStr.length > 5) timeStr else "$timeStr:00"
        val input = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("h:mm a", Locale.getDefault())
        output.format(input.parse(clean)!!)
    } catch (e: Exception) {
        timeStr
    }
}

private fun cleanText(text: String?): String {
    if (text.isNullOrEmpty()) return "N/A"
    return text.replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

private fun formatCurrency(amount: Double, symbol: String): String = "$symbol${String.format("%.1f", amount)}"

private fun getBookingStatusColor(status: String): Color = when (status.lowercase()) {
    "pending" -> Blue
    "partial_paid", "partial_refund", "paid_cash", "paid" -> IOSYellow
    "rejected", "cancelled" -> LimoRed
    "en_route_pu" -> IOSTeal
    "finalized" -> IOSFINALIZE
    else -> Color.Gray
}

private fun getFinalizeGreenColor(): Color = IOSGreen

private fun isWithinTwoHours(booking: DriverBooking): Boolean {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        formatter.timeZone = TimeZone.getDefault()
        val pickup = formatter.parse("${booking.pickupDate} ${booking.pickupTime}") ?: return false
        val diffSeconds = (pickup.time - System.currentTimeMillis()) / 1000
        diffSeconds >= 0 && diffSeconds <= 7200
    } catch (e: Exception) { false }
}

@Composable
private fun shouldShowEnRouteButton(booking: DriverBooking, onDriverEnRouteClick: () -> Unit): Boolean {
    val status = booking.bookingStatus.lowercase()
    val payment = booking.paymentStatus?.lowercase() ?: ""
    val disallowed = setOf("pending", "rejected", "cancelled", "ended", "complete", "completed")
    return isWithinTwoHours(booking) && !disallowed.contains(status) && payment != "paid"
}

private fun getStatusColor(status: String): Color = getBookingStatusColor(status)
