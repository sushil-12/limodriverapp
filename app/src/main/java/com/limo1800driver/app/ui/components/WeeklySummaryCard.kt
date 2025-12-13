package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Weekly Summary Card Component
 * Shows date range, earnings, online time, and rides with week navigation
 */
@Composable
fun WeeklySummaryCard(
    dateRange: String,
    earnings: String,
    onlineTime: String,
    rides: String,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Gray.copy(alpha = 0.18f)
        )
    ) {
        Column {
            // Date Range Header with Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousWeek,
                    enabled = canGoPrevious,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Week",
                        tint = if (canGoPrevious) Color.Black else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = dateRange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                IconButton(
                    onClick = onNextWeek,
                    enabled = canGoNext,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Week",
                        tint = if (canGoNext) Color.Black else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Divider
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )
            
            // Weekly Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Earnings
                SummaryItem(
                    title = "EARNINGS",
                    value = earnings,
                    color = Color(0xFFFF9800), // Orange
                    isEarnings = true,
                    currencySymbol = currencySymbol
                )
                
                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )
                
                // Online Time
                SummaryItem(
                    title = "ONLINE",
                    value = onlineTime,
                    color = Color.Black,
                    isEarnings = false
                )
                
                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )
                
                // Rides
                SummaryItem(
                    title = "RIDES",
                    value = rides,
                    color = Color.Black,
                    isEarnings = false
                )
            }
        }
    }
}

/**
 * Summary Item Component
 */
@Composable
private fun SummaryItem(
    title: String,
    value: String,
    color: Color,
    isEarnings: Boolean,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isEarnings) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

