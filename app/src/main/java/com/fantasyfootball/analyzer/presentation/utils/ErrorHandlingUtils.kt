package com.fantasyfootball.analyzer.presentation.utils

import com.fantasyfootball.analyzer.data.remote.NetworkResult
import com.fantasyfootball.analyzer.presentation.ui.components.AppError
import com.fantasyfootball.analyzer.presentation.ui.components.ErrorSeverity
import com.fantasyfootball.analyzer.presentation.ui.components.ErrorType
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Utility functions for error handling and user-friendly error message generation.
 * Implements requirement 1.4: Comprehensive error handling and user messaging.
 */
object ErrorHandlingUtils {
    
    /**
     * Converts NetworkResult.Error to user-friendly AppError
     */
    fun NetworkResult.Error.toAppError(
        retryAction: (() -> Unit)? = null,
        alternativeAction: (() -> Unit)? = null,
        alternativeActionLabel: String? = null
    ): AppError {
        val errorType = determineErrorType(this.exception)
        val severity = determineErrorSeverity(errorType)
        val userMessage = generateUserFriendlyMessage(errorType, this.message)
        val title = generateErrorTitle(errorType)
        
        return AppError(
            type = errorType,
            severity = severity,
            title = title,
            message = userMessage,
            technicalDetails = "${exception.javaClass.simpleName}: ${exception.message}",
            canRetry = isRetryable(errorType),
            retryAction = retryAction,
            alternativeAction = alternativeAction,
            alternativeActionLabel = alternativeActionLabel
        )
    }
    
