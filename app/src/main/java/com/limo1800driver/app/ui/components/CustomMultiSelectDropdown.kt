package com.limo1800driver.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.theme.* @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CustomMultiSelectDropdown(
    label: String,
    placeholder: String,
    items: List<T>,
    selectedItems: List<T>,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    onItemRemoved: (T) -> Unit,
    // --- Restored External State Control ---
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    // ---------------------------------------
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    enabled: Boolean = true,
    errorMessage: String? = null
) {
    // We keep search internal as it usually resets on close
    var search by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Reset search when sheet closes
    LaunchedEffect(isExpanded) {
        if (!isExpanded) search = ""
    }

    // Logic to filter items based on search
    val filteredItems = remember(items, search) {
        if (search.isBlank()) items
        else items.filter { itemLabel(it).contains(search, ignoreCase = true) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

        // --- Input Container (Matches CommonDropdown Visuals) ---
        val shape = RoundedCornerShape(8.dp)
        val borderColor = when {
            errorMessage != null -> Color(0xFFEF4444)
            isExpanded -> LimoOrange
            else -> Color(0xFFE0E0E0)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .background(Color(0xFFF5F5F5), shape)
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled) {
                    onToggleExpand() // Uses parent's control
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val displayText = if (selectedItems.isEmpty()) {
                        placeholder
                    } else {
                        "${selectedItems.size} Selected"
                    }

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (selectedItems.isEmpty()) Color(0xFF9CA3AF) else AppColors.LimoBlack,
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
                    tint = AppColors.LimoBlack.copy(alpha = 0.6f)
                )
            }
        }

        // --- Selected Chips ---
        if (selectedItems.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(selectedItems) { item ->
                    Surface(
                        color = Color(0xFFFFF7ED), // Light LimoOrange tint
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, LimoOrange.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = itemLabel(item),
                                fontSize = 13.sp,
                                color = AppColors.LimoBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onItemRemoved(item) },
                                tint = LimoOrange
                            )
                        }
                    }
                }
            }
        }

        // --- Error Message ---
        if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    // --- Bottom Sheet (Triggered by isExpanded) ---
    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = onToggleExpand, // Calls parent to close
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
                    text = label.ifBlank { "Select Options" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.LimoBlack
                )

                // Search Bar
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LimoOrange,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // Options List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = selectedItems.contains(item)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemSelected(item) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = itemLabel(item),
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.LimoBlack,
                                modifier = Modifier.weight(1f)
                            )

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onItemSelected(item) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = LimoOrange,
                                    uncheckedColor = Color(0xFFD1D5DB)
                                )
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFFF3F4F6),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}