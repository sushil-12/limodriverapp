package com.limo1800driver.app.data.model.dashboard

import com.google.gson.annotations.SerializedName

data class MobileDataAirlinesData(
    @SerializedName("airlinesData")
    val airlinesData: List<MobileDataAirline> = emptyList()
)

data class MobileDataAirline(
    @SerializedName("id")
    val id: Int,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("country")
    val country: String? = null
) {
    val displayName: String
        get() {
            val c = code.orEmpty().trim()
            val n = name.orEmpty().trim()
            return if (c.isBlank()) n else "$c - $n"
        }
}

data class MobileDataAirportsData(
    @SerializedName("airportsData")
    val airportsData: List<MobileDataAirport> = emptyList()
)

data class MobileDataAirport(
    @SerializedName("id")
    val id: Int,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("lat")
    val lat: Double? = null,
    @SerializedName("long")
    val long: Double? = null
) {
    val displayName: String
        get() {
            val c = code.orEmpty().trim()
            val n = name.orEmpty().trim()
            val cityPart = city.orEmpty().trim()
            val countryPart = country.orEmpty().trim()
            val suffix = listOf(cityPart, countryPart).filter { it.isNotBlank() }.joinToString(", ")
            return if (c.isBlank()) "$n${if (suffix.isBlank()) "" else ", $suffix"}"
            else "$c - $n${if (suffix.isBlank()) "" else ", $suffix"}"
        }
}


