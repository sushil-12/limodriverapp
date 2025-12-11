package com.limo1800driver.app.ui.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

data class CurrencyOption(
    val countryCode: String,
    val countryName: String,
    val code: String,
    val symbol: String
) {
    val display: String
        get() = "$symbol - $code ($countryName)"
}

suspend fun loadCurrencyOptions(context: Context): List<CurrencyOption> =
    withContext(Dispatchers.IO) {
        try {
            val jsonText = context.assets.open("currencyOptions.json")
                .bufferedReader().use { it.readText() }

            val obj = JSONObject(jsonText)
            val keys = obj.keys()

            val result = mutableListOf<CurrencyOption>()

            while (keys.hasNext()) {
                val rawKey = keys.next().toString()
                val countryCode = rawKey.uppercase(Locale.US)

                val value = obj.getJSONObject(rawKey)

                val countryName = value.optString("countryName", countryCode)
                val currencyCode = value.optString("currency")
                    .uppercase(Locale.US)

                val symbol = value.optString("symbol", currencyCode)

                result.add(
                    CurrencyOption(
                        countryCode = countryCode,
                        countryName = countryName,
                        code = currencyCode,
                        symbol = symbol
                    )
                )
            }

            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
