package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.rememberModalBottomSheetState
import com.limo1800driver.app.data.notification.DriverBookingReminderData
import com.limo1800driver.app.ui.theme.LimoBlack
import com.limo1800driver.app.ui.theme.LimoOrange
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --- Constants based on Image ---
val BrandOrange = LimoOrange // Approximate from image
val LightOrangeBg = Color(0xFFFFF6ED) // For warning box
val TextGray = Color(0xFF757575)
val DividerGray = Color(0xFFE0E0E0)
val DarkText = LimoBlack

/**
 * The dynamic data model backing the exact UI shown in the screenshot.
 * Keep this aligned with the design fields (don’t remove fields).
 */
data class RideDetailState(
    val timeRemaining: String,
    val bookingId: String,
    val routeDescription: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val warningMessage: String? = null,
    val passengerName: String,
    val passengerPhone: String
)

/**
 * Public wrapper (keeps the rest of the app stable).
 * Converts backend reminder payload into the designed bottom sheet UI.
 *
 * NOTE:
 * - `onDriverEnRoute` is wired to the orange button ("Start Ride") for now (same behavior as reminder flow).
 * - Pickup button opens Google Maps (or any maps app) to pickup coordinates/address if available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverEnRouteReminderDialog(
    reminder: DriverBookingReminderData,
    isProcessing: Boolean,
    onDriverEnRoute: () -> Unit,
    onNotNow: () -> Unit,
    passengerName: String = "Passenger",
    passengerPhone: String = ""
) {
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            // block swipe-to-dismiss; must choose X / Not Now / Start Ride
            value != SheetValue.Hidden
        }
    )

    val timeRemaining = remember(reminder.pickupDate, reminder.pickupTime, reminder.reminderType) {
        computePickupTimeRemaining(
            pickupDate = reminder.pickupDate,
            pickupTime = reminder.pickupTime,
            reminderType = reminder.reminderType
        )
    }

    val bookingChip = remember(reminder.bookingId) {
        "#${reminder.bookingId}"
    }

    val routeDescription = remember(reminder.pickupAddress, reminder.dropoffAddress) {
        // We don't have turn-by-turn text from backend; keep it meaningful and matches design (2 lines max).
        "Pickup: ${reminder.pickupAddress} | Dropoff: ${reminder.dropoffAddress}"
    }

    // IMPORTANT: passengerName/phone are fetched asynchronously after the reminder arrives.
    // Include them in the remember keys so the UI updates when the fetch completes.
    val state = remember(timeRemaining, bookingChip, routeDescription, reminder, passengerName, passengerPhone) {
        RideDetailState(
            timeRemaining = timeRemaining,
            bookingId = bookingChip,
            routeDescription = routeDescription,
            pickupAddress = reminder.pickupAddress,
            dropoffAddress = reminder.dropoffAddress,
            warningMessage = "Plan to start the ride about 12 min before pickup so you arrive comfortably on time.",
            passengerName = passengerName,
            passengerPhone = passengerPhone.ifBlank { "—" }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { /* block scrim/back dismiss */ },
        sheetState = sheetState,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        DriverRideDetailSheet(
            state = state,
            onClose = { if (!isProcessing) onNotNow() },
            onPickupClick = {
                if (!isProcessing) {
                    openPickupInMaps(
                        context = context,
                        pickupLat = reminder.pickupLat,
                        pickupLng = reminder.pickupLng,
                        pickupAddress = reminder.pickupAddress
                    )
                }
            },
            onStartRideClick = { if (!isProcessing) onDriverEnRoute() },
            onMessageClick = { /* TODO: wire chat navigation when chat module is connected */ },
            onCallClick = { /* TODO: wire dial intent once passenger phone is available in reminder payload */ }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRideDetailSheet(
    state: RideDetailState,
    onClose: () -> Unit,
    onPickupClick: () -> Unit,
    onStartRideClick: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: () -> Unit
) {
    // Top drag handle styling
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 24.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            
            // 1. Header Section (Close btn + Time + Badge)
            Box(modifier = Modifier.fillMaxWidth()) {
                // Close Button (Top Left)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-8).dp) 
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }

                // Main Header Content
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pickup in ${state.timeRemaining}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandOrange
                            )
                        )
                        
                        // Booking ID Badge
                        Surface(
                            color = Color.Black,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = state.bookingId,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = state.routeDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Show More Dropdown
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Show More",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Action Buttons (Pickup / Start Ride)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pickup Button
                Button(
                    onClick = onPickupClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pickup", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }

                // Start Ride Button
                Button(
                    onClick = onStartRideClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Start Ride", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Timeline (Route Visualizer)
            RideTimelineView(
                pickup = state.pickupAddress,
                dropoff = state.dropoffAddress
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Warning / Info Box
            state.warningMessage?.let { msg ->
                Surface(
                    color = LightOrangeBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(20.dp).offset(y = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandOrange,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. Passenger Card
            PassengerInfoCard(
                name = state.passengerName,
                phone = state.passengerPhone,
                onMessage = onMessageClick,
                onCall = onCallClick
            )
        }
    }
}

// --- Helper Components ---

/**
 * Visual timeline with a vertical line connecting pickup and dropoff.
 * Uses IntrinsicSize.Min to ensure the line stretches to match text height.
 */
@Composable
fun RideTimelineView(pickup: String, dropoff: String) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // Visual Indicators Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp) // Fixed width for alignment
        ) {
            // Pickup Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(BrandOrange, CircleShape)
            )
            // The Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f) // Fills available vertical space
                    .background(Color.Black)
            )
            // Dropoff Square
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(BrandOrange, RoundedCornerShape(1.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Address Text Column
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = pickup,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )
            
            // Spacer to push dropoff to bottom, ensuring visual line stretches
            Spacer(modifier = Modifier.height(24.dp)) 
            
            Text(
                text = dropoff,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )
        }
    }
}

