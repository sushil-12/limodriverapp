package com.limo1800driver.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limo1800driver.app.data.socket.DriverChatMessage
import com.limo1800driver.app.ui.components.ShimmerCircle
import com.limo1800driver.app.ui.viewmodel.ChatViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatScreen(
    bookingId: Int,
    customerId: String,
    customerName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    // Quick responses from the design
    val quickResponses = listOf(
        "I've Arrived",
        "I'm on my way!",
        "Stuck in traffic",
        "Can't take call sorry!"
    )

    LaunchedEffect(bookingId, customerId, customerName) {
        viewModel.start(bookingId = bookingId, customerId = customerId, customerName = customerName)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            ChatHeader(
                customerName = uiState.customerName,
                onBack = {
                    viewModel.stop()
                    onBack()
                }
            )
        },
        bottomBar = {
            // Input Area + Quick Responses
            // imePadding ensures this moves up with keyboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.ime)
            ) {
                // Quick Response Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickResponses) { response ->
                        SuggestionChip(
                            onClick = { viewModel.sendMessage(response) },
                            label = { Text(response, color = Color.Black) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFFF5F5F5),
                                labelColor = Color.Black
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Divider(color = Color(0xFFF0F0F0))

                // Input Field
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(), // Handle bottom safe area
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Type a message...", color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF9F9F9), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF9F9F9),
                            unfocusedContainerColor = Color(0xFFF9F9F9)
                        ),
                        singleLine = true,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input.trim())
                                input = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFF9800), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                reverseLayout = true // Starts from bottom
            ) {
                // We reverse the list to match reverseLayout=true
                items(uiState.messages.reversed(), key = { it.id }) { msg ->
                    val isDriver = msg.senderRole == "driver"
                    ChatBubbleRow(message = msg, isFromDriver = isDriver)
                }
            }

            if (uiState.isLoading && uiState.messages.isEmpty()) {
                ShimmerCircle(
                    modifier = Modifier.align(Alignment.Center),
                    size = 32.dp,
                )
            }
        }
    }
}

@Composable
fun ChatHeader(customerName: String, onBack: () -> Unit) {
    Surface(
        color = Color(0xFFF9F9F9), // Light gray header bg from design
        shadowElevation = 0.dp
    ) {
        Column {
            Spacer(modifier = Modifier.statusBarsPadding())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                // Title Area
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = customerName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Chat",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
fun ChatBubbleRow(message: DriverChatMessage, isFromDriver: Boolean) {
    val bubbleColor = if (isFromDriver) Color(0xFFFF9800) else Color(0xFFF2F2F2)
    val textColor = if (isFromDriver) Color.White else Color.Black
    val timeString = formatTime(message.createdAt)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromDriver) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isFromDriver) {
            // DRIVER LAYOUT: Time/Checks LEFT of bubble
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Read",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // The Bubble
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.message,
                color = textColor,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                lineHeight = 20.sp
            )
        }

        if (!isFromDriver) {
            // CUSTOMER LAYOUT: Time RIGHT of bubble
            Text(
                text = timeString,
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun formatTime(createdAt: String): String {
    return runCatching {
        val instant = Instant.parse(createdAt)
        val local = java.time.ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("hh:mma").format(local).lowercase()
    }.getOrDefault("")
}