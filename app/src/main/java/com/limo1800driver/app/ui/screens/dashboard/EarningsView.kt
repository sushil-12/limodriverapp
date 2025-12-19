package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.viewmodel.DriverEarningsViewModel

/**
 * Earnings View Screen
 * Matches iOS EarningsView design
 */
@Composable
fun EarningsView(
    viewModel: DriverEarningsViewModel = hiltViewModel(),
    onNavigateToWallet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val earningsState by viewModel.uiState.collectAsStateWithLifecycle()
    val earningsCardBackground = Color(0xFFE7F6EE) // iOS earningsCardBackground color
    val orangeColor = Color(0xFFFF9800) // iOS orange
    
    val recentPayments = viewModel.recentPayments
    val totalEarnings = viewModel.totalEarnings
    val currencySymbol = viewModel.currencySymbol
    val nextPayoutDate = viewModel.nextPayoutDate
    
    LaunchedEffect(Unit) {
        viewModel.fetchEarningsSummary()
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Top padding
        Spacer(modifier = Modifier.height(24.dp))
        
        // Balance Section with earningsCardBackground - matches iOS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(earningsCardBackground)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BALANCE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = 0.5.sp
                )
                
                // Currency symbol and amount
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (earningsState.isLoading || totalEarnings == "0.00") {
                        // Shimmer placeholder
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(28.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(28.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                    } else {
                        Text(
                            text = currencySymbol ?: "$",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = orangeColor
                        )
                        Text(
                            text = totalEarnings,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                
                // Next payout or description
                if (earningsState.isLoading || totalEarnings == "0.00") {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(12.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                } else {
                    Text(
                        text = nextPayoutDate?.let { "Next payout: $it" } 
                            ?: "Total balance paid to your bank",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Recent Payments Section - matches iOS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Recent Payments",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Recent Payments List
            if (earningsState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ShimmerCircle(
                            size = 24.dp,
                        )
                        Text(
                            text = "Loading recent payments...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            } else if (recentPayments.isEmpty()) {
                // Empty state - matches iOS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "No recent payments",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = "You don't have any recent payments to display",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Recent Payments List - matches iOS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    recentPayments.take(5).forEachIndexed { index, payment ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Black
                                )
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = payment.description ?: "",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = viewModel.formatDate(payment.createdDate),
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = currencySymbol ?: "$",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = orangeColor
                                        )
                                        Text(
                                            text = payment.amount ?: "0.00",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black
                                        )
                                    }
                                    val statusColor = when (payment.status?.uppercase()) {
                                        "PAID" -> Color(0xFF4CAF50)
                                        "PENDING" -> Color(0xFFFF9800)
                                        "FAILED" -> Color(0xFFF44336)
                                        else -> Color.Gray
                                    }
                                    Text(
                                        text = payment.status ?: "",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = statusColor,
                                        modifier = Modifier
                                            .background(
                                                statusColor.copy(alpha = 0.1f),
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            if (index < recentPayments.take(5).size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Gray.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
            
            // See all payments button - matches iOS
            Button(
                onClick = onNavigateToWallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = orangeColor),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "See all payments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}