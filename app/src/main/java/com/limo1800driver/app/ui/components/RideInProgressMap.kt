package com.limo1800driver.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
// FIX: Alias the GMS class to avoid conflict with the Composable function
import com.google.android.gms.maps.GoogleMap as GmsMap
import com.limo1800driver.app.data.socket.DriverActiveRide
import com.limo1800driver.app.rideinprogress.RideLocationSample
import com.limo1800driver.app.R
import kotlinx.coroutines.launch

// FIX: Define Converter as a top-level variable, NOT an extension on Companion
val LatLngVectorConverter = TwoWayConverter<LatLng, AnimationVector2D>(
    convertToVector = { AnimationVector2D(it.latitude.toFloat(), it.longitude.toFloat()) },
    convertFromVector = { LatLng(it.v1.toDouble(), it.v2.toDouble()) }
)

@Composable
fun RideInProgressMap(
    ride: DriverActiveRide?,
    driverLocation: RideLocationSample?,
    activeRoutePolyline: List<LatLng>,
    previewRoutePolyline: List<LatLng>,
    modifier: Modifier = Modifier, // Move modifier here to follow Compose guidelines
    autoFollowEnabled: Boolean = true
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true
        )
    }

    val cameraPositionState = rememberCameraPositionState()

    val mapProperties = remember(isDarkTheme) {
        MapProperties(
            isBuildingEnabled = false,
            isTrafficEnabled = false,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
            } catch (e: Exception) { null }
        )
    }

    val pickup = ride?.let { LatLng(it.pickupLatitude ?: 0.0, it.pickupLongitude ?: 0.0) }?.takeIf { it.latitude != 0.0 }
    val dropoff = ride?.let { LatLng(it.dropoffLatitude ?: 0.0, it.dropoffLongitude ?: 0.0) }?.takeIf { it.latitude != 0.0 }

    // FIX: Use the top-level converter variable defined above
    val animatedDriverPosition = remember {
        Animatable(
            initialValue = driverLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0),
            typeConverter = LatLngVectorConverter
        )
    }
    val animatedDriverBearing = remember { Animatable(driverLocation?.bearingDegrees ?: 0f) }

    LaunchedEffect(driverLocation) {
        driverLocation?.let { sample ->
            val target = LatLng(sample.latitude, sample.longitude)
            val targetBearing = sample.bearingDegrees ?: 0f

            launch {
                animatedDriverPosition.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            }

            launch {
                val current = animatedDriverBearing.value
                var diff = targetBearing - current
                while (diff < -180) diff += 360
                while (diff > 180) diff -= 360
                animatedDriverBearing.animateTo(
                    targetValue = current + diff,
                    animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                )
            }
        }
    }

    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Initial Fit
    LaunchedEffect(pickup, dropoff) {
        if (pickup != null && dropoff != null) {
            val bounds = LatLngBounds.Builder().include(pickup).include(dropoff).build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150), 1000)
        }
    }

    LaunchedEffect(driverLocation, isUserInteracting) {
        if (driverLocation != null && autoFollowEnabled && !isUserInteracting) {
            if (System.currentTimeMillis() - lastInteractionTime > 3000) {
                val currentZoom = cameraPositionState.position.zoom.takeIf { it > 10f } ?: 16f
                val tilt = if (currentZoom > 15f) 45f else 0f

                val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(driverLocation.latitude, driverLocation.longitude))
                        .zoom(currentZoom)
                        .bearing(driverLocation.bearingDegrees ?: 0f)
                        .tilt(tilt)
                        .build()
                )
                cameraPositionState.animate(cameraUpdate, 1000)
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = { }
    ) {
        MapEffect(Unit) { map ->
            // FIX: Use the GmsMap alias to refer to the class constants
            map.setOnCameraMoveStartedListener { reason ->
                if (reason == GmsMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isUserInteracting = true
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
            map.setOnCameraIdleListener {
                isUserInteracting = false
            }
        }

        if (activeRoutePolyline.isNotEmpty()) {
            Polyline(
                points = activeRoutePolyline,
                color = Color.Black,
                width = 16f,
                zIndex = 1f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
            Polyline(
                points = activeRoutePolyline,
                color = Color(0xFFFF9800),
                width = 10f,
                zIndex = 2f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
        }

        if (previewRoutePolyline.isNotEmpty()) {
            Polyline(
                points = previewRoutePolyline,
                color = Color.Gray.copy(alpha = 0.6f),
                width = 10f,
                pattern = listOf(Dash(20f), Gap(10f)),
                jointType = JointType.ROUND
            )
        }

        if (pickup != null) {
            Marker(
                state = MarkerState(pickup),
                title = "Pickup",
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                zIndex = 3f
            )
        }

        if (dropoff != null) {
            Marker(
                state = MarkerState(dropoff),
                title = "Dropoff",
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                zIndex = 3f
            )
        }

        if (driverLocation != null) {
            Marker(
                state = MarkerState(animatedDriverPosition.value),
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_car),
                rotation = animatedDriverBearing.value,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                zIndex = 5f,
                flat = true
            )
        }
    }
}

fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorResId: Int, sizeDp: Int = 32): BitmapDescriptor? {
    return try {
        val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null

        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()

        drawable.setBounds(0, 0, sizePx, sizePx)

        val bitmap = Bitmap.createBitmap(
            sizePx,
            sizePx,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        BitmapDescriptorFactory.defaultMarker()
    }
}