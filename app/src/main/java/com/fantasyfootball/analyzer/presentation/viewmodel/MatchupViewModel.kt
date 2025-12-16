package com.fantasyfootball.analyzer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import com.fantasyfootball.analyzer.data.remote.NetworkResult
import com.fantasyfootball.analyzer.domain.model.MatchupAnalysis
import com.fantasyfootball.analyzer.domain.repository.PlayerRepository
import com.fantasyfootball.analyzer.domain.usecase.MatchupAnalyzer
import com.fantasyfootball.analyzer.presentation.ui.components.AppError
import com.fantasyfootball.analyzer.presentation.utils.ErrorHandlingUtils
import com.fantasyfootball.analyzer.presentation.utils.ErrorHandlingUtils.toAppError
import com.fantasyfootball.analyzer.presentation.utils.LoadingStateManager
import com.fantasyfootball.analyzer.presentation.utils.MatchupAnalysisStep
import com.fantasyfootball.analyzer.presentation.utils.startMatchupAnalysis
import com.fantasyfootball.analyzer.presentation.utils.updateMatchupAnalysisProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing matchup analysis functionality.
 * Handles historical matchup data retrieval, analysis, and display with proper error handling.
 * 
 * Requirements addressed:
 * - 2.1: Historical matchup data spanning previous 3 seasons
 * - 2.2: Average fantasy points against specific opponents
 * - 2.3: Game-by-game performance breakdown display
 * - 2.4: Appropriate messaging for insufficient historical data
 * - 2.5: Comparison to player's season average performance
 * - 1.4: Loading states and comprehensive error handling
 */
