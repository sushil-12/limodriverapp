package com.limo1800driver.app.rideinprogress

import com.google.gson.annotations.SerializedName

data class GoogleDirectionsResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("routes") val routes: List<GoogleRoute> = emptyList()
)

data class GoogleRoute(
    @SerializedName("overview_polyline") val overviewPolyline: GoogleOverviewPolyline? = null,
    @SerializedName("legs") val legs: List<GoogleLeg> = emptyList()
)

data class GoogleOverviewPolyline(
    @SerializedName("points") val points: String? = null
)

data class GoogleLeg(
    @SerializedName("distance") val distance: GoogleValueText? = null,
    @SerializedName("duration") val duration: GoogleValueText? = null
)

data class GoogleValueText(
    @SerializedName("value") val value: Long? = null,
    @SerializedName("text") val text: String? = null
)


