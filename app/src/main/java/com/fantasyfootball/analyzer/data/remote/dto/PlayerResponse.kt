package com.fantasyfootball.analyzer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for ESPN API player response.
 * Maps to the structure returned by ESPN's fantasy football API.
 */
data class PlayerResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("fullName")
    val fullName: String,
    
    @SerializedName("firstName")
    val firstName: String?,
    
    @SerializedName("lastName")
    val lastName: String?,
    
    @SerializedName("defaultPositionId")
    val defaultPositionId: Int,
    
    @SerializedName("eligibleSlots")
    val eligibleSlots: List<Int>?,
    
    @SerializedName("proTeamId")
    val proTeamId: Int,
    
    @SerializedName("injured")
    val injured: Boolean,
    
    @SerializedName("injuryStatus")
    val injuryStatus: String?,
    
    @SerializedName("active")
    val active: Boolean,
    
    @SerializedName("stats")
    val stats: List<PlayerStatsDto>?
)

/**
 * DTO for player statistics within the player response.
 */
data class PlayerStatsDto(
    @SerializedName("seasonId")
    val seasonId: Int,
    
    @SerializedName("scoringPeriodId")
    val scoringPeriodId: Int?,
    
    @SerializedName("stats")
    val stats: Map<String, Double>?,
    
    @SerializedName("appliedTotal")
    val appliedTotal: Double?
)