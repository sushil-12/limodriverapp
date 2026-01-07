package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.theme.LimoOrange
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date/Time Field Component - matches user app EditableField design
 * Used for displaying date/time values that can be clicked to open pickers
 */
@Composable
fun DateTimeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    isRequired: Boolean = false,
    placeholder: String = "Select"
) {
    Column(modifier = modifier) {
        // Label (matches user app: 12sp, Gray, SemiBold, uppercase)
        if (label.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                )
                if (isRequired) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // Field Container (matches user app: 50dp height, F5F5F5 background, 8dp corners)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    if (isError) Color(0xFFEF4444) else Color(0xFFE0E0E0),
                    RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    value.ifEmpty { placeholder },
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = if (value.isEmpty()) Color(0xFF9CA3AF) else AppColors.LimoBlack,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Error Message (matches user app: 12sp, Red, 4dp spacing)
        if (isError && errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Date Field - specialized for date selection
 */
@Composable
fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    isRequired: Boolean = false,
    placeholder: String = "Select date"
) {
    DateTimeField(
        label = label,
        value = formatDate(value),
        onClick = onClick,
        icon = Icons.Default.CalendarToday,
        modifier = modifier,
        isError = isError,
        errorMessage = errorMessage,
        isRequired = isRequired,
        placeholder = placeholder
    )
}

/**
 * Time Field - specialized for time selection
 */
@Composable
fun TimeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    isRequired: Boolean = false,
    placeholder: String = "Select time"
) {
    DateTimeField(
        label = label,
        value = formatTime(value),
        onClick = onClick,
        icon = Icons.Default.AccessTime,
        modifier = modifier,
        isError = isError,
        errorMessage = errorMessage,
        isRequired = isRequired,
        placeholder = placeholder
    )
}

/**
 * Format date string from "yyyy-MM-dd" to "MMM dd, yyyy"
 */
private fun formatDate(dateString: String): String {
    if (dateString.isEmpty()) return ""
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        if (date != null) {
            displayDateFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Format time string from "HH:mm:ss" or "h:mm a" to "h:mm a"
 */
private fun formatTime(timeString: String): String {
    if (timeString.isEmpty()) return ""
    return try {
        // Try multiple input formats
        val inputFormats = listOf(
            "HH:mm:ss",
            "HH:mm",
            "h:mm a",
            "h:mm:ss a",
            "hh:mm a",
            "hh:mm:ss a"
        )
        
        val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        
        for (inputFormat in inputFormats) {
            try {
                val timeFormat = SimpleDateFormat(inputFormat, Locale.getDefault())
                val time = timeFormat.parse(timeString)
                if (time != null) {
                    return displayTimeFormat.format(time)
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        // If all parsing failed, return original string
        timeString
    } catch (e: Exception) {
        timeString
    }
}

