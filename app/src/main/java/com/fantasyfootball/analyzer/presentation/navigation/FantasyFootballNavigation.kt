package com.fantasyfootball.analyzer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fantasyfootball.analyzer.presentation.ui.screens.MatchupAnalysisScreen
import com.fantasyfootball.analyzer.presentation.ui.screens.PlayerProfileScreen
import com.fantasyfootball.analyzer.presentation.ui.screens.PlayerSearchScreen
import com.fantasyfootball.analyzer.presentation.ui.screens.RecommendationsScreen
import com.fantasyfootball.analyzer.presentation.ui.screens.SettingsScreen

/**
 * Navigation component for the Fantasy Football Analyzer app.
 * Defines navigation routes and screen transitions.
 */
@Composable
fun FantasyFootballNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.PlayerSearch.route
    ) {
        composable(Screen.PlayerSearch.route) {
            PlayerSearchScreen(
                onPlayerSelected = { playerId ->
                    navController.navigate("${Screen.PlayerProfile.route}/$playerId")
                },
                onNavigateToRecommendations = {
                    navController.navigate(Screen.Recommendations.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable("${Screen.PlayerProfile.route}/{playerId}") { backStackEntry ->
            val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
            PlayerProfileScreen(
                playerId = playerId,
                onNavigateToMatchup = { playerIdForMatchup, opponentTeam ->
                    navController.navigate("${Screen.MatchupAnalysis.route}/$playerIdForMatchup/$opponentTeam")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("${Screen.MatchupAnalysis.route}/{playerId}/{opponentTeam}") { backStackEntry ->
            val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
            val opponentTeam = backStackEntry.arguments?.getString("opponentTeam") ?: ""
            MatchupAnalysisScreen(
                playerId = playerId,
                opponentTeam = opponentTeam,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Recommendations.route) {
            RecommendationsScreen(
                onPlayerSelected = { playerId ->
                    navController.navigate("${Screen.PlayerProfile.route}/$playerId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Sealed class defining navigation routes.
 */
sealed class Screen(val route: String) {
    object PlayerSearch : Screen("player_search")
    object PlayerProfile : Screen("player_profile")
    object MatchupAnalysis : Screen("matchup_analysis")
    object Recommendations : Screen("recommendations")
    object Settings : Screen("settings")
}