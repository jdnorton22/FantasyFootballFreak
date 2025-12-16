package com.fantasyfootball.analyzer.data.remote

import android.util.Log
import com.fantasyfootball.analyzer.data.remote.dto.ErrorResponse
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility class for handling network operations and converting responses to NetworkResult.
 * Provides consistent error handling, rate limiting, and retry logic across all API calls.
 */
object NetworkHelper {
    
    private const val TAG = "NetworkHelper"
    private val rateLimitManager = RateLimitManager()
    
    /**
     * Safely executes a network call with rate limiting and retry logic.
     * Handles common network exceptions, HTTP error codes, and implements intelligent retry mechanisms.
     * 
     * @param requestId Unique identifier for request deduplication and tracking
     * @param priority Request priority for queue ordering (higher values processed first)
     * @param call The suspend function that makes the network call
     * @return NetworkResult wrapping the response data or error
     */
    suspend fun <T> safeApiCall(
        requestId: String,
        priority: Int = 0,
        call: suspend () -> Response<T>
    ): NetworkResult<T> {
        return rateLimitManager.queueRequest(requestId, priority) {
            executeNetworkCall(call)
        }
    }
    
    /**
     * Legacy method for backward compatibility. Uses a generated request ID.
     * 
     * @param call The suspend function that makes the network call
     * @return NetworkResult wrapping the response data or error
     */
    suspend fun <T> safeApiCall(call: suspend () -> Response<T>): NetworkResult<T> {
        val requestId = "legacy_${System.currentTimeMillis()}_${call.hashCode()}"
        return safeApiCall(requestId, 0, call)
    }
    
    /**
     * Executes the actual network call and handles response conversion.
     */
    private suspend fun <T> executeNetworkCall(call: suspend () -> Response<T>): NetworkResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let { data ->
                    NetworkResult.Success(data)
                } ?: NetworkResult.Error(
                    exception = IllegalStateException("Response body is null"),
                    message = "Empty response received from server"
                )
            } else {
                val errorMessage = parseErrorResponse(response)
                Log.w(TAG, "API call failed with code ${response.code()}: $errorMessage")
                
                // Check for rate limiting
                if (response.code() == 429) {
                    NetworkResult.Error(
                        exception = IOException("HTTP 429 - Rate Limited"),
                        message = "Too many requests. Please try again later."
                    )
                } else {
                    NetworkResult.Error(
                        exception = IOException("HTTP ${response.code()}"),
                        message = errorMessage
                    )
                }
            }
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network unavailable", e)
            NetworkResult.Error(
                exception = e,
                message = "No internet connection available"
            )
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timeout", e)
            NetworkResult.Error(
                exception = e,
                message = "Request timed out. Please try again."
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            NetworkResult.Error(
                exception = e,
                message = "Network error occurred. Please check your connection."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during API call", e)
            NetworkResult.Error(
                exception = e,
                message = "An unexpected error occurred: ${e.message}"
            )
        }
    }
    
    /**
     * Parses error response body to extract meaningful error message.
     * 
     * @param response The failed HTTP response
     * @return Human-readable error message
     */
    private fun <T> parseErrorResponse(response: Response<T>): String {
        return try {
            response.errorBody()?.string()?.let { errorBody ->
                val gson = Gson()
                val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                errorResponse.error.message
            } ?: getDefaultErrorMessage(response.code())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse error response", e)
            getDefaultErrorMessage(response.code())
        }
    }
    
    /**
     * Returns default error message based on HTTP status code.
     * 
     * @param code HTTP status code
     * @return Default error message for the status code
     */
    private fun getDefaultErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Invalid request. Please check your input."
            401 -> "Authentication required."
            403 -> "Access denied."
            404 -> "Requested data not found."
            429 -> "Too many requests. Please try again later."
            500 -> "Server error. Please try again later."
            502, 503, 504 -> "Service temporarily unavailable."
            else -> "Request failed with code $code"
        }
    }
    
    /**
     * Checks if the error is recoverable (temporary network issues).
     * 
     * @param result The NetworkResult.Error to check
     * @return True if the error might be resolved by retrying
     */
    fun isRecoverableError(result: NetworkResult.Error): Boolean {
        return when (result.exception) {
            is SocketTimeoutException,
            is UnknownHostException,
            is IOException -> true
            else -> false
        }
    }
    
    /**
     * Determines if the error indicates rate limiting.
     * 
     * @param result The NetworkResult.Error to check
     * @return True if the error is due to rate limiting
     */
    fun isRateLimitError(result: NetworkResult.Error): Boolean {
        return result.message.contains("Too many requests", ignoreCase = true) ||
                result.message.contains("rate limit", ignoreCase = true) ||
                result.message.contains("429", ignoreCase = true) ||
                result.exception.message?.contains("429") == true
    }
    
    /**
     * Gets the current queue size for monitoring purposes.
     */
    fun getQueueSize(): Int {
        return rateLimitManager.queueSize.value
    }
    
    /**
     * Checks if the rate limit manager is currently processing requests.
     */
    fun isProcessingRequests(): Boolean {
        return rateLimitManager.isProcessing.value
    }
    
    /**
     * Clears the request queue (useful for testing or emergency situations).
     */
    fun clearRequestQueue() {
        rateLimitManager.clearQueue()
    }
    
    /**
     * Shuts down the rate limit manager and cleans up resources.
     */
    fun shutdown() {
        rateLimitManager.shutdown()
    }
}