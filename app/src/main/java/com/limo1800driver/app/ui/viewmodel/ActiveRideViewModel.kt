package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.socket.DriverActiveRide
import com.limo1800driver.app.data.socket.DriverSocketService
import com.limo1800driver.app.data.socket.SocketConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveRideViewModel @Inject constructor(
    private val socketService: DriverSocketService
) : ViewModel() {

    val connectionStatus: StateFlow<SocketConnectionStatus> = socketService.connectionStatus
    val activeRide: StateFlow<DriverActiveRide?> = socketService.activeRide

    fun ensureConnected() {
        viewModelScope.launch {
            socketService.connect()
        }
    }
}


