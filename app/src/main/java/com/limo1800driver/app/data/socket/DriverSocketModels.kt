package com.limo1800driver.app.data.socket

/**
 * Minimal models for the driver Socket.IO layer, aligned with iOS driver app `active_ride` payload.
 * We intentionally keep these models UI-agnostic and resilient to backend type inconsistencies.
 */

data class SocketConnectionStatus(
    val isConnected: Boolean,
    val isReconnecting: Boolean = false,
    val connectionAttempts: Int = 0,
    val error: String? = null
)

data class DriverActiveRide(
    val bookingId: Int,
    val customerId: Int,
    val status: String,
    val statusDisplay: String? = null,
    val pickupAddress: String = "",
    val pickupLatitude: Double? = null,
    val pickupLongitude: Double? = null,
    val pickupDatetime: String? = null,
    val dropoffAddress: String = "",
    val dropoffLatitude: Double? = null,
    val dropoffLongitude: Double? = null,
    val instructions: String? = null,
    val passengers: Int? = null,
    val luggage: Int? = null,
    val customerName: String? = null,
    val customerPhone: String? = null
)

/**
 * Real-time chat message model (matches iOS `RealTimeChatMessage` / backend).
 *
 * Notes:
 * - `createdAt` is kept as String to avoid requiring a custom Gson Date adapter.
 * - `receiverId` can be missing in some notification payloads.
 */
data class DriverChatMessage(
    val id: String,
    val bookingId: Int,
    val senderId: String,
    val receiverId: String? = null,
    val senderRole: String,
    val message: String,
    val createdAt: String
)

data class DriverChatHistoryResponse(
    val success: Boolean,
    val data: List<DriverChatMessage>
)

data class DriverChatSendRequest(
    val bookingId: Int,
    val receiverId: String,
    val message: String
)


