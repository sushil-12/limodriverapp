package com.limo1800driver.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.limo1800driver.app.ui.util.noRippleClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.viewmodel.DashboardAlertType
import com.limo1800driver.app.ui.viewmodel.DashboardStatusAlert

/**
 * Main Entry Point: Dashboard Status Alert Carousel
 */
@Composable
fun DashboardStatusAlertCarousel(
    alerts: List<DashboardStatusAlert>,
    isLoading: Boolean,
    onAlertClick: (DashboardStatusAlert) -> Unit,
    onActionLinkClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // FILTER: Ignore INFO alerts, keep only WARNING and ERROR
    val visibleAlerts = remember(alerts) {
        alerts.filter { it.type != DashboardAlertType.INFO }
    }

    if (isLoading) {
        DashboardStatusAlertLoadingView(modifier = modifier)
        return
    }

    if (visibleAlerts.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { visibleAlerts.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 16.dp
        ) { page ->
            val alert = visibleAlerts[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                DashboardStatusAlertView(
                    alert = alert,
                    onTap = { onAlertClick(alert) },
                    onActionLinkClick = onActionLinkClick,
                    hasMultipleAlerts = visibleAlerts.size > 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Page indicators (dots) - Only show if more than 1 alert
        if (visibleAlerts.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleAlerts.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .background(
                                color = if (isSelected) Color.Black else Color.LightGray,
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
 */
@Composable
fun DashboardStatusAlertView(
    alert: DashboardStatusAlert,
    onTap: (() -> Unit)? = null,
    onActionLinkClick: ((String) -> Unit)? = null,
    hasMultipleAlerts: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = alert.type.backgroundColor
    val iconColor = alert.type.iconColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .then(if (onTap != null && alert.actionLinks.isEmpty()) Modifier.noRippleClickable { onTap() } else Modifier)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp)
                    .background(iconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val iconVector = when (alert.type) {
                    DashboardAlertType.ERROR -> Icons.Default.Info
                    DashboardAlertType.WARNING -> Icons.Default.Warning
                    else -> Icons.Default.Info
                }

                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1F1F1F)
                    ),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF484848),
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Action Links
                if (alert.actionLinks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        alert.actionLinks.forEach { actionLink ->
                            Text(
                                text = actionLink.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline
                                ),
                                modifier = Modifier
                                    .noRippleClickable {
                                        onActionLinkClick?.invoke(actionLink.route)
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Chevron (show if multiple alerts to scroll, or if no action links, or if there's a general onTap)
            if (hasMultipleAlerts || alert.actionLinks.isEmpty() || onTap != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

/**
 * Premium Animated Shimmer Loading View
 */
@Composable
fun DashboardStatusAlertLoadingView(
    modifier: Modifier = Modifier
) {
    // Shimmer Animation Setup
    val shimmerColors = listOf(
        Color(0xFFFDE8E8), // Light Pink Base
        Color(0xFFFEE2E2), // Slightly lighter
        Color(0xFFFDE8E8), // Base again
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    // Layout
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF0F0)) // Static background
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Circle Shimmer
        Spacer(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(brush) // Animated
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Text Lines Shimmer
        Column(modifier = Modifier.weight(1f)) {
            // Title Line
            Spacer(
                modifier = Modifier
                    .width(160.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush) // Animated
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            // Body Line 1
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush) // Animated
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            // Body Line 2
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush) // Animated
            )
        }
    }
}

// --- Colors & Styling Extensions ---

private val DashboardAlertType.backgroundColor: Color
    get() = when (this) {
        DashboardAlertType.ERROR -> Color(0xFFFFF0F0)
        DashboardAlertType.WARNING -> Color(0xFFFFF8E1)
        DashboardAlertType.INFO -> Color(0xFFF5F5F5)
    }

private val DashboardAlertType.iconColor: Color
    get() = when (this) {
        DashboardAlertType.ERROR -> Color(0xFFD32F2F)
        DashboardAlertType.WARNING -> Color(0xFFFFA000)
        DashboardAlertType.INFO -> Color(0xFF757575)
    }