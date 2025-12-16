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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fantasyfootball.analyzer.domain.model.PlayerRecommendation
import kotlin.math.max

/**
 * Chart component for visualizing player recommendations with projected points.
 * Shows horizontal bar chart with confidence indicators.
 * 
 * Requirements addressed:
 * - 5.1: Statistical data visualization for recommendations
 * - 4.3: Projected fantasy points display
 * - 4.4: Visual representation of confidence levels
 */
@Composable
fun RecommendationsChart(
    recommendations: List<PlayerRecommendation>,
    modifier: Modifier = Modifier,
    title: String = "Weekly Recommendations",
    maxDisplayItems: Int = 8
) {
    if (recommendations.isEmpty()) {
        EmptyChartPlaceholder(
            title = title,
            message = "No recommendations available",
            modifier = modifier
        )
        return
    }

    // Take top recommendations for display
    val displayRecommendations = recommendations.take(maxDisplayItems)
    
    // Animation for bars
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "recommendations_animation"
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
                    .height((displayRecommendations.size * 50 + 40).dp)
            ) {
                drawRecommendationsChart(
                    recommendations = displayRecommendations,
                    canvasSize = size,
                    animationProgress = animationProgress,
                    excellentColor = Color(0xFF4CAF50),
                    goodColor = Color(0xFF8BC34A),
                    averageColor = Color(0xFFFF9800),
                    poorColor = Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Chart legend
            RecommendationsLegend()
        }
    }
}

private fun DrawScope.drawRecommendationsChart(
    recommendations: List<PlayerRecommendation>,
    canvasSize: Size,
    animationProgress: Float,
    excellentColor: Color,
    goodColor: Color,
    averageColor: Color,
    poorColor: Color
) {
    if (recommendations.isEmpty()) return
    
    val padding = 20f
    val chartWidth = canvasSize.width - (padding * 2)
    val barHeight = 35f
    val barSpacing = 10f
    
    // Calculate max projected points for scaling
    val maxPoints = recommendations.maxOfOrNull { it.projectedPoints } ?: 0.0
    val scaledMax = maxPoints * 1.1 // Add 10% padding
    
    recommendations.forEachIndexed { index, recommendation ->
        val y = padding + (index * (barHeight + barSpacing))
        
        // Calculate bar width based on projected points
        val barWidth = if (scaledMax > 0) {
            (recommendation.projectedPoints / scaledMax * chartWidth * animationProgress).toFloat()
        } else {
            0f
        }
        
        // Determine bar color based on matchup rating
        val barColor = when (recommendation.matchupRating.name) {
            "EXCELLENT" -> excellentColor
            "GOOD" -> goodColor
            "AVERAGE" -> averageColor
            else -> poorColor
        }
        
        // Draw main bar
        drawRoundRect(
            color = barColor,
            topLeft = Offset(padding, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        
        // Draw confidence indicator (overlay)
        val confidenceWidth = barWidth * recommendation.confidenceLevel.toFloat()
        drawRoundRect(
            color = barColor.copy(alpha = 0.7f),
            topLeft = Offset(padding, y + barHeight * 0.7f),
            size = Size(confidenceWidth, barHeight * 0.3f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        
        // Draw rank indicator
        drawCircle(
            color = Color.White,
            radius = 12.dp.toPx(),
            center = Offset(padding - 15.dp.toPx(), y + barHeight / 2)
        )
        drawCircle(
            color = barColor,
            radius = 10.dp.toPx(),
            center = Offset(padding - 15.dp.toPx(), y + barHeight / 2)
        )
    }
}

@Composable
private fun RecommendationsLegend() {
    Column {
        Text(
            text = "Legend",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(
                color = Color(0xFF4CAF50),
                label = "Excellent"
            )
            LegendItem(
                color = Color(0xFF8BC34A),
                label = "Good"
            )
            LegendItem(
                color = Color(0xFFFF9800),
                label = "Average"
            )
            LegendItem(
                color = Color(0xFFF44336),
                label = "Poor"
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "• Bar length = Projected points\n• Bottom overlay = Confidence level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
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