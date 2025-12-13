package com.limo1800driver.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.model.dashboard.DriverUpdate
import com.limo1800driver.app.ui.components.ShimmerBox
import com.limo1800driver.app.ui.components.ShimmerText
import com.limo1800driver.app.ui.theme.LimoOrange
import com.limo1800driver.app.ui.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

// -- Colors --
private val BackgroundColor = Color(0xFFF9F9F9)
private val CardColor = Color.White
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val DividerColor = Color(0xFFEEEEEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearchBar by remember { mutableStateOf(false) }
    
    LaunchedEffect(showSearchBar) {
        if (!showSearchBar) viewModel.clearSearch()
    }
    
    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) viewModel.clearSearch()
                        }
                    ) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (showSearchBar) "Close Search" else "Search"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSearchBar) {
                SearchBar(
                    searchText = uiState.searchText,
                    onSearchTextChange = { viewModel.updateSearchText(it) },
                    onClearSearch = { viewModel.clearSearch() }
                )
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.notifications.isEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(6) { NotificationItemShimmer() }
                        }
                    }
                    uiState.error != null && uiState.notifications.isEmpty() -> {
                        ErrorView(
                            message = uiState.error,
                            onRetry = viewModel::refreshNotifications
                        )
                    }
                    uiState.notifications.isEmpty() -> {
                        EmptyNotificationsView()
                    }
                    else -> {
                        NotificationList(
                            notifications = uiState.notifications,
                            searchText = uiState.searchText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Surface(
        color = CardColor,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                placeholder = { Text("Search notifications...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = LimoOrange
                )
            )
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<DriverUpdate>,
    searchText: String
) {
    val isSearchMode = searchText.isNotEmpty()
    val groupedNotifications = remember(notifications, isSearchMode) {
        if (isSearchMode) mapOf("Search Results" to notifications)
        else groupNotificationsByDate(notifications)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        groupedNotifications.forEach { (header, items) ->
            item { SectionHeader(header) }
            items(items) { notification ->
                NotificationListItem(notification = notification)
                if (notification.id != items.lastOrNull()?.id) {
                    Divider(
                        color = DividerColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 72.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Surface(color = BackgroundColor, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = TextSecondary
        )
    }
}

@Composable
private fun NotificationListItem(notification: DriverUpdate) {
    // Determine icon and colors based on notification type
    val (icon, iconBgColor, iconTint) = when (notification.type?.lowercase()) {
        "error", "warning" -> Triple(
            Icons.Default.Warning,
            Color(0xFFFFEBEE),
            Color(0xFFF44336)
        )
        "info" -> Triple(
            Icons.Default.Info,
            Color(0xFFE3F2FD),
            Color(0xFF2196F3)
        )
        "success" -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFFE8F5E9),
            Color(0xFF4CAF50)
        )
        else -> Triple(
            Icons.Outlined.Notifications,
            Color(0xFFFFF3E0),
            LimoOrange
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor)
            .clickable { /* Handle click - could navigate to booking details */ }
            .padding(all = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = notification.type?.replaceFirstChar { it.uppercase() } ?: "Notification",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatTimeAgo(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = notification.message ?: "No message",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            
            // Booking ID if available
            notification.bookingId?.let { bookingId ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Booking #$bookingId",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2196F3),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// --- Loading Shimmer Effect ---

@Composable
private fun NotificationItemShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier.size(40.dp),
            shape = CircleShape
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            ShimmerText(
                modifier = Modifier.fillMaxWidth(0.6f),
                height = 16.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerText(
                modifier = Modifier.fillMaxWidth(0.9f),
                height = 12.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerText(
                modifier = Modifier.fillMaxWidth(0.4f),
                height = 12.dp
            )
        }
    }
}

// --- Empty & Error States ---

@Composable
private fun EmptyNotificationsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFF5F5F5), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFFBDBDBD)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Notifications Yet",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We'll let you know when something important happens regarding your bookings.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ErrorView(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message ?: "Something went wrong",
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
        ) {
            Text("Try Again", color = Color.White)
        }
    }
}

// --- Helper Functions ---

private fun groupNotificationsByDate(notifications: List<DriverUpdate>): Map<String, List<DriverUpdate>> {
    val grouped = mutableMapOf<String, MutableList<DriverUpdate>>()
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    notifications.forEach { notification ->
        try {
            val dateStr = notification.createdAt ?: return@forEach
            val date = dateFormat.parse(dateStr) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val group = when {
                cal.timeInMillis >= today.timeInMillis -> "Today"
                cal.timeInMillis >= yesterday.timeInMillis -> "Yesterday"
                else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
            }
            grouped.getOrPut(group) { mutableListOf() }.add(notification)
        } catch (e: Exception) {
            grouped.getOrPut("Older") { mutableListOf() }.add(notification)
        }
    }
    return grouped
}

private fun formatTimeAgo(timestamp: String?): String {
    if (timestamp == null) return ""
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = format.parse(timestamp) ?: return ""
        val diff = Date().time - date.time
        
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        ""
    }
}
