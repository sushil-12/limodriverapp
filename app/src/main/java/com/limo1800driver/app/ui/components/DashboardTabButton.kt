package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dashboard Tab Button Component
 * Matches iOS DashboardTabButton design
 */
@Composable
fun DashboardTabButton(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(78.925048828125.dp)
            .clickable(enabled = !isActive) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color(0xFFFF9800) else Color(0xFFFF9800).copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) Color.Black else Color.Gray
            )
        }
    }
}

/**
 * Dashboard Tab Section
 * Container for Drive and Earnings tabs
 */
@Composable
fun DashboardTabSection(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DashboardTabButton(
            title = "Drive",
            icon = Icons.Default.DirectionsCar,
            isActive = selectedTab == DashboardTab.DRIVE,
            onClick = { onTabSelected(DashboardTab.DRIVE) },
            modifier = Modifier.weight(1f)
        )
        
        DashboardTabButton(
            title = "Earnings",
            icon = Icons.Default.AccountBalanceWallet,
            isActive = selectedTab == DashboardTab.EARNINGS,
            onClick = { onTabSelected(DashboardTab.EARNINGS) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Dashboard Tab Enum
 */
enum class DashboardTab {
    DRIVE,
    EARNINGS
}

