package com.limo1800driver.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun DashboardSegmentedControl(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val items = DashboardTab.values().toList()
    val shape = RoundedCornerShape(12.dp)
    
    // Background Color (Light Gray for the track)
    val trackColor = Color(0xFFF2F2F7) 
    
    BoxWithConstraints(
        modifier = modifier
            .height(52.dp) // Standard easy-to-tap height
            .background(trackColor, shape)
            .padding(4.dp) // Padding for the floating effect
    ) {
        val maxWidth = maxWidth
        val tabWidth = maxWidth / items.size

        // The Sliding Indicator
        // We calculate the offset based on the selected index
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * items.indexOf(selectedTab),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "indicatorOffset"
        )

        // 1. The Moving White Card (Indicator)
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 2.dp, 
                    shape = shape, 
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .background(Color.White, shape)
                .zIndex(0f) // Behind the text
        )

        // 2. The Text Labels (Overlay)
        Row(modifier = Modifier.fillMaxSize().zIndex(1f)) {
            items.forEach { tab ->
                val isSelected = selectedTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                val title: String
                val icon: ImageVector
                when (tab) {
                    DashboardTab.DRIVE -> {
                        title = "Drive"
                        icon = Icons.Default.DirectionsCar
                    }
                    DashboardTab.EARNINGS -> {
                        title = "Earnings"
                        icon = Icons.Default.MonetizationOn
                    }
                }

                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null // Disable ripple for clean custom feel
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Optional: Show Icon alongside text? 
                        // Often text-only is cleaner for tabs, but here is support for both.
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}