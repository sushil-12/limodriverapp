package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.api.DriverChatApi
import com.limo1800driver.app.data.socket.DriverChatMessage
import com.limo1800driver.app.data.socket.DriverChatSendRequest
import com.limo1800driver.app.data.socket.DriverSocketService
import com.limo1800driver.app.data.socket.SocketConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

data class ChatUiState(
    val bookingId: Int = 0,
    val customerId: String = "",
    val customerName: String = "Passenger",
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<DriverChatMessage> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @Named("chat") private val chatRetrofit: Retrofit,
    private val socketService: DriverSocketService
) : ViewModel() {

    private val chatApi: DriverChatApi = chatRetrofit.create(DriverChatApi::class.java)

    private val _bookingId = MutableStateFlow(0)
    private val _customerId = MutableStateFlow("")
    private val _customerName = MutableStateFlow("Passenger")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ChatUiState> = combine(
        _bookingId,
        _customerId,
        _customerName,
        socketService.connectionStatus,
        socketService.chatMessages,
        _isLoading,
        socketService.chatError,
        _error
    ) { values: Array<Any?> ->
        val bookingId = values[0] as Int
        val customerId = values[1] as String
        val customerName = values[2] as String
        val conn = values[3] as SocketConnectionStatus
        val messages = values[4] as List<DriverChatMessage>
        val loading = values[5] as Boolean
        val socketError = values[6] as String?
        val localError = values[7] as String?

        ChatUiState(
            bookingId = bookingId,
            customerId = customerId,
            customerName = customerName,
            isConnected = conn.isConnected,
            isLoading = loading,
            errorMessage = localError ?: socketError,
            messages = messages
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState()
    )

    fun start(bookingId: Int, customerId: String, customerName: String) {
        _bookingId.value = bookingId
        _customerId.value = customerId
        _customerName.value = customerName.ifBlank { "Passenger" }
        _error.value = null

        // Ensure socket is connected and filtered to this booking.
        socketService.connect()
        socketService.setCurrentChatBookingId(bookingId)

        fetchHistory()
    }

    fun stop() {
        // Clear booking filter so background socket can keep running for other features.
        socketService.setCurrentChatBookingId(null)
    }

    fun sendMessage(text: String) {
        val bookingId = _bookingId.value
        val receiverId = _customerId.value
        if (bookingId <= 0 || receiverId.isBlank()) return

        socketService.emitChatMessage(bookingId = bookingId, receiverId = receiverId, message = text)
    }

    fun fetchHistory() {
        val bookingId = _bookingId.value
        if (bookingId <= 0) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val resp = chatApi.getChatHistory(bookingId = bookingId)
                if (resp.success) {
                    socketService.setChatHistory(bookingId, resp.data)
                } else {
                    _error.value = "Failed to load chat history"
                }
            } catch (e: Exception) {
                Timber.tag("ChatViewModel").e(e, "Failed to load chat history")
                _error.value = e.message ?: "Failed to load chat history"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessageViaApiFallback(text: String) {
        val bookingId = _bookingId.value
        val receiverId = _customerId.value
        val trimmed = text.trim()
        if (bookingId <= 0 || receiverId.isBlank() || trimmed.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                chatApi.sendMessage(DriverChatSendRequest(bookingId, receiverId, trimmed))
                // Refresh (simple, robust).
                fetchHistory()
            } catch (e: Exception) {
                Timber.tag("ChatViewModel").e(e, "Failed to send message via API")
                _error.value = e.message ?: "Failed to send message"
            } finally {
                _isLoading.value = false
            }
        }
    }
}


