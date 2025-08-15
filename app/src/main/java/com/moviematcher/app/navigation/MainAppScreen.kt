package com.moviematcher.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moviematcher.app.ui.matches.MatchesScreen
import com.moviematcher.app.ui.settings.SettingsScreen
import com.moviematcher.app.ui.swipe.SwipeScreen

/**
 * Main app screen with bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    userId: String,
    roomId: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Swipe.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Swipe.route) {
                SwipeScreen(
                    userId = userId,
                    roomId = roomId
                )
            }
            
            composable(Screen.Matches.route) {
                MatchesScreen(
                    roomId = roomId
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    roomId = roomId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Bottom navigation items
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        title = "Swipe",
        icon = Icons.Default.PlayArrow,
        route = Screen.Swipe.route
    ),
    BottomNavItem(
        title = "Matches",
        icon = Icons.Default.Favorite,
        route = Screen.Matches.route
    ),
    BottomNavItem(
        title = "Settings",
        icon = Icons.Default.Settings,
        route = Screen.Settings.route
    )
)

/**
 * Data class for bottom navigation items
 */
private data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)