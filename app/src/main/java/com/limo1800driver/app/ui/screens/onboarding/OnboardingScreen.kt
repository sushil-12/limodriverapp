package com.limo1800driver.app.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.R
import com.limo1800driver.app.ui.theme.AppColors

// Defining theme colors based on the request
object AppColors {
    val LimoBlack = Color(0xFF121212)
    val LimoOrange = Color(0xFFFF914D)
    val White = Color.White
}

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit = {}
) {
    val bulletPoints = listOf(
        "Advertise your local services and manage your business better for FREE.",
        "Receive Prearranged and On-Demand bookings at your all-inclusive rates.",
        "Accept / Reject any booking with your cancellation policy. On-demand cancellations are $5 minimum.",
        "STRIPE direct deposits in 3 business days.",
        "We add 25% to your discounted affiliate rates.",
        "We add 25% to your discounted affiliate rates."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.LimoBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // The collage image should be pre-composed and used here
            Image(
                painter = painterResource(id = R.drawable.onboarding_image),
                contentDescription = "Onboarding Collage",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(300.dp) // Adjusted size to match design
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                bulletPoints.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle, // Used Outlined variant
                            contentDescription = null,
                            tint = AppColors.LimoOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = point,
                            color = AppColors.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Standard height for main buttons
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.LimoOrange),
                shape = RoundedCornerShape(12.dp) // Rounded corners as in design
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continue",
                        color = AppColors.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Continue",
                        tint = AppColors.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Add padding for Android gesture navigation to prevent button underlapping
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen()
}