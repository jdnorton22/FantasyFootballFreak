package com.fantasyfootball.analyzer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.remote.NetworkResult
import com.fantasyfootball.analyzer.domain.model.InjuryImpact
import com.fantasyfootball.analyzer.domain.model.MatchupRating
import com.fantasyfootball.analyzer.domain.model.PlayerRecommendation
import com.fantasyfootball.analyzer.domain.repository.PlayerRepository
import com.fantasyfootball.analyzer.domain.usecase.MatchupAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing player recommendations functionality.
 * Handles weekly recommendation generation, ranking, and injury impact analysis.
 * 
 * Requirements addressed:
 * - 4.1: Weekly recommendations based on upcoming matchups
 * - 4.2: Ranking based on historical performance against opponents
 * - 4.3: Projected fantasy points display based on matchup analysis
 * - 4.4: Tie-breaking with consistency metrics for similar projections
 * - 4.5: Injury status impact on recommendation adjustments
 * - 1.4: Loading states and comprehensive error handling
 */
@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val matchupAnalyzer: MatchupAnalyzer,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    private val _recommendations = MutableStateFlow<List<PlayerRecommendation>>(emptyList())
    val recommendations: StateFlow<List<PlayerRecommendation>> = _recommendations.asStateFlow()

    private val _filteredRecommendations = MutableStateFlow<List<PlayerRecommendation>>(emptyList())
    val filteredRecommendations: StateFlow<List<PlayerRecommendation>> = _filteredRecommendations.asStateFlow()

    private val _currentRoster = MutableStateFlow<List<String>>(emptyList())
    val currentRoster: StateFlow<List<String>> = _currentRoster.asStateFlow()

    private val _filterSettings = MutableStateFlow(RecommendationFilter())
    val filterSettings: StateFlow<RecommendationFilter> = _filterSettings.asStateFlow()

    /**
     * Generates weekly recommendations for the provided roster.
     * Implements requirements 4.1-4.5: Complete recommendation system.
     */
    fun generateRecommendations(rosterPlayerIds: List<String>) {
        if (rosterPlayerIds.isEmpty()) {
            _recommendations.value = emptyList()
            _filteredRecommendations.value = emptyList()
            _uiState.value = _uiState.value.copy(
                error = "No players provided for recommendations"
            )
            return
        }

        _currentRoster.value = rosterPlayerIds
        _uiState.value = _uiState.value.copy(
            isLoading = true, 
            error = null,
            hasData = false
        )
        
        viewModelScope.launch {
            try {
                // Validate that all players exist and get their current data
                val validatedPlayers = validateRosterPlayers(rosterPlayerIds)
                
                if (validatedPlayers.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No valid players found in roster"
                    )
                    return@launch
                }

                // Generate recommendations using the matchup analyzer
                when (val recommendationsResult = matchupAnalyzer.generateWeeklyRecommendations(
                    validatedPlayers.map { it.playerId }
                )) {
                    is NetworkResult.Success -> {
                        // Sort recommendations by projected points and consistency
                        val sortedRecommendations = sortRecommendations(recommendationsResult.data)
                        
                        _recommendations.value = sortedRecommendations
                        applyCurrentFilters()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasData = sortedRecommendations.isNotEmpty(),
                            playerCount = sortedRecommendations.size,
                            injuredPlayerCount = sortedRecommendations.count { 
                                it.injuryImpact != InjuryImpact.NONE 
                            }
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = recommendationsResult.message
                        )
                        return@launch
                    }
                    is NetworkResult.Loading -> {
                        // Keep loading state
                        return@launch
                    }
                }
                

                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to generate recommendations"
                )
            }
        }
    }

    /**
     * Validates roster players and retrieves current player data.
     */
    private suspend fun validateRosterPlayers(playerIds: List<String>): List<Player> {
        val validPlayers = mutableListOf<Player>()
        
        for (playerId in playerIds) {
            when (val result = playerRepository.getPlayer(playerId)) {
                is NetworkResult.Success -> {
                    validPlayers.add(result.data)
                }
                is NetworkResult.Error -> {
                    // Log error but continue with other players
                    continue
                }
                is NetworkResult.Loading -> {
                    // Skip loading players for now
                    continue
                }
            }
        }
        
        return validPlayers
    }

    /**
     * Sorts recommendations based on projected points and consistency.
     * Implements requirement 4.4: Tie-breaking with consistency metrics.
     */
    private fun sortRecommendations(recommendations: List<PlayerRecommendation>): List<PlayerRecommendation> {
        return recommendations.sortedWith(compareByDescending<PlayerRecommendation> { it.projectedPoints }
            .thenByDescending { it.consistencyScore }
            .thenBy { it.injuryImpact.ordinal } // Prefer less injured players
        ).mapIndexed { index, recommendation ->
            recommendation.copy(rank = index + 1)
        }
    }

    /**
     * Refreshes recommendations with latest data from network.
     * Implements requirement 1.4: Data refresh functionality.
     */
    fun refreshRecommendations() {
        if (_currentRoster.value.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "No roster available to refresh"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isRefreshing = true)
        
        viewModelScope.launch {
            try {
                // Force refresh player data first
                for (playerId in _currentRoster.value) {
                    playerRepository.getPlayer(playerId, forceRefresh = true)
                }
                
                // Regenerate recommendations with fresh data
                when (val recommendationsResult = matchupAnalyzer.generateWeeklyRecommendations(_currentRoster.value)) {
                    is NetworkResult.Success -> {
                        val sortedRecommendations = sortRecommendations(recommendationsResult.data)
                        
                        _recommendations.value = sortedRecommendations
                        applyCurrentFilters()
                        
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            hasData = sortedRecommendations.isNotEmpty(),
                            playerCount = sortedRecommendations.size,
                            injuredPlayerCount = sortedRecommendations.count { 
                                it.injuryImpact != InjuryImpact.NONE 
                            }
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = "Failed to refresh recommendations: ${recommendationsResult.message}"
                        )
                        return@launch
                    }
                    is NetworkResult.Loading -> {
                        // Keep refreshing state
                        return@launch
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Failed to refresh recommendations: ${e.message}"
                )
            }
        }
    }

    /**
     * Applies filters to recommendations based on current filter settings.
     */
    fun applyFilters(filter: RecommendationFilter) {
        _filterSettings.value = filter
        applyCurrentFilters()
    }

    /**
     * Applies current filter settings to recommendations.
     */
    private fun applyCurrentFilters() {
        val filter = _filterSettings.value
        val filtered = _recommendations.value.filter { recommendation ->
            // Filter by position
            if (filter.positions.isNotEmpty() && 
                !filter.positions.contains(recommendation.player.position)) {
                return@filter false
            }
            
            // Filter by matchup rating
            if (filter.minMatchupRating != null && 
                recommendation.matchupRating.ordinal < filter.minMatchupRating.ordinal) {
                return@filter false
            }
            
            // Filter by injury impact
            if (filter.excludeInjured && recommendation.injuryImpact != InjuryImpact.NONE) {
                return@filter false
            }
            
            // Filter by minimum projected points
            if (filter.minProjectedPoints != null && 
                recommendation.projectedPoints < filter.minProjectedPoints) {
                return@filter false
            }
            
            true
        }
        
        _filteredRecommendations.value = filtered
        _uiState.value = _uiState.value.copy(
            filteredCount = filtered.size
        )
    }

    /**
     * Clears all filters and shows all recommendations.
     */
    fun clearFilters() {
        _filterSettings.value = RecommendationFilter()
        _filteredRecommendations.value = _recommendations.value
        _uiState.value = _uiState.value.copy(
            filteredCount = _recommendations.value.size
        )
    }

    /**
     * Gets recommendations for a specific position.
     */
    fun getRecommendationsForPosition(position: String): List<PlayerRecommendation> {
        return _recommendations.value.filter { it.player.position == position }
    }

    /**
     * Gets top N recommendations.
     */
    fun getTopRecommendations(count: Int): List<PlayerRecommendation> {
        return _filteredRecommendations.value.take(count)
    }

    /**
     * Clears recommendations and resets state.
     */
    fun clearRecommendations() {
        _recommendations.value = emptyList()
        _filteredRecommendations.value = emptyList()
        _currentRoster.value = emptyList()
        _filterSettings.value = RecommendationFilter()
        _uiState.value = RecommendationUiState()
    }

    /**
     * Clears error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for recommendation screens with comprehensive state management.
 * Implements requirement 1.4: Proper loading states and error handling.
 * Implements requirement 4.5: Injury status tracking and display.
 */
data class RecommendationUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasData: Boolean = false,
    val playerCount: Int = 0,
    val filteredCount: Int = 0,
    val injuredPlayerCount: Int = 0,
    val error: String? = null
) {
    /**
     * Returns true if any loading operation is in progress.
     */
    val isAnyLoading: Boolean
        get() = isLoading || isRefreshing

    /**
     * Returns true if filters are currently applied.
     */
    val hasActiveFilters: Boolean
        get() = filteredCount != playerCount

    /**
     * Returns summary message for current state.
     */
    val summaryMessage: String
        get() = when {
            !hasData -> "No recommendations available"
            hasActiveFilters -> "Showing $filteredCount of $playerCount players"
            injuredPlayerCount > 0 -> "$playerCount players ($injuredPlayerCount with injury concerns)"
            else -> "$playerCount players analyzed"
        }
}

/**
 * Filter settings for recommendations.
 * Implements requirement 4.4: Advanced filtering and sorting options.
 */
data class RecommendationFilter(
    val positions: Set<String> = emptySet(),
    val minMatchupRating: MatchupRating? = null,
    val minProjectedPoints: Double? = null,
    val excludeInjured: Boolean = false
)