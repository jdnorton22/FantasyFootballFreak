package com.fantasyfootball.analyzer.presentation.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import kotlin.math.*

/**
 * Interactive chart component for displaying historical matchup data with touch interactions.
 * Users can tap on data points to see detailed information.
 * 
 * Requirements addressed:
 * - 5.1: Interactive charts for historical data display
 * - 2.3: Game-by-game performance breakdown with interactivity
 */
@Composable
fun InteractiveHistoricalChart(
    historicalData: List<MatchupData>,
    modifier: Modifier = Modifier,
    title: String = "Historical Performance"
) {
    var selectedDataPoint by remember { mutableStateOf<MatchupData?>(null) }
    val density = LocalDensity.current
    
    if (historicalData.isEmpty()) {
        EmptyChartPlaceholder(
            title = title,
            message = "No historical data available",
            modifier = modifier
        )
        return
    }

    // Sort data chronologically
    val sortedData = historicalData.sortedBy { it.gameDate }
    
    // Animation for chart appearance
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "chart_animation"
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
            
            // Interactive chart canvas
            var dataPoints by remember { mutableStateOf<List<ChartDataPoint>>(emptyList()) }
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .pointerInput(sortedData) {
                        detectTapGestures { offset ->
                            // Find closest data point to tap
                            val closestPoint = dataPoints.minByOrNull { point ->
                                val distance = sqrt(
                                    (point.screenPosition.x - offset.x).pow(2) +
                                    (point.screenPosition.y - offset.y).pow(2)
                                )
                                distance
                            }
                            
                            // Select point if tap is close enough (within 30dp)
                            closestPoint?.let { point ->
                                val tapRadius = with(density) { 30.dp.toPx() }
                                val distance = sqrt(
                                    (point.screenPosition.x - offset.x).pow(2) +
                                    (point.screenPosition.y - offset.y).pow(2)
                                )
                                if (distance <= tapRadius) {
                                    selectedDataPoint = point.data
                                }
                            }
                        }
                    }
            ) {
                val points = drawInteractiveChart(
                    data = sortedData,
                    canvasSize = size,
                    animationProgress = animationProgress,
                    selectedPoint = selectedDataPoint,
                    primaryColor = Color(0xFF6A1B9A),
                    selectedColor = Color(0xFFE91E63),
                    gridColor = Color(0xFFF3E5F5)
                )
                dataPoints = points
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Selected point details or chart summary
            selectedDataPoint?.let { point ->
                SelectedPointDetails(
                    data = point,
                    onDismiss = { selectedDataPoint = null }
                )
            } ?: run {
                ChartSummary(data = sortedData)
            }
        }
    }
}

/**
 * Data class to hold chart point information including screen position for interaction.
 */
private data class ChartDataPoint(
    val data: MatchupData,
    val screenPosition: Offset
)

private fun DrawScope.drawInteractiveChart(
    data: List<MatchupData>,
    canvasSize: Size,
    animationProgress: Float,
    selectedPoint: MatchupData?,
    primaryColor: Color,
    selectedColor: Color,
    gridColor: Color
): List<ChartDataPoint> {
    if (data.isEmpty()) return emptyList()
    
    val padding = 50f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Calculate data bounds
    val maxPoints = data.maxOfOrNull { it.fantasyPoints }?.toFloat() ?: 0f
    val minPoints = data.minOfOrNull { it.fantasyPoints }?.toFloat() ?: 0f
    val pointsRange = maxPoints - minPoints
    val adjustedMax = maxPoints + (pointsRange * 0.1f)
    val adjustedMin = maxOf(0f, minPoints - (pointsRange * 0.1f))
    
    // Draw background grid
    drawInteractiveGrid(
        canvasSize = canvasSize,
        padding = padding,
        gridColor = gridColor,
        minValue = adjustedMin,
        maxValue = adjustedMax
    )
    
    // Calculate chart points
    val chartPoints = mutableListOf<ChartDataPoint>()
    val screenPoints = mutableListOf<Offset>()
    
    data.forEachIndexed { index, matchup ->
        val x = padding + (index.toFloat() / (data.size - 1).toFloat()) * chartWidth
        val normalizedY = if (adjustedMax > adjustedMin) {
            (matchup.fantasyPoints.toFloat() - adjustedMin) / (adjustedMax - adjustedMin)
        } else {
            0.5f
        }
        val y = padding + chartHeight - (normalizedY * chartHeight * animationProgress)
        val screenPosition = Offset(x, y)
        
        chartPoints.add(ChartDataPoint(matchup, screenPosition))
        screenPoints.add(screenPosition)
    }
    
    // Draw connecting line
    if (screenPoints.size > 1) {
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(screenPoints[0].x, screenPoints[0].y)
        for (i in 1 until screenPoints.size) {
            path.lineTo(screenPoints[i].x, screenPoints[i].y)
        }
        
        drawPath(
            path = path,
            color = primaryColor.copy(alpha = 0.7f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
    
    // Draw data points
    chartPoints.forEach { point ->
        val isSelected = point.data == selectedPoint
        val pointColor = if (isSelected) selectedColor else primaryColor
        val pointRadius = if (isSelected) 8.dp.toPx() else 6.dp.toPx()
        
        // Draw outer circle
        drawCircle(
            color = pointColor,
            radius = pointRadius,
            center = point.screenPosition
        )
        
        // Draw inner circle
        drawCircle(
            color = Color.White,
            radius = pointRadius * 0.5f,
            center = point.screenPosition
        )
        
        // Draw performance rating indicator
        val ratingColor = when {
            point.data.performanceRating >= 8.0 -> Color(0xFF4CAF50)
            point.data.performanceRating >= 6.0 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        
        drawCircle(
            color = ratingColor,
            radius = 2.dp.toPx(),
            center = point.screenPosition
        )
    }
    
    return chartPoints
}

private fun DrawScope.drawInteractiveGrid(
    canvasSize: Size,
    padding: Float,
    gridColor: Color,
    minValue: Float,
    maxValue: Float
) {
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Draw horizontal grid lines
    for (i in 0..5) {
        val y = padding + (i.toFloat() / 5f) * chartHeight
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Draw vertical grid lines
    for (i in 0..6) {
        val x = padding + (i.toFloat() / 6f) * chartWidth
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, padding + chartHeight),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun SelectedPointDetails(
    data: MatchupData,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Game Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${data.season} Week ${data.week}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "vs ${data.opponentTeam}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.1f pts", data.fantasyPoints),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Rating: ${String.format("%.1f", data.performanceRating)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartSummary(data: List<MatchupData>) {
    val avgPoints = data.map { it.fantasyPoints }.average()
    val avgRating = data.map { it.performanceRating }.average()
    val consistency = calculateConsistency(data.map { it.fantasyPoints })
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SummaryItem(
            label = "Avg Points",
            value = String.format("%.1f", avgPoints)
        )
        SummaryItem(
            label = "Avg Rating",
            value = String.format("%.1f", avgRating)
        )
        SummaryItem(
            label = "Consistency",
            value = String.format("%.1f", consistency)
        )
        SummaryItem(
            label = "Games",
            value = data.size.toString()
        )
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateConsistency(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val mean = values.average()
    val variance = values.map { (it - mean).pow(2) }.average()
    val standardDeviation = sqrt(variance)
    // Return inverse of coefficient of variation for consistency score
    return if (mean > 0) 10.0 - (standardDeviation / mean * 10.0) else 0.0
}

@Composable
private fun EmptyChartPlaceholder(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}