package com.fantasyfootball.analyzer.domain.model

import com.fantasyfootball.analyzer.data.local.entity.Player

/**
 * Domain model representing a player recommendation for weekly lineup decisions.
 * Contains analysis-based projections and reasoning for the recommendation.
 * 
 * Requirements addressed:
 * - 4.1: Weekly recommendations based on upcoming matchups
 * - 4.2: Ranking based on historical performance
 * - 4.3: Projected fantasy points
 * - 4.4: Tie-breaking with consistency metrics
 * - 4.5: Injury status impact on recommendations
 */
data class PlayerRecommendation(
    val player: Player,
    val projectedPoints: Double,
    val matchupRating: MatchupRating,
    val confidenceLevel: Double,
    val reasoning: String,
    val consistencyScore: Double,
    val injuryImpact: InjuryImpact,
    val rank: Int
)

/**
 * Rating system for matchup favorability.
 */
enum class MatchupRating {
    EXCELLENT,
    GOOD,
    AVERAGE,
    POOR,
    AVOID
}

/**
 * Impact of injury status on recommendation.
 */
enum class InjuryImpact {
    NONE,
    MINOR,
    MODERATE,
    MAJOR,
    OUT
}