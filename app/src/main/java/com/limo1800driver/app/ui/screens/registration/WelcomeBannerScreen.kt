package com.limo1800driver.app.ui.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.R
import com.limo1800driver.app.ui.theme.LimoWhite

// --- Refined Theme Colors ---
private val LimoBlack = Color(0xFF121212)
private val LimoGold = Color(0xFFF28B2F) // Using your Orange/Gold brand color
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)
private val BackgroundColor = Color(0xFFFAFAFA)
private val SurfaceColor = Color.White

@Composable
fun WelcomeBannerScreen(
    onContinue: () -> Unit = {},
    onClose: () -> Unit = {},
    onTutorialsClick: () -> Unit = {}
) {
    Scaffold(
        containerColor = LimoWhite,
        topBar = {
            // Minimalist Top Bar: Centered Title with Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = LimoBlack
                    )
                }

                Text(
                    text = "Welcome, Partner",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Empty Spacer to balance the layout so text remains perfectly centered
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
        // Removed bottomBar entirely
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Hero Section ---
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                painter = painterResource(R.drawable.logo_banner),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to the 1800LIMO Network",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // --- Features ---
            FeatureSection()

            Spacer(modifier = Modifier.height(32.dp))

            // --- Get Started / Tutorials ---
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TutorialCard(onClick = onTutorialsClick)

            Spacer(modifier = Modifier.height(32.dp))

            // --- Primary Action Button (Moved Inline) ---
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimoBlack,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Go to Dashboard",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom padding to ensure button isn't stuck to the screen edge and gesture navigation
            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun FeatureSection() {
    Column {
        FeatureRow(
            title = "Real-time Alerts",
            subtitle = "Instant SMS and email notifications for new bookings."
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color(0xFFE0E0E0),
            thickness = 0.5.dp
        )
        FeatureRow(
            title = "Smart Rates",
            subtitle = "Customize taxes and tolls for all-inclusive pricing."
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color(0xFFE0E0E0),
            thickness = 0.5.dp
        )
        FeatureRow(
            title = "Flexible Management",
            subtitle = "Accept or reject bookings via app or SMS."
        )
    }
}

@Composable
private fun FeatureRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = LimoGold,
            modifier = Modifier
                .size(22.dp)
                .padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            )
        }
    }
}

@Composable
private fun TutorialCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(LimoGold.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = LimoGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "See How It Works",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Watch a quick tutorial",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- Preview ---

@Preview(
    showBackground = true,
    device = Devices.PIXEL_4_XL,
    showSystemUi = true,
    name = "Elegant Welcome Banner Inline"
)
@Composable
fun WelcomeBannerPreview() {
    MaterialTheme {
        WelcomeBannerScreen()
    }
}