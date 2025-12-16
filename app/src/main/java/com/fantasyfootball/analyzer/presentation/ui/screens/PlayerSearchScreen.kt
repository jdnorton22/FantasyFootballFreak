package com.fantasyfootball.analyzer.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.search.MatchType
import com.fantasyfootball.analyzer.data.search.PlayerSearchResult
import com.fantasyfootball.analyzer.data.search.SearchSuggestion
import com.fantasyfootball.analyzer.data.search.SuggestionType
import com.fantasyfootball.analyzer.presentation.viewmodel.PlayerViewModel
import com.fantasyfootball.analyzer.presentation.ui.components.ErrorDisplay
import com.fantasyfootball.analyzer.presentation.ui.components.LoadingDisplay
import com.fantasyfootball.analyzer.presentation.ui.components.EmptyStateDisplay
import com.fantasyfootball.analyzer.presentation.ui.components.StatusIndicator

/**
 * Screen for searching NFL players.
 * Provides search functionality with results display.
 * 
 * Requirements addressed:
 * - 1.1: Player search functionality
 * - 5.3: Pagination/scrolling for large datasets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSearchScreen(
    onPlayerSelected: (String) -> Unit,
    onNavigateToRecommendations: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchSuggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val autocompleteSuggestions by viewModel.autocompleteSuggestions.collectAsStateWithLifecycle()
    val currentError by viewModel.currentError.collectAsStateWithLifecycle()
    val isAnyLoading by viewModel.isAnyLoading.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    var showRecentSearches by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Fantasy Football Analyzer",
                        modifier = Modifier.semantics {
                            contentDescription = "Fantasy Football Analyzer main screen"
                        }
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics {
                            contentDescription = "Open settings menu"
                        }
                    ) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
        // Enhanced Search bar with autocomplete
        Column {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    showSuggestions = query.isNotEmpty()
                    showRecentSearches = query.isEmpty()
                    viewModel.searchPlayers(query)
                },
                label = { Text("Search Players") },
                placeholder = { Text("Enter player name, team, or position") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    Row {
                        if (searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
                            IconButton(
                                onClick = { showRecentSearches = !showRecentSearches },
                                modifier = Modifier.semantics {
                                    contentDescription = if (showRecentSearches) {
                                        "Hide recent searches"
                                    } else {
                                        "Show recent searches"
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = null
                                )
                            }
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    showSuggestions = false
                                    showRecentSearches = false
                                    viewModel.clearSearch()
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Clear search field"
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear, 
                                    contentDescription = null
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Search for NFL players by name, team, or position"
                    },
                singleLine = true
            )
            
            // Autocomplete suggestions
            if (showSuggestions && (autocompleteSuggestions.isNotEmpty() || searchSuggestions.isNotEmpty())) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        // Search history suggestions
                        items(searchSuggestions) { suggestion ->
                            SuggestionItem(
                                suggestion = suggestion,
                                onClick = {
                                    searchQuery = suggestion.query
                                    showSuggestions = false
                                    viewModel.onSearchSuggestionSelected(suggestion.query)
                                }
                            )
                        }
                        
                        // Autocomplete suggestions
                        items(autocompleteSuggestions) { suggestion ->
                            AutocompleteSuggestionItem(
                                suggestion = suggestion,
                                onClick = {
                                    searchQuery = suggestion
                                    showSuggestions = false
                                    viewModel.searchPlayers(suggestion)
                                }
                            )
                        }
                    }
                }
            }
            
            // Recent searches
            if (showRecentSearches && recentSearches.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        recentSearches.take(5).forEach { recentSearch ->
                            RecentSearchItem(
                                query = recentSearch,
                                onClick = {
                                    searchQuery = recentSearch
                                    showRecentSearches = false
                                    viewModel.searchPlayers(recentSearch)
                                }
                            )
                        }
                        
                        if (recentSearches.size > 5) {
                            TextButton(
                                onClick = { 
                                    // Show all recent searches or navigate to history screen
                                }
                            ) {
                                Text("View All Recent Searches")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendations button
        Button(
            onClick = onNavigateToRecommendations,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Navigate to weekly player recommendations screen"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Weekly Recommendations")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Enhanced loading indicator
        if (isAnyLoading) {
            LoadingDisplay(
                message = "Searching for players...",
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

        // Search results or empty state
        if (searchResults.isEmpty() && searchQuery.isNotEmpty() && !isAnyLoading && currentError == null) {
            EmptyStateDisplay(
                title = "No Players Found",
                message = "No players match your search for '$searchQuery'. Try a different search term or check your spelling.",
                actionLabel = "Clear Search",
                onAction = {
                    searchQuery = ""
                    showSuggestions = false
                    showRecentSearches = false
                    viewModel.clearSearch()
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else if (searchQuery.isEmpty() && !showRecentSearches) {
            // Show popular searches or getting started message
            PopularSearchesSection(
                onSearchSelected = { query ->
                    searchQuery = query
                    viewModel.searchPlayers(query)
                },
                viewModel = viewModel
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { searchResult ->
                    EnhancedPlayerSearchItem(
                        searchResult = searchResult,
                        onClick = { 
                            viewModel.onPlayerSelected(searchResult)
                            onPlayerSelected(searchResult.player.playerId) 
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun EnhancedPlayerSearchItem(
    searchResult: PlayerSearchResult,
    onClick: () -> Unit
) {
    val player = searchResult.player
    val contentDesc = buildString {
        append("Player: ${player.name}, ")
        append("Position: ${player.position}, ")
        append("Team: ${player.team}")
        player.injuryStatus?.let { status ->
            append(", Injury status: $status")
        }
        append(", Match type: ${searchResult.matchType.name.lowercase()}")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics {
                contentDescription = contentDesc
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = searchResult.player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${searchResult.player.position} - ${searchResult.player.team}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Match type indicator
                    MatchTypeChip(matchType = searchResult.matchType)
                    
                    // Relevance score (for debugging - can be removed in production)
                    if (searchResult.relevanceScore > 0) {
                        Text(
                            text = "Score: ${searchResult.relevanceScore}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // Injury status with accessibility
            searchResult.player.injuryStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                StatusIndicator(
                    status = status,
                    contentDescription = "Player injury status: $status"
                )
            }
        }
    }
}

@Composable
private fun MatchTypeChip(matchType: MatchType) {
    val (text, color) = when (matchType) {
        MatchType.EXACT_NAME -> "Exact" to MaterialTheme.colorScheme.primary
        MatchType.PREFIX_NAME -> "Prefix" to MaterialTheme.colorScheme.secondary
        MatchType.CONTAINS_NAME -> "Contains" to MaterialTheme.colorScheme.tertiary
        MatchType.POSITION -> "Position" to MaterialTheme.colorScheme.outline
        MatchType.TEAM -> "Team" to MaterialTheme.colorScheme.outline
        MatchType.FUZZY -> "Fuzzy" to MaterialTheme.colorScheme.surfaceVariant
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (suggestion.type) {
                SuggestionType.RECENT -> Icons.Default.Refresh
                SuggestionType.HISTORY -> Icons.Default.Star
                SuggestionType.AUTOCOMPLETE -> Icons.Default.Search
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.query,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (suggestion.resultCount != null) {
                Text(
                    text = "${suggestion.resultCount} results",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        if (suggestion.frequency > 1) {
            Text(
                text = "${suggestion.frequency}x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AutocompleteSuggestionItem(
    suggestion: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentSearchItem(
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PopularSearchesSection(
    onSearchSelected: (String) -> Unit,
    viewModel: PlayerViewModel
) {
    val popularSearches = remember { viewModel.getPopularSearches() }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Popular Searches",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (popularSearches.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(popularSearches.take(10)) { query ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSearchSelected(query) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = query,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Start Searching",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Search for NFL players by name, team, or position to get started with your fantasy football analysis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}