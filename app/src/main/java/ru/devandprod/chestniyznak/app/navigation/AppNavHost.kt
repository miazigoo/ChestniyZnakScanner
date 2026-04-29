package ru.devandprod.chestniyznak.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.devandprod.chestniyznak.feature.scanner.ScanRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppDestination.Scanner.route,
    ) {
        composable(AppDestination.Scanner.route) {
            ScanRoute()
        }
    }
}
