package com.fantasyfootball.analyzer.presentation.utils

import android.util.Log
import com.fantasyfootball.analyzer.data.remote.NetworkResult
import com.fantasyfootball.analyzer.presentation.ui.components.AppError
import com.fantasyfootball.analyzer.presentation.ui.components.ErrorType
import com.fantasyfootball.analyzer.presentation.utils.ErrorHandlingUtils.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling service for consistent error management across the application.
 * Implements requirement 1.4: Comprehensive error handling and user feedback.
 */
@Singleton
class ErrorHandlingService @Inject constructor() {
    
    companion object {
        private const val TAG = "ErrorHandlingService"
        private const val MAX_ERROR_HISTORY = 10
    }
    
    private val _currentError = MutableStateFlow<AppError?>(null)
    val currentError: StateFlow<AppError?> = _currentError.asStateFlow()
    
    private val _errorHistory = MutableStateFlow<List<AppError>>(emptyList())
    val errorHistory: StateFlow<List<AppError>> = _errorHistory.asStateFlow()
    
    private val _retryAttempts = mutableMapOf<String, Int>()
    
    /**
     * Handles a NetworkResult.Error and converts it to user-friendly AppError
     */
    fun handleNetworkError(
        error: NetworkResult.Error,
        operationId: String,
        retryAction: (() -> Unit)? = null,
        alternativeAction: (() -> Unit)? = null,
        alternativeActionLabel: String? = null
    ): AppError {
        val appError = error.toAppError(
            retryAction = if (retryAction != null) {
                { handleRetry(operationId, retryAction) }
            } else null,
            alternativeAction = alternativeAction,
            alternativeActionLabel = alternativeActionLabel
        )
        
        setCurrentError(appError)
        logErrorIfNeeded(appError)
        
        return appError
    }
    
    /**
     * Handles a generic exception and converts it to user-friendly AppError
     */
    fun handleException(
        exception: Throwable,
        operationId: String,
        context: String = "operation",
        retryAction: (() -> Unit)? = null,
        alternativeAction: (() -> Unit)? = null,
        alternativeActionLabel: String? = null
    ): AppError {
        val errorType = ErrorHandlingUtils.determineErrorType(exception)
        val appError = AppError(
            type = errorType,
            severity = ErrorHandlingUtils.determineErrorSeverity(errorType),
            title = ErrorHandlingUtils.generateErrorTitle(errorType),
            message = ErrorHandlingUtils.generateUserFriendlyMessage(errorType, exception.message ?: ""),
            technicalDetails = ErrorHandlingUtils.formatTechnicalDetails(exception),
            canRetry = ErrorHandlingUtils.isRetryable(errorType),
            retryAction = if (retryAction != null) {
                { handleRetry(operationId, retryAction) }
            } else null,
            alternativeAction = alternativeAction,
            alternativeActionLabel = alternativeActionLabel
        )
        
        setCurrentError(appError)
        logErrorIfNeeded(appError)
        
        return appError
    }
    
    /**
     * Sets the current error and adds it to history
     */
    fun setCurrentError(error: AppError?) {
        _currentError.value = error
        
        error?.let { addToHistory(it) }
    }
    
    /**
     * Clears the current error
     */
    fun clearCurrentError() {
        _currentError.value = null
    }
    
    /**
     * Handles retry logic with exponential backoff
     */
    private fun handleRetry(operationId: String, retryAction: () -> Unit) {
        val currentAttempts = _retryAttempts[operationId] ?: 0
        val newAttempts = currentAttempts + 1
        
        _retryAttempts[operationId] = newAttempts
        
        // Clear current error before retry
        clearCurrentError()
        
        // Execute retry action
        retryAction()
        
        Log.d(TAG, "Retry attempt $newAttempts for operation: $operationId")
    }
    
    /**
     * Resets retry attempts for an operation
     */
    fun resetRetryAttempts(operationId: String) {
        _retryAttempts.remove(operationId)
    }
    
    /**
     * Gets the number of retry attempts for an operation
     */
    fun getRetryAttempts(operationId: String): Int {
        return _retryAttempts[operationId] ?: 0
    }
    
