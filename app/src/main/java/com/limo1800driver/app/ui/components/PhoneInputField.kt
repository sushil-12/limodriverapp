package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.TextStyle
import com.limo1800driver.app.data.model.Countries
import com.limo1800driver.app.data.model.Country
import com.limo1800driver.app.ui.theme.*

@Composable
fun PhoneInputField(
    label: String,
    phone: String,
    onPhoneChange: (String) -> Unit,
    selectedCountry: Country,
    onCountryChange: (Country) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    enabled: Boolean = true
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    // Theme Colors (Local definitions to ensure match without importing external vars)
    val inputBackground = Color(0xFFF3F4F6)
    val inputBorder = Color(0xFFE5E7EB)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Label Section ---
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

        // --- Input Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Country Flag Picker (Gray Box)
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(56.dp) // Standard Height
                    .background(
                        color = inputBackground,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = inputBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = enabled) { showCountryPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedCountry.flag,
                        style = TextStyle(fontSize = 24.sp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Country",
                        tint = AppColors.LimoBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Phone Number Input
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    // Basic filtering to allow only digits
                    if (input.all { it.isDigit() }) {
                        onPhoneChange(input)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = enabled,
                singleLine = true,
                prefix = {
                    Text(
                        text = "${selectedCountry.code} ",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = AppColors.LimoBlack,
                            fontWeight = FontWeight.Normal
                        )
                    )
                },
                placeholder = {
                    Text(
                        "9876543210",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = AppColors.LimoBlack,
                    fontWeight = FontWeight.Normal
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    // Backgrounds
                    focusedContainerColor = inputBackground,
                    unfocusedContainerColor = inputBackground,
                    disabledContainerColor = inputBackground,

                    // Borders
                    focusedBorderColor = AppColors.LimoBlack.copy(alpha = 0.5f),
                    unfocusedBorderColor = inputBorder,

                    cursorColor = AppColors.LimoOrange
                )
            )
        }
    }

    // --- Bottom Sheet Logic ---
    if (showCountryPicker) {
        CountryPickerBottomSheet(
            onDismiss = { showCountryPicker = false },
            onCountrySelected = { country ->
                onCountryChange(country)
                showCountryPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerBottomSheet(
    onDismiss: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select Country",
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.LimoBlack
                    )
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Color(0xFFE5E7EB))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(Countries.list) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCountrySelected(country)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = country.flag,
                                fontSize = 24.sp
                            )
                            Text(
                                text = country.name,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = AppColors.LimoBlack
                                )
                            )
                        }
                        Text(
                            text = country.code,
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E7EB))
                }
            }
        }
    }
}