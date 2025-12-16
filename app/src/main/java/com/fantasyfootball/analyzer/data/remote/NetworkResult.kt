package com.fantasyfootball.analyzer.data.remote

/**
 * Sealed class representing the result of a network operation.
 * Provides type-safe handling of success, error, and loading states.
 */
sealed class NetworkResult<out T> {
    
    /**
     * Represents a successful network operation with data.
     */
    data class Success<T>(val data: T) : NetworkResult<T>()
    
    /**
     * Represents a failed network operation with error information.
     */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error occurred"
    ) : NetworkResult<Nothing>()
    
    /**
     * Represents an ongoing network operation.
     */
    object Loading : NetworkResult<Nothing>()
    
    /**
     * Returns true if this result represents a successful operation.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if this result represents a failed operation.
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Returns true if this result represents an ongoing operation.
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Returns the data if this is a Success result, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Returns the data if this is a Success result, or throws the exception if Error.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get data from loading state")
    }
}