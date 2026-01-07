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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.theme.*

@Composable
fun CommonTextArea(
    label: String,
    placeholder: String,
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    minLines: Int = 3,
    maxLines: Int = 6,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: (@Composable (() -> Unit))? = null,
    labelFontSize: TextUnit = 12.sp,
    textFontSize: TextUnit = 16.sp
) {
    // --- State for "Select All on Focus" logic ---
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var shouldSelectAll by remember { mutableStateOf(false) }

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
                        fontSize = labelFontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                )
                if (isRequired) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        style = TextStyle(
                            fontSize = labelFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    )
                }
            }
        }

        // --- Custom Input Container --- (matches user app: F5F5F5 background, 8dp corners)
        val shape = RoundedCornerShape(8.dp)
        val borderColor = when {
            isFocused -> LimoOrange
            else -> Color(0xFFE0E0E0)
        }

        // Wrapper Box for Border & Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), shape)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp) // Fixed padding
        ) {
            
            // Row to hold Text Field + Optional Trailing Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top, // Align Top for Text Area
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopStart // Align Top
                ) {
                    // Placeholder
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = textFontSize,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    // The Input Field (matches user app: LimoBlack color)
                    BasicTextField(
                        value = TextFieldValue(
                            text = text,
                            // Select all on first focus
                            selection = if (shouldSelectAll && text.isNotEmpty()) {
                                androidx.compose.ui.text.TextRange(0, text.length)
                            } else {
                                androidx.compose.ui.text.TextRange(text.length)
                            }
                        ),
                        onValueChange = {
                            shouldSelectAll = false
                            onValueChange(it.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                val wasFocused = isFocused
                                isFocused = focusState.isFocused
                                if (!wasFocused && focusState.isFocused && text.isNotEmpty()) {
                                    shouldSelectAll = true
                                } else if (!focusState.isFocused) {
                                    shouldSelectAll = false
                                }
                            },
                        enabled = enabled,
                        readOnly = readOnly,
                        textStyle = TextStyle(
                            fontSize = textFontSize,
                            color = AppColors.LimoBlack,
                            fontWeight = FontWeight.Normal
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = false, // Always multi-line
                        minLines = minLines,
                        maxLines = maxLines,
                        cursorBrush = SolidColor(LimoOrange)
                    )
                }

                // --- Trailing Icon (Top aligned) ---
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Add slight top padding to align icon with the first line of text
                    Box(modifier = Modifier.padding(top = 2.dp)) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}