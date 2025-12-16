package com.limo1800driver.app.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Gson adapter that safely parses doubles that may come back as:
 * - number: 12.3
 * - string number: "12.3"
 * - empty string: ""   (treated as null)
 * - null
 *
 * This prevents crashes like: NumberFormatException: empty String
 */
class SafeDoubleTypeAdapter : JsonDeserializer<Double?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Double? {
        if (json == null || json.isJsonNull) return null
        if (!json.isJsonPrimitive) return null

        val primitive = json.asJsonPrimitive
        return when {
            primitive.isNumber -> primitive.asDouble
            primitive.isString -> primitive.asString.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            else -> null
        }
    }
}


