package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.theme.LimoRed

// Ensure standard colors are available for Info if not in your theme
val LimoBlue = Color(0xFF2196F3)

enum class AlertType { SUCCESS, ERROR, WARNING, INFO }

@Composable
fun CommonErrorAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    type: AlertType,
    title: String,
    message: String,
    confirmText: String = "OK",
    onConfirm: () -> Unit = onDismiss,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null
) {
    if (!isVisible) return

    val mainColor = when (type) {
        AlertType.SUCCESS -> LimoGreen
        AlertType.ERROR -> LimoRed
        AlertType.WARNING -> LimoOrange
        AlertType.INFO -> LimoBlue
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false // Allows custom width constraints
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f) // Compact horizontal margin
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp), // Modern M3 curvature
            color = MaterialTheme.colorScheme.surfaceContainerHigh, // Adapts to Light/Dark mode
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Icon Header
                Box(
                    modifier = Modifier
                        .size(64.dp) // Compact size
                        .background(
                            color = mainColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForType(type),
                        contentDescription = null,
                        tint = mainColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Text Content
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // M3 standard for secondary text
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Secondary Button (Dismiss)
                    if (dismissText != null) {
                        TextButton(
                            onClick = { onDismissClick?.invoke() ?: onDismiss() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = dismissText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Primary Button (Confirm)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = mainColor,
                            contentColor = Color.White // Ensure contrast on brand colors
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getIconForType(type: AlertType): ImageVector {
    // Used Rounded icons for a softer, more modern look
    return when (type) {
        AlertType.SUCCESS -> Icons.Rounded.CheckCircle
        AlertType.ERROR -> Icons.Rounded.Error
        AlertType.WARNING -> Icons.Rounded.Warning
        AlertType.INFO -> Icons.Rounded.Info
    }
}

// --- Convenience Wrappers ---

@Composable
fun SuccessAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String = "Success",
    message: String,
    confirmText: String = "OK"
) {
    CommonErrorAlertDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        type = AlertType.SUCCESS,
        title = title,
        message = message,
        confirmText = confirmText
    )
}

@Composable
fun ErrorAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String = "Error",
    message: String,
    confirmText: String = "Retry"
) {
    CommonErrorAlertDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        type = AlertType.ERROR,
        title = title,
        message = message,
        confirmText = confirmText
    )
}

@Composable
fun WarningAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String = "Warning",
    message: String,
    confirmText: String = "Proceed",
    dismissText: String? = "Cancel",
    onDismissClick: (() -> Unit)? = null
) {
    CommonErrorAlertDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        type = AlertType.WARNING,
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onDismissClick = onDismissClick
    )
}

// --- Preview for Development ---
@Preview
@Composable
fun PreviewDialogs() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            CommonErrorAlertDialog(
                isVisible = true,
                onDismiss = {},
                type = AlertType.WARNING,
                title = "Location Required",
                message = "To receive ride requests, please enable location services in your settings.",
                dismissText = "Not Now",
                confirmText = "Enable"
            )
        }
    }
}