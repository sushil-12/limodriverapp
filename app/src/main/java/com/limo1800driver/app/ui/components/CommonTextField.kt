package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    trailingIcon: (@Composable (() -> Unit))? = null
) {
    // State for focus and selection logic
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var shouldSelectAll by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp) // Spacing between Label and Box
    ) {
        // --- Label Section ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray // Matches your EditableTextField style
                )
            )
            if (isRequired) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "*",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444) // Red
                    )
                )
            }
        }

        // --- Input Field Container ---
        // This replaces OutlinedTextField with the custom Box design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp) // Matches your EditableTextField height
                .background(
                    color = Color(0xFFF5F5F5), // Light Gray Background
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    // Dynamic border: Orange when focused, Light Gray when not
                    color = if (isFocused) LimoOrange else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // We wrap the BasicTextField and Placeholder in a Box to stack them
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Placeholder Text
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = Color(0xFF9CA3AF), // Placeholder Gray
                                fontSize = 16.sp
                            )
                        )
                    }

                    // The Editable BasicTextField Logic
                    BasicTextField(
                        value = TextFieldValue(
                            text = text,
                            // Select-All logic
                            selection = if (shouldSelectAll && text.isNotEmpty()) {
                                TextRange(0, text.length)
                            } else {
                                TextRange(text.length)
                            }
                        ),
                        onValueChange = {
                            // Once user starts typing/interacting, disable select-all
                            shouldSelectAll = false
                            onValueChange(it.text)
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black, // Or LimoBlack
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                val wasFocused = isFocused
                                isFocused = focusState.isFocused

                                // Select all text only on first focus, and only if field has content
                                if (!wasFocused && focusState.isFocused && text.isNotEmpty()) {
                                    shouldSelectAll = true
                                } else if (!focusState.isFocused) {
                                    // Reset select-all flag when field loses focus
                                    shouldSelectAll = false
                                }
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        singleLine = singleLine,
                        minLines = minLines,
                        maxLines = maxLines,
                        enabled = enabled,
                        readOnly = readOnly,
                        cursorBrush = SolidColor(LimoOrange) // Custom Cursor Color
                    )
                }

                // Trailing Icon (if provided)
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}