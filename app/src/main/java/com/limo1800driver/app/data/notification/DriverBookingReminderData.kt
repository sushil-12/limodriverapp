package com.limo1800driver.app.data.notification

import org.json.JSONObject

/**
 * Android driver equivalent of iOS `BookingNotificationData` (for booking reminders).
 * Used to power the 2h/1h/30m/15m “Driver En Route” popup and notification deep-links.
 */
data class DriverBookingReminderData(
    val bookingId: Int,
    val customerId: Int,
    val pickupDate: String,
    val pickupTime: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val reminderType: String,
    val formattedDatetime: String,
    val pickupLat: Double?,
    val pickupLng: Double?,
    val dropoffLat: Double?,
    val dropoffLng: Double?
) {
    fun requiresDriverEnRouteAction(): Boolean {
        return reminderType == "2_hours" ||
            reminderType == "1_hour" ||
            reminderType == "30_minutes" ||
            reminderType == "15_minutes"
    }

    fun reminderTypeText(): String {
        return when (reminderType) {
            "2_hours" -> "2 Hours"
            "1_hour" -> "1 Hour"
            "30_minutes" -> "30 Minutes"
            "15_minutes" -> "15 Minutes"
            "5_minutes" -> "5 Minutes"
            else -> "General"
        }
    }

    companion object {
        /**
         * Socket payloads may be either:
         * - flat with fields at root
         * - nested inside `data`
         */
        fun fromSocketJson(json: JSONObject): DriverBookingReminderData? {
            val data = json.optJSONObject("data") ?: json
            return fromNormalizedJson(data)
        }

        fun fromFcmDataMap(map: Map<String, String>): DriverBookingReminderData? {
            fun str(key: String): String? = map[key]?.trim()?.takeIf { it.isNotEmpty() }
            fun int(key: String): Int? = str(key)?.toIntOrNull()
            fun dbl(key: String): Double? = str(key)?.toDoubleOrNull()

            val bookingId = int("booking_id") ?: int("bookingId") ?: return null
            val customerId = int("customer_id") ?: int("customerId") ?: 0
            val pickupDate = str("pickup_date") ?: str("pickupDate") ?: "TBD"
            val pickupTime = str("pickup_time") ?: str("pickupTime") ?: "TBD"
            val pickupAddress = str("pickup_address") ?: str("pickupAddress") ?: "Pickup location will be provided"
            val dropoffAddress = str("dropoff_address") ?: str("dropoffAddress") ?: "Dropoff location will be provided"
            val reminderType = str("reminder_type") ?: str("reminderType") ?: "general"
            val formattedDatetime =
                str("formatted_datetime") ?: str("formattedDatetime") ?: "${pickupDate} ${pickupTime}"

            return DriverBookingReminderData(
                bookingId = bookingId,
                customerId = customerId,
                pickupDate = pickupDate,
                pickupTime = pickupTime,
                pickupAddress = pickupAddress,
                dropoffAddress = dropoffAddress,
                reminderType = reminderType,
                formattedDatetime = formattedDatetime,
                pickupLat = dbl("pickup_address_lat") ?: dbl("pickupLat") ?: dbl("pickup_latitude"),
                pickupLng = dbl("pickup_address_long") ?: dbl("pickupLng") ?: dbl("pickup_longitude"),
                dropoffLat = dbl("dropoff_address_lat") ?: dbl("dropoffLat") ?: dbl("dropoff_latitude"),
                dropoffLng = dbl("dropoff_address_long") ?: dbl("dropoffLng") ?: dbl("dropoff_longitude")
            )
        }

        private fun fromNormalizedJson(data: JSONObject): DriverBookingReminderData? {
            val bookingId = data.optIntCompat("booking_id") ?: data.optIntCompat("bookingId") ?: return null
            val customerId = data.optIntCompat("customer_id") ?: data.optIntCompat("customerId") ?: 0

            val pickupDate = data.optString("pickup_date", data.optString("pickupDate", "TBD")).ifBlank { "TBD" }
            val pickupTime = data.optString("pickup_time", data.optString("pickupTime", "TBD")).ifBlank { "TBD" }
            val pickupAddress = data.optString("pickup_address", data.optString("pickupAddress", "Pickup location will be provided"))
                .ifBlank { "Pickup location will be provided" }
            val dropoffAddress = data.optString("dropoff_address", data.optString("dropoffAddress", "Dropoff location will be provided"))
                .ifBlank { "Dropoff location will be provided" }

            val reminderType = data.optString("reminder_type", data.optString("reminderType", "general")).ifBlank { "general" }
            val formattedDatetime = data.optString("formatted_datetime", data.optString("formattedDatetime", "$pickupDate $pickupTime"))
                .ifBlank { "$pickupDate $pickupTime" }

            return DriverBookingReminderData(
                bookingId = bookingId,
                customerId = customerId,
                pickupDate = pickupDate,
                pickupTime = pickupTime,
                pickupAddress = pickupAddress,
                dropoffAddress = dropoffAddress,
                reminderType = reminderType,
                formattedDatetime = formattedDatetime,
                pickupLat = data.optDoubleCompat("pickup_address_lat") ?: data.optDoubleCompat("pickup_latitude"),
                pickupLng = data.optDoubleCompat("pickup_address_long") ?: data.optDoubleCompat("pickup_longitude"),
                dropoffLat = data.optDoubleCompat("dropoff_address_lat") ?: data.optDoubleCompat("dropoff_latitude"),
                dropoffLng = data.optDoubleCompat("dropoff_address_long") ?: data.optDoubleCompat("dropoff_longitude")
            )
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


