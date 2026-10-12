package com.ludicindex.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ludicindex.app.di.AppContainer
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ludicindex.app.ui.navigation.LudicBottomBar
import com.ludicindex.app.ui.navigation.LudicNavHost
import com.ludicindex.app.ui.navigation.Routes

@Composable
fun LudicApp(container: AppContainer) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        LudicAppScaffold()
    }
}

@Composable
private fun LudicAppScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == null || currentRoute in Routes.topLevel) {
                LudicBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        LudicNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
