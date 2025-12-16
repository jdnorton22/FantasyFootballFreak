package com.fantasyfootball.analyzer.data.search

import com.fantasyfootball.analyzer.data.local.entity.Player
import kotlin.math.max
import kotlin.math.min

/**
 * Fuzzy search engine for player search with typo tolerance and relevance ranking.
 * Implements Levenshtein distance algorithm for fuzzy matching.
 * 
 * Requirements addressed:
 * - 1.1: Player search with fuzzy matching for handling typos and partial names
 */
class FuzzySearchEngine {
    
    companion object {
        private const val MAX_DISTANCE_THRESHOLD = 3
        private const val EXACT_MATCH_SCORE = 100
        private const val PREFIX_MATCH_BONUS = 20
        private const val CONTAINS_MATCH_BONUS = 10
        private const val POSITION_MATCH_BONUS = 15
        private const val TEAM_MATCH_BONUS = 15
    }
    
    /**
     * Searches players with fuzzy matching and relevance scoring.
     * 
     * @param players List of players to search through
     * @param query Search query
     * @return List of players sorted by relevance score
     */
    fun searchPlayers(players: List<Player>, query: String): List<PlayerSearchResult> {
        if (query.isBlank()) return emptyList()
        
        val normalizedQuery = query.trim().lowercase()
        val results = mutableListOf<PlayerSearchResult>()
        
        for (player in players) {
            val score = calculateRelevanceScore(player, normalizedQuery)
            if (score > 0) {
                results.add(
                    PlayerSearchResult(
                        player = player,
                        relevanceScore = score,
                        matchType = determineMatchType(player, normalizedQuery)
                    )
                )
            }
        }
        
        // Sort by relevance score (highest first), then by name
        return results.sortedWith(
            compareByDescending<PlayerSearchResult> { it.relevanceScore }
                .thenBy { it.player.name }
        )
    }
    
    /**
     * Calculates relevance score for a player based on the search query.
     * 
     * @param player Player to score
     * @param query Normalized search query
     * @return Relevance score (0 = no match, higher = better match)
     */
    private fun calculateRelevanceScore(player: Player, query: String): Int {
        val playerName = player.name.lowercase()
        val playerPosition = player.position.lowercase()
        val playerTeam = player.team.lowercase()
        
        var score = 0
        
        // Exact name match gets highest score
        if (playerName == query) {
            return EXACT_MATCH_SCORE
        }
        
        // Check for exact matches in different fields
        if (playerPosition == query) {
            score += POSITION_MATCH_BONUS
        }
        
        if (playerTeam == query) {
            score += TEAM_MATCH_BONUS
        }
        
        // Check for prefix matches
        if (playerName.startsWith(query)) {
            score += PREFIX_MATCH_BONUS
        }
        
        // Check for substring matches
        if (playerName.contains(query)) {
            score += CONTAINS_MATCH_BONUS
        }
        
        // Calculate fuzzy match score for name
        val nameWords = playerName.split(" ")
        val queryWords = query.split(" ")
        
        var bestFuzzyScore = 0
        
        // Check each word in player name against query words
        for (nameWord in nameWords) {
            for (queryWord in queryWords) {
                val distance = levenshteinDistance(nameWord, queryWord)
                val maxLength = max(nameWord.length, queryWord.length)
                
                if (distance <= MAX_DISTANCE_THRESHOLD && maxLength > 0) {
                    val fuzzyScore = ((maxLength - distance) * 10) / maxLength
                    bestFuzzyScore = max(bestFuzzyScore, fuzzyScore)
                }
            }
        }
        
        score += bestFuzzyScore
        
        // Bonus for active players
        if (player.isActive) {
            score += 5
        }
        
        // Penalty for injured players (but still include them)
        if (player.injuryStatus?.contains("OUT", ignoreCase = true) == true) {
            score = max(1, score - 10)
        }
        
        return score
    }
    
    /**
     * Determines the type of match for display purposes.
     */
    private fun determineMatchType(player: Player, query: String): MatchType {
        val playerName = player.name.lowercase()
        val playerPosition = player.position.lowercase()
        val playerTeam = player.team.lowercase()
        
        return when {
            playerName == query -> MatchType.EXACT_NAME
            playerName.startsWith(query) -> MatchType.PREFIX_NAME
            playerName.contains(query) -> MatchType.CONTAINS_NAME
            playerPosition == query -> MatchType.POSITION
            playerTeam == query -> MatchType.TEAM
            else -> MatchType.FUZZY
        }
    }
    
    /**
     * Calculates Levenshtein distance between two strings.
     * Used for fuzzy matching with typo tolerance.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        // Create a matrix to store distances
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        
        // Initialize first row and column
        for (i in 0..len1) {
            matrix[i][0] = i
        }
        for (j in 0..len2) {
            matrix[0][j] = j
        }
        
        // Fill the matrix
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                
                matrix[i][j] = min(
                    min(
                        matrix[i - 1][j] + 1,      // deletion
                        matrix[i][j - 1] + 1       // insertion
                    ),
                    matrix[i - 1][j - 1] + cost    // substitution
                )
            }
        }
        
        return matrix[len1][len2]
    }
    
    /**
     * Generates search suggestions based on partial input.
     * 
     * @param players List of players to generate suggestions from
     * @param partialQuery Partial search query
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggested search terms
     */
    fun generateSuggestions(
        players: List<Player>, 
        partialQuery: String, 
        maxSuggestions: Int = 5
    ): List<String> {
        if (partialQuery.length < 2) return emptyList()
        
        val normalizedQuery = partialQuery.trim().lowercase()
        val suggestions = mutableSetOf<String>()
        
        // Add name suggestions
        for (player in players) {
            val playerName = player.name.lowercase()
            
            // Add full name if it starts with query
            if (playerName.startsWith(normalizedQuery)) {
                suggestions.add(player.name)
            }
            
            // Add individual words that start with query
            val nameWords = player.name.split(" ")
            for (word in nameWords) {
                if (word.lowercase().startsWith(normalizedQuery) && word.length > 2) {
                    suggestions.add(word)
                }
            }
        }
        
        // Add position suggestions
        val positions = players.map { it.position }.distinct()
        for (position in positions) {
            if (position.lowercase().startsWith(normalizedQuery)) {
                suggestions.add(position)
            }
        }
        
        // Add team suggestions
        val teams = players.map { it.team }.distinct()
        for (team in teams) {
            if (team.lowercase().startsWith(normalizedQuery)) {
                suggestions.add(team)
            }
        }
        
        return suggestions.take(maxSuggestions)
    }
}

/**
 * Result of a player search with relevance scoring.
 */
data class PlayerSearchResult(
    val player: Player,
    val relevanceScore: Int,
    val matchType: MatchType
)

/**
 * Type of match found during search.
 */
enum class MatchType {
    EXACT_NAME,
    PREFIX_NAME,
    CONTAINS_NAME,
    POSITION,
    TEAM,
    FUZZY
}