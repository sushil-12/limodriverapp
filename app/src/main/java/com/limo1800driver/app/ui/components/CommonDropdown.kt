package com.limo1800driver.app.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.theme.*

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
    searchable: Boolean = options.size >= 10
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State to track "focus" (simulated when sheet is open)
    val isFocused = sheetOpen

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- Label Section ---
        if (label.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
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
        }

        // --- Custom Input Container (Matches CommonTextField) ---
        val shape = RoundedCornerShape(8.dp)
        // Highlight border orange if the dropdown sheet is currently open
        val borderColor = if (isFocused) LimoOrange else Color(0xFFE0E0E0)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp) // Fixed height like single-line TextField
                .background(Color(0xFFF5F5F5), shape)
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled) { sheetOpen = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Text Content
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedValue.isNullOrBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF9CA3AF), // Placeholder Gray
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = selectedValue,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.LimoBlack,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Trailing Icon
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select",
                    tint = AppColors.LimoBlack.copy(alpha = 0.6f)
                )
            }
        }
    }

    // --- Bottom Sheet Logic (Unchanged functionality) ---
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
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp), // Safe area padding
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sheet Title
                Text(
                    text = label.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.LimoBlack
                )

                // Search Bar (Only if list is long)
                if (searchable) {
                    // Using standard OutlinedTextField for search inside sheet (standard styling)
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = LimoOrange,
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        )
                    )
                }

                val maxListHeight = 420.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filtered.size) { idx ->
                            val option = filtered[idx]
                            val isSelected = option == selectedValue

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueSelected(option)
                                        sheetOpen = false
                                        search = ""
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8F8F8),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = AppColors.LimoBlack,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = LimoOrange
                                        )
                                    }
                                }
                            }
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    text = "No results",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}