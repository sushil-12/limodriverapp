package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.TransferDetails
import com.limo1800driver.app.ui.components.CommonMenuHeader
import com.limo1800driver.app.ui.components.ShimmerText
import com.limo1800driver.app.ui.viewmodel.WalletViewModel

/**
 * Wallet Screen
 * Shows earnings, balance, and transaction history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearchField by remember { mutableStateOf(false) }
    var showWalletDetails by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    
    // Fetch wallet data on first load
    LaunchedEffect(Unit) {
        viewModel.fetchWalletData()
    }
    
    // Show wallet details popup when details are loaded
    LaunchedEffect(uiState.walletDetails) {
        if (uiState.walletDetails != null && uiState.isLoadingDetails.not()) {
            showWalletDetails = true
        }
    }
    
    Scaffold(
        topBar = {
            CommonMenuHeader(
                title = "Wallet",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && uiState.transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading wallet data...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.White),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Balance Section
                    item {
                        BalanceSection(
                            balance = uiState.balance,
                            currencySymbol = uiState.currencySymbol,
                            availableBalance = uiState.availableBalance,
                            paidOutBalance = uiState.paidOutBalance,
                            pendingBalance = uiState.pendingBalance,
                            isLoading = uiState.isLoading,
                            onDetailsClick = {
                                viewModel.fetchWalletDetails()
                            }
                        )
                    }
                    
                    // Search Section
                    item {
                        SearchSection(
                            showSearchField = showSearchField,
                            searchText = searchText,
                            onSearchFieldToggle = { showSearchField = !showSearchField },
                            onSearchTextChange = { searchText = it },
                            onSearch = {
                                viewModel.searchTransactions(searchText.text)
                            },
                            onClearSearch = {
                                searchText = TextFieldValue("")
                                viewModel.clearSearch()
                            },
                            isSearching = uiState.isSearching
                        )
                    }
                    
                    // Transactions List
                    if (uiState.transactions.isEmpty()) {
                        item {
                            EmptyTransactionsState(
                                hasSearchQuery = searchText.text.isNotBlank()
                            )
                        }
                    } else {
                        items(
                            items = uiState.transactions,
                            key = {
                                it.id
                                    ?: "${it.reservationId ?: "unknown"}_${it.createdDatetime ?: "unknown"}_${it.amount ?: 0.0}"
                            }
                        ) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                currencySymbol = uiState.currencySymbol,
                                viewModel = viewModel
                            )
                            
                            if (transaction.id != uiState.transactions.last().id) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = DividerDefaults.Thickness,
                                    color = Color.Gray.copy(alpha = 0.2f)
                                )
                            }
                        }
                        
                        // Load more indicator
                        if (uiState.isLoadingMore && uiState.canLoadMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Loading more transfers...",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Load more trigger
                        if (uiState.transactions.isNotEmpty() && uiState.canLoadMore && !uiState.isLoadingMore) {
                            item {
                                LaunchedEffect(Unit) {
                                    // Trigger load more when reaching end
                                    viewModel.loadMoreTransactions()
                                }
                            }
                        }
                        
                        // End of list indicator
                        if (!uiState.canLoadMore && uiState.transactions.isNotEmpty()) {
                            item {
                                Column {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = DividerDefaults.Thickness,
                                        color = Color.Gray.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = "All transfers loaded (${uiState.transactions.size} total)",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    // Error State
                    uiState.error?.let { error ->
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Wallet Details Dialog
        if (showWalletDetails && uiState.walletDetails != null) {
            WalletDetailsDialog(
                walletDetails = uiState.walletDetails!!,
                isLoading = uiState.isLoadingDetails,
                onDismiss = {
                    showWalletDetails = false
                }
            )
        }
    }
}

/**
 * Balance Section Component
 */
