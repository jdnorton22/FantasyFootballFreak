package com.fantasyfootball.analyzer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for ESPN API team schedule response.
 * Used to retrieve matchup information and opponent data.
 */
data class ScheduleResponse(
    @SerializedName("teamId")
    val teamId: String,
    
    @SerializedName("season")
    val season: Int,
    
    @SerializedName("events")
    val events: List<GameEventDto>
)

/**
 * DTO representing a single game event in the schedule.
 */
data class GameEventDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("week")
    val week: Int,
    
    @SerializedName("season")
    val season: Int,
    
    @SerializedName("competitions")
    val competitions: List<CompetitionDto>
)

/**
 * DTO representing competition details within a game event.
 */
data class CompetitionDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("competitors")
    val competitors: List<CompetitorDto>,
    
    @SerializedName("status")
    val status: GameStatusDto?
)

/**
 * DTO representing a team competitor in a game.
 */
data class CompetitorDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("team")
    val team: TeamDto,
    
    @SerializedName("homeAway")
    val homeAway: String,
    
    @SerializedName("score")
    val score: String?
)

/**
 * DTO representing team information.
 */
data class TeamDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("abbreviation")
    val abbreviation: String,
    
    @SerializedName("displayName")
    val displayName: String,
    
    @SerializedName("shortDisplayName")
    val shortDisplayName: String
)

/**
 * DTO representing game status information.
 */
data class GameStatusDto(
    @SerializedName("type")
    val type: GameStatusTypeDto
)

/**
 * DTO representing game status type details.
 */
data class GameStatusTypeDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("state")
    val state: String,
    
    @SerializedName("completed")
    val completed: Boolean
)