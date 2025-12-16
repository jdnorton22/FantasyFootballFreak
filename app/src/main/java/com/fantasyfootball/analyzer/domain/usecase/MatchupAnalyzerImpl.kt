package com.fantasyfootball.analyzer.domain.usecase

import com.fantasyfootball.analyzer.domain.model.MatchupAnalysis
import com.fantasyfootball.analyzer.domain.model.PlayerRecommendation
import com.fantasyfootball.analyzer.domain.model.MatchupRating
import com.fantasyfootball.analyzer.domain.model.InjuryImpact
import com.fantasyfootball.analyzer.domain.model.PerformanceTrend
import com.fantasyfootball.analyzer.domain.repository.PlayerRepository
import com.fantasyfootball.analyzer.data.remote.NetworkResult
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.data.local.entity.Player
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Implementation of MatchupAnalyzer that provides historical analysis and recommendations.
 * Uses repository pattern to access player data and implements algorithms for
 * calculating projections and performance metrics.
 * 
 * Requirements addressed:
 * - 2.2: Calculate average fantasy points against specific opponents
 * - 2.3: Provide game-by-game performance breakdown
 * - 2.5: Compare player performance vs season averages
 * - 4.1-4.5: Generate weekly recommendations with proper ranking and injury consideration
 */
@Singleton
class MatchupAnalyzerImpl @Inject constructor(
    private val playerRepository: PlayerRepository
) : MatchupAnalyzer {

    companion object {
        private const val MINIMUM_GAMES_FOR_ANALYSIS = 2
        private const val CONFIDENCE_THRESHOLD_GAMES = 5
        private const val CURRENT_SEASON = 2024
        private const val HISTORICAL_SEASONS = 3
    }

    override suspend fun analyzeMatchup(playerId: String, opponentTeam: String): NetworkResult<MatchupAnalysis> {
        return try {
            // Get historical matchup data
            val matchupResult = playerRepository.getMatchupHistory(playerId, opponentTeam)
            if (matchupResult !is NetworkResult.Success) {
                return NetworkResult.Error(Exception("Failed to retrieve matchup history: ${matchupResult}"))
            }

            val historicalGames = matchupResult.data
            if (historicalGames.isEmpty()) {
                return createEmptyMatchupAnalysis(playerId, opponentTeam)
            }

            // Get player info for name
            val playerResult = playerRepository.getPlayer(playerId)
            val playerName = if (playerResult is NetworkResult.Success) {
                playerResult.data.name
            } else {
                "Unknown Player"
            }

            // Calculate season average for comparison
            val seasonAverageResult = calculateSeasonAverage(playerId, CURRENT_SEASON)
            val seasonAverage = if (seasonAverageResult is NetworkResult.Success) {
                seasonAverageResult.data
            } else {
                0.0
            }

            // Perform analysis calculations
            val averageFantasyPoints = historicalGames.map { it.fantasyPoints }.average()
            val projectedPoints = calculateProjectedPointsFromHistory(historicalGames, seasonAverage)
            val confidenceLevel = calculateConfidenceLevel(historicalGames.size)
            val comparisonToSeasonAverage = if (seasonAverage > 0) {
                ((averageFantasyPoints - seasonAverage) / seasonAverage) * 100
            } else {
                0.0
            }
            val performanceTrend = calculatePerformanceTrend(historicalGames)

            val analysis = MatchupAnalysis(
                playerId = playerId,
                playerName = playerName,
                opponentTeam = opponentTeam,
                averageFantasyPoints = averageFantasyPoints,
                historicalGames = historicalGames.sortedByDescending { it.gameDate },
                projectedPoints = projectedPoints,
                confidenceLevel = confidenceLevel,
                comparisonToSeasonAverage = comparisonToSeasonAverage,
                performanceTrend = performanceTrend,
                sampleSize = historicalGames.size
            )

            NetworkResult.Success(analysis)
        } catch (e: Exception) {
            NetworkResult.Error(e)
        }
    }

    override suspend fun generateWeeklyRecommendations(rosterPlayerIds: List<String>): NetworkResult<List<PlayerRecommendation>> {
        return try {
            coroutineScope {
                val recommendations = rosterPlayerIds.map { playerId ->
                    async {
                        generatePlayerRecommendation(playerId)
                    }
                }.awaitAll().filterNotNull()

                // Sort by projected points descending, then by consistency score
                val rankedRecommendations = recommendations
                    .sortedWith(compareByDescending<PlayerRecommendation> { it.projectedPoints }
                        .thenByDescending { it.consistencyScore })
                    .mapIndexed { index, recommendation ->
                        recommendation.copy(rank = index + 1)
                    }

                NetworkResult.Success(rankedRecommendations)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e)
        }
    }

    override suspend fun calculateProjectedPoints(playerId: String, opponentTeam: String): NetworkResult<Double> {
        return try {
            val matchupResult = playerRepository.getMatchupHistory(playerId, opponentTeam)
            if (matchupResult !is NetworkResult.Success) {
                return NetworkResult.Error(Exception("Failed to retrieve matchup history"))
            }

            val historicalGames = matchupResult.data
            if (historicalGames.isEmpty()) {
                // Fallback to season average if no matchup history
                return calculateSeasonAverage(playerId, CURRENT_SEASON)
            }

            val seasonAverageResult = calculateSeasonAverage(playerId, CURRENT_SEASON)
            val seasonAverage = if (seasonAverageResult is NetworkResult.Success) {
                seasonAverageResult.data
            } else {
                0.0
            }

            val projectedPoints = calculateProjectedPointsFromHistory(historicalGames, seasonAverage)
            NetworkResult.Success(projectedPoints)
        } catch (e: Exception) {
            NetworkResult.Error(e)
        }
    }

    override suspend fun calculateSeasonAverage(playerId: String, season: Int): NetworkResult<Double> {
        return try {
            val statsResult = playerRepository.getPlayerStats(playerId, season)
            if (statsResult !is NetworkResult.Success) {
                return NetworkResult.Error(Exception("Failed to retrieve player stats"))
            }

            val stats = statsResult.data
            if (stats.isEmpty()) {
                return NetworkResult.Success(0.0)
            }

            val average = stats.map { it.fantasyPoints }.average()
            NetworkResult.Success(average)
        } catch (e: Exception) {
            NetworkResult.Error(e)
        }
    }

    private suspend fun generatePlayerRecommendation(playerId: String): PlayerRecommendation? {
        return try {
            val playerResult = playerRepository.getPlayer(playerId)
            if (playerResult !is NetworkResult.Success) {
                return null
            }

            val player = playerResult.data
            
            // For this implementation, we'll use a placeholder opponent
            // In a real app, this would come from upcoming schedule data
            val projectedPointsResult = calculateSeasonAverage(playerId, CURRENT_SEASON)
            val projectedPoints = if (projectedPointsResult is NetworkResult.Success) {
                projectedPointsResult.data
            } else {
                0.0
            }

            val consistencyScore = calculateConsistencyScore(playerId)
            val matchupRating = calculateMatchupRating(projectedPoints)
            val injuryImpact = determineInjuryImpact(player.injuryStatus)
            val reasoning = generateRecommendationReasoning(projectedPoints, consistencyScore, injuryImpact)

            PlayerRecommendation(
                player = player,
                projectedPoints = projectedPoints,
                matchupRating = matchupRating,
                confidenceLevel = calculateConfidenceLevel(5), // Default confidence
                reasoning = reasoning,
                consistencyScore = consistencyScore,
                injuryImpact = injuryImpact,
                rank = 0 // Will be set during sorting
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun createEmptyMatchupAnalysis(playerId: String, opponentTeam: String): NetworkResult<MatchupAnalysis> {
        val playerResult = playerRepository.getPlayer(playerId)
        val playerName = if (playerResult is NetworkResult.Success) {
            playerResult.data.name
        } else {
            "Unknown Player"
        }

        val analysis = MatchupAnalysis(
            playerId = playerId,
            playerName = playerName,
            opponentTeam = opponentTeam,
            averageFantasyPoints = 0.0,
            historicalGames = emptyList(),
            projectedPoints = 0.0,
            confidenceLevel = 0.0,
            comparisonToSeasonAverage = 0.0,
            performanceTrend = PerformanceTrend.INSUFFICIENT_DATA,
            sampleSize = 0
        )

        return NetworkResult.Success(analysis)
    }

    private fun calculateProjectedPointsFromHistory(historicalGames: List<MatchupData>, seasonAverage: Double): Double {
        if (historicalGames.isEmpty()) return seasonAverage

        val recentGames = historicalGames.sortedByDescending { it.gameDate }.take(5)
        val recentAverage = recentGames.map { it.fantasyPoints }.average()
        val historicalAverage = historicalGames.map { it.fantasyPoints }.average()

        // Weight recent games more heavily, but consider historical average
        val recentWeight = 0.6
        val historicalWeight = 0.4

        return (recentAverage * recentWeight) + (historicalAverage * historicalWeight)
    }

    private fun calculateConfidenceLevel(sampleSize: Int): Double {
        return when {
            sampleSize >= CONFIDENCE_THRESHOLD_GAMES -> 0.9
            sampleSize >= MINIMUM_GAMES_FOR_ANALYSIS -> 0.6
            else -> 0.3
        }
    }

    private fun calculatePerformanceTrend(historicalGames: List<MatchupData>): PerformanceTrend {
        if (historicalGames.size < MINIMUM_GAMES_FOR_ANALYSIS) {
            return PerformanceTrend.INSUFFICIENT_DATA
        }

        val sortedGames = historicalGames.sortedBy { it.gameDate }
        val firstHalf = sortedGames.take(sortedGames.size / 2)
        val secondHalf = sortedGames.drop(sortedGames.size / 2)

        val firstHalfAverage = firstHalf.map { it.fantasyPoints }.average()
        val secondHalfAverage = secondHalf.map { it.fantasyPoints }.average()

        val difference = secondHalfAverage - firstHalfAverage
        val threshold = firstHalfAverage * 0.1 // 10% threshold

        return when {
            difference > threshold -> PerformanceTrend.IMPROVING
            difference < -threshold -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }

    private suspend fun calculateConsistencyScore(playerId: String): Double {
        return try {
            val statsResult = playerRepository.getPlayerStats(playerId, CURRENT_SEASON)
            if (statsResult !is NetworkResult.Success) {
                return 0.5 // Default consistency
            }

            val stats = statsResult.data
            if (stats.size < 2) {
                return 0.5
            }

            val points = stats.map { it.fantasyPoints }
            val mean = points.average()
            val variance = points.map { (it - mean) * (it - mean) }.average()
            val standardDeviation = sqrt(variance)

            // Lower standard deviation relative to mean indicates higher consistency
            val coefficientOfVariation = if (mean > 0) standardDeviation / mean else 1.0
            
            // Convert to 0-1 scale where 1 is most consistent
            (1.0 - coefficientOfVariation.coerceIn(0.0, 1.0))
        } catch (e: Exception) {
            0.5
        }
    }

    private fun calculateMatchupRating(projectedPoints: Double): MatchupRating {
        return when {
            projectedPoints >= 20.0 -> MatchupRating.EXCELLENT
            projectedPoints >= 15.0 -> MatchupRating.GOOD
            projectedPoints >= 10.0 -> MatchupRating.AVERAGE
            projectedPoints >= 5.0 -> MatchupRating.POOR
            else -> MatchupRating.AVOID
        }
    }

    private fun determineInjuryImpact(injuryStatus: String?): InjuryImpact {
        return when (injuryStatus?.lowercase()) {
            null, "healthy", "" -> InjuryImpact.NONE
            "questionable", "probable" -> InjuryImpact.MINOR
            "doubtful" -> InjuryImpact.MODERATE
            "out", "injured reserve", "ir" -> InjuryImpact.OUT
            else -> InjuryImpact.MINOR
        }
    }

    private fun generateRecommendationReasoning(
        projectedPoints: Double,
        consistencyScore: Double,
        injuryImpact: InjuryImpact
    ): String {
        val pointsDescription = when {
            projectedPoints >= 20.0 -> "excellent projected performance"
            projectedPoints >= 15.0 -> "strong projected performance"
            projectedPoints >= 10.0 -> "average projected performance"
            else -> "below-average projected performance"
        }

        val consistencyDescription = when {
            consistencyScore >= 0.8 -> "very consistent"
            consistencyScore >= 0.6 -> "moderately consistent"
            else -> "inconsistent"
        }

        val injuryNote = when (injuryImpact) {
            InjuryImpact.NONE -> ""
            InjuryImpact.MINOR -> " (minor injury concern)"
            InjuryImpact.MODERATE -> " (significant injury risk)"
            InjuryImpact.MAJOR -> " (major injury concern)"
            InjuryImpact.OUT -> " (currently injured)"
        }

        return "$pointsDescription with $consistencyDescription recent form$injuryNote"
    }
}