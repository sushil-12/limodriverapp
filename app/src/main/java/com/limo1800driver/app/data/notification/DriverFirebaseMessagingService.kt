package com.limo1800driver.app.data.notification

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

/**
 * Driver app Firebase Cloud Messaging receiver.
 *
 * IMPORTANT: Without a registered FirebaseMessagingService in AndroidManifest,
 * the app will not receive data messages and you won't see any logs.
 */
class DriverFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DriverFCM"
        // Must match backend/FCM `android_channel_id` if they set one (your logs show `limoapi_notifications`)
        private const val CHANNEL_ID = "limoapi_notifications"
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        Timber.tag(TAG).d("FirebaseMessagingService created")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("New FCM token: ${token.take(25)}…")
        // TODO: send token to backend if required (driver notification routing)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Timber.tag(TAG).i("══════════════════════════════")
        Timber.tag(TAG).i("FCM message received")
        Timber.tag(TAG).i("From: ${remoteMessage.from}")
        Timber.tag(TAG).i("MessageId: ${remoteMessage.messageId}")
        Timber.tag(TAG).i("Data: ${remoteMessage.data}")
        Timber.tag(TAG).i("Notification: ${remoteMessage.notification?.title} - ${remoteMessage.notification?.body}")

        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "Driver Notification"
        val body = remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: remoteMessage.notification?.body
            ?: "You have a new notification"

        // Always show a system notification (iOS parity: shows banner in foreground too).
        showNotification(title = title, body = body)
        Timber.tag(TAG).i("══════════════════════════════")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Driver Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for driver app (bookings, reminders, live ride)"
        }
        mgr.createNotificationChannel(channel)
    }

    private fun showNotification(title: String, body: String) {
        ensureChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}


