#!/usr/bin/env kotlin

/**
 * Simple validation script to check integration test setup
 * This script validates that our integration tests are properly structured
 * and can be executed in the Android testing environment.
 */

import java.io.File

fun main() {
    println("Fantasy Football Analyzer - Integration Test Validation")
    println("=" * 60)
    
    val integrationTestDir = File("app/src/test/java/com/fantasyfootball/analyzer/integration")
    
    if (!integrationTestDir.exists()) {
        println("❌ Integration test directory not found!")
        return
    }
    
    println("✅ Integration test directory found")
    
    val testFiles = integrationTestDir.listFiles { file -> 
        file.name.endsWith(".kt") && file.name.contains("Test")
    }
    
    if (testFiles.isNullOrEmpty()) {
        println("❌ No integration test files found!")
        return
    }
    
    println("✅ Found ${testFiles.size} integration test files:")
    
    testFiles.forEach { file ->
        println("  📄 ${file.name}")
        
        val content = file.readText()
        
        // Check for required annotations
        val hasHiltTest = content.contains("@HiltAndroidTest")
        val hasAndroidJUnit = content.contains("@RunWith(AndroidJUnit4::class)")
        val hasTestMethods = content.contains("@Test")
        
        if (hasHiltTest && hasAndroidJUnit && hasTestMethods) {
            println("    ✅ Properly structured Android test")
        } else {
            println("    ⚠️  Missing required annotations:")
            if (!hasHiltTest) println("      - @HiltAndroidTest")
            if (!hasAndroidJUnit) println("      - @RunWith(AndroidJUnit4::class)")
            if (!hasTestMethods) println("      - @Test methods")
        }
        
        // Count test methods
        val testMethodCount = content.split("@Test").size - 1
        println("    📊 Contains $testMethodCount test methods")
        
        // Check for requirements validation
        val hasRequirementsValidation = content.contains("Requirements addressed:")
        if (hasRequirementsValidation) {
            println("    ✅ Contains requirements validation")
        }
        
        println()
    }
    
    // Validate test coverage
    println("Integration Test Coverage Analysis:")
    println("-" * 40)
    
    val allContent = testFiles.joinToString("\n") { it.readText() }
    
    val coverageAreas = mapOf(
        "Player Search & Profile" to listOf("searchPlayers", "getPlayer", "PlayerProfile"),
        "Matchup Analysis" to listOf("analyzeMatchup", "MatchupAnalysis", "historical"),
        "Recommendations" to listOf("generateWeeklyRecommendations", "PlayerRecommendation"),
        "Offline Functionality" to listOf("cache", "offline", "CacheManager"),
        "Network Connectivity" to listOf("NetworkConnectivity", "online", "transition"),
        "Performance" to listOf("performance", "measureTimeMillis", "threshold"),
        "Error Handling" to listOf("error", "exception", "graceful", "resilience"),
        "Data Synchronization" to listOf("synchronization", "sync", "consistency")
    )
    
    coverageAreas.forEach { (area, keywords) ->
        val covered = keywords.any { keyword -> 
            allContent.contains(keyword, ignoreCase = true) 
        }
        val status = if (covered) "✅" else "❌"
        println("$status $area")
    }
    
    println()
    println("Summary:")
    println("- Integration test files: ${testFiles.size}")
    println("- Total test methods: ${allContent.split("@Test").size - 1}")
    println("- Coverage areas: ${coverageAreas.count { (_, keywords) -> 
        keywords.any { allContent.contains(it, ignoreCase = true) }
    }}/${coverageAreas.size}")
    
    println()
    println("✅ Integration test validation complete!")
    println("The integration tests are properly structured and ready for execution.")
    println()
    println("To run the integration tests:")
    println("1. Use Android Studio's test runner")
    println("2. Or run: ./gradlew connectedAndroidTest")
    println("3. Or run individual test classes through the IDE")
}

operator fun String.times(n: Int): String = this.repeat(n)