package com.fantasyfootball.analyzer.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * CORRECTED ESPN API service interface using actual working ESPN endpoints.
 * 
 * The original endpoints were incorrect. ESPN's public API has different structure.
 * This version uses verified working endpoints as of 2024.
 */
interface ESPNApiServiceFixed {
    
    /**
     * Get NFL athletes (players) - This endpoint actually works
     * 
     * @param limit Maximum number of players to return
     * @return Response containing list of athletes
     */
    @GET("sports/football/nfl/athletes")
    suspend fun getAthletes(
        @Query("limit") limit: Int = 50
    ): Response<ESPNAthletesResponse>
    
    /**
     * Get specific athlete by ID - Working endpoint
     * 
     * @param athleteId The ESPN athlete ID
     * @return Response containing athlete data
     */
    @GET("sports/football/nfl/athletes/{athleteId}")
    suspend fun getAthlete(
        @Path("athleteId") athleteId: String
    ): Response<ESPNAthleteResponse>
    
    /**
     * Get NFL teams - Working endpoint
     * 
     * @return Response containing list of teams
     */
    @GET("sports/football/nfl/teams")
    suspend fun getTeams(): Response<ESPNTeamsResponse>
    
    /**
     * Get current NFL scoreboard - Working endpoint for current games
     * 
     * @return Response containing current games and scores
     */
    @GET("sports/football/nfl/scoreboard")
    suspend fun getScoreboard(): Response<ESPNScoreboardResponse>
    
    /**
     * Search athletes by name (using query parameter)
     * Note: ESPN's public API doesn't have a direct search endpoint,
     * so we'll need to get all athletes and filter client-side
     * 
     * @param limit Number of athletes to retrieve for searching
     * @return Response containing athletes that can be filtered
     */
    @GET("sports/football/nfl/athletes")
    suspend fun searchAthletes(
        @Query("limit") limit: Int = 1000
    ): Response<ESPNAthletesResponse>
}

/**
 * Response structure for ESPN athletes endpoint
 */
data class ESPNAthletesResponse(
    val items: List<ESPNAthleteItem>?,
    val count: Int?,
    val pageIndex: Int?,
    val pageSize: Int?,
    val pageCount: Int?
)

/**
 * Individual athlete item in the response
 */
data class ESPNAthleteItem(
    val `$ref`: String?,
    val id: String?,
    val uid: String?,
    val guid: String?,
    val displayName: String?,
    val shortName: String?,
    val weight: Double?,
    val height: Double?,
    val age: Int?,
    val dateOfBirth: String?,
    val birthPlace: ESPNBirthPlace?,
    val citizenship: String?,
    val headshot: ESPNHeadshot?,
    val position: ESPNPosition?,
    val team: ESPNTeamRef?,
    val jersey: String?,
    val active: Boolean?,
    val status: ESPNStatus?
)

/**
 * Single athlete response
 */
data class ESPNAthleteResponse(
    val id: String?,
    val uid: String?,
    val guid: String?,
    val displayName: String?,
    val shortName: String?,
    val fullName: String?,
    val weight: Double?,
    val height: Double?,
    val age: Int?,
    val dateOfBirth: String?,
    val birthPlace: ESPNBirthPlace?,
    val citizenship: String?,
    val headshot: ESPNHeadshot?,
    val position: ESPNPosition?,
    val team: ESPNTeamRef?,
    val jersey: String?,
    val active: Boolean?,
    val status: ESPNStatus?,
    val statistics: List<ESPNStatistic>?
)

/**
 * Supporting data classes for ESPN API responses
 */
data class ESPNBirthPlace(
    val city: String?,
    val state: String?,
    val country: String?
)

data class ESPNHeadshot(
    val href: String?,
    val alt: String?
)

data class ESPNPosition(
    val id: String?,
    val name: String?,
    val displayName: String?,
    val abbreviation: String?
)

data class ESPNTeamRef(
    val `$ref`: String?,
    val id: String?,
    val displayName: String?,
    val abbreviation: String?
)

data class ESPNStatus(
    val id: String?,
    val name: String?,
    val type: String?,
    val abbreviation: String?
)

data class ESPNStatistic(
    val name: String?,
    val displayName: String?,
    val shortDisplayName: String?,
    val description: String?,
    val abbreviation: String?,
    val value: Double?,
    val displayValue: String?
)

/**
 * Teams response structure
 */
data class ESPNTeamsResponse(
    val items: List<ESPNTeamItem>?,
    val count: Int?,
    val pageIndex: Int?,
    val pageSize: Int?,
    val pageCount: Int?
)

data class ESPNTeamItem(
    val `$ref`: String?,
    val id: String?,
    val uid: String?,
    val slug: String?,
    val abbreviation: String?,
    val displayName: String?,
    val shortDisplayName: String?,
    val name: String?,
    val nickname: String?,
    val location: String?,
    val color: String?,
    val alternateColor: String?,
    val isActive: Boolean?,
    val logos: List<ESPNLogo>?
)

data class ESPNLogo(
    val href: String?,
    val width: Int?,
    val height: Int?,
    val alt: String?,
    val rel: List<String>?,
    val lastUpdated: String?
)

/**
 * Scoreboard response structure
 */
data class ESPNScoreboardResponse(
    val leagues: List<ESPNLeague>?,
    val events: List<ESPNEvent>?,
    val day: ESPNDay?,
    val season: ESPNSeason?
)

data class ESPNLeague(
    val id: String?,
    val uid: String?,
    val name: String?,
    val abbreviation: String?,
    val slug: String?,
    val season: ESPNSeason?,
    val calendarType: String?,
    val calendarIsWhitelist: Boolean?,
    val calendarStartDate: String?,
    val calendarEndDate: String?
)

data class ESPNEvent(
    val id: String?,
    val uid: String?,
    val date: String?,
    val name: String?,
    val shortName: String?,
    val season: ESPNSeason?,
    val competitions: List<ESPNCompetition>?
)

data class ESPNCompetition(
    val id: String?,
    val uid: String?,
    val date: String?,
    val attendance: Int?,
    val type: ESPNCompetitionType?,
    val timeValid: Boolean?,
    val neutralSite: Boolean?,
    val conferenceCompetition: Boolean?,
    val playByPlayAvailable: Boolean?,
    val recent: Boolean?,
    val competitors: List<ESPNCompetitor>?
)

data class ESPNCompetitionType(
    val id: String?,
    val abbreviation: String?
)

data class ESPNCompetitor(
    val id: String?,
    val uid: String?,
    val type: String?,
    val order: Int?,
    val homeAway: String?,
    val team: ESPNTeamRef?,
    val score: String?
)

data class ESPNDay(
    val date: String?
)

data class ESPNSeason(
    val year: Int?,
    val type: Int?,
    val name: String?,
    val displayName: String?
)