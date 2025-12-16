package com.fantasyfootball.analyzer.domain.model

import com.fantasyfootball.analyzer.data.local.entity.MatchupData

/**
 * Domain model representing the analysis of a player's historical performance against a specific opponent.
 * Contains calculated metrics and projections based on historical matchup data.
 * 
 * Requirements addressed:
 * - 2.2: Average fantasy points against specific opponents
 * - 2.3: Game-by-game performance breakdown
 * - 2.5: Comparison to player's season average
 */
data class MatchupAnalysis(
    val playerId: String,
    val playerName: String,
    val opponentTeam: String,
    val averageFantasyPoints: Double,
    val historicalGames: List<MatchupData>,
    val projectedPoints: Double,
    val confidenceLevel: Double,
    val comparisonToSeasonAverage: Double,
    val performanceTrend: PerformanceTrend,
    val sampleSize: Int
)

/**
 * Represents the trend in player performance against the opponent over time.
 */
enum class PerformanceTrend {
    IMPROVING,
    DECLINING,
    STABLE,
    INSUFFICIENT_DATA
}