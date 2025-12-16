package com.fantasyfootball.analyzer.presentation.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import kotlin.math.max

/**
 * Custom Compose bar chart component for displaying matchup performance data.
 * Shows historical fantasy points against specific opponents with animated bars.
 * 
 * Requirements addressed:
 * - 5.1: Statistical data visualization with bar charts
 * - 2.3: Game-by-game performance breakdown visualization
 */
@Composable
fun MatchupPerformanceBarChart(
    matchupData: List<MatchupData>,
    modifier: Modifier = Modifier,
    title: String = "Matchup Performance",
    animated: Boolean = true
) {
    if (matchupData.isEmpty()) {
        EmptyChartPlaceholder(
            title = title,
            message = "No matchup data available",
            modifier = modifier
        )
        return
    }

    // Sort by date for chronological display
    val sortedData = matchupData.sortedBy { it.gameDate }
    
    // Animation for bars
    val animationProgress by animateFloatAsState(
        targetValue = if (animated) 1f else 1f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "bar_animation"
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
            
            // Chart canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                drawMatchupBars(
                    data = sortedData,
                    canvasSize = size,
                    animationProgress = animationProgress,
                    primaryColor = Color(0xFF2E7D32),
                    secondaryColor = Color(0xFF66BB6A),
                    backgroundColor = Color(0xFFF1F8E9)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Chart statistics
            MatchupChartStats(data = sortedData)
        }
    }
}

private fun DrawScope.drawMatchupBars(
    data: List<MatchupData>,
    canvasSize: Size,
    animationProgress: Float,
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color
) {
    if (data.isEmpty()) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    // Calculate data bounds
    val maxPoints = data.maxOfOrNull { it.fantasyPoints }?.toFloat() ?: 0f
    val avgPoints = data.map { it.fantasyPoints }.average().toFloat()
    val adjustedMax = maxPoints * 1.1f
    
    // Draw background
    drawRect(
        color = backgroundColor.copy(alpha = 0.3f),
        topLeft = Offset(padding, padding),
        size = Size(chartWidth, chartHeight)
    )
    
    // Draw average line
    val avgY = padding + chartHeight - ((avgPoints / adjustedMax) * chartHeight)
    drawLine(
        color = primaryColor.copy(alpha = 0.5f),
        start = Offset(padding, avgY),
        end = Offset(padding + chartWidth, avgY),
        strokeWidth = 2.dp.toPx()
    )
    
    // Calculate bar dimensions
    val barWidth = chartWidth / data.size * 0.7f
    val barSpacing = chartWidth / data.size * 0.3f
    
    // Draw bars
    data.forEachIndexed { index, matchup ->
        val barHeight = (matchup.fantasyPoints.toFloat() / adjustedMax) * chartHeight * animationProgress
        val x = padding + (index * (barWidth + barSpacing)) + (barSpacing / 2)
        val y = padding + chartHeight - barHeight
        
        // Determine bar color based on performance vs average
        val barColor = if (matchup.fantasyPoints > avgPoints) {
            primaryColor
        } else {
            secondaryColor
        }
        
        // Draw bar with rounded corners
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        
        // Draw performance rating indicator (small circle on top)
        val ratingColor = when {
            matchup.performanceRating >= 8.0 -> Color(0xFF4CAF50)
            matchup.performanceRating >= 6.0 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        
        drawCircle(
            color = ratingColor,
            radius = 3.dp.toPx(),
            center = Offset(x + barWidth / 2, y - 8.dp.toPx())
        )
    }
}

@Composable
private fun MatchupChartStats(data: List<MatchupData>) {
    val avgPoints = data.map { it.fantasyPoints }.average()
    val avgRating = data.map { it.performanceRating }.average()
    val bestGame = data.maxByOrNull { it.fantasyPoints }
    val worstGame = data.minByOrNull { it.fantasyPoints }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Avg Points",
                value = String.format("%.1f", avgPoints),
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "Avg Rating",
                value = String.format("%.1f", avgRating),
                color = MaterialTheme.colorScheme.secondary
            )
            StatItem(
                label = "Games",
                value = data.size.toString(),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Best and worst performance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bestGame?.let { game ->
                StatItem(
                    label = "Best (${game.season} W${game.week})",
                    value = String.format("%.1f pts", game.fantasyPoints),
                    color = Color(0xFF4CAF50)
                )
            }
            worstGame?.let { game ->
                StatItem(
                    label = "Worst (${game.season} W${game.week})",
                    value = String.format("%.1f pts", game.fantasyPoints),
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
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