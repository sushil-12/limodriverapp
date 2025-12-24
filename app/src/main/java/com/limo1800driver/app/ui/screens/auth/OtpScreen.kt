package com.limo1800driver.app.ui.screens.auth

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
import com.limo1800driver.app.ui.components.ErrorAlertDialog
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.state.OtpUiEvent
import com.limo1800driver.app.ui.theme.GoogleSansFamily
import com.limo1800driver.app.ui.theme.LimoRed
import com.limo1800driver.app.ui.viewmodel.OtpViewModel
import kotlinx.coroutines.delay

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

    // Single source of truth for the OTP
    var otpValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogTitle by remember { mutableStateOf("") }
    var errorDialogMessage by remember { mutableStateOf("") }

    LaunchedEffect(tempUserId, phoneNumber) {
        viewModel.setInitialData(tempUserId, phoneNumber)
        // Request focus immediately on load
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.nextAction != null) {
            onNext(uiState.nextAction!!)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            errorDialogTitle = "Error"
            errorDialogMessage = error
            showErrorDialog = true
            // Clear OTP on error so user can retry easily
            otpValue = ""
            focusRequester.requestFocus()
        }
    }

    // Auto-submit when length reaches 6
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6) {
            focusManager.clearFocus()
            keyboardController?.hide()
            viewModel.onEvent(OtpUiEvent.OtpChanged(otpValue))
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
            modifier = Modifier.clickable {
                otpValue = ""
                onBack?.invoke()
            }
        )

        Spacer(Modifier.height(40.dp))

        // --- OPTIMIZED OTP INPUT ---
        // This box holds the invisible TextField and the visible boxes
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // 1. The Invisible TextField (Handles all input logic)
            BasicTextField(
                value = otpValue,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        otpValue = it
                    }
                },
                modifier = Modifier
                    .matchParentSize() // Fill the box so clicks anywhere trigger keyboard
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush = SolidColor(Color.Transparent), // Hide default cursor
                textStyle = TextStyle(color = Color.Transparent), // Hide typed text
                decorationBox = { innerTextField -> innerTextField() }
            )

            // 2. The Visual Boxes (Drawn based on otpValue state)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val char = if (index < otpValue.length) otpValue[index].toString() else ""
                    val isFocused = index == otpValue.length

                    OtpDigitVisual(
                        char = char,
                        isFocused = isFocused
                    )
                }
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
            shape = RoundedCornerShape(12.dp),
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

        // --- Loading ---
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ShimmerCircle(
                    size = 32.dp,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }

    ErrorAlertDialog(
        isVisible = showErrorDialog,
        onDismiss = { showErrorDialog = false },
        title = errorDialogTitle,
        message = errorDialogMessage
    )
}

/**
 * Purely visual component for a single OTP box.
 * It does NOT handle input events.
 */
@Composable
private fun OtpDigitVisual(
    char: String,
    isFocused: Boolean
) {
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

        // Optional: Blinking cursor for the active empty box
        if (isFocused && char.isEmpty()) {
            // We can add a simple blinking cursor here if desired
            // For now, the focused border is usually sufficient
        }
    }
}