package com.limo1800driver.app.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.ui.components.CommonMenuHeader
import com.limo1800driver.app.ui.components.CommonRateCalculatorComponent
import com.limo1800driver.app.ui.theme.LimoGreen
import com.limo1800driver.app.ui.theme.LimoGrey
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.utils.RateCalculator
import com.limo1800driver.app.ui.viewmodel.FinalizeRatesViewModel

@Composable
fun FinalizeRatesScreen(
    bookingId: Int,
    mode: String,
    source: String,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val viewModel: FinalizeRatesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val dynamicRates = remember { mutableStateMapOf<String, String>() }
    val taxIsPercent = remember { mutableStateMapOf<String, Boolean>() }
    val numberOfHours = remember { mutableStateOf("") }
    val numberOfVehicles = remember { mutableStateOf("1") }

    var originalGrandTotal by remember { mutableStateOf<Double?>(null) }
    var pendingCharge by remember { mutableStateOf(false) }
    var didPay by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
        if (bookingId != 0) viewModel.load(bookingId)
    }

    // Initialize hour/vehicle defaults from preview when available
    LaunchedEffect(state.preview?.numberOfHours, state.preview?.numberOfVehicles) {
        val hours = state.preview?.numberOfHours
        val vehicles = state.preview?.numberOfVehicles
        if (numberOfHours.value.isBlank() && hours != null) numberOfHours.value = hours.toString()
        if (numberOfVehicles.value == "1" && vehicles != null) numberOfVehicles.value = vehicles.toString()
    }

    val preview = state.preview
    val rates = state.rates

    val totals = if (preview != null && rates != null) {
        RateCalculator.calculate(
            rateArray = rates.rateArray,
            dynamicRates = dynamicRates,
            taxIsPercent = taxIsPercent,
            serviceType = preview.serviceType,
            numberOfHours = numberOfHours.value.toIntOrNull() ?: 0,
            numberOfVehicles = numberOfVehicles.value.toIntOrNull() ?: 1,
            accountType = preview.accountType,
            createdBy = preview.createdBy,
            reservationType = preview.reservationType
        )
    } else null

    LaunchedEffect(totals?.grandTotal) {
        if (originalGrandTotal == null && totals != null && totals.grandTotal > 0) {
            originalGrandTotal = totals.grandTotal
        }
    }

    // Auto-run charge after cards load when user requested it
    LaunchedEffect(state.selectedCard, pendingCharge) {
        if (pendingCharge && state.selectedCard != null && totals != null) {
            viewModel.processChargePayment(bookingId.toString(), totals.grandTotal)
            didPay = true
            pendingCharge = false
        }
    }

    // Navigation on success
    LaunchedEffect(state.successMessage) {
        val msg = state.successMessage
        if (!msg.isNullOrBlank()) {
            viewModel.consumeSuccess()
            if (mode == "acceptReject") {
                onDone()
            } else if (didPay) {
                onDone()
            }
        }
    }

    val bookingStatus = preview?.bookingStatus ?: ""
    val isPending = bookingStatus.equals("pending", ignoreCase = true)
    val isFinalized = bookingStatus.equals("finalized", ignoreCase = true) || state.finalizedSuccessfully
    val canFinalize = totals != null && originalGrandTotal != null &&
        (!isFinalized || RateCalculator.areTotalsDifferent(originalGrandTotal ?: 0.0, totals.grandTotal))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            CommonMenuHeader(
                title = "Finalize Booking",
                subtitle = "Rates",
                onBackClick = onBack
            )

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LimoOrange)
                    }
                }

                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                    }
                }

                preview == null || rates == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No data available")
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 120.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        CommonRateCalculatorComponent(
                            rateArray = rates.rateArray,
                            dynamicRates = dynamicRates,
                            taxIsPercent = taxIsPercent,
                            serviceType = preview.serviceType,
                            numberOfHours = numberOfHours,
                            numberOfVehicles = numberOfVehicles,
                            accountType = preview.accountType,
                            createdBy = preview.createdBy,
                            reservationType = preview.reservationType,
                            currencySymbol = preview.currencySymbol,
                            isEditable = true,
                            showSummary = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFFE6E6E6))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Note- 25% of extra tip will be added to admin share",
                            style = MaterialTheme.typography.bodySmall,
                            color = LimoGrey,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color(0xFFE6E6E6))

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Payment Options",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LimoOrange
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (isFinalized && mode != "acceptReject" && !isPending && totals != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        didPay = true
                                        viewModel.processCashPayment(bookingId.toString(), totals.grandTotal)
                                    },
                                    enabled = !state.isProcessingCash && !state.isProcessingCharge,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF2F2F2),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(if (state.isProcessingCash) "Processing..." else "Paid Cash")
                                }
                                Button(
                                    onClick = {
                                        pendingCharge = true
                                        viewModel.loadCreditCards(bookingId)
                                    },
                                    enabled = !state.isProcessingCash && !state.isProcessingCharge,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF2F2F2),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(if (state.isProcessingCharge) "Processing..." else "Charge")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar (matches "Finalize" CTA style)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mode == "acceptReject" && isPending) {
                    Button(
                        onClick = { viewModel.reject(bookingId) },
                        enabled = !state.isFinalizing,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (state.isFinalizing) "Working..." else "Reject")
                    }
                    Button(
                        onClick = { viewModel.accept(bookingId) },
                        enabled = !state.isFinalizing,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LimoGreen)
                    ) {
                        Text(if (state.isFinalizing) "Working..." else "Accept")
                    }
                } else {
                    Button(
                        onClick = {
                            val hours = numberOfHours.value.toIntOrNull() ?: 0
                            val vehicles = numberOfVehicles.value.toIntOrNull() ?: 1
                            viewModel.finalizeRates(
                                bookingId = bookingId,
                                dynamicRates = dynamicRates,
                                taxIsPercent = taxIsPercent,
                                numberOfHours = hours,
                                numberOfVehicles = vehicles
                            )
                        },
                        enabled = !state.isFinalizing && canFinalize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LimoGreen)
                    ) {
                        Text(if (state.isFinalizing) "Finalizing..." else "Finalize")
                    }
                }
            }
        }
    }
}


