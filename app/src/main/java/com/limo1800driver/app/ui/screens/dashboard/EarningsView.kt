package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.ui.viewmodel.DashboardStatsViewModel

/**
 * Earnings View Screen
 * Displays earnings summary and statistics
 * Placeholder implementation - can be expanded to match iOS EarningsView
 */
@Composable
fun EarningsView(
    viewModel: DashboardStatsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val statsState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.fetchDashboardStats()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Earnings Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Total Earnings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                
                if (statsState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "${statsState.stats?.currencySymbol ?: "$"}${String.format("%.2f", statsState.stats?.monthly?.earnings ?: 0.0)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
        
        // Placeholder for additional earnings content
        Text(
            text = "Earnings details will be displayed here",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

