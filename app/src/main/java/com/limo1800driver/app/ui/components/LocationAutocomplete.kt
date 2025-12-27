package com.limo1800driver.app.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.limo1800driver.app.data.PlacePrediction
import com.limo1800driver.app.data.PlacesService
import com.limo1800driver.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationAutocomplete(
    label: String, // Added Label to match design
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (fullAddress: String, name: String, city: String, state: String, displayText: String, latitude: Double?, longitude: Double?, countryCode: String?, placeLabel: String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter your location",
    isRequired: Boolean = false, // Added Required flag
    typeFilter: com.google.android.libraries.places.api.model.TypeFilter? = null,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // Safely initialize PlacesService
    val placesService = remember(isPreview) {
        if (isPreview) null else try { PlacesService(context) } catch (e: Exception) {
            Log.e("LocationAutocomplete", "Failed to initialize PlacesService", e)
            null
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var predictions by remember { mutableStateOf(emptyList<PlacePrediction>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var suppressSearch by remember { mutableStateOf(false) }
    var userHasTyped by remember { mutableStateOf(false) }

    // Debounced search logic
    LaunchedEffect(value, placesService, suppressSearch) {
        // If the field is pre-filled programmatically (e.g. from API), don't auto-open suggestions.
        if (!userHasTyped) {
            showSuggestions = false
            predictions = emptyList()
            return@LaunchedEffect
        }
        if (suppressSearch) return@LaunchedEffect

        if (value.length >= 2) {
            if (placesService != null) {
                delay(500)
                isLoading = true
                try {
                    predictions = placesService.getPlacePredictions(value, typeFilter)
                    showSuggestions = predictions.isNotEmpty()
                } catch (e: Exception) {
                    Log.e("LocationAutocomplete", "Error getting predictions", e)
                    predictions = emptyList()
                    showSuggestions = false
                }
                isLoading = false
            }
        } else {
            predictions = emptyList()
            showSuggestions = false
        }
    }

    Column(
        modifier = modifier.zIndex(10f), // Ensure dropdown appears on top
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Label Section (Matches CommonTextField) ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                style = AppTextStyles.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280) // Dark Gray
                )
            )
            if (isRequired) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "*",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444) // Red
                    )
                )
            }
        }

        Box {
            // --- Input Field (Matches CommonTextField Styling) ---
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    suppressSearch = false
                    userHasTyped = true
                    onValueChange(newValue)
                },
                placeholder = {
                    Text(
                        placeholder,
                        style = AppTextStyles.bodyMedium.copy(
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                singleLine = true,
                textStyle = AppTextStyles.bodyMedium.copy(
                    fontSize = 16.sp,
                    color = AppColors.LimoBlack,
                    fontWeight = FontWeight.Normal
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    // Background Colors (Light Gray filled look)
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),

                    // Border Colors - red for error, black for focused, light gray otherwise
                    focusedBorderColor = if (errorMessage != null) Color(0xFFEF4444) else AppColors.LimoBlack.copy(alpha = 0.5f),
                    unfocusedBorderColor = if (errorMessage != null) Color(0xFFEF4444) else Color(0xFFE5E7EB),

                    cursorColor = AppColors.LimoOrange
                )
            )

            // --- Suggestions Dropdown ---
            if (showSuggestions && predictions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp) // Offset to appear below the 56dp TextField
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .zIndex(20f), // Higher zIndex to float over other fields
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 250.dp)
                            .fillMaxWidth()
                    ) {
                        items(predictions) { prediction ->
                            LocationSuggestionItem(
                                prediction = prediction,
                                onClick = {
                                    suppressSearch = true
                                    userHasTyped = false
                                    onValueChange(prediction.fullText)
                                    showSuggestions = false
                                    predictions = emptyList()

                                    // Fetch place details
                                    coroutineScope.launch {
                                        if (placesService != null) {
                                            try {
                                                val placeDetails = placesService.getPlaceDetails(prediction.placeId)
                                                if (placeDetails != null) {
                                                    onLocationSelected(
                                                        placeDetails.address,
                                                        placeDetails.name, // pass place name
                                                        placeDetails.city,
                                                        placeDetails.state,
                                                        prediction.fullText,
                                                        placeDetails.latitude,
                                                        placeDetails.longitude,
                                                        placeDetails.country, // ISO country code (short)
                                                        prediction.primaryText
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                Log.e("LocationAutocomplete", "Error fetching place details", e)
                                            }
                                        }
                                    }
                                }
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                        }
                    }
                }
            }
        }

        // Error Message
        errorMessage?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = AppTextStyles.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun LocationSuggestionItem(
    prediction: PlacePrediction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prediction.primaryText,
                style = AppTextStyles.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.LimoBlack,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (prediction.secondaryText.isNotEmpty()) {
                Text(
                    text = prediction.secondaryText,
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280) // Darker Gray for secondary text
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}