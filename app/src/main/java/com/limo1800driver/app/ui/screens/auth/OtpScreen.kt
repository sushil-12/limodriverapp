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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.state.OtpUiEvent
import com.limo1800driver.app.ui.theme.AppColors
import com.limo1800driver.app.ui.theme.GoogleSansFamily
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

    // Function to check completeness and trigger verify
    fun checkAndSubmit() {
        if (otpValues.all { it.isNotEmpty() }) {
            // Hide keyboard immediately for better UX
            focusManager.clearFocus()
            keyboardController?.hide()
            
            // Sync full OTP to VM and Trigger Verify
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
        Spacer(Modifier.height(60.dp))

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
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = AppColors.LimoBlack,
                lineHeight = 30.sp
            )
        )

        Spacer(Modifier.height(8.dp))

        // --- Change Number Link ---
        Text(
            text = "Change your mobile number?",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = AppColors.LimoBlack.copy(alpha = 0.7f),
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable { onBack?.invoke() }
        )

        Spacer(Modifier.height(32.dp))

        // --- OTP Input Boxes ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
                                    // Check if this was the last digit needed
                                    checkAndSubmit()
                                }
                            }
                        } else if (newValue.length == 6 && index == 0) {
                            // Paste support
                            newValue.forEachIndexed { i, char ->
                                if (i < 6 && char.isDigit()) otpValues[i] = char.toString()
                            }
                            checkAndSubmit()
                        }
                    },
                    modifier = Modifier
                        .width(48.dp)
                        .height(56.dp)
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
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.LimoBlack
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.LimoBlack,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

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
            shape = RoundedCornerShape(50),
            color = Color(0xFFF3F4F6),
            enabled = uiState.canResend
        ) {
            Text(
                text = "Resend code via SMS $timerString",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = if (uiState.canResend) AppColors.LimoBlack else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansFamily
                )
            )
        }

        // --- Messages ---
        uiState.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = TextStyle(fontSize = 14.sp))
        }
        
        uiState.message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = Color(0xFF4CAF50), style = TextStyle(fontSize = 14.sp))
        }
        
        // --- Loading Indicator ---
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = AppColors.LimoOrange,
                modifier = Modifier.size(32.dp).align(Alignment.CenterHorizontally)
            )
        }
    }
}