package com.fantasyfootball.analyzer.domain.repository

import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import com.fantasyfootball.analyzer.data.remote.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for player data operations.
 * Defines the contract for accessing player information, statistics, and matchup data
 * with offline-first architecture and single source of truth pattern.
 * 
 * Requirements addressed:
 * - 3.1, 3.2: Offline-first data access with caching
 * - 3.4: Data synchronization between local and remote sources
 * - 6.1, 6.2: Request throttling and duplicate request prevention
 */
interface PlayerRepository {
    
    /**
     * Retrieves a player by ID with offline-first approach.
     * Returns cached data immediately if available, then updates from network.
     * 
     * @param playerId The unique player identifier
     * @param forceRefresh Whether to force a network refresh
     * @return NetworkResult containing player data or error
     */
    suspend fun getPlayer(playerId: String, forceRefresh: Boolean = false): NetworkResult<Player>
    
    /**
     * Observes player data changes with reactive updates.
     * 
     * @param playerId The unique player identifier
     * @return Flow of player data that updates when cache changes
     */
    fun observePlayer(playerId: String): Flow<Player?>
    
    /**
     * Retrieves player statistics for a specific season.
     * Implements caching and network synchronization.
     * 
     * @param playerId The unique player identifier
     * @param season The NFL season year
     * @param forceRefresh Whether to force a network refresh
     * @return NetworkResult containing player statistics or error
     */
    suspend fun getPlayerStats(playerId: String, season: Int, forceRefresh: Boolean = false): NetworkResult<List<PlayerStats>>
    
    /**
     * Observes player statistics changes with reactive updates.
     * 
     * @param playerId The unique player identifier
     * @return Flow of player statistics that updates when cache changes
     */
    fun observePlayerStats(playerId: String): Flow<List<PlayerStats>>
    
    /**
     * Retrieves historical matchup data between a player and opponent team.
     * Returns data spanning the previous 3 seasons as per requirements.
     * 
     * @param playerId The unique player identifier
     * @param opponentTeam The opposing team name
     * @param forceRefresh Whether to force a network refresh
     * @return NetworkResult containing matchup history or error
     */
    suspend fun getMatchupHistory(playerId: String, opponentTeam: String, forceRefresh: Boolean = false): NetworkResult<List<MatchupData>>
    
    /**
     * Searches for players by name or team with fuzzy matching.
     * Implements request throttling and caching for search results.
     * 
     * @param query The search query
     * @param forceRefresh Whether to force a network refresh
     * @return NetworkResult containing list of matching players or error
     */
    suspend fun searchPlayers(query: String, forceRefresh: Boolean = false): NetworkResult<List<Player>>
    
    /**
     * Synchronizes all cached data with remote sources.
     * Called automatically when network connectivity is restored.
     * 
     * @return NetworkResult indicating sync success or failure
     */
    suspend fun syncData(): NetworkResult<Unit>
    
    /**
     * Clears expired cache data and performs maintenance.
     * 
     * @return NetworkResult indicating cleanup success or failure
     */
    suspend fun clearExpiredCache(): NetworkResult<Unit>
    
    /**
     * Checks if cached data is available for offline access.
     * 
     * @param playerId The unique player identifier
     * @return True if cached data exists and is usable offline
     */
    suspend fun hasCachedData(playerId: String): Boolean
    
    /**
     * Gets the freshness status of cached data.
     * 
     * @param playerId The unique player identifier
     * @return True if cached data is fresh (within 24 hours)
     */
    suspend fun isCacheDataFresh(playerId: String): Boolean
    
    /**
     * Retrieves all players for local search operations.
     * 
     * @param forceRefresh Whether to force a network refresh
     * @return NetworkResult containing all players or error
     */
    suspend fun getAllPlayers(forceRefresh: Boolean = false): NetworkResult<List<Player>>
    
    /**
     * Gets cached players for offline search functionality.
     * 
     * @return List of cached players
     */
    suspend fun getCachedPlayers(): List<Player>
}