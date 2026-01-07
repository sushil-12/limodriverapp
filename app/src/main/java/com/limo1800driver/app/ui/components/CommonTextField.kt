package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
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
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.AppColors

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
    trailingIcon: (@Composable (() -> Unit))? = null,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null
) {
    // Internal focus requester to handle clicks on the container
    val internalFocusRequester = remember { FocusRequester() }
    val currentFocusRequester = focusRequester ?: internalFocusRequester

    // Track focus state
    var isFocused by remember { mutableStateOf(false) }

    // Internal state for TextFieldValue to manage selection (cursor)
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = text)) }

    // 1. Sync External Changes:
    // If the parent updates 'text' (e.g., masking or reset), update our internal state.
    // We only update if the text content is actually different to avoid interfering with typing.
    if (text != textFieldValue.text) {
        textFieldValue = textFieldValue.copy(
            text = text,
            // If text changed externally, move cursor to the end to prevent index errors
            selection = TextRange(text.length)
        )
    }

    // 2. Smooth "Select All" Logic:
    // We use LaunchedEffect to trigger this whenever 'isFocused' becomes true.
    // This ensures it happens after any initial touch events, preventing the cursor
    // from jumping back to the touch position immediately.
    LaunchedEffect(isFocused) {
        if (isFocused) {
            val currentText = textFieldValue.text
            if (currentText.isNotEmpty()) {
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(0, currentText.length)
                )
            }
        }
    }

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

        // --- Input Field Container --- (matches user app: 50dp height, F5F5F5 background, 8dp corners)
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
                .clickable(enabled = enabled) { currentFocusRequester.requestFocus() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Placeholder
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = Color(0xFF9CA3AF),
                                fontSize = 16.sp
                            )
                        )
                    }

                    // The Input Field (matches user app: 16sp, LimoBlack)
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            // Only notify parent if text actually changed
                            if (text != newValue.text) {
                                onValueChange(newValue.text)
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = AppColors.LimoBlack,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(currentFocusRequester)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        singleLine = singleLine,
                        minLines = minLines,
                        maxLines = maxLines,
                        enabled = enabled,
                        readOnly = readOnly,
                        cursorBrush = SolidColor(LimoOrange),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                }

                // Trailing Icon
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
                        trailingIcon()
                    }
                }
            }
        }

        // --- Error Message Section --- (matches user app: 12sp, Red, 4dp spacing)
        errorMessage?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
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