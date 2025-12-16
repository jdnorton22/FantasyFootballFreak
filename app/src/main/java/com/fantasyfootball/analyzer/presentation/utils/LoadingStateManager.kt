package com.fantasyfootball.analyzer.presentation.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages loading states for long-running operations with progress tracking.
 * Implements requirement 1.4: Loading states and progress indicators for long-running operations.
 */
class LoadingStateManager {
    
    private val _loadingOperations = MutableStateFlow<Map<String, LoadingOperation>>(emptyMap())
    val loadingOperations: StateFlow<Map<String, LoadingOperation>> = _loadingOperations.asStateFlow()
    
    private val _isAnyLoading = MutableStateFlow(false)
    val isAnyLoading: StateFlow<Boolean> = _isAnyLoading.asStateFlow()
    
    private val mutex = Mutex()
    
    /**
     * Represents a loading operation with progress tracking
     */
    data class LoadingOperation(
        val id: String,
        val description: String,
        val progress: Float = 0f,
        val isIndeterminate: Boolean = true,
        val startTime: Long = System.currentTimeMillis(),
        val estimatedDuration: Long? = null,
        val canCancel: Boolean = false,
        val onCancel: (() -> Unit)? = null
    ) {
        /**
         * Gets elapsed time in milliseconds
         */
        val elapsedTime: Long
            get() = System.currentTimeMillis() - startTime
        
        /**
         * Gets estimated remaining time in milliseconds
         */
        val estimatedRemainingTime: Long?
            get() = estimatedDuration?.let { duration ->
                if (progress > 0f) {
                    ((duration.toDouble() / progress.toDouble()) - elapsedTime.toDouble()).toLong().coerceAtLeast(0L)
                } else {
                    (duration - elapsedTime).coerceAtLeast(0L)
                }
            }
        
        /**
         * Gets progress percentage as integer
         */
        val progressPercentage: Int
            get() = (progress * 100).toInt()
    }
    
    /**
     * Starts a new loading operation
     */
    suspend fun startLoading(
        operationId: String,
        description: String,
        isIndeterminate: Boolean = true,
        estimatedDuration: Long? = null,
        canCancel: Boolean = false,
        onCancel: (() -> Unit)? = null
    ) {
        mutex.withLock {
            val operation = LoadingOperation(
                id = operationId,
                description = description,
                isIndeterminate = isIndeterminate,
                estimatedDuration = estimatedDuration,
                canCancel = canCancel,
                onCancel = onCancel
            )
            
            val currentOperations = _loadingOperations.value.toMutableMap()
            currentOperations[operationId] = operation
            _loadingOperations.value = currentOperations
            _isAnyLoading.value = true
        }
    }
    
    /**
     * Updates progress for an existing loading operation
     */
    suspend fun updateProgress(operationId: String, progress: Float, description: String? = null) {
        mutex.withLock {
            val currentOperations = _loadingOperations.value.toMutableMap()
            val existingOperation = currentOperations[operationId]
            
            if (existingOperation != null) {
                currentOperations[operationId] = existingOperation.copy(
                    progress = progress.coerceIn(0f, 1f),
                    description = description ?: existingOperation.description,
                    isIndeterminate = false
                )
                _loadingOperations.value = currentOperations
            }
        }
    }
    
    /**
     * Completes a loading operation
     */
    suspend fun completeLoading(operationId: String) {
        mutex.withLock {
            val currentOperations = _loadingOperations.value.toMutableMap()
            currentOperations.remove(operationId)
            _loadingOperations.value = currentOperations
            _isAnyLoading.value = currentOperations.isNotEmpty()
        }
    }
    
    /**
     * Cancels a loading operation
     */
    suspend fun cancelLoading(operationId: String) {
        mutex.withLock {
            val currentOperations = _loadingOperations.value.toMutableMap()
            val operation = currentOperations[operationId]
            
            operation?.onCancel?.invoke()
            currentOperations.remove(operationId)
            _loadingOperations.value = currentOperations
            _isAnyLoading.value = currentOperations.isNotEmpty()
        }
    }
    
    /**
     * Clears all loading operations
     */
    suspend fun clearAll() {
        mutex.withLock {
            _loadingOperations.value = emptyMap()
            _isAnyLoading.value = false
        }
    }
    
    /**
     * Gets a specific loading operation
     */
    fun getOperation(operationId: String): LoadingOperation? {
        return _loadingOperations.value[operationId]
    }
    
    /**
     * Checks if a specific operation is loading
     */
    fun isLoading(operationId: String): Boolean {
        return _loadingOperations.value.containsKey(operationId)
    }
    
    /**
     * Gets all currently loading operations
     */
    fun getAllOperations(): List<LoadingOperation> {
        return _loadingOperations.value.values.toList()
    }
    
    /**
     * Gets the primary loading operation (longest running or highest priority)
     */
    fun getPrimaryOperation(): LoadingOperation? {
        val operations = _loadingOperations.value.values
        return operations.minByOrNull { it.startTime } // Longest running operation
    }
}

/**
 * Predefined operation IDs for common loading scenarios
 */
object LoadingOperationIds {
    const val PLAYER_SEARCH = "player_search"
    const val PLAYER_STATS = "player_stats"
    const val MATCHUP_ANALYSIS = "matchup_analysis"
    const val RECOMMENDATIONS = "recommendations"
    const val DATA_SYNC = "data_sync"
    const val CACHE_REFRESH = "cache_refresh"
    const val HISTORICAL_DATA = "historical_data"
}

/**
 * Extension functions for common loading scenarios
 */