    /**
     * Determines error type based on exception
     */
    private fun determineErrorType(exception: Throwable): ErrorType {
        return when (exception) {
            is UnknownHostException, is ConnectException -> ErrorType.NETWORK_UNAVAILABLE
            is SocketTimeoutException -> ErrorType.API_RATE_LIMIT
            is SSLException -> ErrorType.NETWORK_UNAVAILABLE
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
    
    /**
     * Determines error severity based on error type
     */
    private fun determineErrorSeverity(errorType: ErrorType): ErrorSeverity {
        return when (errorType) {
            ErrorType.NETWORK_UNAVAILABLE -> ErrorSeverity.WARNING
            ErrorType.API_RATE_LIMIT -> ErrorSeverity.WARNING
            ErrorType.INSUFFICIENT_DATA -> ErrorSeverity.INFO
            ErrorType.CACHE_EXPIRED -> ErrorSeverity.INFO
            ErrorType.INVALID_INPUT -> ErrorSeverity.ERROR
            ErrorType.UNKNOWN_ERROR -> ErrorSeverity.ERROR
        }
    }
    
    /**
     * Generates user-friendly error titles
     */
    private fun generateErrorTitle(errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.NETWORK_UNAVAILABLE -> "Connection Problem"
            ErrorType.API_RATE_LIMIT -> "Service Temporarily Busy"
            ErrorType.INSUFFICIENT_DATA -> "Limited Data Available"
            ErrorType.CACHE_EXPIRED -> "Data Needs Refresh"
            ErrorType.INVALID_INPUT -> "Invalid Input"
            ErrorType.UNKNOWN_ERROR -> "Something Went Wrong"
        }
    }
    
    /**
     * Generates user-friendly error messages
     */
    private fun generateUserFriendlyMessage(errorType: ErrorType, originalMessage: String): String {
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
    
    /**
     * Determines if an error type is retryable
     */
    private fun isRetryable(errorType: ErrorType): Boolean {
        return when (errorType) {
            ErrorType.NETWORK_UNAVAILABLE -> true
            ErrorType.API_RATE_LIMIT -> true
            ErrorType.INSUFFICIENT_DATA -> false
            ErrorType.CACHE_EXPIRED -> true
            ErrorType.INVALID_INPUT -> false
            ErrorType.UNKNOWN_ERROR -> true
        }
    }
    
    /**
     * Creates error for insufficient historical data scenarios
     * Implements requirement 2.4: Appropriate messaging for insufficient historical data
     */
    fun createInsufficientDataError(
        playerName: String,
        opponentTeam: String,
        availableGames: Int,
        minimumRequired: Int = 3,
        fallbackAction: (() -> Unit)? = null
    ): AppError {
        return AppError(
            type = ErrorType.INSUFFICIENT_DATA,
            severity = ErrorSeverity.INFO,
            title = "Limited Matchup History",
            message = "Only $availableGames games found between $playerName and $opponentTeam " +
                    "(minimum $minimumRequired recommended). Analysis will use league averages " +
                    "and position rankings to provide the best possible recommendations.",
            canRetry = false,
            alternativeAction = fallbackAction,
            alternativeActionLabel = "View League Averages"
        )
    }
    
    /**
     * Creates error for offline mode scenarios
     */
    fun createOfflineModeError(
        dataType: String,
        lastUpdated: Long? = null,
        retryAction: (() -> Unit)? = null
    ): AppError {
        val timeAgo = lastUpdated?.let { timestamp ->
            val now = System.currentTimeMillis()
            val diffHours = (now - timestamp) / (1000 * 60 * 60)
            when {
                diffHours < 1 -> "less than an hour ago"
                diffHours < 24 -> "$diffHours hours ago"
                else -> "${diffHours / 24} days ago"
            }
        } ?: "some time ago"
        
        return AppError(
            type = ErrorType.NETWORK_UNAVAILABLE,
            severity = ErrorSeverity.INFO,
            title = "Offline Mode",
            message = "You're currently offline. Showing cached $dataType from $timeAgo. " +
                    "Connect to the internet to get the latest information.",
            canRetry = true,
            retryAction = retryAction
        )
    }
    
    /**
     * Creates error for rate limiting scenarios with exponential backoff suggestion
     */
    fun createRateLimitError(
        retryAfterSeconds: Int? = null,
        retryAction: (() -> Unit)? = null
    ): AppError {
        val waitTime = retryAfterSeconds?.let { seconds ->
            when {
                seconds < 60 -> "$seconds seconds"
                seconds < 3600 -> "${seconds / 60} minutes"
                else -> "${seconds / 3600} hours"
            }
        } ?: "a moment"
        
        return AppError(
            type = ErrorType.API_RATE_LIMIT,
            severity = ErrorSeverity.WARNING,
            title = "Service Temporarily Busy",
            message = "Too many requests have been made recently. Please wait $waitTime before trying again. " +
                    "This helps ensure reliable service for all users.",
            canRetry = true,
            retryAction = retryAction
        )
    }
    
    /**
     * Creates error for cache-related issues
     */
    fun createCacheError(
        operation: String,
        retryAction: (() -> Unit)? = null,
        clearCacheAction: (() -> Unit)? = null
    ): AppError {
        return AppError(
            type = ErrorType.CACHE_EXPIRED,
            severity = ErrorSeverity.WARNING,
            title = "Cache Issue",
            message = "There was a problem with cached data during $operation. " +
                    "You can try refreshing the data or clearing the cache.",
            canRetry = true,
            retryAction = retryAction,
            alternativeAction = clearCacheAction,
            alternativeActionLabel = "Clear Cache"
        )
    }
    
    /**
     * Creates error for search-related issues
     */
    fun createSearchError(
        query: String,
        hasLocalResults: Boolean,
        retryAction: (() -> Unit)? = null,
        showLocalAction: (() -> Unit)? = null
    ): AppError {
        val message = if (hasLocalResults) {
            "Unable to search online for '$query'. Showing cached results instead. " +
            "Connect to the internet for the most up-to-date player information."
        } else {
            "No results found for '$query'. Try a different search term or check your spelling."
        }
        
        return AppError(
            type = if (hasLocalResults) ErrorType.NETWORK_UNAVAILABLE else ErrorType.INVALID_INPUT,
            severity = if (hasLocalResults) ErrorSeverity.INFO else ErrorSeverity.WARNING,
            title = if (hasLocalResults) "Showing Cached Results" else "No Results Found",
            message = message,
            canRetry = true,
            retryAction = retryAction,
            alternativeAction = if (hasLocalResults) showLocalAction else null,
            alternativeActionLabel = if (hasLocalResults) "View All Cached Players" else null
        )
    }
    
    /**
     * Creates error for data loading timeouts
     */
    fun createTimeoutError(
        operation: String,
        retryAction: (() -> Unit)? = null,
        useOfflineAction: (() -> Unit)? = null
    ): AppError {
        return AppError(
            type = ErrorType.NETWORK_UNAVAILABLE,
            severity = ErrorSeverity.WARNING,
            title = "Request Timed Out",
            message = "The request to $operation is taking longer than expected. " +
                    "This might be due to a slow connection or server issues.",
            canRetry = true,
            retryAction = retryAction,
            alternativeAction = useOfflineAction,
            alternativeActionLabel = "Use Offline Data"
        )
    }
    
    /**
     * Formats exception for technical details display
     */
    fun formatTechnicalDetails(exception: Throwable): String {
        return buildString {
            appendLine("Exception: ${exception.javaClass.simpleName}")
            appendLine("Message: ${exception.message}")
            exception.cause?.let { cause ->
                appendLine("Cause: ${cause.javaClass.simpleName}")
                appendLine("Cause Message: ${cause.message}")
            }
            
            // Include relevant stack trace elements (first few)
            val relevantStackTrace = exception.stackTrace
                .take(3)
                .filter { it.className.contains("fantasyfootball") }
            
            if (relevantStackTrace.isNotEmpty()) {
                appendLine("\nStack Trace:")
                relevantStackTrace.forEach { element ->
                    appendLine("  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
                }
            }
        }
    }
    
    /**
     * Determines if an error should be logged for debugging
     */
    fun shouldLogError(errorType: ErrorType): Boolean {
        return when (errorType) {
            ErrorType.NETWORK_UNAVAILABLE -> false // Common, don't spam logs
            ErrorType.API_RATE_LIMIT -> false // Expected behavior
            ErrorType.INSUFFICIENT_DATA -> false // Not really an error
            ErrorType.CACHE_EXPIRED -> false // Normal operation
            ErrorType.INVALID_INPUT -> true // Should be validated earlier
            ErrorType.UNKNOWN_ERROR -> true // Definitely should be logged
        }
    }
    
    /**
     * Gets appropriate retry delay based on error type
     */
    fun getRetryDelay(errorType: ErrorType, attemptNumber: Int): Long {
        return when (errorType) {
            ErrorType.NETWORK_UNAVAILABLE -> minOf(1000L * (1L shl attemptNumber), 30000L) // Exponential backoff, max 30s
            ErrorType.API_RATE_LIMIT -> minOf(5000L * (1L shl attemptNumber), 300000L) // Longer backoff for rate limits, max 5min
            ErrorType.CACHE_EXPIRED -> 1000L // Quick retry for cache issues
            ErrorType.UNKNOWN_ERROR -> minOf(2000L * (1L shl attemptNumber), 60000L) // Moderate backoff, max 1min
            else -> 1000L // Default 1 second
        }
    }
}