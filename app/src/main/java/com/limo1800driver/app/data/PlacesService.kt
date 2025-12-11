package com.limo1800driver.app.data

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AddressComponent
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.limo1800driver.app.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log

class PlacesService(private val context: Context) {

    init {
        // Initialize Places API if not already initialized
        if (!Places.isInitialized()) {
            try {
                Places.initialize(context, NetworkConfig.GOOGLE_PLACES_API_KEY)
                Log.d("PlacesService", "Places API initialized successfully")
            } catch (e: Exception) {
                Log.e("PlacesService", "Failed to initialize Places API", e)
            }
        } else {
            Log.d("PlacesService", "Places API is already initialized")
        }
    }

    suspend fun getPlacePredictions(
        query: String,
        typeFilter: TypeFilter? = null
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
        try {
            if (query.length < 2) return@withContext emptyList()

            Log.d("PlacesService", "Getting predictions for query: $query")
            val token = AutocompleteSessionToken.newInstance()
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(token)
                .apply {
                    typeFilter?.let { setTypeFilter(it) }
                }
            
            val request = requestBuilder.build()
            val response = Places.createClient(context).findAutocompletePredictions(request).await()
            val predictions = response.autocompletePredictions.map { prediction ->
                PlacePrediction(
                    placeId = prediction.placeId,
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString(),
                    fullText = prediction.getFullText(null).toString()
                )
            }.take(10)
            
            Log.d("PlacesService", "Found ${predictions.size} predictions")
            predictions
        } catch (e: Exception) {
            Log.e("PlacesService", "Error getting place predictions", e)
            emptyList()
        }
    }

    suspend fun getPlaceDetails(placeId: String): PlaceDetails? = withContext(Dispatchers.IO) {
        try {
            val request = FetchPlaceRequest.builder(
                placeId,
                listOf(
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.ADDRESS_COMPONENTS,
                    Place.Field.LAT_LNG
                )
            ).build()

            val response = Places.createClient(context).fetchPlace(request).await()
            val place = response.place
            val addressComponents = place.addressComponents?.asList() ?: emptyList()
            val latLng = place.latLng

            val streetNumber = addressComponents.find { it.types.contains("street_number") }?.name ?: ""
            val route = addressComponents.find { it.types.contains("route") }?.name ?: ""
            val city = addressComponents.find { it.types.contains("locality") }?.name
                ?: addressComponents.find { it.types.contains("postal_town") }?.name ?: ""
            val state = addressComponents.find { it.types.contains("administrative_area_level_1") }?.name ?: ""
            val postalCodeComponent = addressComponents.find { it.types.contains("postal_code") }
            val postalCode = postalCodeComponent?.name ?: ""
            val countryComponent = addressComponents.find { it.types.contains("country") }
            // Use the ISO 3166-1 alpha-2 country code (shortName) to satisfy backend expectations
            val country = countryComponent?.shortName ?: countryComponent?.name ?: ""

            PlaceDetails(
                name = place.name ?: "",
                address = place.address ?: "",
                postalCode = postalCode,
                city = city,
                state = state,
                country = country,
                streetNumber = streetNumber,
                route = route,
                latitude = latLng?.latitude,
                longitude = latLng?.longitude
            )
        } catch (e: Exception) {
            Log.e("PlacesService", "Error getting place details", e)
            null
        }
    }
}

data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

data class PlaceDetails(
    val name: String,
    val address: String,
    val postalCode: String,
    val city: String,
    val state: String,
    val country: String = "",
    val streetNumber: String = "",
    val route: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

