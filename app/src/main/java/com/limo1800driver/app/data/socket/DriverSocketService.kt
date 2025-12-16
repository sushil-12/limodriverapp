package com.limo1800driver.app.data.socket

import com.limo1800driver.app.data.notification.DriverBookingReminderData
import com.limo1800driver.app.data.storage.TokenManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import java.util.UUID
import java.time.Instant

/**
 * Driver Socket.IO service.
 *
 * Mirrors iOS driver `SimpleSocketIOService` responsibilities:
 * - Connect with secret + userId + userType=driver
 * - Listen for `active_ride` payload and expose it as state
 * - Provide helpers to emit `booking.status.update` and `driver.location.update`
 */
@Singleton
class DriverSocketService @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var socket: Socket? = null
    private var isManualDisconnect: Boolean = false
    private var connectionAttempts: Int = 0
    private var isReconnecting: Boolean = false

    private val _connectionStatus = MutableStateFlow(SocketConnectionStatus(isConnected = false))
    val connectionStatus: StateFlow<SocketConnectionStatus> = _connectionStatus.asStateFlow()

    private val _activeRide = MutableStateFlow<DriverActiveRide?>(null)
    val activeRide: StateFlow<DriverActiveRide?> = _activeRide.asStateFlow()

    // Chat (in-app messages)
    private val _chatMessages = MutableStateFlow<List<DriverChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<DriverChatMessage>> = _chatMessages.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _currentChatBookingId = MutableStateFlow<Int?>(null)
    val currentChatBookingId: StateFlow<Int?> = _currentChatBookingId.asStateFlow()

    private var connectedUserId: String = "unknown"

    // Booking reminder popup data (2h/1h/30m/15m)
    private val _bookingReminder = MutableStateFlow<DriverBookingReminderData?>(null)
    val bookingReminder: StateFlow<DriverBookingReminderData?> = _bookingReminder.asStateFlow()

    companion object {
        private const val TAG = "DriverSocketService"

        // Must match iOS / backend
        private const val SOCKET_URL = "https://limortservice.infodevbox.com"
        private const val SECRET = "limoapi_notifications_secret_2024_xyz789"

        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
        private const val BACKOFF_MULTIPLIER = 1.5
        private const val CONNECTION_TIMEOUT_MS = 30000L
    }

    fun connect() {
        scope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token.isNullOrBlank()) {
                    Timber.tag(TAG).e("No access token available; cannot connect Socket.IO")
                    updateConnectionStatus(false, error = "No access token")
                    return@launch
                }

                val userId = extractUserIdFromJwt(token) ?: "unknown"
                connectedUserId = userId
                if (userId == "unknown") {
                    Timber.tag(TAG).w("Socket connecting with unknown userId (token parsing failed)")
                }

                disconnectInternal()

                val options = IO.Options().apply {
                    forceNew = true
                    reconnection = false // we implement our own backoff
                    timeout = CONNECTION_TIMEOUT_MS

                    // Socket.IO v4 servers often read from auth; iOS uses connectParams.
                    // To be resilient, we set BOTH auth and query with same values.
                    auth = mutableMapOf(
                        "client" to "android_driver",
                        "secret" to SECRET,
                        "userId" to userId,
                        "userType" to "driver"
                    )

                    // Older servers / middleware may read query params
                    query = "client=android_driver&secret=$SECRET&userId=$userId&userType=driver"

                    transports = arrayOf("polling", "websocket")
                    upgrade = false
                }

                val uri = URI.create(SOCKET_URL)
                socket = IO.socket(uri, options)
                setupEventListeners()

                isManualDisconnect = false
                Timber.tag(TAG).d("Connecting Socket.IO: $SOCKET_URL userId=$userId userType=driver")
                socket?.connect()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error connecting Socket.IO")
                updateConnectionStatus(false, error = e.message ?: "Connection failed")
                scheduleReconnect()
            }
        }
    }

    fun getConnectedUserId(): String = connectedUserId

    fun disconnectForLogout() {
        isManualDisconnect = true
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try {
            socket?.off()
            socket?.disconnect()
        } catch (_: Exception) {
        } finally {
            socket = null
            updateConnectionStatus(false, error = "Disconnected")
        }
    }

    fun clearActiveRide() {
        _activeRide.value = null
    }

    fun setCurrentChatBookingId(bookingId: Int?) {
        _currentChatBookingId.value = bookingId
        // Clear previous messages when switching chats to avoid mixing threads.
        _chatMessages.value = emptyList()
        _chatError.value = null
    }

    fun setChatHistory(bookingId: Int, messages: List<DriverChatMessage>) {
        if (_currentChatBookingId.value != null && _currentChatBookingId.value != bookingId) return
        _chatMessages.value = messages
            .distinctBy { it.id }
            .sortedBy { parseCreatedAtEpochMs(it.createdAt) }
    }

    fun emitChatMessage(bookingId: Int, receiverId: String, message: String) {
        val s = socket ?: return
        if (!s.connected()) {
            _chatError.value = "Not connected to chat server"
            return
        }

        val trimmed = message.trim()
        if (trimmed.isBlank()) return

        // Optimistic UI insert (matches iOS behavior)
        val optimistic = DriverChatMessage(
            id = UUID.randomUUID().toString(),
            bookingId = bookingId,
            senderId = getConnectedUserId(),
            receiverId = receiverId,
            senderRole = "driver",
            message = trimmed,
            createdAt = Instant.now().toString()
        )
        appendChatMessageIfMatches(optimistic)

        val payload = JSONObject().apply {
            put("bookingId", bookingId)
            put("receiverId", receiverId)
            put("message", trimmed)
        }
        s.emit("chat.message", payload)
    }

    /**
     * Optimistically seed an active ride locally.
     *
     * Why:
     * - Server often emits `active_ride` only periodically / on reconnect.
     * - UI must transition immediately on button taps (iOS behavior).
     */
    fun setActiveRide(ride: DriverActiveRide) {
        _activeRide.value = ride
    }

    /**
     * Optimistically update the active ride status when we emit booking.status.update.
     * This keeps the UI responsive even if the backend doesn't immediately push a new `active_ride`.
     */
    fun updateActiveRideStatus(bookingId: Int, status: String, statusDisplay: String? = null) {
        val current = _activeRide.value ?: return
        if (current.bookingId != bookingId) return
        _activeRide.value = current.copy(
            status = status,
            statusDisplay = statusDisplay ?: current.statusDisplay
        )
    }

    fun clearBookingReminder() {
        _bookingReminder.value = null
    }

    private fun setupEventListeners() {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT) {
            Timber.tag(TAG).d("Socket.IO connected")
            connectionAttempts = 0
            isReconnecting = false
            updateConnectionStatus(true)
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            Timber.tag(TAG).w("Socket.IO disconnected: ${args.joinToString()}")
            updateConnectionStatus(false, error = "Disconnected")
            if (!isManualDisconnect) scheduleReconnect()
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Timber.tag(TAG).e("Socket.IO connect error: ${args.joinToString()}")
            connectionAttempts++
            isReconnecting = true
            updateConnectionStatus(false, error = "Connection error")
            if (!isManualDisconnect) scheduleReconnect()
        }

        // iOS listens for this and converts it to BookingNotificationData -> RideInProgressView.
        s.on("active_ride") { args ->
            try {
                val root = args.firstOrNull() as? JSONObject
                Timber.tag(TAG).d("active_ride received: ${root?.toString()}")
                if (root == null) return@on

                val success = root.optBoolean("success", false)
                if (!success) {
                    Timber.tag(TAG).d("active_ride success=false; clearing activeRide")
                    _activeRide.value = null
                    return@on
                }

                val data = root.optJSONObject("data") ?: run {
                    Timber.tag(TAG).w("active_ride missing data object; clearing activeRide")
                    _activeRide.value = null
                    return@on
                }

                val bookingId = data.optIntCompat("booking_id") ?: return@on
                val status = data.optString("status", "")
                val statusDisplay = data.optString("status_display", null)

                val locations = data.optJSONObject("locations")
                val pickup = locations?.optJSONObject("pickup")
                val dropoff = locations?.optJSONObject("dropoff")

                val pickupAddress = pickup?.optString("address") ?: ""
                val pickupDatetime = pickup?.optString("datetime")
                val pickupLat = pickup?.optDoubleCompat("latitude")
                val pickupLng = pickup?.optDoubleCompat("longitude")

                val dropoffAddress = dropoff?.optString("address") ?: ""
                val dropoffLat = dropoff?.optDoubleCompat("latitude")
                val dropoffLng = dropoff?.optDoubleCompat("longitude")

                val customer = data.optJSONObject("customer")
                val customerId = customer?.optIntCompat("id") ?: data.optIntCompat("customer_id") ?: 0
                val customerName = customer?.optString("name")
                    ?: listOfNotNull(customer?.optString("first_name"), customer?.optString("last_name"))
                        .joinToString(" ")
                        .ifBlank { null }
                val customerPhone = customer?.optString("phone")
                    ?: customer?.optString("mobile")
                    ?: customer?.optString("cell")

                val tripDetails = data.optJSONObject("trip_details")
                val passengers = tripDetails?.optIntCompat("passengers")
                val luggage = tripDetails?.optIntCompat("luggage")

                val activeRide = DriverActiveRide(
                    bookingId = bookingId,
                    customerId = customerId,
                    status = status,
                    statusDisplay = statusDisplay,
                    pickupAddress = pickupAddress,
                    pickupLatitude = pickupLat,
                    pickupLongitude = pickupLng,
                    pickupDatetime = pickupDatetime,
                    dropoffAddress = dropoffAddress,
                    dropoffLatitude = dropoffLat,
                    dropoffLongitude = dropoffLng,
                    instructions = data.optString("instructions", null),
                    passengers = passengers,
                    luggage = luggage,
                    customerName = customerName,
                    customerPhone = customerPhone
                )

                Timber.tag(TAG).d("Active ride parsed: bookingId=${activeRide.bookingId} status=${activeRide.status}")
                // Guard against stale server payloads causing UI to "jump backwards".
                // Example: we optimistically transition to en_route_do, but the server may still
                // temporarily emit active_ride with on_location; we should not downgrade locally.
                val current = _activeRide.value
                if (current != null && current.bookingId == activeRide.bookingId) {
                    val currentRank = statusRank(current.status)
                    val incomingRank = statusRank(activeRide.status)
                    if (currentRank > incomingRank) {
                        Timber.tag(TAG).w(
                            "Ignoring stale active_ride status downgrade bookingId=$bookingId " +
                                "current=${current.status} incoming=${activeRide.status}"
                        )
                        return@on
                    }
                }
                _activeRide.value = activeRide
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error parsing active_ride")
            }
        }

        // iOS listens to booking_reminder and booking_notification and shows a Driver En Route popup
        s.on("booking_reminder") { args ->
            try {
                val payload = args.firstOrNull() as? JSONObject ?: return@on
                handleBookingReminderPayload(payload, source = "booking_reminder")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error processing booking_reminder")
            }
        }

        s.on("booking_notification") { args ->
            try {
                val payload = args.firstOrNull() as? JSONObject ?: return@on
                handleBookingReminderPayload(payload, source = "booking_notification")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error processing booking_notification")
            }
        }

        // Some servers emit all notifications through user.notifications
        s.on("user.notifications") { args ->
            try {
                val payload = args.firstOrNull() as? JSONObject ?: return@on
                // Handle chat messages (if present) and reminders (if present)
                handleChatNotificationPayload(payload, isFlatStructure = false)
                // Only treat as reminder if it contains reminder_type + booking_id
                handleBookingReminderPayload(payload, source = "user.notifications")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error processing user.notifications")
            }
        }

        // iOS also listens to a flat structure event name
        s.on("user_notification") { args ->
            try {
                val payload = args.firstOrNull() as? JSONObject ?: return@on
                handleChatNotificationPayload(payload, isFlatStructure = true)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error processing user_notification")
            }
        }

        s.on("chat.message.sent") { _ ->
            Timber.tag(TAG).d("chat.message.sent received")
        }

        s.on("chat.error") { args ->
            val msg = (args.firstOrNull() as? JSONObject)?.optString("message")
                ?: args.firstOrNull()?.toString()
                ?: "Chat error"
            _chatError.value = msg
            Timber.tag(TAG).e("chat.error: $msg")
        }
    }

    private fun handleChatNotificationPayload(payload: JSONObject, isFlatStructure: Boolean) {
        // iOS supports both nested and flat structures. Mirror that here.
        val messageObj: JSONObject? = if (isFlatStructure) {
            val type = payload.optString("type", "")
            if (type == "chat_message") payload else null
        } else {
            val dataObj = payload.optJSONObject("data") ?: return
            val type = dataObj.optString("type", "")
            if (type == "chat_message") dataObj else null
        }

        if (messageObj == null) return

        val bookingId = messageObj.optIntCompat("bookingId") ?: messageObj.optIntCompat("booking_id") ?: return
        val senderId = messageObj.optString("senderId", messageObj.optString("sender_id", "unknown"))
        val receiverId = messageObj.optString("receiverId", messageObj.optString("receiver_id", "")).ifBlank { null }
        val senderRole = messageObj.optString("senderRole", messageObj.optString("sender_role", ""))
        val message = messageObj.optString("message", "")
        val createdAt = messageObj.optString("createdAt", messageObj.optString("created_at", Instant.now().toString()))
        val id = messageObj.optString("_id", messageObj.optString("id", UUID.randomUUID().toString()))

        val chatMessage = DriverChatMessage(
            id = id,
            bookingId = bookingId,
            senderId = senderId,
            receiverId = receiverId,
            senderRole = senderRole.ifBlank { "customer" },
            message = message,
            createdAt = createdAt
        )

        appendChatMessageIfMatches(chatMessage)
    }

    private fun appendChatMessageIfMatches(message: DriverChatMessage) {
        val currentBookingId = _currentChatBookingId.value
        if (currentBookingId != null && message.bookingId != currentBookingId) return

        val existing = _chatMessages.value
        if (existing.any { it.id == message.id }) return

        _chatMessages.value = (existing + message)
            .sortedBy { parseCreatedAtEpochMs(it.createdAt) }
    }

    private fun parseCreatedAtEpochMs(createdAt: String?): Long {
        if (createdAt.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0L)
    }

    private fun handleBookingReminderPayload(payload: JSONObject, source: String) {
        val reminder = DriverBookingReminderData.fromSocketJson(payload) ?: return
        // Only pop the modal for the same reminder types as iOS
        if (!reminder.requiresDriverEnRouteAction()) return

        Timber.tag(TAG).d("Booking reminder received from $source: bookingId=${reminder.bookingId} type=${reminder.reminderType}")
        _bookingReminder.value = reminder
    }

    fun emitDriverLocationUpdate(
        bookingId: Int,
        customerId: Int,
        latitude: Double,
        longitude: Double,
        heading: Int,
        speed: Int
    ) {
        val s = socket ?: return
        if (!s.connected()) return

        val payload = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("bookingId", bookingId.toString())
            put("customerId", customerId.toString())
            put("accuracy", 10)
            put("heading", heading)
            put("speed", speed.coerceAtLeast(0))
        }

        s.emit("driver.location.update", payload)
    }

    fun emitBookingStatusUpdate(
        bookingId: Int,
        customerId: Int,
        status: String,
        driverId: String,
        currentLocation: Pair<Double, Double>?,
        pickupInfo: Triple<Double?, Double?, String>,
        dropoffInfo: Triple<Double?, Double?, String>
    ) {
        val s = socket ?: return
        if (!s.connected()) return

        val payload = JSONObject().apply {
            put("bookingId", bookingId.toString())
            put("customerId", customerId.toString())
            put("driverId", driverId)
            put("status", status)
            put("timestamp", java.time.Instant.now().toString())
            put(
                "location",
                JSONObject().apply {
                    put("latitude", currentLocation?.first ?: 0.0)
                    put("longitude", currentLocation?.second ?: 0.0)
                }
            )
            put(
                "pickup_info",
                JSONObject().apply {
                    put("latitude", pickupInfo.first ?: 0.0)
                    put("longitude", pickupInfo.second ?: 0.0)
                    put("address", pickupInfo.third)
                }
            )
            put(
                "dropoff_info",
                JSONObject().apply {
                    put("latitude", dropoffInfo.first ?: 0.0)
                    put("longitude", dropoffInfo.second ?: 0.0)
                    put("address", dropoffInfo.third)
                }
            )
            put("metadata", JSONObject())
        }

        s.emit("booking.status.update", payload)
        Timber.tag(TAG).d("Emitted booking.status.update status=$status bookingId=$bookingId payload=$payload")
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect) return
        if (connectionAttempts >= MAX_RECONNECT_ATTEMPTS) {
            updateConnectionStatus(false, error = "Max reconnect attempts reached")
            return
        }

        scope.launch {
            val delayMs = calculateReconnectDelayMs(connectionAttempts)
            Timber.tag(TAG).d("Reconnecting in ${delayMs}ms (attempt ${connectionAttempts + 1}/$MAX_RECONNECT_ATTEMPTS)")
            delay(delayMs)
            connect()
        }
    }

    private fun calculateReconnectDelayMs(attempt: Int): Long {
        val raw = (INITIAL_RECONNECT_DELAY_MS * BACKOFF_MULTIPLIER.pow(attempt.toDouble())).toLong()
        return raw.coerceAtMost(MAX_RECONNECT_DELAY_MS)
    }

    private fun updateConnectionStatus(
        isConnected: Boolean,
        error: String? = null
    ) {
        _connectionStatus.value = SocketConnectionStatus(
            isConnected = isConnected,
            isReconnecting = isReconnecting,
            connectionAttempts = connectionAttempts,
            error = error
        )
    }

    private fun extractUserIdFromJwt(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadJson = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val obj = JSONObject(payloadJson)
            // Common keys; depends on backend.
            obj.optString("sub")
                .ifBlank { obj.optString("user_id") }
                .ifBlank { obj.optString("id") }
                .ifBlank { null }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse JWT for userId")
            null
        }
    }

    /**
     * A simple monotonic progression ordering for ride statuses used by this client UI.
     * This lets us avoid "jumping backwards" when the server emits a stale `active_ride`.
     */
    private fun statusRank(status: String?): Int {
        return when (status?.trim().orEmpty()) {
            "en_route_pu" -> 0
            "on_location" -> 1
            "en_route_do" -> 2
            "ended" -> 3
            else -> -1
        }
    }
}

private fun JSONObject.optIntCompat(key: String): Int? {
    if (!has(key)) return null
    return when (val v = opt(key)) {
        is Int -> v
        is Long -> v.toInt()
        is Double -> v.toInt()
        is String -> v.trim().toIntOrNull()
        else -> null
    }
}

private fun JSONObject.optDoubleCompat(key: String): Double? {
    if (!has(key)) return null
    return when (val v = opt(key)) {
        is Double -> v
        is Int -> v.toDouble()
        is Long -> v.toDouble()
        is String -> v.trim().toDoubleOrNull()
        else -> null
    }
}


