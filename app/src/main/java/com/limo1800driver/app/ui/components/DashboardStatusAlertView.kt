package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.viewmodel.DashboardAlertType
import com.limo1800driver.app.ui.viewmodel.DashboardStatusAlert

/**
 * Main Entry Point: Dashboard Status Alert Carousel
 * Now handles lists by stacking them vertically (standard for critical alerts).
 */
@Composable
fun DashboardStatusAlertCarousel(
    alerts: List<DashboardStatusAlert>,
    isLoading: Boolean,
    onAlertClick: (DashboardStatusAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        DashboardStatusAlertLoadingView(modifier = modifier)
        return
    }

    if (alerts.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { alerts.size })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                // Keep a consistent pager height so cards are uniform and match the screenshot.
                .height(108.dp)
        ) { page ->
            val alert = alerts[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Parent screens often apply horizontal padding; keep this neutral like ScheduledPickupsPager.
                    .padding(horizontal = 0.dp)
            ) {
                DashboardStatusAlertView(
                    alert = alert,
                    onTap = { onAlertClick(alert) },
                    modifier = Modifier
                        .fillMaxWidth()
                        // Small side padding so the card doesn't feel stuck to the edges while swiping.
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .height(92.dp)
                )
            }
        }

        // Page indicators (dots)
        if (alerts.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                alerts.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .background(
                                color = if (isSelected) Color.Black else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Single Dashboard Status Alert View
 * REDESIGNED: Matches the "Red Card" visual style provided.
 */
@Composable
fun DashboardStatusAlertView(
    alert: DashboardStatusAlert,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // We use the extension properties for colors to keep it dynamic (Error/Warning/Info)
    val backgroundColor = alert.type.backgroundColor
    val iconColor = alert.type.iconColor
    // For the icon background, we use a slightly darker shade of the icon color or specific red
    val iconBackgroundColor = if (alert.type == DashboardAlertType.ERROR) Color(0xFFD32F2F) else iconColor

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Icon (Circle with 'i')
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(iconBackgroundColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (alert.type) {
                DashboardAlertType.ERROR -> Icons.Default.Error
                DashboardAlertType.WARNING -> Icons.Default.Warning
                DashboardAlertType.INFO -> Icons.Default.Info
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Text Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF757575), // Grey text for body
                    lineHeight = 20.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Dashboard Status Alert Loading View
 * Updated to match the dimensions of the new design.
 */
@Composable
fun DashboardStatusAlertLoadingView(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFDEAEC)) // Light red placeholder
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color(0xFFD32F2F),
            strokeWidth = 2.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            )
        }
    }
}

/**
 * Extension properties for DashboardAlertType
 * (Kept your existing logic for dynamic colors)
 */
private val DashboardAlertType.backgroundColor: Color
    get() = when (this) {
        DashboardAlertType.ERROR -> Color(0xFFFFF0F0) // Matches the new Red Design
        DashboardAlertType.WARNING -> Color(0xFFFFF8E1) // Light orange
        DashboardAlertType.INFO -> Color(0xFFE3F2FD) // Light blue
    }

private val DashboardAlertType.iconColor: Color
    get() = when (this) {
        DashboardAlertType.ERROR -> Color(0xFFD32F2F) // Dark Red
        DashboardAlertType.WARNING -> Color(0xFFF57C00) // Dark Orange
        DashboardAlertType.INFO -> Color(0xFF1976D2) // Dark Blue
    }