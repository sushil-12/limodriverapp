package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.components.BottomActionBar
import com.limo1800driver.app.ui.theme.*
import com.limo1800driver.app.ui.viewmodel.PrivacyTermsViewModel

@Composable
fun PrivacyTermsScreen(
    onNext: (String?) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: PrivacyTermsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Checkbox State
    var agreed by remember { mutableStateOf(false) }

    // Handle success navigation
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onNext(uiState.nextStep)
        }
    }

    // Show error dialog
    var showErrorDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            showErrorDialog = true
        }
    }

    // Reset Logic
    fun onReset() {
        agreed = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // --- 1. Header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp)
        ) {
            Text(
                text = "Accept 1800limo’s Terms & Review Privacy Notice",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.LimoBlack,
                    lineHeight = 34.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. Rich Text Content ---
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
        ) {
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Gray, fontSize = 15.sp)) {
                    append("By selecting “I Agree” below, I have reviewed and agree to the ")
                }

                pushStringAnnotation(tag = "TERMS", annotation = "terms")
                withStyle(
                    style = SpanStyle(
                        color = AppColors.LimoOrange,
                        fontSize = 15.sp,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Terms of Use")
                }
                pop()

                withStyle(style = SpanStyle(color = Color.Gray, fontSize = 15.sp)) {
                    append(" and acknowledge the ")
                }

                pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                withStyle(
                    style = SpanStyle(
                        color = AppColors.LimoOrange,
                        fontSize = 15.sp,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Privacy policy")
                }
                pop()

                withStyle(style = SpanStyle(color = Color.Gray, fontSize = 15.sp)) {
                    append(". I am at least 18 years of age.")
                }
            }

            ClickableText(
                text = annotatedString,
                style = TextStyle(fontFamily = GoogleSansFamily, lineHeight = 22.sp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(
                        tag = "TERMS",
                        start = offset,
                        end = offset
                    )
                        .firstOrNull()?.let {
                            // Handle Terms Link Click
                        }
                    annotatedString.getStringAnnotations(
                        tag = "PRIVACY",
                        start = offset,
                        end = offset
                    )
                        .firstOrNull()?.let {
                            // Handle Privacy Link Click
                        }
                }
            )
        }

        // Push the Checkbox and Buttons to the bottom
        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))

        // --- 3. Agreement Checkbox Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { agreed = !agreed }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "I Agree",
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.LimoBlack
                )
            )

            // Custom Checkbox to match Black Square design
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (agreed) AppColors.LimoBlack else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        BorderStroke(2.dp, if (agreed) AppColors.LimoBlack else Color.LightGray),
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (agreed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- 4. Bottom Action Bar ---
        BottomActionBar(
            showBackButton = true,
            onBack = onBack,
            onReset = { onReset() },
            onNext = {
                if (!agreed) {
                    showErrorDialog = true
                    return@BottomActionBar
                }
                viewModel.completePrivacyTerms(privacyAccepted = true, termsAccepted = true)
            },
            isLoading = uiState.isLoading
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearError()
            },
            containerColor = Color.White,
            title = { Text("Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (uiState.error != null) uiState.error!!
                    else "Please accept the Terms & Privacy Policy to continue."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    viewModel.clearError()
                }) {
                    Text("OK", color = AppColors.LimoOrange)
                }
            }
        )
    }
}