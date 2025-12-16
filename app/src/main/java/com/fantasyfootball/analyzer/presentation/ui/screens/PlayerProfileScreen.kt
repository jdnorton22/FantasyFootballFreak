package com.fantasyfootball.analyzer.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.presentation.viewmodel.PlayerViewModel
import com.fantasyfootball.analyzer.presentation.ui.components.charts.FantasyPointsTrendChart
import com.fantasyfootball.analyzer.presentation.ui.components.ErrorDisplay
import com.fantasyfootball.analyzer.presentation.ui.components.LoadingDisplay
import com.fantasyfootball.analyzer.presentation.ui.components.DataFreshnessIndicator
import com.fantasyfootball.analyzer.presentation.ui.components.InsufficientDataDisplay
import com.fantasyfootball.analyzer.presentation.ui.components.StatusIndicator

/**
 * Screen displaying detailed player profile with statistics.
 * Shows current season stats and player information.
 * 
 * Requirements addressed:
 * - 1.1: Player data retrieval and display
 * - 1.2: Fantasy points, yards, touchdowns display
 * - 1.3: Position, team, injury status display
 * - 5.1: Statistical data visualization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    playerId: String,
    onNavigateToMatchup: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPlayer by viewModel.selectedPlayer.collectAsStateWithLifecycle()
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
    val currentError by viewModel.currentError.collectAsStateWithLifecycle()
    val isAnyLoading by viewModel.isAnyLoading.collectAsStateWithLifecycle()
    val isDataFresh by viewModel.isDataFresh.collectAsStateWithLifecycle()

    // Load player data when screen is first displayed
    LaunchedEffect(playerId) {
        // This would typically load the player by ID
        // For now, we'll assume the player is already selected
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top app bar with accessibility
        TopAppBar(
            title = { 
                Text(
                    "Player Profile",
                    modifier = Modifier.semantics {
                        contentDescription = "Player profile screen"
                    }
                ) 
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics {
                        contentDescription = "Navigate back to previous screen"
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = null
                    )
                }
            }
        )

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Enhanced loading indicator
            if (isAnyLoading) {
                LoadingDisplay(
                    message = "Loading player profile...",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Enhanced error display
            currentError?.let { error ->
                ErrorDisplay(
                    error = error,
                    onRetry = { viewModel.retryLastOperation() },
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Data freshness indicator
            DataFreshnessIndicator(
                isFresh = isDataFresh,
                onRefresh = { viewModel.refreshPlayerData() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Player information
            selectedPlayer?.let { player ->
                PlayerInfoCard(player = player)
                Spacer(modifier = Modifier.height(16.dp))

                // Matchup analysis button with accessibility
                Button(
                    onClick = { 
                        // For demo purposes, using a sample opponent team
                        onNavigateToMatchup(player.playerId, "DAL") 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Analyze ${player.name}'s historical performance against Dallas Cowboys"
                        }
                ) {
                    Text("Analyze Matchup vs DAL")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Player statistics with trend chart or insufficient data display
            if (playerStats.isNotEmpty()) {
                // Fantasy points trend chart
                FantasyPointsTrendChart(
                    playerStats = playerStats,
                    title = "Fantasy Points Trend"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Current Season Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                playerStats.forEach { stats ->
                    PlayerStatsCard(stats = stats)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (!isAnyLoading && currentError == null) {
                // Show insufficient data display when no stats are available
                InsufficientDataDisplay(
                    title = "Limited Statistics Available",
                    message = "No current season statistics found for this player. This could be due to the player being inactive, injured, or data not being available yet.",
                    onRefresh = { viewModel.refreshPlayerData() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PlayerInfoCard(player: Player) {
    val cardContentDesc = buildString {
        append("Player information: ")
        append("${player.name}, ")
        append("${player.position} for ${player.team}")
        player.injuryStatus?.let { status ->
            append(", injury status: $status")
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = cardContentDesc
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Position",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = player.position,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "Team",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = player.team,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusIndicator(
                        status = player.injuryStatus ?: "Healthy",
                        contentDescription = "Player status: ${player.injuryStatus ?: "Healthy"}"
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerStatsCard(stats: PlayerStats) {
    val statsContentDesc = buildString {
        append("Statistics for week ${stats.week ?: "season"} ${stats.season}: ")
        append("${String.format("%.1f", stats.fantasyPoints)} fantasy points, ")
        append("${stats.rushingYards} rushing yards, ")
        append("${stats.passingYards} passing yards, ")
        append("${stats.receivingYards} receiving yards, ")
        append("${stats.touchdowns} touchdowns")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = statsContentDesc
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Week ${stats.week ?: "Season"} - ${stats.season}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Fantasy Points (prominent display)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Fantasy Points",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = String.format("%.1f", stats.fantasyPoints),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Other statistics in a grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Rush Yds", value = stats.rushingYards.toString())
                StatItem(label = "Pass Yds", value = stats.passingYards.toString())
                StatItem(label = "Rec Yds", value = stats.receivingYards.toString())
                StatItem(label = "TDs", value = stats.touchdowns.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}