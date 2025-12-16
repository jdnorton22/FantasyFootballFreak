package com.fantasyfootball.analyzer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.data.remote.NetworkResult
import com.fantasyfootball.analyzer.data.search.FuzzySearchEngine
import com.fantasyfootball.analyzer.data.search.PlayerSearchResult
import com.fantasyfootball.analyzer.data.search.SearchHistoryManager
import com.fantasyfootball.analyzer.data.search.SearchSuggestion
import com.fantasyfootball.analyzer.domain.repository.PlayerRepository
import com.fantasyfootball.analyzer.presentation.ui.components.AppError
import com.fantasyfootball.analyzer.presentation.utils.ErrorHandlingUtils
import com.fantasyfootball.analyzer.presentation.utils.ErrorHandlingUtils.toAppError
import com.fantasyfootball.analyzer.presentation.utils.LoadingStateManager
import com.fantasyfootball.analyzer.presentation.utils.startPlayerSearch
import com.fantasyfootball.analyzer.presentation.utils.startPlayerStatsLoading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing player data and search functionality.
 * Handles player search, profile data, and loading states with reactive UI updates.
 * 
 * Requirements addressed:
 * - 1.1: Player search and data retrieval with current season statistics
 * - 1.2: Display fantasy points, yards, touchdowns, position, team, injury status
 * - 1.3: Player profile loading with complete statistical data
 * - 1.4: Loading states, error handling, and appropriate user messaging
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val searchHistoryManager: SearchHistoryManager,
    private val fuzzySearchEngine: FuzzySearchEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PlayerSearchResult>>(emptyList())
    val searchResults: StateFlow<List<PlayerSearchResult>> = _searchResults.asStateFlow()
    
    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val searchSuggestions: StateFlow<List<SearchSuggestion>> = _searchSuggestions.asStateFlow()
    
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()
    
    private val _autocompleteSuggestions = MutableStateFlow<List<String>>(emptyList())
    val autocompleteSuggestions: StateFlow<List<String>> = _autocompleteSuggestions.asStateFlow()
    
    private var allPlayers: List<Player> = emptyList()
    private var currentSearchQuery: String = ""

    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer: StateFlow<Player?> = _selectedPlayer.asStateFlow()

    private val _playerStats = MutableStateFlow<List<PlayerStats>>(emptyList())
    val playerStats: StateFlow<List<PlayerStats>> = _playerStats.asStateFlow()

    private val _isDataFresh = MutableStateFlow(true)
    val isDataFresh: StateFlow<Boolean> = _isDataFresh.asStateFlow()

    // Enhanced error handling and loading state management
    private val loadingStateManager = LoadingStateManager()
    val loadingOperations = loadingStateManager.loadingOperations
    val isAnyLoading = loadingStateManager.isAnyLoading

    private val _currentError = MutableStateFlow<AppError?>(null)
    val currentError: StateFlow<AppError?> = _currentError.asStateFlow()
    
    init {
        // Load initial data
        loadAllPlayers()
        observeSearchHistory()
    }

    /**
     * Loads all players for local fuzzy search.
     */
    private fun loadAllPlayers() {
        viewModelScope.launch {
            try {
                when (val result = playerRepository.getAllPlayers()) {
                    is NetworkResult.Success -> {
                        allPlayers = result.data
                    }
                    is NetworkResult.Error -> {
                        // Try to get cached players
                        allPlayers = playerRepository.getCachedPlayers()
                    }
                    is NetworkResult.Loading -> {
                        // Keep loading
                    }
                }
            } catch (e: Exception) {
                // Fallback to cached players
                allPlayers = playerRepository.getCachedPlayers()
            }
        }
    }
    
    /**
     * Observes search history changes.
     */
    private fun observeSearchHistory() {
        viewModelScope.launch {
            searchHistoryManager.recentSearches.collectLatest { recent ->
                _recentSearches.value = recent
            }
        }
    }
    
    /**
     * Searches for players with fuzzy matching and enhanced error handling.
     * Implements requirement 1.1: Player search with fuzzy matching and autocomplete.
     */
    fun searchPlayers(query: String) {
        currentSearchQuery = query
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchSuggestions.value = emptyList()
            _autocompleteSuggestions.value = emptyList()
            _uiState.value = _uiState.value.copy(isSearching = false, searchError = null)
            _currentError.value = null
            return
        }

        _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
        _currentError.value = null
        
        viewModelScope.launch {
            try {
                // Start loading state with progress tracking
                loadingStateManager.startPlayerSearch(query)
                
                // Perform local fuzzy search first for immediate results
                val localResults = if (allPlayers.isNotEmpty()) {
                    fuzzySearchEngine.searchPlayers(allPlayers, query)
                } else {
                    emptyList()
                }
                
                _searchResults.value = localResults
                
                // Generate autocomplete suggestions
                generateAutocompleteSuggestions(query)
                
                // Try to get fresh data from repository
                when (val result = playerRepository.searchPlayers(query)) {
                    is NetworkResult.Success -> {
                        // Update with fresh results using fuzzy search
                        val fuzzyResults = fuzzySearchEngine.searchPlayers(result.data, query)
                        _searchResults.value = fuzzyResults
                        
                        // Add to search history
                        searchHistoryManager.addToHistory(query, fuzzyResults.size)
                        
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchError = null
                        )
                        loadingStateManager.completeLoading("player_search")
                    }
                    is NetworkResult.Error -> {
                        // Keep local results if available
                        val hasLocalResults = localResults.isNotEmpty()
                        
                        if (hasLocalResults) {
                            // Add to search history with local results
                            searchHistoryManager.addToHistory(query, localResults.size)
                        }
                        
                        val appError = ErrorHandlingUtils.createSearchError(
                            query = query,
                            hasLocalResults = hasLocalResults,
                            retryAction = { searchPlayers(query) }
                        )
                        
                        _currentError.value = appError
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchError = if (hasLocalResults) null else appError.message
                        )
                        loadingStateManager.completeLoading("player_search")
                    }
                    is NetworkResult.Loading -> {
                        // Loading state managed by LoadingStateManager
                    }
                }
            } catch (e: Exception) {
                val hasLocalResults = _searchResults.value.isNotEmpty()
                val appError = ErrorHandlingUtils.createSearchError(
                    query = query,
                    hasLocalResults = hasLocalResults,
                    retryAction = { searchPlayers(query) }
                )
                
                _currentError.value = appError
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = if (hasLocalResults) null else appError.message
                )
                loadingStateManager.completeLoading("player_search")
            }
        }
    }
    
    /**
     * Generates autocomplete suggestions based on partial query.
     */
    private fun generateAutocompleteSuggestions(partialQuery: String) {
        if (partialQuery.length < 2) {
            _autocompleteSuggestions.value = _recentSearches.value.take(5)
            return
        }
        
        // Get suggestions from search history
        val historySuggestions = searchHistoryManager.getSearchSuggestions(partialQuery, 3)
        _searchSuggestions.value = historySuggestions
        
        // Get autocomplete suggestions from fuzzy search engine
        val autocompleteSuggestions = if (allPlayers.isNotEmpty()) {
            fuzzySearchEngine.generateSuggestions(allPlayers, partialQuery, 5)
        } else {
            emptyList()
        }
        
        _autocompleteSuggestions.value = autocompleteSuggestions
    }
    
    /**
     * Handles selection of a player from search results.
     */
    fun onPlayerSelected(playerSearchResult: PlayerSearchResult) {
        // Add to search history with selected player
        searchHistoryManager.addToHistory(
            query = currentSearchQuery,
            resultCount = _searchResults.value.size,
            selectedPlayerId = playerSearchResult.player.playerId
        )
        
        selectPlayer(playerSearchResult.player)
    }
    
    /**
     * Handles selection of a search suggestion.
     */
    fun onSearchSuggestionSelected(suggestion: String) {
        searchPlayers(suggestion)
    }
    
    /**
     * Gets popular searches for display.
     */
    fun getPopularSearches(): List<String> {
        return searchHistoryManager.getPopularSearches(10).map { it.query }
    }
    
    /**
     * Clears search history.
     */
    fun clearSearchHistory() {
        searchHistoryManager.clearHistory()
    }

    /**
     * Selects a player and loads their complete profile data.
     * Implements requirements 1.2, 1.3: Complete player profile with statistics.
     */
    fun selectPlayer(player: Player) {
        _selectedPlayer.value = player
        loadPlayerStats(player.playerId)
        
        // Start observing player data for reactive updates
        observePlayerData(player.playerId)
    }

    /**
     * Loads current season statistics with enhanced error handling and progress tracking.
     * Implements requirements 1.2 and 1.4: Display statistics with comprehensive error handling.
     */
    private fun loadPlayerStats(playerId: String) {
        _uiState.value = _uiState.value.copy(isLoadingStats = true, statsError = null)
        _currentError.value = null
        
        viewModelScope.launch {
            try {
                // Check data freshness first
                _isDataFresh.value = playerRepository.isCacheDataFresh(playerId)
                
                // Start loading with progress tracking
                val playerName = _selectedPlayer.value?.name ?: "Player"
                loadingStateManager.startPlayerStatsLoading(playerName)
                
                val currentSeason = getCurrentSeason()
                when (val result = playerRepository.getPlayerStats(playerId, currentSeason)) {
                    is NetworkResult.Success -> {
                        _playerStats.value = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoadingStats = false,
                            statsError = null,
                            hasData = true
                        )
                        loadingStateManager.completeLoading("player_stats")
                    }
                    is NetworkResult.Error -> {
                        val appError = result.toAppError(
                            retryAction = { loadPlayerStats(playerId) },
                            alternativeAction = if (_playerStats.value.isNotEmpty()) {
                                { /* Show cached data */ }
                            } else null,
                            alternativeActionLabel = if (_playerStats.value.isNotEmpty()) {
                                "Use Cached Data"
                            } else null
                        )
                        
                        _currentError.value = appError
                        _uiState.value = _uiState.value.copy(
                            isLoadingStats = false,
                            statsError = appError.message,
                            hasData = _playerStats.value.isNotEmpty()
                        )
                        loadingStateManager.completeLoading("player_stats")
                    }
                    is NetworkResult.Loading -> {
                        // Loading state managed by LoadingStateManager
                    }
                }
            } catch (e: Exception) {
                val appError = ErrorHandlingUtils.createTimeoutError(
                    operation = "load player statistics",
                    retryAction = { loadPlayerStats(playerId) },
                    useOfflineAction = if (_playerStats.value.isNotEmpty()) {
                        { /* Use cached data */ }
                    } else null
                )
                
                _currentError.value = appError
                _uiState.value = _uiState.value.copy(
                    isLoadingStats = false,
                    statsError = appError.message,
                    hasData = _playerStats.value.isNotEmpty()
                )
                loadingStateManager.completeLoading("player_stats")
            }
        }
    }

    /**
     * Observes player data changes for reactive UI updates.
     * Implements requirement 1.4: Reactive UI updates with LiveData.
     */
    private fun observePlayerData(playerId: String) {
        viewModelScope.launch {
            playerRepository.observePlayer(playerId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to observe player data: ${e.message}"
                    )
                }
                .collectLatest { player ->
                    if (player != null) {
                        _selectedPlayer.value = player
                        // Update data freshness when player data changes
                        _isDataFresh.value = playerRepository.isCacheDataFresh(playerId)
                    }
                }
        }
        
        viewModelScope.launch {
            playerRepository.observePlayerStats(playerId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        statsError = "Failed to observe player stats: ${e.message}"
                    )
                }
                .collectLatest { stats ->
                    _playerStats.value = stats
                    _uiState.value = _uiState.value.copy(hasData = stats.isNotEmpty())
                }
        }
    }

    /**
     * Refreshes player data from network.
     * Implements requirement 1.4: Data refresh functionality.
     */
    fun refreshPlayerData() {
        _selectedPlayer.value?.let { player ->
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            viewModelScope.launch {
                try {
                    // Force refresh from network
                    when (val result = playerRepository.getPlayer(player.playerId, forceRefresh = true)) {
                        is NetworkResult.Success -> {
                            _selectedPlayer.value = result.data
                            loadPlayerStats(player.playerId) // Reload stats too
                            _uiState.value = _uiState.value.copy(isRefreshing = false)
                            _isDataFresh.value = true
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = "Failed to refresh player data: ${result.message}"
                            )
                        }
                        is NetworkResult.Loading -> {
                            // Keep refreshing state
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = "Failed to refresh player data: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clears current error state.
     */
    fun clearError() {
        _currentError.value = null
        _uiState.value = _uiState.value.copy(
            searchError = null,
            statsError = null,
            error = null
        )
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
     * Clears search results.
     */
    fun clearSearch() {
        _searchResults.value = emptyList()
        _uiState.value = _uiState.value.copy(searchError = null)
    }

    /**
     * Clears selected player and associated data.
     */
    fun clearSelection() {
        _selectedPlayer.value = null
        _playerStats.value = emptyList()
        _uiState.value = PlayerUiState()
        _isDataFresh.value = true
    }

    /**
     * Gets current NFL season year.
     */
    private fun getCurrentSeason(): Int {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        
        // NFL season starts in September (month 8 in 0-based calendar)
        return if (currentMonth >= 8) currentYear else currentYear - 1
    }
}

/**
 * UI state for player-related screens with comprehensive loading and error states.
 * Implements requirement 1.4: Proper loading states and error handling.
 */
data class PlayerUiState(
    val isSearching: Boolean = false,
    val isLoadingStats: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasData: Boolean = false,
    val searchError: String? = null,
    val statsError: String? = null,
    val error: String? = null
) {
    /**
     * Returns true if any loading operation is in progress.
     */
    val isLoading: Boolean
        get() = isSearching || isLoadingStats || isRefreshing

    /**
     * Returns true if there are any errors present.
     */
    val hasError: Boolean
        get() = searchError != null || statsError != null || error != null

    /**
     * Returns the most relevant error message for display.
     */
    val displayError: String?
        get() = error ?: statsError ?: searchError
}