package com.limo1800driver.app.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.components.SelectionMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonDropdown(
    label: String,
    placeholder: String,
    selectedValue: String?,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    enabled: Boolean = true,
    searchable: Boolean = options.size >= 10,
    errorMessage: String? = null,
    onDropdownOpened: (() -> Unit)? = null,
    selectionMode: SelectionMode = SelectionMode.NORMAL,
    selectedYear: String? = null,
    selectedMonth: String? = null
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFocused = sheetOpen

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- Label Section --- (matches user app: 12sp, Gray, SemiBold, uppercase)
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
        }

        // --- Custom Input Container --- (matches user app: 50dp height, F5F5F5 background, 8dp corners)
        val shape = RoundedCornerShape(8.dp)
        val borderColor = when {
            errorMessage != null -> Color(0xFFEF4444)
            isFocused -> LimoOrange
            else -> Color(0xFFE0E0E0)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFF5F5F5), shape)
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled) {
                    sheetOpen = true
                    onDropdownOpened?.invoke()
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedValue ?: placeholder,
                        style = TextStyle(
                            color = if (selectedValue.isNullOrBlank()) Color(0xFF9CA3AF) else AppColors.LimoBlack,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select",
                    tint = Color.Gray
                )
            }
        }

        // --- Error Message Section --- (matches user app: 12sp, Red, 4dp spacing)
        if (!errorMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
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

    // --- Bottom Sheet Logic ---
    if (sheetOpen) {
        val filtered = remember(options, search) {
            if (!searchable || search.isBlank()) options
            else options.filter { it.contains(search, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = {
                sheetOpen = false
                search = ""
            },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = label.ifBlank { "Select Option" }.lowercase().replaceFirstChar { it.uppercase() },
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.LimoBlack
                    )
                )

                if (searchable) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimoOrange,
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        )
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    when (selectionMode) {
                        SelectionMode.NORMAL -> {
                            // Regular dropdown list
                            LazyColumn {
                                items(filtered.size) { idx ->
                                    val option = filtered[idx]
                                    val isSelected = option == selectedValue

                                    Column {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                onValueSelected(option)
                                                sheetOpen = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0xFFFFF7ED) else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = option, color = AppColors.LimoBlack)
                                                if (isSelected) Icon(Icons.Default.Check, null, tint = LimoOrange)
                                            }
                                        }

                                        // Add light divider between options (except after the last item)
                                        if (idx < filtered.size - 1) {
                                            HorizontalDivider(
                                                color = Color(0xFFE5E7EB),
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SelectionMode.YEAR -> {
                            // Calendar-style year picker
                            YearPicker(
                                selectedYear = selectedValue,
                                onYearSelected = { year ->
                                    onValueSelected(year)
                                    sheetOpen = false
                                }
                            )
                        }

                        SelectionMode.MONTH -> {
                            // Calendar-style month picker for selected year
                            MonthPicker(
                                selectedYear = selectedYear,
                                selectedMonth = selectedValue,
                                onMonthSelected = { month ->
                                    onValueSelected(month)
                                    sheetOpen = false
                                }
                            )
                        }

                        SelectionMode.DAY -> {
                            // Calendar-style day picker for selected year and month
                            DayPicker(
                                selectedYear = selectedYear,
                                selectedMonth = selectedMonth,
                                selectedDay = selectedValue,
                                onDaySelected = { day ->
                                    onValueSelected(day)
                                    sheetOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Calendar-style Year Picker
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun YearPicker(
    selectedYear: String?,
    onYearSelected: (String) -> Unit
) {
    val currentYear = LocalDate.now().year
    val startYear = currentYear - 100 // Show last 100 years
    val endYear = currentYear + 10 // Show next 10 years
    val yearCount = endYear - startYear + 1

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(yearCount) { index ->
            val year = startYear + index
            val yearString = year.toString()
            val isSelected = yearString == selectedYear

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onYearSelected(yearString) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) LimoOrange.copy(alpha = 0.1f) else Color(0xFFF8F8F8)
            ) {
                Text(
                    text = yearString,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) LimoOrange else AppColors.LimoBlack
                )
            }
        }
    }
}

// Calendar-style Month Picker
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MonthPicker(
    selectedYear: String?,
    selectedMonth: String?,
    onMonthSelected: (String) -> Unit
) {
    val months = (1..12).map { monthNum ->
        val month = java.time.Month.of(monthNum)
        monthNum.toString().padStart(2, '0') to month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Show selected year if available
        selectedYear?.let {
            Text(
                text = "Select month for $it",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Grid layout for months (3 columns)
        val rows = months.chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (monthValue, monthName) ->
                    val isSelected = monthValue == selectedMonth

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onMonthSelected(monthValue) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) LimoOrange.copy(alpha = 0.1f) else Color(0xFFF8F8F8)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = monthName.take(3), // Show first 3 letters (Jan, Feb, etc.)
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) LimoOrange else AppColors.LimoBlack,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Fill remaining space if row has fewer than 3 items
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Calendar-style Day Picker
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayPicker(
    selectedYear: String?,
    selectedMonth: String?,
    selectedDay: String?,
    onDaySelected: (String) -> Unit
) {
    val year = selectedYear?.toIntOrNull() ?: LocalDate.now().year
    val month = selectedMonth?.toIntOrNull() ?: 1

    val yearMonth = try {
        YearMonth.of(year, month)
    } catch (e: Exception) {
        YearMonth.now()
    }

    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7 // 0 = Sunday, 6 = Saturday

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with month/year
        Text(
            text = "${java.time.Month.of(month).getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} $year",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.LimoBlack,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Day headers (Sun, Mon, Tue, etc.)
        Row(modifier = Modifier.fillMaxWidth()) {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        val totalCells = 42 // 6 weeks * 7 days
        val days = (1..totalCells).map { cellIndex ->
            val dayOfMonth = cellIndex - firstDayOfWeek
            if (dayOfMonth in 1..daysInMonth) dayOfMonth else null
        }

        val rows = days.chunked(7)
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day != null) {
                        val dayString = day.toString().padStart(2, '0')
                        val isSelected = dayString == selectedDay

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clickable { onDaySelected(dayString) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) LimoOrange.copy(alpha = 0.2f) else Color(0xFFF8F8F8)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) LimoOrange else AppColors.LimoBlack
                                )
                            }
                        }
                    } else {
                        // Empty cell for days not in this month
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                    }
                }
            }
        }
    }
}