package com.fantasyfootball.analyzer.data.remote

import com.fantasyfootball.analyzer.data.remote.dto.PlayerResponse
import com.fantasyfootball.analyzer.data.remote.dto.ScheduleResponse
import com.fantasyfootball.analyzer.data.remote.dto.StatsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ESPN API service interface for retrieving fantasy football and NFL data.
 * Defines endpoints for player information, statistics, and team schedules.
 */
interface ESPNApiService {
    
    /**
     * Retrieves comprehensive player data including basic info and current season stats.
     * 
     * @param playerId The unique ESPN player identifier
     * @return Response containing player data with embedded statistics
     */
    @GET("fantasy/football/players/{playerId}")
    suspend fun getPlayerData(@Path("playerId") playerId: String): Response<PlayerResponse>
    
    /**
     * Retrieves detailed player statistics for a specific season.
     * 
     * @param playerId The unique ESPN player identifier
     * @param season The NFL season year (e.g., 2023)
     * @param week Optional specific week number (null for season totals)
     * @return Response containing detailed player statistics
     */
    @GET("football/nfl/players/{playerId}/stats")
    suspend fun getPlayerStats(
        @Path("playerId") playerId: String,
        @Query("season") season: Int,
        @Query("week") week: Int? = null
    ): Response<StatsResponse>
    
    /**
     * Retrieves team schedule information for matchup analysis.
     * 
     * @param teamId The unique ESPN team identifier
     * @param season The NFL season year (e.g., 2023)
     * @return Response containing team schedule with opponent information
     */
    @GET("football/nfl/teams/{teamId}/schedule")
    suspend fun getTeamSchedule(
        @Path("teamId") teamId: String,
        @Query("season") season: Int
    ): Response<ScheduleResponse>
    
    /**
     * Searches for players by name or partial name match.
     * 
     * @param query The search query (player name or partial name)
     * @param limit Maximum number of results to return (default 20)
     * @return Response containing list of matching players
     */
    @GET("fantasy/football/players/search")
    suspend fun searchPlayers(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<List<PlayerResponse>>
    
    /**
     * Retrieves historical matchup data for a player against specific opponents.
     * 
     * @param playerId The unique ESPN player identifier
     * @param opponentTeamId The opposing team identifier
     * @param seasons Number of previous seasons to include (default 3)
     * @return Response containing historical performance data
     */
    @GET("fantasy/football/players/{playerId}/matchups")
    suspend fun getPlayerMatchupHistory(
        @Path("playerId") playerId: String,
        @Query("opponent") opponentTeamId: String,
        @Query("seasons") seasons: Int = 3
    ): Response<List<StatsResponse>>
    
    /**
     * Retrieves all active NFL players for comprehensive search functionality.
     * 
     * @param limit Maximum number of players to return (default 2000)
     * @param active Whether to include only active players (default true)
     * @return Response containing list of all players
     */
    @GET("fantasy/football/players")
    suspend fun getAllPlayers(
        @Query("limit") limit: Int = 2000,
        @Query("active") active: Boolean = true
    ): Response<List<PlayerResponse>>
}