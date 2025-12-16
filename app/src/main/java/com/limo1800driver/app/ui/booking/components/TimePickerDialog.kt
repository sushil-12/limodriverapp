package com.limo1800driver.app.ui.booking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.limo1800driver.app.ui.theme.LimoBlack
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.LimoWhite
import java.util.Calendar
import java.util.Date

/**
 * Ported 1:1 from limouserapp's clock dialog so driver app uses the same UX.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TimePickerDialog(
    selectedTime: Date,
    onTimeSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    var showKeyboardInput by remember { mutableStateOf(false) }

    val initialCalendar = Calendar.getInstance().apply { time = selectedTime }
    val initialHour24 = initialCalendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialCalendar.get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour24,
        initialMinute = initialMinute,
        is24Hour = false
    )

    var selectedHour12 by remember {
        val hour = initialHour24
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        mutableIntStateOf(hour12)
    }
    var selectedMinuteKeyboard by remember { mutableIntStateOf(initialMinute) }
    var isAMKeyboard by remember { mutableStateOf(initialHour24 < 12) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .size(width = 328.dp, height = if (showKeyboardInput) 256.dp else 524.dp)
                .background(LimoWhite, RoundedCornerShape(12.dp))
                .border(1.dp, LimoBlack.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            if (showKeyboardInput) {
                KeyboardTimeInputView(
                    selectedHour = selectedHour12,
                    selectedMinute = selectedMinuteKeyboard,
                    isAM = isAMKeyboard,
                    onHourChange = { selectedHour12 = it },
                    onMinuteChange = { selectedMinuteKeyboard = it },
                    onAMPMChange = { isAMKeyboard = it },
                    onClose = { showKeyboardInput = false },
                    onConfirm = {
                        val newTime = updateTimeFromComponents(selectedTime, selectedHour12, selectedMinuteKeyboard, isAMKeyboard)
                        onTimeSelected(newTime)
                        onDismiss()
                    }
                )
            } else {
                ClockTimePickerMaterial3(
                    state = timePickerState,
                    onKeyboardTap = {
                        val hour24 = timePickerState.hour
                        selectedHour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
                        selectedMinuteKeyboard = timePickerState.minute
                        isAMKeyboard = hour24 < 12
                        showKeyboardInput = true
                    },
                    onConfirm = {
                        val newTime = Calendar.getInstance().apply {
                            time = selectedTime
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time

                        onTimeSelected(newTime)
                        onDismiss()
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ClockTimePickerMaterial3(
    state: TimePickerState,
    onKeyboardTap: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TimePicker(
            state = state,
            modifier = Modifier.weight(1f),
            colors = TimePickerDefaults.colors(
                clockDialSelectedContentColor = LimoWhite,
                clockDialUnselectedContentColor = LimoBlack.copy(alpha = 0.6f),
                clockDialColor = LimoOrange.copy(alpha = 0.05f),
                selectorColor = LimoOrange,
                periodSelectorBorderColor = LimoBlack.copy(alpha = 0.2f),
                periodSelectorSelectedContainerColor = LimoOrange,
                periodSelectorUnselectedContainerColor = LimoWhite,
                periodSelectorSelectedContentColor = LimoWhite,
                periodSelectorUnselectedContentColor = LimoBlack,
                timeSelectorSelectedContainerColor = LimoOrange,
                timeSelectorUnselectedContainerColor = LimoBlack.copy(alpha = 0.05f),
                timeSelectorSelectedContentColor = LimoWhite,
                timeSelectorUnselectedContentColor = LimoBlack
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onKeyboardTap,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = "Keyboard",
                    tint = LimoBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onCancel) {
                    Text(
                        "CANCEL",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "DONE",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LimoWhite
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardTimeInputView(
    selectedHour: Int,
    selectedMinute: Int,
    isAM: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAMPMChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    var hourText by remember(selectedHour) { mutableStateOf(selectedHour.toString()) }
    var minuteText by remember(selectedMinute) { mutableStateOf(String.format("%02d", selectedMinute)) }
    var isAMState by remember(isAM) { mutableStateOf(isAM) }

    var showError by remember { mutableStateOf(false) }

    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                "ENTER TIME",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimoBlack.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { text ->
                        val filteredText = text.filter { it.isDigit() }
                        hourText = filteredText.take(2)
                        if (filteredText.length == 2 && filteredText.toIntOrNull() in 1..12) {
                            minuteFocusRequester.requestFocus()
                        }
                    },
                    modifier = Modifier
                        .size(width = 96.dp, height = 80.dp)
                        .focusRequester(hourFocusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                val hour = hourText.toIntOrNull()
                                val validatedHour = when {
                                    hour in 1..12 -> hour!!
                                    else -> 12
                                }
                                hourText = validatedHour.toString()
                                onHourChange(validatedHour)
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = LimoBlack,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LimoWhite,
                        unfocusedContainerColor = LimoWhite,
                        focusedBorderColor = LimoOrange,
                        unfocusedBorderColor = LimoBlack.copy(alpha = 0.2f),
                        focusedTextColor = LimoBlack,
                        unfocusedTextColor = LimoBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    "Hour",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LimoBlack.copy(alpha = 0.6f)
                    )
                )
            }

            Text(
                ":",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = LimoBlack
                )
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { text ->
                        minuteText = text.filter { it.isDigit() }.take(2)
                    },
                    modifier = Modifier
                        .size(width = 96.dp, height = 80.dp)
                        .focusRequester(minuteFocusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                val minute = minuteText.toIntOrNull()
                                val validatedMinute = when {
                                    minute in 0..59 -> minute!!
                                    else -> 0
                                }
                                minuteText = String.format("%02d", validatedMinute)
                                onMinuteChange(validatedMinute)
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = LimoBlack,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("00", color = LimoBlack.copy(alpha = 0.3f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LimoBlack.copy(alpha = 0.05f),
                        unfocusedContainerColor = LimoBlack.copy(alpha = 0.05f),
                        focusedBorderColor = LimoOrange,
                        unfocusedBorderColor = LimoBlack.copy(alpha = 0.2f),
                        focusedTextColor = LimoBlack,
                        unfocusedTextColor = LimoBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    "Minute",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LimoBlack.copy(alpha = 0.6f)
                    )
                )
            }

            Column(
                modifier = Modifier
                    .border(1.dp, LimoBlack.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Button(
                    onClick = {
                        isAMState = true
                        onAMPMChange(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAMState) LimoOrange else LimoWhite
                    ),
                    modifier = Modifier
                        .width(52.dp)
                        .height(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        "AM",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAMState) LimoWhite else LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.width(52.dp),
                    color = LimoBlack.copy(alpha = 0.2f)
                )

                Button(
                    onClick = {
                        isAMState = false
                        onAMPMChange(false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isAMState) LimoOrange else LimoWhite
                    ),
                    modifier = Modifier
                        .width(52.dp)
                        .height(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        "PM",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isAMState) LimoWhite else LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }

        if (showError) {
            Text(
                "Please enter a valid time",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimoOrange
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Clock",
                    tint = LimoBlack.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onClose) {
                    Text(
                        "CANCEL",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LimoBlack.copy(alpha = 0.7f)
                        )
                    )
                }

                Button(
                    onClick = {
                        val hour = hourText.toIntOrNull()
                        val minute = minuteText.toIntOrNull()

                        if (hour in 1..12 && minute in 0..59) {
                            showError = false
                            onHourChange(hour!!)
                            onMinuteChange(minute!!)
                            onConfirm()
                        } else {
                            showError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "OK",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LimoWhite
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun updateTimeFromComponents(baseDate: Date, hour: Int, minute: Int, isAM: Boolean): Date {
    val calendar = Calendar.getInstance()
    calendar.time = baseDate

    var hour24 = hour
    if (!isAM && hour != 12) {
        hour24 = hour + 12
    } else if (isAM && hour == 12) {
        hour24 = 0
    }

    calendar.set(Calendar.HOUR_OF_DAY, hour24)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}


