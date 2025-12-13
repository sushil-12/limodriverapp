package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverUpdateItem
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Dashboard Updates/Alerts
 * Manages dashboard status alerts (error, warning, info)
 */
@HiltViewModel
class DashboardUpdatesViewModel @Inject constructor(
    private val dashboardRepository: DriverDashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUpdatesUiState())
    val uiState: StateFlow<DashboardUpdatesUiState> = _uiState.asStateFlow()
    
    /**
     * Fetch driver updates/alerts from API
     */
    fun fetchDriverUpdates(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !forceRefresh) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            dashboardRepository.getDriverUpdates()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        val alerts = transformUpdates(response.data)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            alerts = alerts,
                            error = null
                        )
                        Timber.d("Dashboard updates loaded: ${alerts.size} alerts")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        Timber.e("Dashboard updates API error: ${response.message}")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load dashboard updates"
                    )
                    Timber.e(exception, "Failed to fetch dashboard updates")
                }
        }
    }
    
    /**
     * Transform API updates into alerts
     */
    private fun transformUpdates(data: com.limo1800driver.app.data.model.dashboard.DriverUpdatesData): List<DashboardStatusAlert> {
        val alerts = mutableListOf<DashboardStatusAlert>()
        
        // Process error items
        data.error?.forEach { item ->
            item.toAlert(DashboardAlertType.ERROR)?.let { alerts.add(it) }
        }
        
        // Process warning items
        data.warning?.forEach { item ->
            item.toAlert(DashboardAlertType.WARNING)?.let { alerts.add(it) }
        }
        
        // Process info items
        data.info?.forEach { item ->
            item.toAlert(DashboardAlertType.INFO)?.let { alerts.add(it) }
        }
        
        // Fallback: Process legacy updates format
        data.updates?.forEach { update ->
            val alertType = when (update.type?.lowercase()) {
                "error" -> DashboardAlertType.ERROR
                "warning" -> DashboardAlertType.WARNING
                "info" -> DashboardAlertType.INFO
                else -> null
            }
            
            alertType?.let {
                alerts.add(
                    DashboardStatusAlert(
                        title = update.message ?: "Update",
                        message = update.message ?: "",
                        type = it,
                        actionRequired = false,
                        category = null
                    )
                )
            }
        }
        
        return alerts
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Dashboard Updates
 */
data class DashboardUpdatesUiState(
    val isLoading: Boolean = false,
    val alerts: List<DashboardStatusAlert> = emptyList(),
    val error: String? = null
)

/**
 * Dashboard Status Alert
 */
data class DashboardStatusAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: DashboardAlertType,
    val actionRequired: Boolean = false,
    val category: String? = null
)

/**
 * Dashboard Alert Type
 */
enum class DashboardAlertType {
    ERROR,
    WARNING,
    INFO
}

/**
 * Extension function to convert DriverUpdateItem to DashboardStatusAlert
 */
private fun DriverUpdateItem.toAlert(type: DashboardAlertType): DashboardStatusAlert? {
    val title = this.title ?: when (type) {
        DashboardAlertType.ERROR -> "Error"
        DashboardAlertType.WARNING -> "Warning"
        DashboardAlertType.INFO -> "Information"
    }
    
    val message = this.message ?: return null
    
    return DashboardStatusAlert(
        title = title,
        message = message,
        type = type,
        actionRequired = this.actionRequired ?: false,
        category = this.category
    )
}

