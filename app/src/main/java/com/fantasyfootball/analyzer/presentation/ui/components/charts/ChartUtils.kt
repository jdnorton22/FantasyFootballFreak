package com.fantasyfootball.analyzer.presentation.ui.components.charts

import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utility functions for chart data processing and calculations.
 * Provides common statistical calculations and data transformations for charts.
 * 
 * Requirements addressed:
 * - 5.1: Statistical data visualization support utilities
 */
object ChartUtils {
    
    /**
     * Calculates consistency score based on standard deviation.
     * Lower standard deviation = higher consistency score.
     */
    fun calculateConsistencyScore(values: List<Double>): Double {
        if (values.isEmpty() || values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        
        // Convert to 0-10 scale where 10 is most consistent
        val coefficientOfVariation = if (mean > 0) standardDeviation / mean else 1.0
        return maxOf(0.0, 10.0 - (coefficientOfVariation * 10.0))
    }
    
    /**
     * Normalizes values to 0-1 range for chart display.
     */
    fun normalizeValues(values: List<Double>): List<Double> {
        if (values.isEmpty()) return emptyList()
        
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        val range = max - min
        
        return if (range > 0) {
            values.map { (it - min) / range }
        } else {
            values.map { 0.5 } // All values are the same
        }
    }
    
    /**
     * Calculates moving average for trend smoothing.
     */
    fun calculateMovingAverage(values: List<Double>, windowSize: Int): List<Double> {
        if (values.size < windowSize) return values
        
        val result = mutableListOf<Double>()
        for (i in 0 until values.size - windowSize + 1) {
            val window = values.subList(i, i + windowSize)
            result.add(window.average())
        }
        return result
    }
    
    /**
     * Creates ComparisonStats from PlayerStats list.
     */
    fun createComparisonStats(playerStats: List<PlayerStats>): ComparisonStats {
        if (playerStats.isEmpty()) {
            return ComparisonStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val fantasyPoints = playerStats.map { it.fantasyPoints }.average()
        val rushingYards = playerStats.map { it.rushingYards.toDouble() }.average()
        val passingYards = playerStats.map { it.passingYards.toDouble() }.average()
        val receivingYards = playerStats.map { it.receivingYards.toDouble() }.average()
        val touchdowns = playerStats.map { it.touchdowns.toDouble() }.average()
        val consistency = calculateConsistencyScore(playerStats.map { it.fantasyPoints })
        
        return ComparisonStats(
            fantasyPoints = fantasyPoints,
            rushingYards = rushingYards,
            passingYards = passingYards,
            receivingYards = receivingYards,
            touchdowns = touchdowns,
            consistency = consistency
        )
    }
    
    /**
     * Creates ComparisonStats from MatchupData list.
     */
    fun createComparisonStatsFromMatchups(matchupData: List<MatchupData>): ComparisonStats {
        if (matchupData.isEmpty()) {
            return ComparisonStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val fantasyPoints = matchupData.map { it.fantasyPoints }.average()
        val consistency = calculateConsistencyScore(matchupData.map { it.fantasyPoints })
        
        // For matchup data, we don't have detailed stats, so we'll estimate based on fantasy points
        val estimatedRushingYards = fantasyPoints * 6.5 // Rough estimation
        val estimatedPassingYards = fantasyPoints * 18.0
        val estimatedReceivingYards = fantasyPoints * 4.5
        val estimatedTouchdowns = fantasyPoints / 6.0
        
        return ComparisonStats(
            fantasyPoints = fantasyPoints,
            rushingYards = estimatedRushingYards,
            passingYards = estimatedPassingYards,
            receivingYards = estimatedReceivingYards,
            touchdowns = estimatedTouchdowns,
            consistency = consistency
        )
    }
    
    /**
     * Determines chart color based on performance level.
     */
    fun getPerformanceColor(value: Double, average: Double): androidx.compose.ui.graphics.Color {
        val ratio = if (average > 0) value / average else 1.0
        
        return when {
            ratio >= 1.2 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Excellent - Green
            ratio >= 1.1 -> androidx.compose.ui.graphics.Color(0xFF8BC34A) // Good - Light Green
            ratio >= 0.9 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Average - Orange
            ratio >= 0.8 -> androidx.compose.ui.graphics.Color(0xFFFF5722) // Below Average - Red Orange
            else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Poor - Red
        }
    }
    
    /**
     * Formats fantasy points for display.
     */
    fun formatFantasyPoints(points: Double): String {
        return String.format("%.1f", points)
    }
    
    /**
     * Formats percentage values for display.
     */
    fun formatPercentage(value: Double): String {
        return String.format("%.1f%%", value * 100)
    }
    
    /**
     * Calculates trend direction from a list of values.
     */
    fun calculateTrend(values: List<Double>): TrendDirection {
        if (values.size < 2) return TrendDirection.STABLE
        
        val firstHalf = values.take(values.size / 2).average()
        val secondHalf = values.drop(values.size / 2).average()
        
        val difference = secondHalf - firstHalf
        val threshold = firstHalf * 0.1 // 10% threshold
        
        return when {
            difference > threshold -> TrendDirection.IMPROVING
            difference < -threshold -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }
    
    enum class TrendDirection {
        IMPROVING,
        DECLINING,
        STABLE
    }
}

/**
 * Extension functions for common chart operations.
 */
fun List<PlayerStats>.toFantasyPointsList(): List<Double> = map { it.fantasyPoints }

fun List<MatchupData>.toMatchupFantasyPointsList(): List<Double> = map { it.fantasyPoints }

fun List<PlayerStats>.getWeeklyTrend(): List<Double> {
    return sortedBy { it.week ?: 0 }.map { it.fantasyPoints }
}

fun List<MatchupData>.getChronologicalTrend(): List<Double> {
    return sortedBy { it.gameDate }.map { it.fantasyPoints }
}