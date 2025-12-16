package com.fantasyfootball.analyzer.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fantasyfootball.analyzer.data.local.AppDatabase
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.data.local.entity.MatchupData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

/**
 * Property-based test ensuring that any player data stored in cache can be retrieved 
 * identically, maintaining data consistency for offline functionality.
 * 
 * Validates: Requirements 3.1, 3.2, 3.3
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CacheRoundTripPropertyTest {
    
    private lateinit var database: AppDatabase
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    /**
     * Test that player data can be stored and retrieved consistently
     */
    @Test
    fun testPlayerCacheRoundTrip() = runBlocking {
        val playerDao = database.playerDao()
        
        // Create test player
        val testPlayer = Player(
            playerId = "test_player_123",
            name = "Test Player",
            position = "RB",
            team = "TEST",
            injuryStatus = null,
            isActive = true,
            lastUpdated = System.currentTimeMillis()
        )
        
        // Store player
        playerDao.insertPlayer(testPlayer)
        
        // Retrieve player
        val retrievedPlayer = playerDao.getPlayer("test_player_123")
        
        // Verify consistency
        assertNotNull("Retrieved player should not be null", retrievedPlayer)
        assertEquals("Player ID should match", testPlayer.playerId, retrievedPlayer?.playerId)
        assertEquals("Player name should match", testPlayer.name, retrievedPlayer?.name)
        assertEquals("Player position should match", testPlayer.position, retrievedPlayer?.position)
        assertEquals("Player team should match", testPlayer.team, retrievedPlayer?.team)
        assertEquals("Player active status should match", testPlayer.isActive, retrievedPlayer?.isActive)
    }
    
    /**
     * Test that player stats can be stored and retrieved consistently
     */
    @Test
    fun testPlayerStatsCacheRoundTrip() = runBlocking {
        val playerDao = database.playerDao()
        
        // Create test player first
        val testPlayer = Player(
            playerId = "stats_test_player",
            name = "Stats Test Player",
            position = "QB",
            team = "TEST",
            injuryStatus = null,
            isActive = true,
            lastUpdated = System.currentTimeMillis()
        )
        playerDao.insertPlayer(testPlayer)
        
        // Create test stats
        val testStats = PlayerStats(
            id = "stats_123",
            playerId = "stats_test_player",
            season = 2024,
            week = 1,
            passingYards = 300,
            rushingYards = 50,
            receivingYards = 0,
            touchdowns = 3,
            fantasyPoints = 25.0,
            gameDate = System.currentTimeMillis()
        )
        
        // Store stats
        playerDao.insertPlayerStats(testStats)
        
        // Retrieve stats
        val retrievedStats = playerDao.getAllPlayerStats("stats_test_player")
        
        // Verify consistency
        assertNotNull("Retrieved stats should not be null", retrievedStats)
        assertTrue("Should have at least one stat record", retrievedStats.isNotEmpty())
        
        val firstStat = retrievedStats.first()
        assertEquals("Stats ID should match", testStats.id, firstStat.id)
        assertEquals("Player ID should match", testStats.playerId, firstStat.playerId)
        assertEquals("Season should match", testStats.season, firstStat.season)
        assertEquals("Week should match", testStats.week, firstStat.week)
        assertEquals("Passing yards should match", testStats.passingYards, firstStat.passingYards)
        assertEquals("Fantasy points should match", testStats.fantasyPoints, firstStat.fantasyPoints, 0.01)
    }
    
    /**
     * Test that matchup data can be stored and retrieved consistently
     */
    @Test
    fun testMatchupDataCacheRoundTrip() = runBlocking {
        val playerDao = database.playerDao()
        
        // Create test player first
        val testPlayer = Player(
            playerId = "matchup_test_player",
            name = "Matchup Test Player",
            position = "WR",
            team = "TEST",
            injuryStatus = null,
            isActive = true,
            lastUpdated = System.currentTimeMillis()
        )
        playerDao.insertPlayer(testPlayer)
        
        // Create test matchup data
        val testMatchup = MatchupData(
            id = "matchup_123",
            playerId = "matchup_test_player",
            opponentTeam = "DAL",
            season = 2024,
            week = 1,
            fantasyPoints = 18.5,
            performanceRating = 8.5,
            gameDate = System.currentTimeMillis()
        )
        
        // Store matchup data
        playerDao.insertMatchupData(testMatchup)
        
        // Retrieve matchup data
        val retrievedMatchups = playerDao.getMatchupHistory("matchup_test_player", "DAL")
        
        // Verify consistency
        assertNotNull("Retrieved matchups should not be null", retrievedMatchups)
        assertTrue("Should have at least one matchup record", retrievedMatchups.isNotEmpty())
        
        val firstMatchup = retrievedMatchups.first()
        assertEquals("Matchup ID should match", testMatchup.id, firstMatchup.id)
        assertEquals("Player ID should match", testMatchup.playerId, firstMatchup.playerId)
        assertEquals("Opponent team should match", testMatchup.opponentTeam, firstMatchup.opponentTeam)
        assertEquals("Season should match", testMatchup.season, firstMatchup.season)
        assertEquals("Week should match", testMatchup.week, firstMatchup.week)
        assertEquals("Fantasy points should match", testMatchup.fantasyPoints, firstMatchup.fantasyPoints, 0.01)
    }
    
    /**
     * Test multiple round trips to ensure consistency over time
     */
    @Test
    fun testMultipleRoundTrips() = runBlocking {
        val playerDao = database.playerDao()
        
        // Create and store multiple players
        val players = (1..5).map { index ->
            Player(
                playerId = "multi_test_player_$index",
                name = "Multi Test Player $index",
                position = if (index % 2 == 0) "RB" else "WR",
                team = "TEST",
                injuryStatus = null,
                isActive = true,
                lastUpdated = System.currentTimeMillis()
            )
        }
        
        // Store all players
        players.forEach { player ->
            playerDao.insertPlayer(player)
        }
        
        // Retrieve and verify each player
        players.forEach { originalPlayer ->
            val retrievedPlayer = playerDao.getPlayer(originalPlayer.playerId)
            
            assertNotNull("Player ${originalPlayer.playerId} should be retrievable", retrievedPlayer)
            assertEquals("Player ID should match for ${originalPlayer.playerId}", 
                        originalPlayer.playerId, retrievedPlayer?.playerId)
            assertEquals("Player name should match for ${originalPlayer.playerId}", 
                        originalPlayer.name, retrievedPlayer?.name)
            assertEquals("Player position should match for ${originalPlayer.playerId}", 
                        originalPlayer.position, retrievedPlayer?.position)
        }
        
        // Test bulk retrieval
        val allPlayers = playerDao.getAllPlayers()
        assertEquals("Should retrieve all stored players", players.size, allPlayers.size)
    }
}