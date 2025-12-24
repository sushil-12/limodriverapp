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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
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
import kotlinx.coroutines.launch

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

    // Focus Requesters for the 6 boxes
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // State for the 6 digits
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogTitle by remember { mutableStateOf("") }
    var errorDialogMessage by remember { mutableStateOf("") }

    LaunchedEffect(tempUserId, phoneNumber) {
        // Clear any previous state when navigating to this screen
        otpValues.replaceAll { "" }
        viewModel.setInitialData(tempUserId, phoneNumber)

        // Request focus on the first box after a short delay to ensure the UI is ready
        scope.launch {
            delay(100) // Small delay to ensure UI is composed
            try {
                focusRequesters[0].requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failures
            }
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.nextAction != null) {
            onNext(uiState.nextAction!!)
        }
    }

    // Show error dialog when there's an API error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            errorDialogTitle = "Error"
            errorDialogMessage = error
            showErrorDialog = true
        }
    }

    // --- Logic Helpers ---

    fun submitOtp() {
        if (otpValues.all { it.isNotEmpty() }) {
            focusManager.clearFocus()
            keyboardController?.hide()
            viewModel.onEvent(OtpUiEvent.OtpChanged(otpValues.joinToString("")))
            viewModel.onEvent(OtpUiEvent.VerifyOtp)
        }
    }

    fun handleInput(index: Int, newValue: String) {
        // Handle Paste (length == 6)
        if (newValue.length == 6 && newValue.all { it.isDigit() }) {
            newValue.forEachIndexed { i, char ->
                if (i < 6) otpValues[i] = char.toString()
            }
            submitOtp()
            return
        }

        // Handle Normal Input
        if (newValue.length <= 1) {
            if (newValue.all { it.isDigit() }) {
                otpValues[index] = newValue
                if (newValue.isNotEmpty()) {
                    // Auto-advance
                    if (index < 5) {
                        focusRequesters[index + 1].requestFocus()
                    } else {
                        // Last digit entered
                        submitOtp()
                    }
                }
            }
        }
    }

    fun handleBackspace(index: Int) {
        if (otpValues[index].isEmpty() && index > 0) {
            focusRequesters[index - 1].requestFocus()
        } else {
            otpValues[index] = ""
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
                // Clear OTP state before navigating back
                otpValues.replaceAll { "" }
                onBack?.invoke()
            }
        )

        Spacer(Modifier.height(40.dp))

        // --- Optimized OTP Input Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { index ->
                OtpDigitInput(
                    value = otpValues[index],
                    focusRequester = focusRequesters[index],
                    onValueChange = { handleInput(index, it) },
                    onBackspace = { handleBackspace(index) }
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

        // --- Error Messages ---
        uiState.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = LimoRed, style = TextStyle(fontSize = 14.sp))
        }

        uiState.message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = LimoRed, style = TextStyle(fontSize = 14.sp))
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

    // Error Alert Dialog
    ErrorAlertDialog(
        isVisible = showErrorDialog,
        onDismiss = {
            showErrorDialog = false
            // Clear the error from viewModel when dialog is dismissed
            // Note: This is handled by the LaunchedEffect clearing the error
        },
        title = errorDialogTitle,
        message = errorDialogMessage
    )

    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Cancel any ongoing operations in the viewModel
            // The viewModel's onCleared will handle coroutine cleanup
        }
    }
}

// --- Isolated Component for Performance & Event Handling ---
@Composable
private fun OtpDigitInput(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isFocused -> Color.Black
        value.isNotEmpty() -> Color.Black // Optional: Keep border black if filled
        else -> Color(0xFFE5E7EB)
    }

    val containerColor = if (isFocused) Color.Transparent else Color(0xFFF9FAFB)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .size(width = 46.dp, height = 46.dp) // Adjusted to 56dp for better touch target
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                // CRITICAL: Handle backspace on KeyDown to prevent "stuck" state
                if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace) {
                    onBackspace()
                    true // Consume the event so the text field doesn't get confused
                } else {
                    false
                }
            },
        textStyle = TextStyle(
            fontSize = 19.sp, // Slightly larger for better readability
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            fontFamily = GoogleSansFamily
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(Color.Black), // Custom clean cursor
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor, RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}
