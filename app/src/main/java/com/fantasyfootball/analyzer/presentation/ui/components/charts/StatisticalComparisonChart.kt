package com.fantasyfootball.analyzer.presentation.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Custom Compose chart component for comparing season vs opponent performance.
 * Displays radar/spider chart showing multiple statistical categories.
 * 
 * Requirements addressed:
 * - 5.1: Statistical comparison charts for season vs opponent performance
 * - 2.5: Comparison to player's season average
 */
@Composable
fun StatisticalComparisonChart(
    seasonStats: ComparisonStats,
    opponentStats: ComparisonStats,
    modifier: Modifier = Modifier,
    title: String = "Season vs Opponent Comparison"
) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "comparison_animation"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Radar chart canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                drawRadarChart(
                    seasonStats = seasonStats,
                    opponentStats = opponentStats,
                    canvasSize = size,
                    animationProgress = animationProgress,
                    seasonColor = Color(0xFF1976D2),
                    opponentColor = Color(0xFFD32F2F),
                    gridColor = Color(0xFFE0E0E0)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            ComparisonLegend(
                seasonStats = seasonStats,
                opponentStats = opponentStats
            )
        }
    }
}

/**
 * Data class representing statistical comparison data for radar chart.
 */
data class ComparisonStats(
    val fantasyPoints: Double,
    val rushingYards: Double,
    val passingYards: Double,
    val receivingYards: Double,
    val touchdowns: Double,
    val consistency: Double // Standard deviation inverse for consistency metric
) {
    fun getStatValues(): List<Double> = listOf(
        fantasyPoints,
        rushingYards,
        passingYards,
        receivingYards,
        touchdowns,
        consistency
    )
    
    fun getStatLabels(): List<String> = listOf(
        "Fantasy Pts",
        "Rush Yds",
        "Pass Yds",
        "Rec Yds",
        "TDs",
        "Consistency"
    )
}

private fun DrawScope.drawRadarChart(
    seasonStats: ComparisonStats,
    opponentStats: ComparisonStats,
    canvasSize: Size,
    animationProgress: Float,
    seasonColor: Color,
    opponentColor: Color,
    gridColor: Color
) {
    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
    val radius = minOf(canvasSize.width, canvasSize.height) / 2 - 60f
    
    val statLabels = seasonStats.getStatLabels()
    val seasonValues = seasonStats.getStatValues()
    val opponentValues = opponentStats.getStatValues()
    
    // Normalize values to 0-1 range for radar chart
    val maxValues = seasonValues.zip(opponentValues) { s, o -> maxOf(s, o) }
    val normalizedSeasonValues = seasonValues.mapIndexed { index, value ->
        if (maxValues[index] > 0) value / maxValues[index] else 0.0
    }
    val normalizedOpponentValues = opponentValues.mapIndexed { index, value ->
        if (maxValues[index] > 0) value / maxValues[index] else 0.0
    }
    
    // Draw grid circles
    for (i in 1..5) {
        val gridRadius = radius * (i / 5f)
        drawCircle(
            color = gridColor,
            radius = gridRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
    
    // Draw axis lines and labels
    val angleStep = 2 * PI / statLabels.size
    for (i in statLabels.indices) {
        val angle = i * angleStep - PI / 2 // Start from top
        val endPoint = Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius
        )
        
        // Draw axis line
        drawLine(
            color = gridColor,
            start = center,
            end = endPoint,
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Draw season performance polygon
    drawStatPolygon(
        values = normalizedSeasonValues,
        center = center,
        radius = radius,
        color = seasonColor.copy(alpha = 0.3f),
        strokeColor = seasonColor,
        animationProgress = animationProgress
    )
    
    // Draw opponent performance polygon
    drawStatPolygon(
        values = normalizedOpponentValues,
        center = center,
        radius = radius,
        color = opponentColor.copy(alpha = 0.3f),
        strokeColor = opponentColor,
        animationProgress = animationProgress
    )
}

private fun DrawScope.drawStatPolygon(
    values: List<Double>,
    center: Offset,
    radius: Float,
    color: Color,
    strokeColor: Color,
    animationProgress: Float
) {
    if (values.isEmpty()) return
    
    val angleStep = 2 * PI / values.size
    val points = mutableListOf<Offset>()
    
    // Calculate polygon points
    for (i in values.indices) {
        val angle = i * angleStep - PI / 2 // Start from top
        val normalizedValue = values[i] * animationProgress
        val pointRadius = radius * normalizedValue.toFloat()
        
        val point = Offset(
            center.x + cos(angle).toFloat() * pointRadius,
            center.y + sin(angle).toFloat() * pointRadius
        )
        points.add(point)
    }
    
    // Draw filled polygon
    if (points.size >= 3) {
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        path.close()
        
        drawPath(
            path = path,
            color = color
        )
        
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    
    // Draw data points
    points.forEach { point ->
        drawCircle(
            color = strokeColor,
            radius = 4.dp.toPx(),
            center = point
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = point
        )
    }
}

@Composable
private fun ComparisonLegend(
    seasonStats: ComparisonStats,
    opponentStats: ComparisonStats
) {
    Column {
        // Legend indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendIndicator(
                color = Color(0xFF1976D2),
                label = "Season Average"
            )
            LegendIndicator(
                color = Color(0xFFD32F2F),
                label = "vs Opponent"
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Detailed comparison
        ComparisonTable(
            seasonStats = seasonStats,
            opponentStats = opponentStats
        )
    }
}

@Composable
private fun LegendIndicator(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ComparisonTable(
    seasonStats: ComparisonStats,
    opponentStats: ComparisonStats
) {
    val labels = seasonStats.getStatLabels()
    val seasonValues = seasonStats.getStatValues()
    val opponentValues = opponentStats.getStatValues()
    
    Column {
        labels.forEachIndexed { index, label ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("%.1f", seasonValues[index]),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("%.1f", opponentValues[index]),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
            }
            if (index < labels.size - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}