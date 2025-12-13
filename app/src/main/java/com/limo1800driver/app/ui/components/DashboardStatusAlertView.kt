package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limo1800driver.app.ui.viewmodel.DashboardAlertType
import com.limo1800driver.app.ui.viewmodel.DashboardStatusAlert

/**
 * Dashboard Status Alert Carousel
 * Displays alerts in a horizontal pager with indicators
 */
@Composable
fun DashboardStatusAlertCarousel(
    alerts: List<DashboardStatusAlert>,
    isLoading: Boolean,
    onAlertClick: (DashboardStatusAlert) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        DashboardStatusAlertLoadingView(modifier = modifier)
    } else if (alerts.isNotEmpty()) {
        Column(modifier = modifier) {
            val pagerState = rememberPagerState(pageCount = { alerts.size })
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) { page ->
                if (page < alerts.size) {
                    DashboardStatusAlertView(
                        alert = alerts[page],
                        onTap = { onAlertClick(alerts[page]) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }
            }
            
            // Page Indicators
            if (alerts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    alerts.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .background(
                                    color = if (pagerState.currentPage == index) Color.Black else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 4.dp)
                        )
                    }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onTap != null) {
                    Modifier.clickable { onTap() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = alert.type.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = alert.type.iconColor,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = alert.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.message,
                        fontSize = 15.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dashboard Status Alert Loading View
 */
@Composable
fun DashboardStatusAlertLoadingView(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE) // Light red background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.Red,
                strokeWidth = 2.dp
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(18.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
                
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(14.dp)
                        .background(Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Extension properties for DashboardAlertType
 */
private val DashboardAlertType.backgroundColor: Color
    get() = when (this) {
        DashboardAlertType.ERROR -> Color(0xFFFFEBEE) // Light red
        DashboardAlertType.WARNING -> Color(0xFFFFF3E0) // Light orange
        DashboardAlertType.INFO -> Color(0xFFE3F2FD) // Light blue
    }

private val DashboardAlertType.iconColor: Color
    get() = when (this) {
        DashboardAlertType.ERROR -> Color.Red
        DashboardAlertType.WARNING -> Color(0xFFFF9800) // Orange
        DashboardAlertType.INFO -> Color.Blue
    }

