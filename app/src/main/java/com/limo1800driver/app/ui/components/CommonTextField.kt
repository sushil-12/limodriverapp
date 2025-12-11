package com.limo1800driver.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.theme.*

@Composable
fun CommonTextField(
    label: String,
    placeholder: String,
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
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
                    color = Color(0xFF6B7280) // Dark Gray for label text
                )
            )
            if (isRequired) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "*",
                    style = AppTextStyles.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444) // Red for asterisk
                    )
                )
            }
        }

        // --- Input Field Section ---
        OutlinedTextField(
            value = text,
            onValueChange = onValueChange,
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
                .height(56.dp), // Standard fixed height for consistency
            enabled = enabled,
            singleLine = true,
            textStyle = AppTextStyles.bodyMedium.copy(
                fontSize = 16.sp,
                color = AppColors.LimoBlack,
                fontWeight = FontWeight.Normal
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(8.dp), // Rounded corners matching screenshot
            colors = OutlinedTextFieldDefaults.colors(
                // Background Colors (Light Gray filled look)
                focusedContainerColor = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF3F4F6),
                disabledContainerColor = Color(0xFFF3F4F6),

                // Border Colors
                focusedBorderColor = AppColors.LimoBlack.copy(alpha = 0.5f),
                unfocusedBorderColor = Color(0xFFE5E7EB), // Subtle gray border
                errorBorderColor = Color(0xFFEF4444),

                // Cursor
                cursorColor = AppColors.LimoOrange
            )
        )
    }
}