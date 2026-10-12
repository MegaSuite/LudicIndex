package com.ludicindex.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ludicindex.app.ui.detail.GameDetailScreen
import com.ludicindex.app.ui.home.HomeScreen
import com.ludicindex.app.ui.settings.SettingsScreen
import com.ludicindex.app.ui.transfer.ImportScreen
import com.ludicindex.app.ui.wishlist.WishlistScreen

@Composable
fun LudicNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onGameClick = { igdbId -> navController.navigate(Routes.gameDetail(igdbId)) },
                onOpenSettings = { navController.switchTab(Routes.SETTINGS) },
            )
        }

        composable(Routes.WISHLIST) {
            WishlistScreen(
                onGameClick = { igdbId -> navController.navigate(Routes.gameDetail(igdbId)) },
                onBrowseHome = { navController.switchTab(Routes.HOME) },
                onImport = { navController.navigate(Routes.IMPORT) },
            )
        }

        composable(Routes.IMPORT) {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onOpenImport = { navController.navigate(Routes.IMPORT) })
        }

        composable(
            route = Routes.GAME_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_IGDB_ID) { type = NavType.LongType }),
        ) { entry ->
            GameDetailScreen(
                igdbId = entry.arguments?.getLong(Routes.ARG_IGDB_ID) ?: 0L,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.switchTab(Routes.SETTINGS) },
                onGameClick = { igdbId -> navController.navigate(Routes.gameDetail(igdbId)) },
            )
        }
    }
}

/** Same semantics as tapping the bottom bar: single instance, state preserved. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
