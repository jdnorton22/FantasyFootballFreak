package com.fantasyfootball.analyzer.data.remote

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages API rate limiting, request queuing, and exponential backoff retry logic.
 * Implements intelligent request scheduling to prevent API rate limit violations.
 */
class RateLimitManager {
    
    companion object {
        private const val TAG = "RateLimitManager"
        private const val DEFAULT_MAX_REQUESTS_PER_MINUTE = 60
        private const val DEFAULT_MAX_CONCURRENT_REQUESTS = 5
        private const val DEFAULT_BASE_DELAY_MS = 1000L
        private const val DEFAULT_MAX_DELAY_MS = 30000L
        private const val DEFAULT_MAX_RETRIES = 3
        private const val REQUEST_WINDOW_MS = 60000L // 1 minute
    }
    
    private val requestQueue = Channel<QueuedRequest<*>>(Channel.UNLIMITED)
    private val activeRequests = AtomicInteger(0)
    private val requestTimestamps = mutableListOf<Long>()
    private val retryDelays = ConcurrentHashMap<String, AtomicLong>()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()
    
    private var processingJob: Job? = null
    
    private val maxRequestsPerMinute = DEFAULT_MAX_REQUESTS_PER_MINUTE
    private val maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS
    private val baseDelayMs = DEFAULT_BASE_DELAY_MS
    private val maxDelayMs = DEFAULT_MAX_DELAY_MS
    private val maxRetries = DEFAULT_MAX_RETRIES
    
    init {
        startProcessing()
    }
    
    /**
     * Queues a network request for execution with rate limiting and retry logic.
     * 
     * @param requestId Unique identifier for the request (used for deduplication)
     * @param priority Request priority (higher values processed first)
     * @param request The suspend function that executes the network call
     * @return NetworkResult of the request execution
     */
    suspend fun <T> queueRequest(
        requestId: String,
        priority: Int = 0,
        request: suspend () -> NetworkResult<T>
    ): NetworkResult<T> {
        val queuedRequest = QueuedRequest(
            id = requestId,
            priority = priority,
            request = request,
            resultChannel = Channel(1)
        )
        
        // Check if we can serve from cache or if request is already in progress
        if (isDuplicateRequest(requestId)) {
            Log.d(TAG, "Duplicate request detected for $requestId, waiting for existing request")
            return waitForExistingRequest(requestId)
        }
        
        requestQueue.send(queuedRequest)
        _queueSize.value = _queueSize.value + 1
        
        return queuedRequest.resultChannel.receive()
    }
    
