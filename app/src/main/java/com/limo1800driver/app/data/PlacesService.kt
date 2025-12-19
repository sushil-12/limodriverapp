package com.limo1800driver.app.data

import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.limo1800driver.app.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PlacesService(private val context: Context) {

    // 1. OPTIMIZATION: Initialize Client once to save memory/battery
    private val placesClient: PlacesClient by lazy {
        Places.createClient(context)
    }

    init {
        if (!Places.isInitialized()) {
            try {
                // Use applicationContext to prevent activity leaks
                Places.initialize(context.applicationContext, NetworkConfig.GOOGLE_PLACES_API_KEY)
                Log.d("PlacesService", "Places API initialized successfully")
            } catch (e: Exception) {
                Log.e("PlacesService", "Failed to initialize Places API", e)
            }
        }
    }

    suspend fun getPlacePredictions(
        query: String,
        typeFilter: TypeFilter? = null
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
        try {
            if (query.length < 2) return@withContext emptyList()

            val token = AutocompleteSessionToken.newInstance()
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(token)

            typeFilter?.let { requestBuilder.setTypeFilter(it) }

            val request = requestBuilder.build()

            // Use the single instance client
            val response = placesClient.findAutocompletePredictions(request).await()

            response.autocompletePredictions.map { prediction ->
                PlacePrediction(
                    placeId = prediction.placeId,
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString(),
                    fullText = prediction.getFullText(null).toString()
                )
            }.take(10)
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

            val response = placesClient.fetchPlace(request).await()
            val place = response.place
            val addressComponents = place.addressComponents?.asList() ?: emptyList()
            val latLng = place.latLng

            // --- Extraction Logic ---

            val streetNumber = addressComponents.find { it.types.contains("street_number") }?.name ?: ""
            val route = addressComponents.find { it.types.contains("route") }?.name ?: ""

            // 2. ROBUSTNESS: Check sublocality if locality is missing (common in large metros)
            val city = addressComponents.find { it.types.contains("locality") }?.name
                ?: addressComponents.find { it.types.contains("sublocality") }?.name
                ?: addressComponents.find { it.types.contains("postal_town") }?.name
                ?: ""

            // 3. DATA FIX: Use shortName for State (e.g., "NY" vs "New York")
            val stateComponent = addressComponents.find { it.types.contains("administrative_area_level_1") }
            val state = stateComponent?.shortName ?: stateComponent?.name ?: ""

            val postalCode = addressComponents.find { it.types.contains("postal_code") }?.name ?: ""

            // 4. DATA FIX: Use shortName for Country (e.g., "US" vs "United States")
            val countryComponent = addressComponents.find { it.types.contains("country") }
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