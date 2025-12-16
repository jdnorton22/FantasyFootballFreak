package com.fantasyfootball.analyzer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for ESPN API statistics response.
 * Represents detailed player statistics for a specific season or game.
 */
data class StatsResponse(
    @SerializedName("playerId")
    val playerId: String,
    
    @SerializedName("season")
    val season: Int,
    
    @SerializedName("week")
    val week: Int?,
    
    @SerializedName("gameDate")
    val gameDate: String?,
    
    @SerializedName("opponent")
    val opponent: String?,
    
    @SerializedName("fantasyPoints")
    val fantasyPoints: Double,
    
    @SerializedName("rushingYards")
    val rushingYards: Int,
    
    @SerializedName("passingYards")
    val passingYards: Int,
    
    @SerializedName("receivingYards")
    val receivingYards: Int,
    
    @SerializedName("rushingTouchdowns")
    val rushingTouchdowns: Int,
    
    @SerializedName("passingTouchdowns")
    val passingTouchdowns: Int,
    
    @SerializedName("receivingTouchdowns")
    val receivingTouchdowns: Int,
    
    @SerializedName("totalTouchdowns")
    val totalTouchdowns: Int,
    
    @SerializedName("fumbles")
    val fumbles: Int?,
    
    @SerializedName("interceptions")
    val interceptions: Int?,
    
    @SerializedName("receptions")
    val receptions: Int?,
    
    @SerializedName("targets")
    val targets: Int?
)