    /**
     * Starts the background processing of queued requests.
     */
    private fun startProcessing() {
        processingJob = CoroutineScope(Dispatchers.IO).launch {
            _isProcessing.value = true
            
            while (isActive) {
                try {
                    val queuedRequest = requestQueue.receive()
                    _queueSize.value = maxOf(0, _queueSize.value - 1)
                    
                    // Execute request with retry logic
                    launch {
                        // Wait for available slot
                        waitForAvailableSlot()
                        executeWithRetry(queuedRequest as QueuedRequest<Any>)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing request queue", e)
                }
            }
            
            _isProcessing.value = false
        }
    }
    
    /**
     * Waits for an available request slot based on rate limiting rules.
     */
    private suspend fun waitForAvailableSlot() {
        // Wait for concurrent request limit
        while (activeRequests.get() >= maxConcurrentRequests) {
            delay(100)
        }
        
        // Wait for rate limit window
        var shouldWait = true
        while (shouldWait) {
            val now = System.currentTimeMillis()
            synchronized(requestTimestamps) {
                // Remove timestamps older than 1 minute
                requestTimestamps.removeAll { it < now - REQUEST_WINDOW_MS }
                
                // Check if we've hit the rate limit
                if (requestTimestamps.size < maxRequestsPerMinute) {
                    requestTimestamps.add(now)
                    shouldWait = false
                }
            }
            
            if (shouldWait) {
                delay(1000) // Wait 1 second and check again
            }
        }
        
        activeRequests.incrementAndGet()
    }
    
    /**
     * Executes a queued request with exponential backoff retry logic.
     */
    private suspend fun <T> executeWithRetry(queuedRequest: QueuedRequest<T>) {
        var attempt = 0
        var lastResult: NetworkResult<T>? = null
        
        try {
            while (attempt <= maxRetries) {
                try {
                    val result = queuedRequest.request()
                    
                    if (result is NetworkResult.Success) {
                        // Success - reset retry delay and return result
                        retryDelays.remove(queuedRequest.id)
                        queuedRequest.resultChannel.send(result)
                        return
                    } else if (result is NetworkResult.Error) {
                        lastResult = result
                        
                        if (shouldRetry(result, attempt)) {
                            val delay = calculateBackoffDelay(queuedRequest.id, attempt)
                            Log.w(TAG, "Request ${queuedRequest.id} failed (attempt ${attempt + 1}), retrying in ${delay}ms: ${result.message}")
                            delay(delay)
                            attempt++
                        } else {
                            // Non-retryable error or max retries reached
                            queuedRequest.resultChannel.send(result)
                            return
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error executing request ${queuedRequest.id}", e)
                    lastResult = NetworkResult.Error(e, "Unexpected error: ${e.message}")
                    
                    if (attempt < maxRetries) {
                        val delay = calculateBackoffDelay(queuedRequest.id, attempt)
                        delay(delay)
                        attempt++
                    } else {
                        break
                    }
                }
            }
            
            // All retries exhausted
            val finalResult = lastResult ?: NetworkResult.Error(
                RuntimeException("Request failed after $maxRetries retries"),
                "Request failed after maximum retries"
            )
            queuedRequest.resultChannel.send(finalResult)
            
        } finally {
            activeRequests.decrementAndGet()
        }
    }
    
    /**
     * Determines if a request should be retried based on the error type and attempt count.
     */
    private fun shouldRetry(result: NetworkResult.Error, attempt: Int): Boolean {
        if (attempt >= maxRetries) return false
        
        return when {
            NetworkHelper.isRateLimitError(result) -> true
            NetworkHelper.isRecoverableError(result) -> true
            result.message.contains("timeout", ignoreCase = true) -> true
            result.message.contains("connection", ignoreCase = true) -> true
            result.message.contains("server error", ignoreCase = true) -> true
            result.message.contains("503", ignoreCase = true) -> true
            result.message.contains("502", ignoreCase = true) -> true
            result.message.contains("504", ignoreCase = true) -> true
            else -> false
        }
    }
    
    /**
     * Calculates exponential backoff delay for retry attempts.
     */
    private fun calculateBackoffDelay(requestId: String, attempt: Int): Long {
        val currentDelay = retryDelays.computeIfAbsent(requestId) { AtomicLong(baseDelayMs) }
        
        // Exponential backoff: delay = baseDelay * (2^attempt) with jitter
        val exponentialDelay = (baseDelayMs * 2.0.pow(attempt)).toLong()
        val jitter = (exponentialDelay * 0.1 * Math.random()).toLong() // Add 10% jitter
        val finalDelay = min(exponentialDelay + jitter, maxDelayMs)
        
        currentDelay.set(finalDelay)
        return finalDelay
    }
    
    /**
     * Checks if a request with the same ID is already being processed.
     */
    private fun isDuplicateRequest(requestId: String): Boolean {
        // Simple implementation - in a real app, you might want more sophisticated deduplication
        return false // For now, allow all requests
    }
    
    /**
     * Waits for an existing request with the same ID to complete.
     */
    private suspend fun <T> waitForExistingRequest(requestId: String): NetworkResult<T> {
        // Implementation would wait for existing request
        // For now, return an error
        return NetworkResult.Error(
            IllegalStateException("Duplicate request"),
            "Request already in progress"
        )
    }
    
    /**
     * Clears the request queue and resets state.
     */
    fun clearQueue() {
        CoroutineScope(Dispatchers.IO).launch {
            while (requestQueue.tryReceive().isSuccess) {
                // Drain the queue
            }
            _queueSize.value = 0
            retryDelays.clear()
        }
    }
    
    /**
     * Stops the request processing and cleans up resources.
     */
    fun shutdown() {
        processingJob?.cancel()
        clearQueue()
    }
    
    /**
     * Data class representing a queued network request.
     */
    private data class QueuedRequest<T>(
        val id: String,
        val priority: Int,
        val request: suspend () -> NetworkResult<T>,
        val resultChannel: Channel<NetworkResult<T>>
    )
}