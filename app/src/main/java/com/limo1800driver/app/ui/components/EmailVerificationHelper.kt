package com.limo1800driver.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.viewmodel.EmailVerificationViewModel
import com.limo1800driver.app.ui.viewmodel.EmailVerificationState
import kotlinx.coroutines.launch

/**
 * Email Verification Helper Component
 * Shows verification status and resend options below email fields
 */
@Composable
fun EmailVerificationHelper(
    email: String,
    emailType: EmailType,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val emailVerificationState by viewModel.emailVerificationState.collectAsState()
    val scope = rememberCoroutineScope()
    var isResending by remember { mutableStateOf(false) }
    var resendMessage by remember { mutableStateOf<String?>(null) }
    var resendError by remember { mutableStateOf<String?>(null) }

    // Clear messages when email changes
    LaunchedEffect(email) {
        resendMessage = null
        resendError = null
    }

    // Determine if verification is required for this specific email
    val needsVerification = when (emailVerificationState) {
        is EmailVerificationState.Success -> {
            val data = (emailVerificationState as EmailVerificationState.Success).data
            when (emailType) {
                EmailType.MAIN -> data.mainEmail.verificationRequired && !data.mainEmail.isVerified && data.mainEmail.email == email
                EmailType.DISPATCH -> data.dispatchEmail.verificationRequired && !data.dispatchEmail.isVerified && data.dispatchEmail.email == email
            }
        }
        else -> false
    }

    if (!needsVerification) return

    Column(modifier = modifier.fillMaxWidth()) {
        // "Verification required" text
        Text(
            text = "Verification required",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFD32F2F), // Red color
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Resend code button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Didn't receive the code?",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF666666),
                    fontSize = 12.sp
                )
            )

            Text(
                text = if (isResending) "Sending..." else "Resend code",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF1976D2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable(enabled = !isResending) {
                    scope.launch {
                        isResending = true
                        resendMessage = null
                        resendError = null

                        val emailTypeString = when (emailType) {
                            EmailType.MAIN -> "primary"
                            EmailType.DISPATCH -> "dispatch"
                        }

                        viewModel.resendVerificationEmail(emailTypeString) { result ->
                            result.onSuccess { response ->
                                resendMessage = response.message
                                if (response.autoVerified == true) {
                                    resendMessage = "Email auto-verified successfully!"
                                }
                            }.onFailure { error ->
                                resendError = error.message ?: "Failed to resend verification email"
                            }
                            isResending = false
                        }
                    }
                }
            )
        }

        // Success/Error messages from resend operation
        resendMessage?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF2E7D32), // Green color for success
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        resendError?.let { error ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFD32F2F), // Red color for error
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Info note
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "You can find the email from info@1800limo.com. Please check your spam folder too with subject \"Verify your Email\"",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF666666),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        )
    }
}

enum class EmailType {
    MAIN,
    DISPATCH
}