/**
 * The Passenger details card at the bottom
 */
@Composable
fun PassengerInfoCard(
    name: String,
    phone: String,
    onMessage: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Passenger",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Body
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NAME:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(DividerGray)
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PHONE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Message Button
                    Button(
                        onClick = onMessage,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send a message",
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Call Button
                    Button(
                        onClick = onCall,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(50.dp), // Square button
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}

private fun computePickupTimeRemaining(
    pickupDate: String,
    pickupTime: String,
    reminderType: String
): String {
    // iOS reminder UI is driven by reminderType; server times can be timezone-shifted.
    // We still try to compute real countdown, but NEVER allow known reminder types to collapse to "0 min".
    val baseMinutes = when (reminderType) {
        "2_hours" -> 120L
        "1_hour" -> 60L
        "30_minutes" -> 30L
        "15_minutes" -> 15L
        else -> null
    }

    val computedMinutes: Long? = runCatching {
        val date = LocalDate.parse(pickupDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        val time = parseTimeFlexible(pickupTime)
        val pickup = LocalDateTime.of(date, time)
        val now = LocalDateTime.now(ZoneId.systemDefault())
        Duration.between(now, pickup).toMinutes()
    }.getOrNull()

    val minutesToShow = when {
        baseMinutes != null && (computedMinutes == null || computedMinutes <= 1L) -> baseMinutes
        baseMinutes != null && computedMinutes != null && computedMinutes < 0L -> baseMinutes
        computedMinutes != null -> computedMinutes.coerceAtLeast(0L)
        baseMinutes != null -> baseMinutes
        else -> 0L
    }

    val hours = minutesToShow / 60
    val mins = minutesToShow % 60
    return if (hours > 0) "${hours} hr ${mins} min" else "${mins} min"
}

private fun parseTimeFlexible(raw: String): LocalTime {
    val t = raw.trim()
    // Most common backend formats:
    // - HH:mm:ss
    // - HH:mm
    return when {
        t.length >= 8 -> LocalTime.parse(t.substring(0, 8), DateTimeFormatter.ofPattern("HH:mm:ss"))
        else -> LocalTime.parse(t, DateTimeFormatter.ofPattern("HH:mm"))
    }
}

private fun openPickupInMaps(
    context: android.content.Context,
    pickupLat: Double?,
    pickupLng: Double?,
    pickupAddress: String
) {
    val uri = if (pickupLat != null && pickupLng != null && pickupLat != 0.0 && pickupLng != 0.0) {
        android.net.Uri.parse("google.navigation:q=$pickupLat,$pickupLng")
    } else {
        android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(pickupAddress)}")
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
        // Prefer Google Maps if installed
        setPackage("com.google.android.apps.maps")
    }
    runCatching {
        context.startActivity(intent)
    }.recoverCatching {
        // Fallback: any maps app
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
    }
}