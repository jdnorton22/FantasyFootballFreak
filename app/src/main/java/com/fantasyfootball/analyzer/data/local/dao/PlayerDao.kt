package com.fantasyfootball.analyzer.data.local.dao

import androidx.room.*
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Player-related database operations.
 * Provides CRUD operations and complex queries for player data.
 */
@Dao
interface PlayerDao {
    
    // Player CRUD operations
    @Query("SELECT * FROM players WHERE playerId = :playerId")
    suspend fun getPlayer(playerId: String): Player?
    
    @Query("SELECT * FROM players WHERE playerId = :playerId")
    fun observePlayer(playerId: String): Flow<Player?>
    
    @Query("SELECT * FROM players WHERE name LIKE '%' || :query || '%' OR team LIKE '%' || :query || '%'")
    suspend fun searchPlayers(query: String): List<Player>
    
    @Query("SELECT * FROM players ORDER BY name ASC")
    suspend fun getAllPlayers(): List<Player>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<Player>)
    
    @Update
    suspend fun updatePlayer(player: Player)
    
    @Delete
    suspend fun deletePlayer(player: Player)
    
    @Query("DELETE FROM players WHERE playerId = :playerId")
    suspend fun deletePlayerById(playerId: String)
    
    // PlayerStats operations
    @Query("SELECT * FROM player_stats WHERE playerId = :playerId AND season = :season ORDER BY week ASC")
    suspend fun getPlayerStatsBySeason(playerId: String, season: Int): List<PlayerStats>
    
    @Query("SELECT * FROM player_stats WHERE playerId = :playerId AND season = :season AND week = :week")
    suspend fun getPlayerStatsForWeek(playerId: String, season: Int, week: Int): PlayerStats?
    
    @Query("SELECT * FROM player_stats WHERE playerId = :playerId ORDER BY gameDate DESC")
    suspend fun getAllPlayerStats(playerId: String): List<PlayerStats>
    
    @Query("SELECT * FROM player_stats WHERE playerId = :playerId ORDER BY gameDate DESC")
    fun observePlayerStats(playerId: String): Flow<List<PlayerStats>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: PlayerStats)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStatsList(statsList: List<PlayerStats>)
    
    @Query("DELETE FROM player_stats WHERE playerId = :playerId")
    suspend fun deletePlayerStats(playerId: String)
    
    // MatchupData operations
    @Query("SELECT * FROM matchup_data WHERE playerId = :playerId AND opponentTeam = :opponentTeam ORDER BY gameDate DESC")
    suspend fun getMatchupHistory(playerId: String, opponentTeam: String): List<MatchupData>
    
    @Query("SELECT * FROM matchup_data WHERE playerId = :playerId AND opponentTeam = :opponentTeam AND season >= :minSeason ORDER BY gameDate DESC")
    suspend fun getRecentMatchupHistory(playerId: String, opponentTeam: String, minSeason: Int): List<MatchupData>
    
    @Query("SELECT * FROM matchup_data WHERE playerId = :playerId ORDER BY gameDate DESC")
    suspend fun getAllMatchupData(playerId: String): List<MatchupData>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchupData(matchupData: MatchupData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchupDataList(matchupDataList: List<MatchupData>)
    
    @Query("DELETE FROM matchup_data WHERE playerId = :playerId")
    suspend fun deleteMatchupData(playerId: String)
    
    // Complex queries for analytics
    @Query("""
        SELECT AVG(fantasyPoints) 
        FROM matchup_data 
        WHERE playerId = :playerId AND opponentTeam = :opponentTeam
    """)
    suspend fun getAverageFantasyPointsVsOpponent(playerId: String, opponentTeam: String): Double?
    
    @Query("""
        SELECT AVG(fantasyPoints) 
        FROM player_stats 
        WHERE playerId = :playerId AND season = :season
    """)
    suspend fun getSeasonAverageFantasyPoints(playerId: String, season: Int): Double?
    
    @Query("""
        SELECT COUNT(*) 
        FROM players 
        WHERE lastUpdated < :timestamp
    """)
    suspend fun getOutdatedPlayersCount(timestamp: Long): Int
    
    @Query("""
        DELETE FROM players 
        WHERE lastUpdated < :timestamp
    """)
    suspend fun deleteOutdatedPlayers(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int
    
    @Query("SELECT COUNT(*) FROM player_stats")
    suspend fun getPlayerStatsCount(): Int
    
    @Query("SELECT COUNT(*) FROM matchup_data")
    suspend fun getMatchupDataCount(): Int
}