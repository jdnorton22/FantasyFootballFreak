package com.fantasyfootball.analyzer.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fantasyfootball.analyzer.presentation.navigation.FantasyFootballNavigation
import com.fantasyfootball.analyzer.presentation.ui.theme.FantasyFootballAnalyzerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Fantasy Football Analyzer application.
 * Uses Jetpack Compose for UI and Hilt for dependency injection.
 * 
 * Requirements addressed:
 * - 5.5: Android API level 24 support
 * - Navigation Component setup for screen transitions
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FantasyFootballAnalyzerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    FantasyFootballNavigation(navController = navController)
                }
            }
        }
    }
}