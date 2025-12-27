package com.limo1800driver.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.limo1800driver.app.ui.viewmodel.EmailVerificationState
import com.limo1800driver.app.ui.viewmodel.EmailVerificationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationAlert(
    modifier: Modifier = Modifier,
    onVerifyEmailClick: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val currentRealState by viewModel.emailVerificationState.collectAsState()

    // UI State: default to expanded
    var isExpanded by remember { mutableStateOf(true) }

    // Data State: Retain last known data
    var displayState by remember { mutableStateOf<EmailVerificationState?>(null) }

    LaunchedEffect(currentRealState) {
        if (currentRealState !is EmailVerificationState.Loading) {
            // Force expand if a new Error occurs
            if (currentRealState is EmailVerificationState.Error && displayState !is EmailVerificationState.Error) {
                isExpanded = true
            }
            displayState = currentRealState
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            viewModel.refreshEmailVerificationStatus()
        }
    }

    val shouldShow = when (val state = displayState) {
        is EmailVerificationState.Success -> viewModel.needsEmailVerification()
        is EmailVerificationState.Error -> true
        else -> false
    }

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300)))
                    .using(SizeTransform(clip = false))
            },
            label = "AlertMorph"
        ) { expanded ->
            if (expanded) {
                // FIX: State is defined INSIDE this block.
                // Every time 'expanded' becomes true, a fresh state is created (resetting the swipe).
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            isExpanded = false
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { Box(Modifier.fillMaxSize()) }, // Invisible background
                    content = {
                        // The Full Card Content
                        when (val state = displayState) {
                            is EmailVerificationState.Success -> {
                                val unverifiedEmails = viewModel.getUnverifiedEmails()
                                val (title, message) = when {
                                    unverifiedEmails.size > 1 -> Pair("Verify ${unverifiedEmails.size} Emails", unverifiedEmails.joinToString(", "))
                                    unverifiedEmails.isNotEmpty() -> Pair("Verify Email", unverifiedEmails.first())
                                    else -> Pair("Action Required", "Please verify your email address")
                                }

                                FullAlertCard(
                                    title = title,
                                    message = message,
                                    icon = Icons.Outlined.MarkEmailUnread,
                                    containerColor = Color(0xFFF5F5DC),
                                    contentColor = Color(0xFF4E342E),
                                    actionLabel = "Verify",
                                    onActionClick = onVerifyEmailClick
                                )
                            }
                            is EmailVerificationState.Error -> {
                                FullAlertCard(
                                    title = "Sync Error",
                                    message = "Tap to retry connection",
                                    icon = Icons.Outlined.Warning,
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    actionLabel = "Retry",
                                    onActionClick = { viewModel.refreshEmailVerificationStatus() }
                                )
                            }
                            else -> {}
                        }
                    }
                )
            } else {
                // --- MINIMIZED PILL (Aligned to Right) ---
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd // FIX: Align Right
                ) {
                    MinimizedAlertPill(
                        onClick = { isExpanded = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun FullAlertCard(
    title: String,
    message: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.9f)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onActionClick,
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun MinimizedAlertPill(
    onClick: () -> Unit
) {
    // FIX: Always Red (Error Container colors)
    val containerColor = MaterialTheme.colorScheme.errorContainer
    val contentColor = MaterialTheme.colorScheme.onErrorContainer

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 4.dp,
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Attention Needed",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}