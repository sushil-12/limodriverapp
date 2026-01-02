package com.limo1800driver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limo1800driver.app.data.model.registration.EmailVerificationData
import com.limo1800driver.app.data.repository.DriverRegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for email verification status
 */
@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val registrationRepository: DriverRegistrationRepository,
    private val tokenManager: com.limo1800driver.app.data.storage.TokenManager
) : ViewModel() {

    private val _emailVerificationState = MutableStateFlow<EmailVerificationState>(EmailVerificationState.Loading)
    val emailVerificationState: StateFlow<EmailVerificationState> = _emailVerificationState

    init {
        // Only check email verification status if user is authenticated
        if (tokenManager.isAuthenticated()) {
        checkEmailVerificationStatus()
        } else {
            // Set to a non-loading state when not authenticated
            // This prevents the component from showing and making API calls
            _emailVerificationState.value = EmailVerificationState.Success(
                com.limo1800driver.app.data.model.registration.EmailVerificationData(
                    mainEmail = com.limo1800driver.app.data.model.registration.EmailStatus(
                        email = null,
                        isVerified = true,
                        verificationRequired = false
                    ),
                    dispatchEmail = com.limo1800driver.app.data.model.registration.DispatchEmailStatus(
                        email = null,
                        isVerified = true,
                        verificationRequired = false,
                        exists = false
                    ),
                    emailsMatch = false,
                    autoVerificationApplied = false
                )
            )
        }
    }

    /**
     * Check email verification status
     * Only makes API call if user is authenticated
     */
    fun checkEmailVerificationStatus() {
        // Don't make API call if user is not authenticated
        if (!tokenManager.isAuthenticated()) {
            Timber.d("EmailVerificationVM: User not authenticated, skipping email verification check")
            return
        }

        viewModelScope.launch {
            _emailVerificationState.value = EmailVerificationState.Loading

            registrationRepository.getEmailVerificationStatus().fold(
                onSuccess = { data ->
                    _emailVerificationState.value = EmailVerificationState.Success(data)
                    Timber.d("Email verification status loaded: main=${data.mainEmail.isVerified}, dispatch=${data.dispatchEmail.isVerified}")
                },
                onFailure = { error ->
                    _emailVerificationState.value = EmailVerificationState.Error(error.message ?: "Failed to load email verification status")
                    Timber.e(error, "Failed to load email verification status")
                }
            )
        }
    }

    /**
     * Refresh email verification status
     */
    fun refreshEmailVerificationStatus() {
        checkEmailVerificationStatus()
    }

    /**
     * Check if email verification is needed
     */
    fun needsEmailVerification(): Boolean {
        return when (val state = _emailVerificationState.value) {
            is EmailVerificationState.Success -> {
                val data = state.data
                (data.mainEmail.verificationRequired && !data.mainEmail.isVerified) ||
                (data.dispatchEmail.verificationRequired && !data.dispatchEmail.isVerified)
            }
            else -> false
        }
    }

    /**
     * Get unverified emails
     */
    fun getUnverifiedEmails(): List<String> {
        return when (val state = _emailVerificationState.value) {
            is EmailVerificationState.Success -> {
                val data = state.data
                val unverifiedEmails = mutableListOf<String>()

                if (data.mainEmail.verificationRequired && !data.mainEmail.isVerified && data.mainEmail.email != null) {
                    unverifiedEmails.add(data.mainEmail.email!!)
                }

                if (data.dispatchEmail.verificationRequired && !data.dispatchEmail.isVerified) {
                    // For dispatch email, even if it doesn't exist yet, we still need to show it needs verification
                    if (data.dispatchEmail.email != null) {
                        unverifiedEmails.add(data.dispatchEmail.email!!)
                    } else {
                        // If dispatch email doesn't exist yet, show a generic message
                        unverifiedEmails.add("Dispatch Email")
                    }
                }

                unverifiedEmails
            }
            else -> emptyList()
        }
    }
}

sealed class EmailVerificationState {
    object Loading : EmailVerificationState()
    data class Success(val data: EmailVerificationData) : EmailVerificationState()
    data class Error(val message: String) : EmailVerificationState()
}
