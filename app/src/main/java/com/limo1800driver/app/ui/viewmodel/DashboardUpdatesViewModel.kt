package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.dashboard.DriverUpdateItem
import com.limo1800driver.app.data.repository.DriverDashboardRepository
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import com.limo1800driver.app.ui.navigation.NavRoutes
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
    private val dashboardRepository: DriverDashboardRepository,
    private val registrationRepository: DriverRegistrationRepository
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
            
            // Fetch both dashboard updates and email verification status in parallel
            val dashboardUpdatesResult = dashboardRepository.getDriverUpdates()
            val emailVerificationResult = registrationRepository.getEmailVerificationStatus()
            
            dashboardUpdatesResult.onSuccess { response ->
                if (response.success && response.data != null) {
                    val alerts = transformUpdates(response.data).toMutableList()
                    
                    // Add email verification alerts (fail silently if not authenticated or API fails)
                    emailVerificationResult.onSuccess { emailData ->
                        val emailAlerts = createEmailVerificationAlerts(emailData)
                        alerts.addAll(emailAlerts)
                        Timber.d("Email verification alerts added: ${emailAlerts.size}")
                    }.onFailure { exception ->
                        // Silently handle email verification failures (e.g., user not authenticated)
                        // Don't block dashboard updates from showing
                        Timber.d(exception, "Email verification status not available (user may not be authenticated)")
                    }
                    
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
            
            dashboardUpdatesResult.onFailure { exception ->
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
     * Create email verification alerts based on verification status
     */
    private fun createEmailVerificationAlerts(emailData: com.limo1800driver.app.data.model.registration.EmailVerificationData): List<DashboardStatusAlert> {
        val alerts = mutableListOf<DashboardStatusAlert>()
        
        val mainEmailNeedsVerification = emailData.mainEmail.verificationRequired && !emailData.mainEmail.isVerified
        val dispatchEmailNeedsVerification = emailData.dispatchEmail.verificationRequired && !emailData.dispatchEmail.isVerified

        // Individual alerts if only one needs verification
        if (mainEmailNeedsVerification) {
            val mainEmail = emailData.mainEmail.email ?: "not set"
            alerts.add(
                DashboardStatusAlert(
                    title = "Main Email Verification Required",
                    message = "Your Main Email ($mainEmail) needs to be verified. Check your inbox for verification link.",
                    type = DashboardAlertType.WARNING,
                    actionRequired = true,
                    category = "email_verification_main",
                    actionLinks = listOf(
                        AlertActionLink("Verify Email", "action://verify_email?email=$mainEmail"),
                        AlertActionLink("Change Main Email", NavRoutes.BasicInfoFromAccountSettings)
                    )
                )
            )
        }

        if (dispatchEmailNeedsVerification) {
            val dispatchEmail = emailData.dispatchEmail.email ?: "not set"
            alerts.add(
                DashboardStatusAlert(
                    title = "Dispatch Email Verification Required",
                    message = "Your Dispatch Email ($dispatchEmail) needs to be verified. Check your inbox for verification link.",
                    type = DashboardAlertType.WARNING,
                    actionRequired = true,
                    category = "email_verification_dispatch",
                    actionLinks = listOf(
                        AlertActionLink("Verify Email", "action://verify_email?email=$dispatchEmail"),
                        AlertActionLink("Change Dispatch Email", NavRoutes.CompanyInfoFromAccountSettings)
                    )
                )
            )
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
    val category: String? = null,
    val actionLinks: List<AlertActionLink> = emptyList()
)

/**
 * Action link for alert navigation
 */
data class AlertActionLink(
    val label: String,
    val route: String
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