@Composable
private fun BalanceSection(
    balance: Double,
    currencySymbol: String,
    availableBalance: Double,
    paidOutBalance: Double,
    pendingBalance: Double,
    isLoading: Boolean,
    onDetailsClick: () -> Unit
) {
    Column {
        // Main Balance Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAF2E3)) // Light cream/orange background
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoading) {
                    ShimmerText(
                        modifier = Modifier.width(120.dp),
                        height = 13.dp
                    )
                    ShimmerText(
                        modifier = Modifier.width(150.dp),
                        height = 28.dp
                    )
                    ShimmerText(
                        modifier = Modifier.width(200.dp),
                        height = 12.dp
                    )
                } else {
                    Text(
                        text = "TOTAL EARNINGS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        letterSpacing = 0.5.sp
                    )
                    
                    Text(
                        text = "$currencySymbol${String.format("%.2f", balance)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = "Payout schedule information", // TODO: Get from API
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Details Button
            if (!isLoading) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Black
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Color.Black
                    )
                ) {
                    Text(
                        text = "Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        // Balance Breakdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAF2E3))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Available Balance
            BalanceItem(
                label = "AVAILABLE",
                value = "$currencySymbol${String.format("%.2f", availableBalance)}",
                isLoading = isLoading
            )
            
            // Paid Out
            BalanceItem(
                label = "PAID OUT",
                value = "$currencySymbol${String.format("%.2f", paidOutBalance)}",
                color = Color(0xFF4CAF50), // Green
                isLoading = isLoading
            )
            
            // Pending
            BalanceItem(
                label = "PENDING",
                value = "$currencySymbol${String.format("%.2f", pendingBalance)}",
                color = Color(0xFFFF9800), // Orange
                isLoading = isLoading
            )
        }
    }
}

/**
 * Balance Item Component
 */
@Composable
private fun BalanceItem(
    label: String,
    value: String,
    color: Color = Color.Black,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
//        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLoading) {
            ShimmerText(
                modifier = Modifier.width(60.dp),
                height = 12.dp
            )
            ShimmerText(
                modifier = Modifier.width(50.dp),
                height = 18.dp
            )
        } else {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                letterSpacing = 0.3.sp
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Search Section Component
 */
@Composable
private fun SearchSection(
    showSearchField: Boolean,
    searchText: TextFieldValue,
    onSearchFieldToggle: () -> Unit,
    onSearchTextChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    isSearching: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All Transfers",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            // Search Icon Button
            IconButton(
                onClick = onSearchFieldToggle,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    imageVector = if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (showSearchField) "Close Search" else "Search",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
            }
        }
        
        // Search Field (Animated)
        AnimatedVisibility(
            visible = showSearchField,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search transfers...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        if (searchText.text.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Gray.copy(alpha = 0.08f),
                        focusedContainerColor = Color.Gray.copy(alpha = 0.08f)
                    )
                )
                
                // Search Button
                if (searchText.text.isNotEmpty()) {
                    Button(
                        onClick = onSearch,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800) // Orange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Search",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Transaction Row Component
 */
@Composable
private fun TransactionRow(
    transaction: TransferDetails,
    currencySymbol: String,
    viewModel: WalletViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Black
        )
        
        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = viewModel.getTransactionDescription(transaction),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 1
            )
            
            Text(
                text = viewModel.formatTransactionDate(transaction.createdDatetime),
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            transaction.reservationId?.let { bookingId ->
                Text(
                    text = "Reservation #$bookingId",
                    fontSize = 11.sp,
                    color = Color.Blue
                )
            }
        }
        
        // Amount + Status
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF9800) // Orange
                )
                Text(
                    text = String.format("%.2f", transaction.amount ?: 0.0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
            
            transaction.status?.let { type ->
                val statusColor = viewModel.getTransactionStatusColor(type)
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = type.replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty Transactions State
 */
@Composable
private fun EmptyTransactionsState(
    hasSearchQuery: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.Gray.copy(alpha = 0.6f)
        )
        
        Text(
            text = if (hasSearchQuery) "No transfers found" else "No transfers yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
        )
        
        Text(
            text = if (hasSearchQuery) "Try adjusting your search terms" else "Your transfer history will appear here",
            fontSize = 14.sp,
            color = Color.Gray.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Wallet Details Dialog
 */
@Composable
private fun WalletDetailsDialog(
    walletDetails: com.limo1800driver.app.data.model.dashboard.DriverWalletDetailsData,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wallet Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Total Earnings
                    DetailRow(
                        label = "Total Earnings",
                        value = "${walletDetails.currencySymbol ?: "$"}${
                            String.format(
                                "%.2f",
                                walletDetails.totalEarnings ?: 0.0
                            )
                        }"
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    // Total Withdrawals
                    DetailRow(
                        label = "Total Withdrawals",
                        value = "${walletDetails.currencySymbol ?: "$"}${
                            String.format(
                                "%.2f",
                                walletDetails.totalWithdrawals ?: 0.0
                            )
                        }"
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    // Current Balance
                    DetailRow(
                        label = "Current Balance",
                        value = "${walletDetails.currencySymbol ?: "$"}${
                            String.format(
                                "%.2f",
                                walletDetails.currentBalance ?: 0.0
                            )
                        }"
                    )
                }
            }
        }
    }
}

/**
 * Detail Row Component
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
