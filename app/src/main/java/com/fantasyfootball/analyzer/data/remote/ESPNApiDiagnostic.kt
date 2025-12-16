package com.fantasyfootball.analyzer.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Diagnostic tool for testing ESPN API connectivity and endpoints.
 * Helps identify issues with API endpoints and data retrieval.
 */
@Singleton
class ESPNApiDiagnostic @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    
    companion object {
        private const val TAG = "ESPNApiDiagnostic"
        
        // Test different ESPN API endpoints
        private val TEST_ENDPOINTS = listOf(
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/athletes",
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/teams",
            "https://fantasy.espn.com/apis/v3/games/ffl/seasons/2024/segments/0/leagues",
            "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/athletes",
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard"
        )
    }
    
    /**
     * Runs comprehensive diagnostic tests on ESPN API endpoints.
     */
    suspend fun runDiagnostics(): DiagnosticResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting ESPN API diagnostics...")
        
        val results = mutableListOf<EndpointTest>()
        
        for (endpoint in TEST_ENDPOINTS) {
            val result = testEndpoint(endpoint)
            results.add(result)
            Log.i(TAG, "Endpoint: $endpoint - Status: ${result.status} - Response: ${result.responseCode}")
        }
        
        // Test specific player search functionality
        val playerSearchResult = testPlayerSearch()
        results.add(playerSearchResult)
        
        DiagnosticResult(
            timestamp = System.currentTimeMillis(),
            endpointTests = results,
            overallStatus = if (results.any { it.status == TestStatus.SUCCESS }) {
                TestStatus.SUCCESS
            } else {
                TestStatus.FAILURE
            }
        )
    }
    
    /**
     * Tests a specific endpoint for connectivity and response.
     */
    private suspend fun testEndpoint(url: String): EndpointTest {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "FantasyFootballAnalyzer/1.0")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            EndpointTest(
                endpoint = url,
                status = if (response.isSuccessful) TestStatus.SUCCESS else TestStatus.FAILURE,
                responseCode = response.code,
                responseMessage = response.message,
                responseBody = response.body?.string()?.take(500) // First 500 chars
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error testing endpoint: $url", e)
            EndpointTest(
                endpoint = url,
                status = TestStatus.ERROR,
                responseCode = -1,
                responseMessage = e.message ?: "Unknown error",
                responseBody = null
            )
        }
    }
    
    /**
     * Tests player search functionality with known working endpoints.
     */
    private suspend fun testPlayerSearch(): EndpointTest {
        val searchEndpoints = listOf(
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/athletes?limit=10",
            "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/athletes?limit=10"
        )
        
        for (endpoint in searchEndpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Accept", "application/json")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    return EndpointTest(
                        endpoint = endpoint,
                        status = TestStatus.SUCCESS,
                        responseCode = response.code,
                        responseMessage = "Player search working",
                        responseBody = response.body?.string()?.take(500)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Player search endpoint failed: $endpoint", e)
            }
        }
        
        return EndpointTest(
            endpoint = "player_search_test",
            status = TestStatus.FAILURE,
            responseCode = -1,
            responseMessage = "All player search endpoints failed",
            responseBody = null
        )
    }
    
    /**
     * Gets suggested working ESPN API endpoints based on diagnostic results.
     */
    fun getSuggestedEndpoints(): List<String> {
        return listOf(
            // Working ESPN API endpoints (as of 2024)
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/athletes",
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/teams",
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard",
            "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/athletes",
            "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/teams"
        )
    }
}

/**
 * Result of diagnostic tests.
 */
data class DiagnosticResult(
    val timestamp: Long,
    val endpointTests: List<EndpointTest>,
    val overallStatus: TestStatus
)

/**
 * Result of testing a specific endpoint.
 */
data class EndpointTest(
    val endpoint: String,
    val status: TestStatus,
    val responseCode: Int,
    val responseMessage: String,
    val responseBody: String?
)

/**
 * Status of a diagnostic test.
 */
enum class TestStatus {
    SUCCESS,
    FAILURE,
    ERROR
}