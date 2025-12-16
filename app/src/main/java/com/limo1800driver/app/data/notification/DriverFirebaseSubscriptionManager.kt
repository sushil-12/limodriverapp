package com.limo1800driver.app.data.notification

import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors iOS behavior: subscribe to a topic equal to the logged-in userId (string).
 * Backend commonly targets this topic instead of individual device tokens.
 */
@Singleton
class DriverFirebaseSubscriptionManager @Inject constructor() {

    companion object {
        private const val TAG = "DriverFCM"
    }

    fun subscribeToUserTopic(userId: String) {
        val topic = userId.trim()
        if (topic.isEmpty()) return

        Timber.tag(TAG).d("Subscribing to FCM topic: $topic")
        FirebaseMessaging.getInstance()
            .subscribeToTopic(topic)
            .addOnSuccessListener {
                Timber.tag(TAG).d("✅ Subscribed to topic: $topic")
            }
            .addOnFailureListener { e ->
                Timber.tag(TAG).e(e, "❌ Failed to subscribe to topic: $topic")
            }
    }
}


