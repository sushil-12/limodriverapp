package com.limo1800driver.app.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.state.OtpUiEvent
import com.limo1800driver.app.ui.theme.GoogleSansFamily
import com.limo1800driver.app.ui.viewmodel.OtpViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RESEND_INTERVAL_SECONDS = 30

@Composable
fun OtpScreen(
    tempUserId: String,
    phoneNumber: String,
    onNext: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: OtpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // OTP - sync with ViewModel state
    var otpValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // 🔹 RESEND TIMER STATE
    var resendCooldown by remember { mutableIntStateOf(RESEND_INTERVAL_SECONDS) }
    var canResend by remember { mutableStateOf(false) }

    // Sync OTP value with ViewModel state (clears when resend succeeds)
    LaunchedEffect(uiState.otp) {
        if (uiState.otp.isEmpty() && otpValue.isNotEmpty()) {
            otpValue = ""
        }
    }

    // 🔹 Start resend timer immediately on screen load
    LaunchedEffect(Unit) {
        resendCooldown = RESEND_INTERVAL_SECONDS
        canResend = false
        while (resendCooldown > 0) {
            delay(1_000)
            resendCooldown--
        }
        canResend = true
    }

    LaunchedEffect(tempUserId, phoneNumber) {
        viewModel.setInitialData(tempUserId, phoneNumber)
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.nextAction != null) {
            Log.d("OtpScreen", "OTP verified successfully, preparing to navigate to: ${uiState.nextAction}")
            // Hide keyboard before navigating - add delay to ensure keyboard fully closes
            focusManager.clearFocus()
            keyboardController?.hide()
            Log.d("OtpScreen", "Keyboard hide requested, waiting 150ms before navigation")
            // Wait for keyboard to fully close before navigating
            delay(150)
            Log.d("OtpScreen", "Navigating to next screen: ${uiState.nextAction}")
            onNext(uiState.nextAction!!)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            otpValue = ""
            focusRequester.requestFocus()
        }
    }

    // Handle OTP changes: clear error and auto-submit
    LaunchedEffect(otpValue) {
        // Clear error when user starts typing
        if (otpValue.isNotEmpty() && uiState.error != null) {
            viewModel.onEvent(OtpUiEvent.ClearError)
        }

        // Auto-submit when OTP is complete
        if (otpValue.length == 6) {
            focusManager.clearFocus()
            keyboardController?.hide()
            viewModel.onEvent(OtpUiEvent.OtpChanged(otpValue))
            viewModel.onEvent(OtpUiEvent.VerifyOtp)
        }
    }

    // Hide keyboard when leaving the screen
    DisposableEffect(Unit) {
        Log.d("OtpScreen", "OtpScreen composable disposed - hiding keyboard")
        onDispose {
            focusManager.clearFocus()
            keyboardController?.hide()
            Log.d("OtpScreen", "Keyboard hidden in onDispose")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(100.dp))

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

        Text(
            text = "Change your mobile number?",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontSize = 16.sp,
                color = Color.Black.copy(alpha = 0.6f),
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable {
                // Hide keyboard before going back
                focusManager.clearFocus()
                keyboardController?.hide()
                otpValue = ""
                onBack?.invoke()
            }
        )

        Spacer(Modifier.height(40.dp))



        // Error message display


        // OTP Input
        Box(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = otpValue,
                onValueChange = {
                    if (it.length <= 6 && it.all(Char::isDigit)) {
                        otpValue = it
                    }
                },
                modifier = Modifier
                    .matchParentSize()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = TextStyle(color = Color.Transparent),
                decorationBox = { it() }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { index ->
                    val char = otpValue.getOrNull(index)?.toString() ?: ""
                    OtpDigitVisual(
                        char = char,
                        isFocused = index == otpValue.length
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        val timerText = if (!canResend) {
            "(0:${String.format("%02d", resendCooldown)})"
        } else ""

        // Success message display (for resend)
        uiState.message?.let { message ->
            Text(
                text = message,
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontSize = 14.sp,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontSize = 14.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        // 🔹 RESEND BUTTON
        Surface(
            onClick = {
                if (canResend && !uiState.isLoading) {
                    // Clear OTP immediately when resend is clicked
                    otpValue = ""
                    viewModel.onEvent(OtpUiEvent.ResendOtp)

                    resendCooldown = RESEND_INTERVAL_SECONDS
                    canResend = false

                    scope.launch {
                        while (resendCooldown > 0) {
                            delay(1_000)
                            resendCooldown--
                        }
                        canResend = true
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF3F4F6),
            enabled = canResend || resendCooldown > 0
        ) {
            Text(
                text = "Resend code via SMS $timerText",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = TextStyle(
                    fontSize = 16.sp,
                    color = if (canResend) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansFamily
                )
            )
        }

        if (uiState.isLoading) {
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ShimmerCircle(size = 32.dp)
            }
        }
    }
}

@Composable
private fun OtpDigitVisual(char: String, isFocused: Boolean) {
    val borderColor = if (isFocused) Color.Black else Color(0xFFE5E7EB)
    val containerColor = if (isFocused) Color.White else Color(0xFFF9FAFB)

    Box(
        modifier = Modifier
            .size(46.dp)
            .background(containerColor, RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                fontFamily = GoogleSansFamily,
                textAlign = TextAlign.Center
            )
        )
    }
}
