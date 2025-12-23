package com.limo1800driver.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    // Uber-like Styling: High Contrast
    val containerColor = Color(0xFFF3F3F3) // Very subtle light grey
    val indicatorColor = Color.Black       // Bold Black Indicator
    val activeTextColor = Color.White
    val inactiveTextColor = Color(0xFF5E5E5E) // Dark Grey

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp) // Compact height (was 52dp)
            .clip(CircleShape) // Fully rounded pill shape
            .background(containerColor)
            .padding(4.dp) // Tight padding for the floating effect
    ) {
        val maxWidth = maxWidth
        val tabWidth = maxWidth / items.size

        // --- 1. The Sliding Black Indicator ---
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * items.indexOf(selectedTab),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "indicatorOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(indicatorColor)
                .zIndex(0f)
        )

        // --- 2. The Text Labels ---
        Row(modifier = Modifier.fillMaxSize().zIndex(1f)) {
            items.forEach { tab ->
                val isSelected = selectedTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                // Smooth color transition for text
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) activeTextColor else inactiveTextColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == DashboardTab.DRIVE) "Drive" else "Earnings",
                        fontSize = 14.sp, // Slightly smaller, crisper font
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        letterSpacing = (-0.5).sp // Tighter tracking looks more premium
                    )
                }
            }
        }
    }
}