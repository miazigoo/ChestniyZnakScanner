package ru.devandprod.chestniyznak.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.feature.auth.AuthRoute
import ru.devandprod.chestniyznak.feature.auth.AuthViewModel
import ru.devandprod.chestniyznak.feature.scanner.ScanRoute
import ru.devandprod.chestniyznak.feature.settings.SettingsRoute
import ru.devandprod.chestniyznak.feature.themes.ThemeSelectionRoute

@Composable
fun AppNavHost(
    selectedTheme: AppThemeOption,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    when {
        authState.session.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        authState.session.isAuthenticated -> {
            AuthenticatedNavHost(
                selectedTheme = selectedTheme,
                currentUserName = authState.session.displayName.ifBlank { authState.session.username },
                onLogoutRequest = authViewModel::onLogoutRequested,
            )
        }
        else -> {
            AuthRoute(
                state = authState,
                onLoginClicked = authViewModel::onLoginClicked,
            )
        }
    }
}

@Composable
private fun AuthenticatedNavHost(
    selectedTheme: AppThemeOption,
    currentUserName: String,
    onLogoutRequest: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppDestination.Scanner.route,
    ) {
        composable(AppDestination.Scanner.route) {
            ScanRoute(
                currentUserName = currentUserName,
                onLogoutRequest = onLogoutRequest,
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
            )
        }
        composable(AppDestination.Settings.route) {
            SettingsRoute(
                currentTheme = selectedTheme,
                onBack = { navController.popBackStack() },
                onOpenThemeSelection = { navController.navigate(AppDestination.ThemeSelection.route) },
            )
        }
        composable(AppDestination.ThemeSelection.route) {
            ThemeSelectionRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
