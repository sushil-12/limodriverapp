package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.components.ShimmerCircle

/**
 * Dashboard Summary Item Component
 * Displays earnings or upcoming rides statistics
 */
@Composable
fun DashboardSummaryItem(
    title: String,
    value: String,
    color: Color,
    isEarnings: Boolean,
    currencySymbol: String? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            letterSpacing = 0.5.sp
        )
        
        if (isLoading) {
            ShimmerCircle(
                size = 20.dp,
                strokeWidth = 2.dp
            )
        } else if (isEarnings) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencySymbol ?: "$",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = value.replace("$ ", "").replace("$", ""),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

/**
 * Dashboard Statistics Panel
 * Container for earnings and upcoming rides
 */
@Composable
fun DashboardStatisticsPanel(
    monthlyEarnings: String,
    upcomingRides: String,
    currencySymbol: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.73.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardSummaryItem(
            title = "EARNINGS",
            value = monthlyEarnings,
            color = Color(0xFFFF9800), // Orange
            isEarnings = true,
            currencySymbol = currencySymbol,
            isLoading = isLoading,
            modifier = Modifier.weight(1f)
        )
        
        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
        )
        
        DashboardSummaryItem(
            title = "UPCOMING RIDES",
            value = upcomingRides,
            color = Color.Black,
            isEarnings = false,
            isLoading = isLoading,
            modifier = Modifier.weight(1f)
        )
    }
}

