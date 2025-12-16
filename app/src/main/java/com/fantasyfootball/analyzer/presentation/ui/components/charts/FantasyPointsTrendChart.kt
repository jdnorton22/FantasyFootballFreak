package com.fantasyfootball.analyzer.presentation.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import kotlin.math.max
import kotlin.math.min

/**
 * Custom Compose chart component for displaying fantasy points trends over time.
 * Shows player performance progression with interactive data points.
 * 
 * Requirements addressed:
 * - 5.1: Statistical data visualization with charts and graphs
 */
@Composable
fun FantasyPointsTrendChart(
    playerStats: List<PlayerStats>,
    modifier: Modifier = Modifier,
    title: String = "Fantasy Points Trend"
) {
    val density = LocalDensity.current
    
    if (playerStats.isEmpty()) {
        EmptyChartPlaceholder(
            title = title,
            message = "No data available",
            modifier = modifier
        )
        return
    }

    // Sort stats by week for proper trend display
    val sortedStats = playerStats.sortedBy { it.week ?: 0 }
    
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
            
            // Chart canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                drawFantasyPointsTrend(
                    stats = sortedStats,
                    canvasSize = size,
                    primaryColor = Color(0xFF1976D2),
                    backgroundColor = Color(0xFFF5F5F5)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Legend and stats
            ChartLegend(stats = sortedStats)
        }
    }
}

private fun DrawScope.drawFantasyPointsTrend(
    stats: List<PlayerStats>,
    canvasSize: androidx.compose.ui.geometry.Size,
    primaryColor: Color,
    backgroundColor: Color
) {
    if (stats.isEmpty()) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Calculate data bounds
    val maxPoints = stats.maxOfOrNull { it.fantasyPoints }?.toFloat() ?: 0f
    val minPoints = stats.minOfOrNull { it.fantasyPoints }?.toFloat() ?: 0f
    val pointsRange = maxPoints - minPoints
    val adjustedMax = maxPoints + (pointsRange * 0.1f)
    val adjustedMin = max(0f, minPoints - (pointsRange * 0.1f))
    
    // Draw background grid
    drawGrid(
        canvasSize = canvasSize,
        padding = padding,
        gridColor = backgroundColor
    )
    
    // Calculate points for line chart
    val points = mutableListOf<Offset>()
    stats.forEachIndexed { index, stat ->
        val x = padding + (index.toFloat() / (stats.size - 1).toFloat()) * chartWidth
        val normalizedY = if (adjustedMax > adjustedMin) {
            (stat.fantasyPoints.toFloat() - adjustedMin) / (adjustedMax - adjustedMin)
        } else {
            0.5f
        }
        val y = padding + chartHeight - (normalizedY * chartHeight)
        points.add(Offset(x, y))
    }
    
    // Draw trend line
    if (points.size > 1) {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    
    // Draw data points
    points.forEachIndexed { index, point ->
        drawCircle(
            color = primaryColor,
            radius = 6.dp.toPx(),
            center = point
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = point
        )
    }
    
    // Draw Y-axis labels
    drawYAxisLabels(
        canvasSize = canvasSize,
        padding = padding,
        minValue = adjustedMin,
        maxValue = adjustedMax
    )
}

private fun DrawScope.drawGrid(
    canvasSize: androidx.compose.ui.geometry.Size,
    padding: Float,
    gridColor: Color
) {
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Horizontal grid lines
    for (i in 0..4) {
        val y = padding + (i.toFloat() / 4f) * chartHeight
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Vertical grid lines
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

private fun DrawScope.drawYAxisLabels(
    canvasSize: androidx.compose.ui.geometry.Size,
    padding: Float,
    minValue: Float,
    maxValue: Float
) {
    // This would typically use TextPainter for proper text rendering
    // For simplicity, we'll skip text rendering in this canvas implementation
    // In a production app, you'd use AndroidView with custom drawing or a proper charting library
}

@Composable
private fun ChartLegend(stats: List<PlayerStats>) {
    if (stats.isEmpty()) return
    
    val avgPoints = stats.map { it.fantasyPoints }.average()
    val maxPoints = stats.maxOfOrNull { it.fantasyPoints } ?: 0.0
    val minPoints = stats.minOfOrNull { it.fantasyPoints } ?: 0.0
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(label = "Average", value = String.format("%.1f", avgPoints))
        LegendItem(label = "High", value = String.format("%.1f", maxPoints))
        LegendItem(label = "Low", value = String.format("%.1f", minPoints))
        LegendItem(label = "Games", value = stats.size.toString())
    }
}

@Composable
private fun LegendItem(label: String, value: String) {
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