    /**
     * Adds error to history with size limit
     */
    private fun addToHistory(error: AppError) {
        val currentHistory = _errorHistory.value.toMutableList()
        currentHistory.add(0, error) // Add to beginning
        
        // Limit history size
        if (currentHistory.size > MAX_ERROR_HISTORY) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _errorHistory.value = currentHistory
    }
    
    /**
     * Logs error if it should be logged for debugging
     */
    private fun logErrorIfNeeded(error: AppError) {
        if (ErrorHandlingUtils.shouldLogError(error.type)) {
            Log.e(TAG, "Error occurred: ${error.title} - ${error.message}")
            error.technicalDetails?.let { details ->
                Log.e(TAG, "Technical details: $details")
            }
        } else {
            Log.d(TAG, "Handled error: ${error.title}")
        }
    }
    
    /**
     * Creates specific error types with consistent handling
     */
    fun createInsufficientDataError(
        playerName: String,
        opponentTeam: String,
        availableGames: Int,
        minimumRequired: Int = 3,
        fallbackAction: (() -> Unit)? = null
    ): AppError {
        val error = ErrorHandlingUtils.createInsufficientDataError(
            playerName = playerName,
            opponentTeam = opponentTeam,
            availableGames = availableGames,
            minimumRequired = minimumRequired,
            fallbackAction = fallbackAction
        )
        
        setCurrentError(error)
        return error
    }
    
    fun createOfflineModeError(
        dataType: String,
        lastUpdated: Long? = null,
        retryAction: (() -> Unit)? = null
    ): AppError {
        val error = ErrorHandlingUtils.createOfflineModeError(
            dataType = dataType,
            lastUpdated = lastUpdated,
            retryAction = retryAction
        )
        
        setCurrentError(error)
        return error
    }
    
    fun createRateLimitError(
        retryAfterSeconds: Int? = null,
        retryAction: (() -> Unit)? = null
    ): AppError {
        val error = ErrorHandlingUtils.createRateLimitError(
            retryAfterSeconds = retryAfterSeconds,
            retryAction = retryAction
        )
        
        setCurrentError(error)
        return error
    }
    
    fun createSearchError(
        query: String,
        hasLocalResults: Boolean,
        retryAction: (() -> Unit)? = null,
        showLocalAction: (() -> Unit)? = null
    ): AppError {
        val error = ErrorHandlingUtils.createSearchError(
            query = query,
            hasLocalResults = hasLocalResults,
            retryAction = retryAction,
            showLocalAction = showLocalAction
        )
        
        setCurrentError(error)
        return error
    }
    
    fun createTimeoutError(
        operation: String,
        retryAction: (() -> Unit)? = null,
        useOfflineAction: (() -> Unit)? = null
    ): AppError {
        val error = ErrorHandlingUtils.createTimeoutError(
            operation = operation,
            retryAction = retryAction,
            useOfflineAction = useOfflineAction
        )
        
        setCurrentError(error)
        return error
    }
    
    fun createCacheError(
        operation: String,
        retryAction: (() -> Unit)? = null,
        clearCacheAction: (() -> Unit)? = null
    ): AppError {
        val error = ErrorHandlingUtils.createCacheError(
            operation = operation,
            retryAction = retryAction,
            clearCacheAction = clearCacheAction
        )
        
        setCurrentError(error)
        return error
    }
    
    /**
     * Gets error statistics for debugging/monitoring
     */
    fun getErrorStatistics(): ErrorStatistics {
        val history = _errorHistory.value
        val errorsByType = history.groupBy { it.type }
        val errorsBySeverity = history.groupBy { it.severity }
        
        return ErrorStatistics(
            totalErrors = history.size,
            errorsByType = errorsByType.mapValues { it.value.size },
            errorsBySeverity = errorsBySeverity.mapValues { it.value.size },
            mostCommonError = errorsByType.maxByOrNull { it.value.size }?.key,
            recentErrors = history.take(5)
        )
    }
    
    /**
     * Clears error history
     */
    fun clearErrorHistory() {
        _errorHistory.value = emptyList()
        _retryAttempts.clear()
    }
}

/**
 * Data class for error statistics
 */
