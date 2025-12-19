package com.limo1800driver.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.state.OtpUiEvent
import com.limo1800driver.app.ui.theme.GoogleSansFamily
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.viewmodel.OtpViewModel

@Composable
fun OtpScreen(
    tempUserId: String,
    phoneNumber: String,
    onNext: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: OtpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Store digits locally for immediate UI updates
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }

    LaunchedEffect(tempUserId, phoneNumber) {
        viewModel.setInitialData(tempUserId, phoneNumber)
        // Delay focus slightly to ensure the keyboard pops up smoothly
        focusRequesters[0].requestFocus()
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.nextAction != null) {
            onNext(uiState.nextAction!!)
        }
    }

    fun moveFocus(currentIndex: Int, forward: Boolean) {
        if (forward) {
            if (currentIndex < 5) {
                focusRequesters[currentIndex + 1].requestFocus()
            } else {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        } else {
            if (currentIndex > 0) {
                focusRequesters[currentIndex - 1].requestFocus()
            }
        }
    }

    fun checkAndSubmit() {
        if (otpValues.all { it.isNotEmpty() }) {
            focusManager.clearFocus()
            keyboardController?.hide()
            viewModel.onEvent(OtpUiEvent.OtpChanged(otpValues.joinToString("")))
            viewModel.onEvent(OtpUiEvent.VerifyOtp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(50.dp))

        // --- Header ---
        Text(
            text = buildAnnotatedString {
                append("Enter the 6-digit code sent via SMS at ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(phoneNumber)
                }
                append(".")
            },
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                color = Color.Black,
                lineHeight = 32.sp
            )
        )

        Spacer(Modifier.height(12.dp))

        // --- Change Number Link ---
        Text(
            text = "Change your mobile number?",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color.Black.copy(alpha = 0.6f),
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable { onBack?.invoke() }
        )

        Spacer(Modifier.height(40.dp))

        // --- OTP Input Boxes ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(6) { index ->
                OutlinedTextField(
                    value = otpValues[index],
                    onValueChange = { newValue ->
                        if (newValue.length <= 1) {
                            if (newValue.all { it.isDigit() }) {
                                otpValues[index] = newValue
                                if (newValue.isNotEmpty()) {
                                    moveFocus(index, forward = true)
                                    checkAndSubmit()
                                }
                            }
                        } else if (newValue.length == 6 && index == 0) {
                            newValue.forEachIndexed { i, char ->
                                if (i < 6 && char.isDigit()) otpValues[i] = char.toString()
                            }
                            checkAndSubmit()
                        }
                    },
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp) // Increased height to match screenshot
                        .focusRequester(focusRequesters[index])
                        .onKeyEvent { event ->
                            if (event.key == Key.Backspace && otpValues[index].isEmpty()) {
                                moveFocus(index, forward = false)
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- Resend Code Pill ---
        val timerString = if (uiState.resendCooldown > 0) {
            "(0:${String.format("%02d", uiState.resendCooldown)})"
        } else {
            ""
        }

        Surface(
            onClick = {
                if (uiState.canResend) viewModel.onEvent(OtpUiEvent.ResendOtp)
            },
            shape = RoundedCornerShape(12.dp), // Squared-off pill shape
            color = Color(0xFFF3F4F6),
            enabled = uiState.canResend || uiState.resendCooldown > 0
        ) {
            Text(
                text = "Resend code via SMS $timerString",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = TextStyle(
                    fontSize = 16.sp,
                    color = if (uiState.canResend) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansFamily
                )
            )
        }

        // --- Error Messages ---
        uiState.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = TextStyle(fontSize = 14.sp))
        }

        uiState.message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = LimoGreen, style = TextStyle(fontSize = 14.sp))
        }

        // --- Loading ---
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                strokeWidth = 2.dp
            )
        }
    }
}


@Preview(
    name = "OTP Screen",
    showBackground = true,
)
@Composable
fun OtpScreenPreview() {
    MaterialTheme {
        OtpScreen(
            tempUserId = "temp-user-123",
            phoneNumber = "+91 98765 43210",
            onNext = {},
            onBack = {}
        )
    }
}