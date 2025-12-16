package com.fantasyfootball.analyzer.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasyfootball.analyzer.domain.model.InjuryImpact
import com.fantasyfootball.analyzer.domain.model.MatchupRating
import com.fantasyfootball.analyzer.domain.model.PlayerRecommendation
import com.fantasyfootball.analyzer.presentation.viewmodel.RecommendationViewModel
import com.fantasyfootball.analyzer.presentation.ui.components.charts.RecommendationsChart

/**
 * Screen displaying weekly player recommendations.
 * Shows ranked list of players with projections and analysis.
 * 
 * Requirements addressed:
 * - 4.1: Weekly recommendations based on matchups
 * - 4.2: Ranking based on historical performance
 * - 4.3: Projected fantasy points display
 * - 4.4: Tie-breaking with consistency metrics
 * - 4.5: Injury status impact
 * - 5.3: Pagination for large datasets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    onPlayerSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()

    // Sample roster player IDs for demo purposes
    val sampleRosterIds = remember {
        listOf("player1", "player2", "player3", "player4", "player5")
    }

    // Load recommendations when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.generateRecommendations(sampleRosterIds)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top app bar
        TopAppBar(
            title = { Text("Weekly Recommendations") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.refreshRecommendations() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recommendations visualization and list
            if (recommendations.isNotEmpty()) {
                // Recommendations chart
                RecommendationsChart(
                    recommendations = recommendations,
                    title = "Top Recommendations Overview"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Detailed Recommendations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recommendations) { recommendation ->
                        RecommendationCard(
                            recommendation = recommendation,
                            onClick = { onPlayerSelected(recommendation.player.playerId) }
                        )
                    }
                }
            } else if (!uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recommendations available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add players to your roster to get personalized recommendations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: PlayerRecommendation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with rank and player name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rank badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "#${recommendation.rank}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = recommendation.player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${recommendation.player.position} - ${recommendation.player.team}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Projected points
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.1f", recommendation.projectedPoints),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "proj pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Matchup rating and confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Matchup rating chip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = getMatchupRatingColor(recommendation.matchupRating),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = recommendation.matchupRating.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                // Confidence and consistency
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Confidence: ${(recommendation.confidenceLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Consistency: ${String.format("%.1f", recommendation.consistencyScore)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Injury impact warning
            if (recommendation.injuryImpact != InjuryImpact.NONE) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Injury Impact: ${recommendation.injuryImpact.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Reasoning
            if (recommendation.reasoning.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recommendation.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getMatchupRatingColor(rating: MatchupRating): androidx.compose.ui.graphics.Color {
    return when (rating) {
        MatchupRating.EXCELLENT -> MaterialTheme.colorScheme.primary
        MatchupRating.GOOD -> MaterialTheme.colorScheme.secondary
        MatchupRating.AVERAGE -> MaterialTheme.colorScheme.tertiary
        MatchupRating.POOR -> MaterialTheme.colorScheme.error
        MatchupRating.AVOID -> MaterialTheme.colorScheme.error
    }
}