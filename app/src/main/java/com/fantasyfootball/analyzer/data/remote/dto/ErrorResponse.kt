package com.fantasyfootball.analyzer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for ESPN API error responses.
 * Standardizes error handling across all API endpoints.
 */
data class ErrorResponse(
    @SerializedName("error")
    val error: ErrorDetails,
    
    @SerializedName("timestamp")
    val timestamp: String?,
    
    @SerializedName("path")
    val path: String?
)

/**
 * DTO containing detailed error information.
 */
data class ErrorDetails(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("details")
    val details: String?
)