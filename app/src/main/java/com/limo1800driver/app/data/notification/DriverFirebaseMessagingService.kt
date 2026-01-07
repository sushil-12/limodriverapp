package com.limo1800driver.app.data.notification

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.limo1800driver.app.MainActivity
import com.limo1800driver.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Driver app Firebase Cloud Messaging receiver.
 *
 * IMPORTANT: Without a registered FirebaseMessagingService in AndroidManifest,
 * the app will not receive data messages and you won't see any logs.
 */
class DriverFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DriverFCM"
        // Notification channels
        private const val CHANNEL_ID_BOOKINGS = "limoapi_bookings"
        private const val CHANNEL_ID_REMINDERS = "limoapi_reminders"
        private const val CHANNEL_ID_GENERAL = "limoapi_general"

        /**
         * Extract booking ID from message text using regex patterns
         * Example: "A new booking has been created with booking #1820" -> 1820
         */
        fun extractBookingIdFromMessage(message: String): Int? {
            // Pattern to match # followed by digits (e.g., #1820)
            val hashPattern = Pattern.compile("#(\\d+)")
            val hashMatcher = hashPattern.matcher(message)

            if (hashMatcher.find()) {
                val bookingIdString = hashMatcher.group(1)
                return bookingIdString?.toIntOrNull()
            }

            // Pattern to match "booking" followed by digits (e.g., "booking 1820")
            val bookingPattern = Pattern.compile("booking\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
            val bookingMatcher = bookingPattern.matcher(message)

            if (bookingMatcher.find()) {
                val bookingIdString = bookingMatcher.group(1)
                return bookingIdString?.toIntOrNull()
            }

            // Pattern to match just digits that look like booking IDs (4+ digits)
            val digitPattern = Pattern.compile("(\\d{4,})")
            val digitMatcher = digitPattern.matcher(message)

            if (digitMatcher.find()) {
                val bookingIdString = digitMatcher.group(1)
                return bookingIdString?.toIntOrNull()
            }

            return null
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannels()
        Timber.tag(TAG).d("FirebaseMessagingService created")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("New FCM token: ${token.take(25)}…")
        // TODO: send token to backend if required (driver notification routing)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Timber.tag(TAG).i("╔════════════════════════════════════════╗")
        Timber.tag(TAG).i("║  FCM MESSAGE RECEIVED                 ║")
        Timber.tag(TAG).i("╚════════════════════════════════════════╝")
        Timber.tag(TAG).i("⏰ Timestamp: ${java.util.Date()}")
        Timber.tag(TAG).i("📮 From: ${remoteMessage.from}")
        Timber.tag(TAG).i("🆔 MessageId: ${remoteMessage.messageId}")

        // Log detailed payload breakdown
        Timber.tag(TAG).i("📦 Data Payload:")
        remoteMessage.data.forEach { (key, value) ->
            Timber.tag(TAG).i("   📋 $key: $value")
        }

        Timber.tag(TAG).i("🔔 Notification Payload:")
        remoteMessage.notification?.let { notification ->
            Timber.tag(TAG).i("   📌 Title: ${notification.title}")
            Timber.tag(TAG).i("   📌 Body: ${notification.body}")
            Timber.tag(TAG).i("   🏷️  Icon: ${notification.icon}")
            Timber.tag(TAG).i("   🔊 Sound: ${notification.sound}")
            Timber.tag(TAG).i("   🏷️  Tag: ${notification.tag}")
            Timber.tag(TAG).i("   🏷️  Color: ${notification.color}")
            Timber.tag(TAG).i("   🏷️  Click Action: ${notification.clickAction}")
        } ?: Timber.tag(TAG).i("   📌 No notification payload (data-only message)")

        // Extract notification content
        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "Driver Notification"
        val body = remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: remoteMessage.notification?.body
            ?: "You have a new notification"

        Timber.tag(TAG).i("📋 Final Title: $title")
        Timber.tag(TAG).i("📋 Final Body: $body")

        // Extract booking data for navigation
        val bookingData = extractBookingDataFromFcm(remoteMessage)
        if (bookingData != null) {
            Timber.tag(TAG).i("📋 Extracted booking data: bookingId=${bookingData.bookingId}, isReminder=${bookingData.requiresDriverEnRouteAction()}")
        }

        // Always show a system notification (iOS parity: shows banner in foreground too).
        showNotification(title = title, body = body, bookingData = bookingData, remoteMessage = remoteMessage)
        Timber.tag(TAG).i("════════════════════════════════════════")
    }

    /**
     * Extract booking data from FCM message using the same logic as DriverBookingReminderData
     * Includes fallback to extract booking ID from message text if payload doesn't contain it
     */
    private fun extractBookingDataFromFcm(remoteMessage: RemoteMessage): DriverBookingReminderData? {
        // First try to extract from the data payload
        var bookingData = DriverBookingReminderData.fromFcmDataMap(remoteMessage.data)

        // If no booking data found in payload, try to extract booking ID from message text
        if (bookingData == null) {
            val title = remoteMessage.data["title"]
                ?: remoteMessage.notification?.title
                ?: ""
            val body = remoteMessage.data["body"]
                ?: remoteMessage.data["message"]
                ?: remoteMessage.notification?.body
                ?: ""

            val messageText = "$title $body"
            val bookingId = extractBookingIdFromMessage(messageText)

            if (bookingId != null) {
                Timber.tag(TAG).i("📋 Extracted booking ID $bookingId from message text: '$messageText'")
                // Create minimal booking data with just the ID
                bookingData = DriverBookingReminderData(
                    bookingId = bookingId,
                    customerId = 0,
                    pickupDate = "TBD",
                    pickupTime = "TBD",
                    pickupAddress = "Pickup location will be provided",
                    dropoffAddress = "Dropoff location will be provided",
                    reminderType = "general",
                    formattedDatetime = "TBD",
                    pickupLat = null,
                    pickupLng = null,
                    dropoffLat = null,
                    dropoffLng = null
                )
            }
        }

        return bookingData
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Booking notifications channel (highest priority)
        val bookingChannel = NotificationChannel(
            CHANNEL_ID_BOOKINGS,
            "Booking Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New bookings and booking updates"
            vibrationPattern = longArrayOf(0, 250, 250, 250)
            enableVibration(true)
        }

        // Reminder notifications channel (high priority)
        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            "Booking Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pickup time reminders and alerts"
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            enableVibration(true)
        }

        // General notifications channel (default priority)
        val generalChannel = NotificationChannel(
            CHANNEL_ID_GENERAL,
            "General Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General app notifications"
            vibrationPattern = longArrayOf(0, 250)
            enableVibration(true)
        }

        mgr.createNotificationChannels(listOf(bookingChannel, reminderChannel, generalChannel))
    }

    private fun showNotification(title: String, body: String, bookingData: DriverBookingReminderData? = null, remoteMessage: RemoteMessage) {
        ensureChannels()

        // Determine channel and priority based on notification type
        val (channelId, priority) = determineChannelAndPriority(bookingData, remoteMessage)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // Add notification data to intent for MainActivity processing
            remoteMessage.data.forEach { (key, value) ->
                putExtra(key, value)
            }

            // Add notification title and body
            putExtra("notification_title", title)
            putExtra("notification_body", body)

            // Add event type if present
            remoteMessage.data["event"]?.let { event ->
                putExtra("event", event)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply {
                // Add vibration for high priority notifications
                if (priority == NotificationCompat.PRIORITY_HIGH) {
                    setVibrate(longArrayOf(0, 250, 250, 250))
                }
            }
            .build()

        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)

        Timber.tag(TAG).i("🔔 Notification shown - Channel: $channelId, Priority: $priority")

        // Handle foreground reminder notifications (iOS parity)
        if (bookingData?.requiresDriverEnRouteAction() == true && isAppInForeground()) {
            Timber.tag(TAG).i("🚨 FOREGROUND REMINDER NOTIFICATION - Triggering dialog immediately")
            triggerForegroundReminderDialog(bookingData)
        }
    }

    /**
     * Determine the appropriate notification channel and priority based on content
     */
    private fun determineChannelAndPriority(bookingData: DriverBookingReminderData?, remoteMessage: RemoteMessage): Pair<String, Int> {
        val event = remoteMessage.data["event"]?.lowercase()

        return when {
            // Booking reminders get highest priority
            bookingData?.requiresDriverEnRouteAction() == true || event?.contains("reminder") == true -> {
                Pair(CHANNEL_ID_REMINDERS, NotificationCompat.PRIORITY_HIGH)
            }
            // Booking-related notifications
            event?.contains("booking") == true || bookingData != null -> {
                Pair(CHANNEL_ID_BOOKINGS, NotificationCompat.PRIORITY_HIGH)
            }
            // Default to general notifications
            else -> {
                Pair(CHANNEL_ID_GENERAL, NotificationCompat.PRIORITY_DEFAULT)
            }
        }
    }

    /**
     * Check if the app is currently in foreground
     */
    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false

        val packageName = packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    /**
     * Trigger reminder dialog immediately when app is in foreground (iOS parity)
     */
    private fun triggerForegroundReminderDialog(bookingData: DriverBookingReminderData) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = "SHOW_REMINDER_DIALOG"
                // Add booking data
                putExtra("booking_id", bookingData.bookingId.toString())
                putExtra("pickup_date", bookingData.pickupDate)
                putExtra("pickup_time", bookingData.pickupTime)
                putExtra("pickup_address", bookingData.pickupAddress)
                putExtra("dropoff_address", bookingData.dropoffAddress)
                putExtra("reminder_type", bookingData.reminderType)
                putExtra("formatted_datetime", bookingData.formattedDatetime)
                bookingData.pickupLat?.let { putExtra("pickup_address_lat", it) }
                bookingData.pickupLng?.let { putExtra("pickup_address_long", it) }
                bookingData.dropoffLat?.let { putExtra("dropoff_address_lat", it) }
                bookingData.dropoffLng?.let { putExtra("dropoff_address_long", it) }
            }

            startActivity(intent)
            Timber.tag(TAG).i("✅ Triggered foreground reminder dialog for booking ${bookingData.bookingId}")
        } catch (e: Exception) {
            Timber.tag(TAG).e("❌ Failed to trigger foreground reminder dialog: ${e.message}")
        }
    }
}