data class ErrorStatistics(
    val totalErrors: Int,
    val errorsByType: Map<ErrorType, Int>,
    val errorsBySeverity: Map<com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity, Int>,
    val mostCommonError: ErrorType?,
    val recentErrors: List<AppError>
)

/**
 * Extension functions for ErrorHandlingUtils to work with ErrorHandlingService
 */
private fun ErrorHandlingUtils.determineErrorType(exception: Throwable): ErrorType {
    return when (exception) {
        is java.net.UnknownHostException, is java.net.ConnectException -> ErrorType.NETWORK_UNAVAILABLE
        is java.net.SocketTimeoutException -> ErrorType.API_RATE_LIMIT
        is javax.net.ssl.SSLException -> ErrorType.NETWORK_UNAVAILABLE
        is IllegalArgumentException -> ErrorType.INVALID_INPUT
        else -> {
            val message = exception.message?.lowercase() ?: ""
            when {
                message.contains("rate limit") || message.contains("too many requests") -> ErrorType.API_RATE_LIMIT
                message.contains("network") || message.contains("connection") -> ErrorType.NETWORK_UNAVAILABLE
                message.contains("cache") || message.contains("expired") -> ErrorType.CACHE_EXPIRED
                message.contains("insufficient") || message.contains("not enough") -> ErrorType.INSUFFICIENT_DATA
                else -> ErrorType.UNKNOWN_ERROR
            }
        }
    }
}

private fun ErrorHandlingUtils.determineErrorSeverity(errorType: ErrorType): com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity {
    return when (errorType) {
        ErrorType.NETWORK_UNAVAILABLE -> com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity.WARNING
        ErrorType.API_RATE_LIMIT -> com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity.WARNING
        ErrorType.INSUFFICIENT_DATA -> com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity.INFO
        ErrorType.CACHE_EXPIRED -> com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity.INFO
        ErrorType.INVALID_INPUT -> com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity.ERROR
        ErrorType.UNKNOWN_ERROR -> com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity.ERROR
    }
}

private fun ErrorHandlingUtils.generateErrorTitle(errorType: ErrorType): String {
    return when (errorType) {
        ErrorType.NETWORK_UNAVAILABLE -> "Connection Problem"
        ErrorType.API_RATE_LIMIT -> "Service Temporarily Busy"
        ErrorType.INSUFFICIENT_DATA -> "Limited Data Available"
        ErrorType.CACHE_EXPIRED -> "Data Needs Refresh"
        ErrorType.INVALID_INPUT -> "Invalid Input"
        ErrorType.UNKNOWN_ERROR -> "Something Went Wrong"
    }
}

private fun ErrorHandlingUtils.generateUserFriendlyMessage(errorType: ErrorType, originalMessage: String): String {
    return when (errorType) {
        ErrorType.NETWORK_UNAVAILABLE -> 
            "Unable to connect to the internet. Please check your connection and try again. " +
            "You can still view previously loaded data while offline."
        
        ErrorType.API_RATE_LIMIT -> 
            "The service is currently busy handling many requests. Please wait a moment and try again. " +
            "This helps ensure the best experience for all users."
        
        ErrorType.INSUFFICIENT_DATA -> 
            "There isn't enough historical data available for this analysis. " +
            "We'll show what we can and use league averages where needed."
        
        ErrorType.CACHE_EXPIRED -> 
            "The data you're viewing may be outdated. Connect to the internet to get the latest information."
        
        ErrorType.INVALID_INPUT -> 
            "The information provided isn't valid. Please check your input and try again."
        
        ErrorType.UNKNOWN_ERROR -> 
            "An unexpected error occurred. Please try again, and if the problem persists, " +
            "you can still use cached data for your analysis."
    }
}

private fun ErrorHandlingUtils.isRetryable(errorType: ErrorType): Boolean {
    return when (errorType) {
        ErrorType.NETWORK_UNAVAILABLE -> true
        ErrorType.API_RATE_LIMIT -> true
        ErrorType.INSUFFICIENT_DATA -> false
        ErrorType.CACHE_EXPIRED -> true
        ErrorType.INVALID_INPUT -> false
        ErrorType.UNKNOWN_ERROR -> true
    }
}