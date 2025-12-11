package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.limo1800driver.app.ui.theme.*

@Composable
fun CommonDropdown(
    label: String,
    placeholder: String,
    selectedValue: String?,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

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

        // --- Dropdown Trigger Section ---
        Box {
            OutlinedTextField(
                value = selectedValue ?: "",
                onValueChange = {}, // Read-only, so no change handling needed here
                placeholder = {
                    Text(
                        text = placeholder,
                        style = AppTextStyles.bodyMedium.copy(
                            color = Color(0xFF9CA3AF), // Placeholder Gray
                            fontSize = 16.sp
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Fixed height matching TextField
                enabled = enabled,
                readOnly = true, // Key change: Keeps text bold but prevents typing
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select",
                        tint = AppColors.LimoBlack.copy(alpha = 0.6f)
                    )
                },
                textStyle = AppTextStyles.bodyMedium.copy(
                    fontSize = 16.sp,
                    color = AppColors.LimoBlack,
                    fontWeight = FontWeight.Normal
                ),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    // Background Colors (Light Gray filled look)
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    disabledContainerColor = Color(0xFFF3F4F6),

                    // Border Colors
                    focusedBorderColor = AppColors.LimoBlack.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color(0xFFE5E7EB), // Subtle gray border
                    disabledBorderColor = Color(0xFFE5E7EB),

                    // Icons/Cursor
                    cursorColor = AppColors.LimoOrange
                )
            )

            // Transparent overlay to capture clicks perfectly
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = enabled) { expanded = true }
            )

            // --- Dropdown Menu ---
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Slightly smaller than full width for visual balance
                    .background(Color.White),
                properties = PopupProperties(focusable = true)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = AppTextStyles.bodyMedium.copy(
                                    fontSize = 16.sp,
                                    color = AppColors.LimoBlack
                                )
                            )
                        },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}