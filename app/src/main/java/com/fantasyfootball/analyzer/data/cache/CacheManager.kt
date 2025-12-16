package com.fantasyfootball.analyzer.data.cache

import com.fantasyfootball.analyzer.data.local.dao.PlayerDao
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages caching operations for player data, statistics, and matchup information.
 * Implements cache size monitoring, data freshness validation, and automatic cleanup.
 * 
 * Requirements addressed:
 * - 3.1: Store retrieved data in cache for offline access
 * - 3.2: Display cached data when network unavailable
 * - 3.3: Indicate data freshness to users
 * - 3.5: Remove oldest entries when cache exceeds 100MB
 */
@Singleton
class CacheManager @Inject constructor(
    private val playerDao: PlayerDao
) {
    
    companion object {
        // Cache size limit in bytes (100MB)
        private const val MAX_CACHE_SIZE_BYTES = 100 * 1024 * 1024L
        
        // Data freshness threshold (24 hours in milliseconds)
        private const val DATA_FRESHNESS_THRESHOLD = 24 * 60 * 60 * 1000L
        
        // Estimated bytes per entity for cache size calculation
        private const val BYTES_PER_PLAYER = 200L
        private const val BYTES_PER_PLAYER_STATS = 150L
        private const val BYTES_PER_MATCHUP_DATA = 180L
    }
    
    /**
     * Caches player data with current timestamp
     */
    suspend fun cachePlayerData(player: Player) = withContext(Dispatchers.IO) {
        val updatedPlayer = player.copy(lastUpdated = System.currentTimeMillis())
        playerDao.insertPlayer(updatedPlayer)
        
        // Check cache size after insertion
        checkAndManageCacheSize()
    }
    
    /**
     * Caches multiple players efficiently
     */
    suspend fun cachePlayersData(players: List<Player>) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val updatedPlayers = players.map { it.copy(lastUpdated = currentTime) }
        playerDao.insertPlayers(updatedPlayers)
        
        // Check cache size after bulk insertion
        checkAndManageCacheSize()
    }
    
    /**
     * Retrieves cached player data if available
     */
    suspend fun getCachedPlayer(playerId: String): Player? = withContext(Dispatchers.IO) {
        playerDao.getPlayer(playerId)
    }
    
    /**
     * Caches player statistics data
     */
    suspend fun cachePlayerStats(stats: PlayerStats) = withContext(Dispatchers.IO) {
        playerDao.insertPlayerStats(stats)
        checkAndManageCacheSize()
    }
    
    /**
     * Caches multiple player statistics efficiently
     */
    suspend fun cachePlayerStatsList(statsList: List<PlayerStats>) = withContext(Dispatchers.IO) {
        playerDao.insertPlayerStatsList(statsList)
        checkAndManageCacheSize()
    }
    
    /**
     * Retrieves cached player statistics
     */
    suspend fun getCachedPlayerStats(playerId: String, season: Int): List<PlayerStats> = withContext(Dispatchers.IO) {
        playerDao.getPlayerStatsBySeason(playerId, season)
    }
    
    /**
     * Caches matchup data
     */
    suspend fun cacheMatchupData(matchupData: MatchupData) = withContext(Dispatchers.IO) {
        playerDao.insertMatchupData(matchupData)
        checkAndManageCacheSize()
    }
    
    /**
     * Caches multiple matchup data entries efficiently
     */
    suspend fun cacheMatchupDataList(matchupDataList: List<MatchupData>) = withContext(Dispatchers.IO) {
        playerDao.insertMatchupDataList(matchupDataList)
        checkAndManageCacheSize()
    }
    
    /**
     * Retrieves cached matchup data
     */
    suspend fun getCachedMatchupData(playerId: String, opponentTeam: String): List<MatchupData> = withContext(Dispatchers.IO) {
        playerDao.getMatchupHistory(playerId, opponentTeam)
    }
    
    /**
     * Checks if cached data is still valid (within 24 hours)
     */
    suspend fun isCacheValid(playerId: String): Boolean = withContext(Dispatchers.IO) {
        val player = playerDao.getPlayer(playerId)
        if (player == null) {
            false
        } else {
            val currentTime = System.currentTimeMillis()
            (currentTime - player.lastUpdated) <= DATA_FRESHNESS_THRESHOLD
        }
    }
    
    /**
     * Checks if cached data is fresh (within threshold)
     * Returns true if data is fresh, false if stale or missing
     */
    suspend fun isCacheFresh(playerId: String): Boolean = withContext(Dispatchers.IO) {
        isCacheValid(playerId)
    }
    
    /**
     * Gets the age of cached data in milliseconds
     * Returns null if player not found in cache
     */
    suspend fun getCacheAge(playerId: String): Long? = withContext(Dispatchers.IO) {
        val player = playerDao.getPlayer(playerId)
        player?.let { 
            System.currentTimeMillis() - it.lastUpdated 
        }
    }
    
    /**
     * Clears expired cache entries (older than 24 hours)
     */
    suspend fun clearExpiredCache() = withContext(Dispatchers.IO) {
        val expirationTime = System.currentTimeMillis() - DATA_FRESHNESS_THRESHOLD
        playerDao.deleteOutdatedPlayers(expirationTime)
    }
    
    /**
     * Gets current cache size in bytes (estimated)
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        val playerCount = playerDao.getPlayerCount()
        val statsCount = playerDao.getPlayerStatsCount()
        val matchupCount = playerDao.getMatchupDataCount()
        
        (playerCount * BYTES_PER_PLAYER) + 
        (statsCount * BYTES_PER_PLAYER_STATS) + 
        (matchupCount * BYTES_PER_MATCHUP_DATA)
    }
    
    /**
     * Checks cache size and performs cleanup if necessary
     * Removes oldest entries first when cache exceeds limit
     */
    private suspend fun checkAndManageCacheSize() {
        val currentSize = getCacheSize()
        
        if (currentSize > MAX_CACHE_SIZE_BYTES) {
            performCacheEviction()
        }
    }
    
    /**
     * Performs cache eviction by removing oldest entries
     * Continues until cache size is under the limit
     */
    private suspend fun performCacheEviction() {
        // Calculate target size (90% of max to avoid frequent evictions)
        val targetSize = (MAX_CACHE_SIZE_BYTES * 0.9).toLong()
        
        var currentSize = getCacheSize()
        
        // Remove expired data first
        clearExpiredCache()
        currentSize = getCacheSize()
        
        // If still over limit, remove oldest entries
        while (currentSize > targetSize) {
            val oldestTimestamp = getOldestCacheTimestamp()
            if (oldestTimestamp != null) {
                // Remove entries older than the oldest timestamp + 1 hour buffer
                val evictionThreshold = oldestTimestamp + (60 * 60 * 1000L)
                playerDao.deleteOutdatedPlayers(evictionThreshold)
                
                val newSize = getCacheSize()
                if (newSize >= currentSize) {
                    // No progress made, break to avoid infinite loop
                    break
                }
                currentSize = newSize
            } else {
                // No data to evict
                break
            }
        }
    }
    
    /**
     * Gets the timestamp of the oldest cached entry
     */
    private suspend fun getOldestCacheTimestamp(): Long? {
        // This would require a custom query to find the oldest lastUpdated timestamp
        // For now, we'll use a simple approach by getting expired data count
        val currentTime = System.currentTimeMillis()
        val oneDayAgo = currentTime - DATA_FRESHNESS_THRESHOLD
        val oneWeekAgo = currentTime - (7 * DATA_FRESHNESS_THRESHOLD)
        
        // Check if there are entries older than one week
        val veryOldCount = playerDao.getOutdatedPlayersCount(oneWeekAgo)
        if (veryOldCount > 0) {
            return oneWeekAgo
        }
        
        // Check if there are entries older than one day
        val oldCount = playerDao.getOutdatedPlayersCount(oneDayAgo)
        if (oldCount > 0) {
            return oneDayAgo
        }
        
        return null
    }
    
    /**
     * Clears all cached data
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        // Note: This would require additional DAO methods to clear all tables
        // For now, we'll clear by setting a future timestamp
        val futureTime = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L) // 1 year from now
        playerDao.deleteOutdatedPlayers(futureTime)
    }
    
    /**
     * Gets cache statistics for monitoring
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        CacheStats(
            totalSizeBytes = getCacheSize(),
            maxSizeBytes = MAX_CACHE_SIZE_BYTES,
            playerCount = playerDao.getPlayerCount(),
            statsCount = playerDao.getPlayerStatsCount(),
            matchupCount = playerDao.getMatchupDataCount(),
            expiredCount = playerDao.getOutdatedPlayersCount(System.currentTimeMillis() - DATA_FRESHNESS_THRESHOLD)
        )
    }
}

/**
 * Data class representing cache statistics
 */
data class CacheStats(
    val totalSizeBytes: Long,
    val maxSizeBytes: Long,
    val playerCount: Int,
    val statsCount: Int,
    val matchupCount: Int,
    val expiredCount: Int
) {
    val usagePercentage: Double = (totalSizeBytes.toDouble() / maxSizeBytes.toDouble()) * 100.0
    val isNearLimit: Boolean = usagePercentage > 80.0
}