suspend fun LoadingStateManager.startPlayerSearch(query: String) {
    startLoading(
        operationId = LoadingOperationIds.PLAYER_SEARCH,
        description = "Searching for '$query'...",
        isIndeterminate = true,
        estimatedDuration = 3000L // 3 seconds
    )
}

suspend fun LoadingStateManager.startPlayerStatsLoading(playerName: String) {
    startLoading(
        operationId = LoadingOperationIds.PLAYER_STATS,
        description = "Loading stats for $playerName...",
        isIndeterminate = true,
        estimatedDuration = 5000L // 5 seconds
    )
}

suspend fun LoadingStateManager.startMatchupAnalysis(playerName: String, opponent: String) {
    startLoading(
        operationId = LoadingOperationIds.MATCHUP_ANALYSIS,
        description = "Analyzing $playerName vs $opponent...",
        isIndeterminate = false,
        estimatedDuration = 8000L // 8 seconds for complex analysis
    )
}

suspend fun LoadingStateManager.startRecommendationsLoading() {
    startLoading(
        operationId = LoadingOperationIds.RECOMMENDATIONS,
        description = "Generating weekly recommendations...",
        isIndeterminate = false,
        estimatedDuration = 10000L // 10 seconds for multiple player analysis
    )
}

suspend fun LoadingStateManager.startDataSync() {
    startLoading(
        operationId = LoadingOperationIds.DATA_SYNC,
        description = "Synchronizing data...",
        isIndeterminate = false,
        estimatedDuration = 15000L, // 15 seconds for full sync
        canCancel = true
    )
}

suspend fun LoadingStateManager.startCacheRefresh() {
    startLoading(
        operationId = LoadingOperationIds.CACHE_REFRESH,
        description = "Refreshing cached data...",
        isIndeterminate = true,
        estimatedDuration = 5000L // 5 seconds
    )
}

suspend fun LoadingStateManager.startHistoricalDataLoading(seasons: Int) {
    startLoading(
        operationId = LoadingOperationIds.HISTORICAL_DATA,
        description = "Loading $seasons seasons of historical data...",
        isIndeterminate = false,
        estimatedDuration = seasons * 3000L // 3 seconds per season
    )
}

/**
 * Progress update helpers for multi-step operations
 */
suspend fun LoadingStateManager.updateMatchupAnalysisProgress(step: MatchupAnalysisStep) {
    val (progress, description) = when (step) {
        MatchupAnalysisStep.LOADING_PLAYER_DATA -> 0.2f to "Loading player data..."
        MatchupAnalysisStep.LOADING_HISTORICAL_DATA -> 0.4f to "Loading historical matchups..."
        MatchupAnalysisStep.CALCULATING_AVERAGES -> 0.6f to "Calculating averages..."
        MatchupAnalysisStep.GENERATING_PROJECTIONS -> 0.8f to "Generating projections..."
        MatchupAnalysisStep.FINALIZING_ANALYSIS -> 1.0f to "Finalizing analysis..."
    }
    
    updateProgress(LoadingOperationIds.MATCHUP_ANALYSIS, progress, description)
}

suspend fun LoadingStateManager.updateRecommendationsProgress(step: RecommendationsStep, playerCount: Int, currentPlayer: Int) {
    val baseProgress = when (step) {
        RecommendationsStep.LOADING_ROSTER -> 0.1f
        RecommendationsStep.ANALYZING_PLAYERS -> 0.1f + (0.7f * currentPlayer / playerCount)
        RecommendationsStep.RANKING_PLAYERS -> 0.8f
        RecommendationsStep.FINALIZING_RECOMMENDATIONS -> 0.9f
    }
    
    val description = when (step) {
        RecommendationsStep.LOADING_ROSTER -> "Loading roster..."
        RecommendationsStep.ANALYZING_PLAYERS -> "Analyzing player ${currentPlayer + 1} of $playerCount..."
        RecommendationsStep.RANKING_PLAYERS -> "Ranking players..."
        RecommendationsStep.FINALIZING_RECOMMENDATIONS -> "Finalizing recommendations..."
    }
    
    updateProgress(LoadingOperationIds.RECOMMENDATIONS, baseProgress, description)
}

suspend fun LoadingStateManager.updateDataSyncProgress(step: DataSyncStep) {
    val (progress, description) = when (step) {
        DataSyncStep.CHECKING_NETWORK -> 0.1f to "Checking network connection..."
        DataSyncStep.CLEARING_EXPIRED_CACHE -> 0.2f to "Clearing expired cache..."
        DataSyncStep.SYNCING_PLAYER_DATA -> 0.5f to "Syncing player data..."
        DataSyncStep.SYNCING_STATS_DATA -> 0.7f to "Syncing statistics..."
        DataSyncStep.SYNCING_MATCHUP_DATA -> 0.9f to "Syncing matchup data..."
        DataSyncStep.FINALIZING_SYNC -> 1.0f to "Finalizing synchronization..."
    }
    
    updateProgress(LoadingOperationIds.DATA_SYNC, progress, description)
}

/**
 * Enum classes for multi-step operation progress tracking
 */
enum class MatchupAnalysisStep {
    LOADING_PLAYER_DATA,
    LOADING_HISTORICAL_DATA,
    CALCULATING_AVERAGES,
    GENERATING_PROJECTIONS,
    FINALIZING_ANALYSIS
}

enum class RecommendationsStep {
    LOADING_ROSTER,
    ANALYZING_PLAYERS,
    RANKING_PLAYERS,
    FINALIZING_RECOMMENDATIONS
}

enum class DataSyncStep {
    CHECKING_NETWORK,
    CLEARING_EXPIRED_CACHE,
    SYNCING_PLAYER_DATA,
    SYNCING_STATS_DATA,
    SYNCING_MATCHUP_DATA,
    FINALIZING_SYNC
}