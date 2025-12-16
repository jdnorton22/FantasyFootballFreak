package com.fantasyfootball.analyzer.domain.usecase

import com.fantasyfootball.analyzer.domain.model.MatchupAnalysis
import com.fantasyfootball.analyzer.domain.model.PlayerRecommendation
import com.fantasyfootball.analyzer.data.remote.NetworkResult

/**
 * Interface for analyzing player matchups and generating recommendations.
 * Provides methods for historical analysis and projection calculations.
 * 
 * Requirements addressed:
 * - 2.2, 2.3, 2.5: Historical matchup analysis and comparisons
 * - 4.1, 4.2, 4.3, 4.4, 4.5: Weekly recommendations and rankings
 */
interface MatchupAnalyzer {
    
    /**
     * Analyzes a player's historical performance against a specific opponent.
     * Calculates averages, trends, and projections based on historical data.
     * 
     * @param playerId The unique player identifier
     * @param opponentTeam The opposing team name
     * @return NetworkResult containing matchup analysis or error
     */
    suspend fun analyzeMatchup(playerId: String, opponentTeam: String): NetworkResult<MatchupAnalysis>
    
    /**
     * Generates weekly recommendations for a list of roster players.
     * Ranks players based on upcoming matchups and historical performance.
     * 
     * @param rosterPlayerIds List of player IDs to analyze
     * @return NetworkResult containing ranked recommendations or error
     */
    suspend fun generateWeeklyRecommendations(rosterPlayerIds: List<String>): NetworkResult<List<PlayerRecommendation>>
    
    /**
     * Calculates projected fantasy points for a player against a specific opponent.
     * Uses historical data and current season performance for projections.
     * 
     * @param playerId The unique player identifier
     * @param opponentTeam The opposing team name
     * @return NetworkResult containing projected points or error
     */
    suspend fun calculateProjectedPoints(playerId: String, opponentTeam: String): NetworkResult<Double>
    
    /**
     * Calculates a player's season average fantasy points.
     * Used for comparison against matchup-specific performance.
     * 
     * @param playerId The unique player identifier
     * @param season The NFL season year
     * @return NetworkResult containing season average or error
     */
    suspend fun calculateSeasonAverage(playerId: String, season: Int): NetworkResult<Double>
}