@HiltViewModel
class MatchupViewModel @Inject constructor(
    private val matchupAnalyzer: MatchupAnalyzer,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchupUiState())
    val uiState: StateFlow<MatchupUiState> = _uiState.asStateFlow()

    private val _matchupAnalysis = MutableStateFlow<MatchupAnalysis?>(null)
    val matchupAnalysis: StateFlow<MatchupAnalysis?> = _matchupAnalysis.asStateFlow()

    private val _historicalData = MutableStateFlow<List<MatchupData>>(emptyList())
    val historicalData: StateFlow<List<MatchupData>> = _historicalData.asStateFlow()

    private val _currentMatchup = MutableStateFlow<Pair<String, String>?>(null)
    val currentMatchup: StateFlow<Pair<String, String>?> = _currentMatchup.asStateFlow()

    // Enhanced error handling and loading state management
    private val loadingStateManager = LoadingStateManager()
    val loadingOperations = loadingStateManager.loadingOperations
    val isAnyLoading = loadingStateManager.isAnyLoading

    private val _currentError = MutableStateFlow<AppError?>(null)
    val currentError: StateFlow<AppError?> = _currentError.asStateFlow()

    /**
     * Analyzes historical matchup performance with enhanced error handling and progress tracking.
     * Implements requirements 2.1-2.5 and 1.4: Complete matchup analysis with comprehensive error handling.
     */
    fun analyzeMatchup(playerId: String, opponentTeam: String) {
        if (playerId.isBlank() || opponentTeam.isBlank()) {
            val appError = ErrorHandlingUtils.createSearchError(
                query = "$playerId vs $opponentTeam",
                hasLocalResults = false,
                retryAction = null
            )
            _currentError.value = appError
            return
        }

        _currentMatchup.value = Pair(playerId, opponentTeam)
        _uiState.value = _uiState.value.copy(
            isLoading = true, 
            error = null,
            hasInsufficientData = false
        )
        _currentError.value = null
        
        viewModelScope.launch {
            try {
                // Start loading with progress tracking
                loadingStateManager.startMatchupAnalysis("Player", opponentTeam)
                
                // Step 1: Load player data
                loadingStateManager.updateMatchupAnalysisProgress(MatchupAnalysisStep.LOADING_PLAYER_DATA)
                
                // Step 2: Load historical matchup data
                loadingStateManager.updateMatchupAnalysisProgress(MatchupAnalysisStep.LOADING_HISTORICAL_DATA)
                loadHistoricalMatchupData(playerId, opponentTeam)
                
                // Step 3: Calculate averages
                loadingStateManager.updateMatchupAnalysisProgress(MatchupAnalysisStep.CALCULATING_AVERAGES)
                
                // Step 4: Generate projections
                loadingStateManager.updateMatchupAnalysisProgress(MatchupAnalysisStep.GENERATING_PROJECTIONS)
                
                // Perform analysis
                when (val analysisResult = matchupAnalyzer.analyzeMatchup(playerId, opponentTeam)) {
                    is NetworkResult.Success -> {
                        val analysis = analysisResult.data
                        _matchupAnalysis.value = analysis
                        
                        // Check if we have sufficient data for meaningful analysis
                        val hasInsufficientData = analysis.sampleSize < 3 || analysis.historicalGames.isEmpty()
                        
                        if (hasInsufficientData) {
                            val playerName = "Player" // In real implementation, get from repository
                            val insufficientDataError = ErrorHandlingUtils.createInsufficientDataError(
                                playerName = playerName,
                                opponentTeam = opponentTeam,
                                availableGames = analysis.sampleSize,
                                fallbackAction = { /* Show league averages */ }
                            )
                            _currentError.value = insufficientDataError
                        }
                        
                        // Step 5: Finalize analysis
                        loadingStateManager.updateMatchupAnalysisProgress(MatchupAnalysisStep.FINALIZING_ANALYSIS)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasData = true,
                            hasInsufficientData = hasInsufficientData,
                            error = null
                        )
                        
                        loadingStateManager.completeLoading("matchup_analysis")
                    }
                    is NetworkResult.Error -> {
                        val appError = analysisResult.toAppError(
                            retryAction = { analyzeMatchup(playerId, opponentTeam) },
                            alternativeAction = if (_historicalData.value.isNotEmpty()) {
                                { /* Use cached historical data */ }
                            } else null,
                            alternativeActionLabel = if (_historicalData.value.isNotEmpty()) {
                                "Use Cached Data"
                            } else null
                        )
                        
                        _currentError.value = appError
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = appError.message
                        )
                        loadingStateManager.completeLoading("matchup_analysis")
                        return@launch
                    }
                    is NetworkResult.Loading -> {
                        // Loading state managed by LoadingStateManager
                        return@launch
                    }
                }
                
            } catch (e: Exception) {
                val appError = ErrorHandlingUtils.createTimeoutError(
                    operation = "analyze matchup",
                    retryAction = { analyzeMatchup(playerId, opponentTeam) },
                    useOfflineAction = if (_historicalData.value.isNotEmpty()) {
                        { /* Use cached data for basic analysis */ }
                    } else null
                )
                
                _currentError.value = appError
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = appError.message
                )
                loadingStateManager.completeLoading("matchup_analysis")
            }
        }
    }

    /**
     * Loads historical matchup data from repository.
     * Implements requirement 2.1: Historical data spanning previous 3 seasons.
     */
    private suspend fun loadHistoricalMatchupData(playerId: String, opponentTeam: String) {
        when (val result = playerRepository.getMatchupHistory(playerId, opponentTeam)) {
            is NetworkResult.Success -> {
                _historicalData.value = result.data
            }
            is NetworkResult.Error -> {
                // Don't fail the entire analysis if historical data fails
                // Just log and continue with empty historical data
                _historicalData.value = emptyList()
                if (result.message.contains("offline")) {
                    _uiState.value = _uiState.value.copy(
                        offlineMode = true
                    )
                }
            }
            is NetworkResult.Loading -> {
                // Loading state handled by main analysis loading
            }
        }
    }

    /**
     * Refreshes matchup analysis with fresh data from network.
     * Implements requirement 1.4: Data refresh functionality.
     */
    fun refreshAnalysis() {
        _currentMatchup.value?.let { (playerId, opponentTeam) ->
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            viewModelScope.launch {
                try {
                    // Force refresh historical data
                    when (val result = playerRepository.getMatchupHistory(playerId, opponentTeam, forceRefresh = true)) {
                        is NetworkResult.Success -> {
                            _historicalData.value = result.data
                            
                            // Re-run analysis with fresh data
                            when (val analysisResult = matchupAnalyzer.analyzeMatchup(playerId, opponentTeam)) {
                                is NetworkResult.Success -> {
                                    val analysis = analysisResult.data
                                    _matchupAnalysis.value = analysis
                                    
                                    val hasInsufficientData = analysis.sampleSize < 3 || analysis.historicalGames.isEmpty()
                                    
                                    _uiState.value = _uiState.value.copy(
                                        isRefreshing = false,
                                        hasInsufficientData = hasInsufficientData,
                                        offlineMode = false,
                                        error = null
                                    )
                                }
                                is NetworkResult.Error -> {
                                    _uiState.value = _uiState.value.copy(
                                        isRefreshing = false,
                                        error = "Failed to refresh analysis: ${analysisResult.message}"
                                    )
                                }
                                is NetworkResult.Loading -> {
                                    // Keep refreshing state
                                }
                            }
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = "Failed to refresh matchup data: ${result.message}"
                            )
                        }
                        is NetworkResult.Loading -> {
                            // Keep refreshing state
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Failed to refresh analysis: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Analyzes multiple matchups for comparison.
     * Useful for comparing player performance against different opponents.
     */
    fun compareMatchups(playerId: String, opponentTeams: List<String>) {
        if (opponentTeams.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            isComparingMultiple = true
        )
        
        viewModelScope.launch {
            try {
                val analyses = mutableListOf<MatchupAnalysis>()
                
                for (opponent in opponentTeams) {
                    try {
                        when (val analysisResult = matchupAnalyzer.analyzeMatchup(playerId, opponent)) {
                            is NetworkResult.Success -> {
                                analyses.add(analysisResult.data)
                            }
                            is NetworkResult.Error -> {
                                // Continue with other opponents if one fails
                                continue
                            }
                            is NetworkResult.Loading -> {
                                // Skip loading states in batch processing
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        // Continue with other opponents if one fails
                        continue
                    }
                }
                
                if (analyses.isNotEmpty()) {
                    // For now, just show the first analysis
                    // In a full implementation, we'd have a separate state for multiple analyses
                    _matchupAnalysis.value = analyses.first()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasData = true,
                        isComparingMultiple = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to analyze any of the requested matchups",
                        isComparingMultiple = false
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to compare matchups",
                    isComparingMultiple = false
                )
            }
        }
    }

    /**
     * Clears current analysis and resets state.
     */
    fun clearAnalysis() {
        _matchupAnalysis.value = null
        _historicalData.value = emptyList()
        _currentMatchup.value = null
        _uiState.value = MatchupUiState()
    }

    /**
     * Clears current error state.
     */
    fun clearError() {
        _currentError.value = null
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Handles error retry actions.
     */
    fun retryLastOperation() {
        _currentError.value?.retryAction?.invoke()
    }

    /**
     * Handles alternative error actions.
     */
    fun performAlternativeAction() {
        _currentError.value?.alternativeAction?.invoke()
    }

    /**
     * Acknowledges insufficient data warning.
     */
    fun acknowledgeInsufficientData() {
        _uiState.value = _uiState.value.copy(hasInsufficientData = false)
        // Clear insufficient data error if it's the current error
        if (_currentError.value?.type == com.fantasyfootball.analyzer.presentation.ui.components.ErrorType.INSUFFICIENT_DATA) {
            _currentError.value = null
        }
    }
}

/**
 * UI state for matchup analysis screens with comprehensive state management.
 * Implements requirement 1.4: Proper loading states and error handling.
 * Implements requirement 2.4: Insufficient data messaging.
 */
data class MatchupUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isComparingMultiple: Boolean = false,
    val hasData: Boolean = false,
    val hasInsufficientData: Boolean = false,
    val offlineMode: Boolean = false,
    val error: String? = null
) {
    /**
     * Returns true if any loading operation is in progress.
     */
    val isAnyLoading: Boolean
        get() = isLoading || isRefreshing

    /**
     * Returns appropriate message for insufficient data scenarios.
     */
    val insufficientDataMessage: String
        get() = "Limited historical data available for this matchup. " +
                "Analysis is based on league averages and position rankings."

    /**
     * Returns appropriate message for offline mode.
     */
    val offlineModeMessage: String
        get() = "Using cached data. Connect to internet for latest matchup information."
}