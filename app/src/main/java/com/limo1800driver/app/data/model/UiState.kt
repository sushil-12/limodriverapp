package com.limo1800driver.app.data.model

/**
 * Generic UI state for handling loading, success, and error states
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
