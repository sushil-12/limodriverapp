package com.limo1800driver.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for Limo Driver app
 * Initializes Hilt, logging, and notification system
 */
@HiltAndroidApp
class LimoDriverApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()

        // Ensure Firebase is initialized (usually handled by google-services, but explicit init helps debugging).
        runCatching { FirebaseApp.initializeApp(this) }
            .onSuccess { Timber.tag("DriverFCM").d("FirebaseApp initialized") }
            .onFailure { Timber.tag("DriverFCM").e(it, "FirebaseApp init failed") }

        // Create notification channels early (prevents FirebaseMessaging warnings)
        createNotificationChannels()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Log current FCM token for debugging
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    Timber.tag("DriverFCM").d("FCM token (debug): ${token.take(25)}…")
                }
                .addOnFailureListener { e ->
                    Timber.tag("DriverFCM").e(e, "Failed to get FCM token")
                }
        }

        Timber.tag("LimoDriverApp").d("Application initialized")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            "limoapi_notifications",
            "Limo Driver Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Bookings, reminders, active ride updates"
        }
        mgr.createNotificationChannel(channel)
    }
}

