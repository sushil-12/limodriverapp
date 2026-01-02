package com.limo1800driver.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

    // --- STYLING CONSTANTS (Matched to Image) ---
    val activeColor = Color(0xFF2B1A16) // The Dark Brown/Black from the image
    val containerColor = Color.White
    val activeTextColor = Color.White
    val inactiveTextColor = Color(0xFF2B1A16) // Match the dark text to the brand color
    val borderColor = Color(0xFFEEEEEE) // Subtle border for definition

    BoxWithConstraints(
        modifier = modifier
            .height(54.dp) // Taller height to match the pill shape in image
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                spotColor = Color(0x20000000) // Soft shadow
            )
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape)
            .padding(4.dp) // Padding between container edge and the moving indicator
    ) {
        val maxWidth = maxWidth
        val tabWidth = maxWidth / items.size

        // --- 1. The Sliding Dark Indicator ---
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * items.indexOf(selectedTab),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "indicatorOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(activeColor)
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
                        // Uppercase to match "PICKUP / DELIVERY" style
                        text = (if (tab == DashboardTab.DRIVE) "Drive" else "Earnings").uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        letterSpacing = 1.sp // Wider spacing for that premium look
                    )
                }
            }
        }
    }
}