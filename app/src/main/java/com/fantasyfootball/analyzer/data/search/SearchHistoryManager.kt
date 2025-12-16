package com.fantasyfootball.analyzer.data.search

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages search history and recent searches functionality.
 * Provides persistent storage and retrieval of user search queries.
 * 
 * Requirements addressed:
 * - 1.1: Search history and recent searches functionality
 */
@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    
    companion object {
        private const val PREFS_NAME = "search_history_prefs"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val MAX_HISTORY_SIZE = 50
        private const val MAX_RECENT_SIZE = 10
    }
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory.asStateFlow()
    
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()
    
    init {
        loadSearchHistory()
        loadRecentSearches()
    }
    
    /**
     * Adds a search query to history with metadata.
     * 
     * @param query The search query
     * @param resultCount Number of results returned
     * @param selectedPlayerId ID of player selected from results (if any)
     */
    fun addToHistory(query: String, resultCount: Int, selectedPlayerId: String? = null) {
        if (query.isBlank()) return
        
        val trimmedQuery = query.trim()
        val timestamp = System.currentTimeMillis()
        
        // Update search history
        val currentHistory = _searchHistory.value.toMutableList()
        
        // Remove existing entry if present
        currentHistory.removeAll { it.query.equals(trimmedQuery, ignoreCase = true) }
        
        // Add new entry at the beginning
        val newItem = SearchHistoryItem(
            query = trimmedQuery,
            timestamp = timestamp,
            resultCount = resultCount,
            selectedPlayerId = selectedPlayerId,
            frequency = getQueryFrequency(trimmedQuery) + 1
        )
        
        currentHistory.add(0, newItem)
        
        // Limit size
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _searchHistory.value = currentHistory
        saveSearchHistory()
        
        // Update recent searches
        addToRecentSearches(trimmedQuery)
    }
    
    /**
     * Adds a query to recent searches list.
     */
    private fun addToRecentSearches(query: String) {
        val currentRecent = _recentSearches.value.toMutableList()
        
        // Remove existing entry if present
        currentRecent.remove(query)
        
        // Add to beginning
        currentRecent.add(0, query)
        
        // Limit size
        if (currentRecent.size > MAX_RECENT_SIZE) {
            currentRecent.removeAt(currentRecent.size - 1)
        }
        
        _recentSearches.value = currentRecent
        saveRecentSearches()
    }
    
    /**
     * Gets search suggestions based on history and partial query.
     * 
     * @param partialQuery Partial search query
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggested search terms
     */
    fun getSearchSuggestions(partialQuery: String, maxSuggestions: Int = 5): List<SearchSuggestion> {
        if (partialQuery.length < 2) {
            // Return recent searches for short queries
            return _recentSearches.value.take(maxSuggestions).map { query ->
                SearchSuggestion(
                    query = query,
                    type = SuggestionType.RECENT,
                    frequency = getQueryFrequency(query)
                )
            }
        }
        
        val normalizedQuery = partialQuery.trim().lowercase()
        val suggestions = mutableListOf<SearchSuggestion>()
        
        // Get matching history items
        val historyMatches = _searchHistory.value
            .filter { it.query.lowercase().contains(normalizedQuery) }
            .sortedWith(
                compareByDescending<SearchHistoryItem> { it.frequency }
                    .thenByDescending { it.timestamp }
            )
            .take(maxSuggestions)
            .map { historyItem ->
                SearchSuggestion(
                    query = historyItem.query,
                    type = SuggestionType.HISTORY,
                    frequency = historyItem.frequency,
                    resultCount = historyItem.resultCount
                )
            }
        
        suggestions.addAll(historyMatches)
        
        return suggestions.take(maxSuggestions)
    }
    
    /**
     * Gets popular search queries based on frequency.
     * 
     * @param maxResults Maximum number of results to return
     * @return List of popular search queries
     */
    fun getPopularSearches(maxResults: Int = 10): List<SearchHistoryItem> {
        return _searchHistory.value
            .sortedWith(
                compareByDescending<SearchHistoryItem> { it.frequency }
                    .thenByDescending { it.timestamp }
            )
            .take(maxResults)
    }
    
    /**
     * Gets frequency count for a specific query.
     */
    private fun getQueryFrequency(query: String): Int {
        return _searchHistory.value
            .find { it.query.equals(query, ignoreCase = true) }
            ?.frequency ?: 0
    }
    
    /**
     * Clears all search history.
     */
    fun clearHistory() {
        _searchHistory.value = emptyList()
        _recentSearches.value = emptyList()
        saveSearchHistory()
        saveRecentSearches()
    }
    
    /**
     * Removes a specific query from history.
     */
    fun removeFromHistory(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.removeAll { it.query.equals(query, ignoreCase = true) }
        _searchHistory.value = currentHistory
        saveSearchHistory()
        
        val currentRecent = _recentSearches.value.toMutableList()
        currentRecent.remove(query)
        _recentSearches.value = currentRecent
        saveRecentSearches()
    }
    
    /**
     * Loads search history from SharedPreferences.
     */
    private fun loadSearchHistory() {
        try {
            val historyJson = sharedPrefs.getString(KEY_SEARCH_HISTORY, null)
            if (historyJson != null) {
                val type = object : TypeToken<List<SearchHistoryItem>>() {}.type
                val history: List<SearchHistoryItem> = gson.fromJson(historyJson, type)
                _searchHistory.value = history
            }
        } catch (e: Exception) {
            // If loading fails, start with empty history
            _searchHistory.value = emptyList()
        }
    }
    
    /**
     * Saves search history to SharedPreferences.
     */
    private fun saveSearchHistory() {
        try {
            val historyJson = gson.toJson(_searchHistory.value)
            sharedPrefs.edit()
                .putString(KEY_SEARCH_HISTORY, historyJson)
                .apply()
        } catch (e: Exception) {
            // Ignore save errors
        }
    }
    
    /**
     * Loads recent searches from SharedPreferences.
     */
    private fun loadRecentSearches() {
        try {
            val recentJson = sharedPrefs.getString(KEY_RECENT_SEARCHES, null)
            if (recentJson != null) {
                val type = object : TypeToken<List<String>>() {}.type
                val recent: List<String> = gson.fromJson(recentJson, type)
                _recentSearches.value = recent
            }
        } catch (e: Exception) {
            // If loading fails, start with empty list
            _recentSearches.value = emptyList()
        }
    }
    
    /**
     * Saves recent searches to SharedPreferences.
     */
    private fun saveRecentSearches() {
        try {
            val recentJson = gson.toJson(_recentSearches.value)
            sharedPrefs.edit()
                .putString(KEY_RECENT_SEARCHES, recentJson)
                .apply()
        } catch (e: Exception) {
            // Ignore save errors
        }
    }
}

/**
 * Represents a search history item with metadata.
 */
data class SearchHistoryItem(
    val query: String,
    val timestamp: Long,
    val resultCount: Int,
    val selectedPlayerId: String? = null,
    val frequency: Int = 1
)

/**
 * Represents a search suggestion with type and metadata.
 */
data class SearchSuggestion(
    val query: String,
    val type: SuggestionType,
    val frequency: Int = 0,
    val resultCount: Int? = null
)

/**
 * Type of search suggestion.
 */
enum class SuggestionType {
    RECENT,
    HISTORY,
    AUTOCOMPLETE
}