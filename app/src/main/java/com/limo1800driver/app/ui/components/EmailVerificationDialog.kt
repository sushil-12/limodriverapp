package com.limo1800driver.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import timber.log.Timber

/**
 * User-friendly email verification dialog that appears when both
 * main email and dispatch email need verification.
 */
@Composable
fun EmailVerificationDialog(
    mainEmail: String?,
    dispatchEmail: String?,
    onDismiss: () -> Unit,
    onChangeMainEmail: () -> Unit,
    onChangeDispatchEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = Color(0xFFFF9800).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = "Email Verification Required",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message
                Text(
                    text = "Both your Main Email and Dispatch Email need to be verified to continue using the app. Please check your email inbox for verification links.",
                    fontSize = 15.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!mainEmail.isNullOrBlank()) {
                        EmailInfoRow(
                            label = "Main Email",
                            email = mainEmail
                        )
                    }
                    if (!dispatchEmail.isNullOrBlank()) {
                        EmailInfoRow(
                            label = "Dispatch Email",
                            email = dispatchEmail
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Verify Email Button (Primary Action)
                    val emailToVerify = mainEmail ?: dispatchEmail
                    if (!emailToVerify.isNullOrBlank()) {
                        Button(
                            onClick = {
                                openGmailForVerification(context, emailToVerify)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify Email",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Change Main Email Button
                    Button(
                        onClick = {
                            onChangeMainEmail()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    ) {
                        Text(
                            text = "Change Main Email",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Change Dispatch Email Button
                    OutlinedButton(
                        onClick = {
                            onChangeDispatchEmail()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1976D2)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 2.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1976D2))
                        )
                    ) {
                        Text(
                            text = "Change Dispatch Email",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailInfoRow(
    label: String,
    email: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )
        Text(
            text = email,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Opens Gmail app with search for verification emails.
 * Falls back to generic email client if Gmail is not available.
 * 
 * Strategy:
 * 1. Try Gmail app with search query (best UX)
 * 2. Try Gmail web with search
 * 3. Open Gmail app directly (user can search manually)
 * 4. Fallback to any email client
 */
private fun openGmailForVerification(context: Context, emailAddress: String) {
    try {
        // Search query for verification emails - common patterns
        val searchQuery = "subject:(verification OR verify) OR from:(1800limo OR noreply)"
        
        // Method 1: Try Gmail app with search deep link
        // Format: googlegmail://co?q=search_query
        val gmailSearchUri = Uri.parse("googlegmail://co?q=${Uri.encode(searchQuery)}")
        val gmailSearchIntent = Intent(Intent.ACTION_VIEW, gmailSearchUri).apply {
            setPackage("com.google.android.gm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(gmailSearchIntent)
            Timber.d("✅ Opened Gmail app with search for verification: $emailAddress")
            return
        } catch (e: Exception) {
            Timber.d("Gmail app with search not available: ${e.message}")
        }
        
        // Method 2: Try Gmail web with search
        val gmailWebUri = Uri.parse("https://mail.google.com/mail/u/0/#search/${Uri.encode(searchQuery)}")
        val webIntent = Intent(Intent.ACTION_VIEW, gmailWebUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(webIntent)
            Timber.d("✅ Opened Gmail web with search")
            return
        } catch (e: Exception) {
            Timber.d("Gmail web not available: ${e.message}")
        }
        
        // Method 3: Open Gmail app directly (user can search manually)
        val gmailAppUri = Uri.parse("googlegmail://")
        val gmailAppIntent = Intent(Intent.ACTION_VIEW, gmailAppUri).apply {
            setPackage("com.google.android.gm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(gmailAppIntent)
            Timber.d("✅ Opened Gmail app (user can search manually)")
            return
        } catch (e: Exception) {
            Timber.d("Gmail app not installed: ${e.message}")
        }
        
        // Method 4: Fallback - Open any email client
        // Try to open Gmail web in browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(browserIntent)
            Timber.d("✅ Opened Gmail in browser")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open any email client or browser")
        }
        
    } catch (e: Exception) {
        Timber.e(e, "Error opening Gmail for verification")
    }
}

