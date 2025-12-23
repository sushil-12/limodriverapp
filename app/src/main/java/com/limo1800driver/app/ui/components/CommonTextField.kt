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
    val internalFocusRequester = remember { FocusRequester() }
    val currentFocusRequester = focusRequester ?: internalFocusRequester
    var isFocused by remember { mutableStateOf(false) }

    // 1. Internal state to manage cursor position (Selection)
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = text)) }

    // 2. Sync Logic: If parent updates 'text' (e.g. prefill or reset), update internal state.
    // We check (text != textFieldValue.text) to avoid overwriting the cursor while the user is typing.
    if (text != textFieldValue.text) {
        textFieldValue = textFieldValue.copy(
            text = text,
            // If text changed externally, move cursor to end to be safe
            selection = TextRange(text.length)
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- Label Section ---
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

        // --- Input Field Container ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = when {
                        errorMessage != null -> Color(0xFFEF4444)
                        isFocused -> LimoOrange
                        else -> Color(0xFFE0E0E0)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { currentFocusRequester.requestFocus() },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = Color(0xFF9CA3AF),
                                fontSize = 16.sp
                            )
                        )
                    }

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            // Update internal state (keeps cursor position)
                            textFieldValue = newValue
                            // Notify parent of text change
                            onValueChange(newValue.text)
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(currentFocusRequester)
                            .onFocusChanged { focusState ->
                                // 3. Handle "Select All" logic here
                                if (focusState.isFocused && !isFocused) {
                                    // Field just gained focus
                                    if (textFieldValue.text.isNotEmpty()) {
                                        textFieldValue = textFieldValue.copy(
                                            selection = TextRange(0, textFieldValue.text.length)
                                        )
                                    }
                                }
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

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
                        trailingIcon()
                    }
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}