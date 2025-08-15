package com.moviematcher.app.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Sealed class representing all screens in the app
 */
sealed class Screen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    object Auth : Screen("auth")
    object Pairing : Screen("pairing")
    object MainApp : Screen("main_app")
    
    // Deep link for room invites
    object JoinRoom : Screen(
        route = "join_room",
        arguments = listOf(
            navArgument("inviteCode") {
                type = NavType.StringType
            }
        )
    )
    
    // Main app screens (used within bottom navigation)
    object Swipe : Screen("swipe")
    object Matches : Screen("matches")
    object Settings : Screen("settings")
}