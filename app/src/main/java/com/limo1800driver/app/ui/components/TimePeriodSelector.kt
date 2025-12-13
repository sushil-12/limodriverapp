package com.limo1800driver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.limo1800driver.app.ui.viewmodel.TimePeriod

/**
 * Time Period Selector Component
 * Reused from user app design
 */
@Composable
fun TimePeriodSelector(
    selectedTimePeriod: TimePeriod,
    onTimePeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TimePeriod.values().forEach { period ->
                val isSelected = selectedTimePeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onTimePeriodChange(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.Gray
                    )
                }
            }
        }
    }
}

