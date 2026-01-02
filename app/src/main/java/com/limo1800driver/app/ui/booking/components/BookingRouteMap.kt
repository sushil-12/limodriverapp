package com.limo1800driver.app.ui.screens.booking

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import com.limo1800driver.app.ui.theme.LimoOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.compose.ui.res.stringResource
import com.limo1800driver.app.R
import com.limo1800driver.app.data.network.NetworkConfig

@Composable
public fun BookingRouteMap(
    pickup: LatLng,
    dropoff: LatLng,
    extraStops: List<LatLng>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- State ---
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // --- Camera ---
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickup, 12f)
    }

    // --- Map Style ---
    val mapProperties = remember(mapType) {
        MapProperties(
            mapType = mapType,
            isTrafficEnabled = false,
            // Attempt to load custom style, handle failure gracefully
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
            } catch (e: Exception) { null }
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    }

    // --- Fetch Route Logic ---
    LaunchedEffect(pickup, dropoff, extraStops) {
        isLoading = true
        try {
            val points = DirectionsRepository.getRouteWithWaypoints(
                origin = pickup,
                dest = dropoff,
                waypoints = extraStops,
                apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            routePoints = points

            // Auto-center camera to fit the new route
            if (points.isNotEmpty()) {
                val bounds = LatLngBounds.builder().apply {
                    points.forEach { include(it) }
                    // Also ensure markers are strictly in bounds
                    include(pickup)
                    include(dropoff)
                    extraStops.forEach { include(it) }
                }.build()

                // 100 padding for breathing room
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        } catch (e: Exception) {
            Log.e("BookingRouteMap", "Error fetching route", e)
            // Fallback: draw straight lines if API fails
            routePoints = listOf(pickup) + extraStops + listOf(dropoff)
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = mapProperties,
            uiSettings = mapUiSettings,
            cameraPositionState = cameraPositionState
        ) {
            // 1. The Route Polyline (Professional Double-Layer)
            if (routePoints.isNotEmpty()) {
                // Layer A: The Border (Thicker, Darker)
                Polyline(
                    points = routePoints,
                    color = Color(0xFF1A1A1A), // Almost black border
                    width = 20f,
                    zIndex = 1f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )

                // Layer B: The Main Route (Thinner, Brand Color)
                Polyline(
                    points = routePoints,
                    color = Color(0xFF2196F3), // Keeping Blue as per your original file, or switch to LimoOrange
                    width = 12f,
                    zIndex = 2f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
            }

            // 2. Pickup Marker
            Marker(
                state = MarkerState(position = pickup),
                title = "Pickup",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                zIndex = 3f
            )

            // 3. Extra Stops
            extraStops.forEachIndexed { index, stop ->
                Marker(
                    state = MarkerState(position = stop),
                    title = "Stop ${index + 1}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    zIndex = 3f
                )
            }

            // 4. Destination Marker
            Marker(
                state = MarkerState(position = dropoff),
                title = "Dropoff",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                zIndex = 3f
            )
        }

        // --- Clean Control Panel ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MapIconButton(
                    icon = Icons.Default.Add,
                    onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } }
                )
                MapIconButton(
                    icon = Icons.Default.Remove,
                    onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) } }
                )
                Divider(modifier = Modifier.width(20.dp).padding(vertical = 2.dp))
                MapIconButton(
                    icon = Icons.Default.CenterFocusStrong,
                    onClick = {
                        val allPoints = listOf(pickup) + extraStops + listOf(dropoff)
                        val bounds = LatLngBounds.builder().apply {
                            allPoints.forEach { include(it) }
                            routePoints.forEach { include(it) }
                        }.build()
                        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100)) }
                    }
                )
                MapIconButton(
                    icon = Icons.Default.Layers,
                    isActive = mapType == MapType.HYBRID,
                    onClick = { mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL }
                )
            }
        }

        // --- Loading Indicator ---
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp),
                color = LimoOrange,
                strokeWidth = 3.dp
            )
        }
    }
}

// --- Reusable UI Component for Map Buttons ---

@Composable
private fun MapIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) LimoOrange.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) LimoOrange else Color.Gray,
            modifier = Modifier.size(18.dp)
        )
    }
}

// --- Directions API Helper (Networking) ---

object DirectionsRepository {
    private val client = OkHttpClient()

    /**
     * Fetches route from Google Directions API supporting Waypoints.
     */
    suspend fun getRouteWithWaypoints(
        origin: LatLng,
        dest: LatLng,
        waypoints: List<LatLng>,
        apiKey: String
    ): List<LatLng> {
        return withContext(Dispatchers.IO) {

            // Construct Waypoints String: "lat,lng|lat,lng|..."
            val waypointsStr = if (waypoints.isNotEmpty()) {
                "&waypoints=optimize:true|" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            } else ""

            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${dest.latitude},${dest.longitude}" +
                    waypointsStr +
                    "&mode=driving" +
                    "&key=$apiKey"

            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)

                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val points = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                    // Decodes Google's compressed polyline string
                    return@withContext PolyUtil.decode(points)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext emptyList()
        }
    }
}