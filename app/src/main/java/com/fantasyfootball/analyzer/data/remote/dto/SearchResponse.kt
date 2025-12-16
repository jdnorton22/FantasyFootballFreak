package com.fantasyfootball.analyzer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for ESPN API search response wrapper.
 * Wraps search results with metadata about the search operation.
 */
data class SearchResponse(
    @SerializedName("results")
    val results: List<PlayerResponse>,
    
    @SerializedName("totalResults")
    val totalResults: Int,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("pageSize")
    val pageSize: Int,
    
    @SerializedName("hasMore")
    val hasMore: Boolean
)