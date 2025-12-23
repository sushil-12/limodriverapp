package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.theme.LimoOrange
import java.util.*

/**
 * Enum to track which step of the date selection is active
 */
enum class DateStep { MONTH, DAY, YEAR }

/**
 * Configuration for date picker validation
 */
data class DatePickerConfig(
    val allowFutureDates: Boolean = true,
    val allowPastDates: Boolean = true,
    val minAgeYears: Int? = null,
    val maxAgeYears: Int? = null,
    val yearRange: IntRange = 1950..Calendar.getInstance().get(Calendar.YEAR)
)

/**
 * Main Date Picker Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerComponent(
    label: String,
    selectedMonth: String?,
    selectedDay: String?,
    selectedYear: String?,
    onDateSelected: (month: String, day: String, year: String) -> Unit,
    modifier: Modifier = Modifier,
    config: DatePickerConfig = DatePickerConfig(),
    errorMessage: String? = null,
    isRequired: Boolean = false
) {
    var activeDateStep by remember { mutableStateOf<DateStep?>(null) }

    Column(modifier = modifier) {
        // Main Label
        if (label.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280) // Cool Gray
                    )
                )
                if (isRequired) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Inputs Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Month
            DateDropdownField(
                value = selectedMonth,
                placeholder = "Month",
                modifier = Modifier.weight(1.2f), // Give month slightly more space
                isError = errorMessage != null,
                onClick = { activeDateStep = DateStep.MONTH }
            )

            // Day
            DateDropdownField(
                value = selectedDay,
                placeholder = "Day",
                modifier = Modifier.weight(1f),
                isError = errorMessage != null,
                onClick = { activeDateStep = DateStep.DAY }
            )

            // Year
            DateDropdownField(
                value = selectedYear,
                placeholder = "Year",
                modifier = Modifier.weight(1.1f),
                isError = errorMessage != null,
                onClick = { activeDateStep = DateStep.YEAR }
            )
        }

        // Error Message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = errorMessage,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    // Date Selection Bottom Sheet
    if (activeDateStep != null) {
        ModalBottomSheet(
            onDismissRequest = { activeDateStep = null },
            containerColor = Color.White,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            DateSelectionContent(
                step = activeDateStep!!,
                config = config,
                onMonthSelected = { month ->
                    onDateSelected(month, selectedDay ?: "", selectedYear ?: "")
                    activeDateStep = DateStep.DAY
                },
                onDaySelected = { day ->
                    onDateSelected(selectedMonth ?: "", day, selectedYear ?: "")
                    activeDateStep = DateStep.YEAR
                },
                onYearSelected = { year ->
                    onDateSelected(selectedMonth ?: "", selectedDay ?: "", year)
                    activeDateStep = null
                },
                currentMonth = selectedMonth,
                currentDay = selectedDay,
                currentYear = selectedYear
            )
        }
    }
}

/**
 * Individual date dropdown field component - Cleaner Look
 */
@Composable
private fun DateDropdownField(
    value: String?,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isError) Color(0xFFEF4444) else Color(0xFFE5E7EB)
    val backgroundColor = Color.White // Cleaner white background
    val textColor = if (value.isNullOrBlank()) Color(0xFF9CA3AF) else AppColors.LimoBlack

    Box(
        modifier = modifier
            .height(52.dp) // Standard touch target
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value ?: placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = if (value.isNullOrBlank()) FontWeight.Normal else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * The Content of the Date Picker Sheet.
 */
@Composable
private fun DateSelectionContent(
    step: DateStep,
    config: DatePickerConfig,
    onMonthSelected: (String) -> Unit,
    onDaySelected: (String) -> Unit,
    onYearSelected: (String) -> Unit,
    currentMonth: String?,
    currentDay: String?,
    currentYear: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = when (step) {
                DateStep.MONTH -> "Select Month"
                DateStep.DAY -> "Select Day"
                DateStep.YEAR -> "Select Year"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.LimoBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val heightModifier = Modifier.height(350.dp)

        when (step) {
            DateStep.MONTH -> {
                val months = (1..12).map { String.format("%02d", it) }
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = heightModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(months.size) { index ->
                        val value = months[index]
                        val name = monthNames[index]
                        val isSelected = value == currentMonth

                        DateGridItem(
                            text = name,
                            subText = null, // Removed subtext for cleaner look (Jan is obvious)
                            isSelected = isSelected,
                            shape = RoundedCornerShape(12.dp), // Pill shape
                            onClick = { onMonthSelected(value) }
                        )
                    }
                }
            }
            DateStep.DAY -> {
                val days = (1..31).map { String.format("%02d", it) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = heightModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(days.size) { index ->
                        val day = days[index]
                        val isSelected = day == currentDay

                        DateGridItem(
                            text = day.toInt().toString(), // "1" instead of "01" looks cleaner
                            isSelected = isSelected,
                            shape = CircleShape, // Circular for days
                            isDayView = true,
                            onClick = { onDaySelected(day) }
                        )
                    }
                }
            }
            DateStep.YEAR -> {
                val filteredYears = config.yearRange.map { it.toString() }.reversed()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // 3 columns for years is easier to read than 4
                    modifier = heightModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredYears.size) { index ->
                        val year = filteredYears[index]
                        val isSelected = year == currentYear
                        val isValidYear = isValidYearForConfig(year, config, currentMonth, currentDay)

                        DateGridItem(
                            text = year,
                            isSelected = isSelected,
                            isEnabled = isValidYear,
                            shape = RoundedCornerShape(12.dp),
                            onClick = { if (isValidYear) onYearSelected(year) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cleaner Grid Item Component
 */
@Composable
private fun DateGridItem(
    text: String,
    subText: String? = null,
    isSelected: Boolean,
    isEnabled: Boolean = true,
    shape: Shape,
    isDayView: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !isEnabled -> Color(0xFFF3F4F6)
        isSelected -> LimoOrange
        else -> Color.White
    }

    val borderColor = when {
        !isEnabled -> Color.Transparent
        isSelected -> LimoOrange
        else -> Color(0xFFE5E7EB) // Subtle border for unselected
    }

    val contentColor = when {
        !isEnabled -> Color(0xFF9CA3AF)
        isSelected -> Color.White
        else -> Color(0xFF374151)
    }

    Box(
        modifier = Modifier
            .aspectRatio(if (isDayView) 1f else 2.2f) // Aspect ratio adjusts based on type
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                fontSize = if (isDayView) 14.sp else 16.sp
            )
            if (subText != null) {
                Text(
                    text = subText,
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- Logic Helpers (Unchanged) ---

private fun isValidYearForConfig(
    year: String,
    config: DatePickerConfig,
    currentMonth: String?,
    currentDay: String?
): Boolean {
    val yearInt = year.toIntOrNull() ?: return true
    val currentCalendar = Calendar.getInstance()

    if (config.minAgeYears != null || config.maxAgeYears != null) {
        val birthCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, yearInt)
            set(Calendar.MONTH, (currentMonth?.toIntOrNull() ?: 1) - 1)
            set(Calendar.DAY_OF_MONTH, currentDay?.toIntOrNull() ?: 1)
        }

        val age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (birthCalendar.after(currentCalendar)) return false
        if (config.minAgeYears != null && age < config.minAgeYears) return false
        if (config.maxAgeYears != null && age > config.maxAgeYears) return false
    }

    if (!config.allowFutureDates && yearInt > currentCalendar.get(Calendar.YEAR)) return false
    if (!config.allowPastDates && yearInt < currentCalendar.get(Calendar.YEAR)) return false

    return true
}