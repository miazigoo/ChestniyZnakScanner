package ru.devandprod.chestniyznak.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.devandprod.chestniyznak.core.scanner.ScannerCommand
import ru.devandprod.chestniyznak.core.scanner.ScannerCommandBus
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.feature.auth.AuthRoute
import ru.devandprod.chestniyznak.feature.auth.AuthViewModel
import ru.devandprod.chestniyznak.feature.boxdetail.BoxDetailRoute
import ru.devandprod.chestniyznak.feature.boxedit.BoxEditRoute
import ru.devandprod.chestniyznak.feature.boxlookup.BoxLookupRoute
import ru.devandprod.chestniyznak.feature.boxes.BoxesListRoute
import ru.devandprod.chestniyznak.feature.menu.MenuRoute
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
                onOpenMenu = { navController.navigate(AppDestination.Menu.route) },
            )
        }
        composable(AppDestination.Menu.route) {
            MenuRoute(
                onBack = { navController.popBackStack() },
                onOpenBox = {
                    ScannerCommandBus.send(ScannerCommand.SwitchToTsd)
                    ScannerCommandBus.send(ScannerCommand.OpenBox)
                    navController.popBackStack()
                },
                onShowCurrentBox = {
                    navController.navigate(AppDestination.BoxLookup.route)
                },
                onOpenBoxesList = {
                    navController.navigate(AppDestination.boxesRoute("all"))
                },
                onOpenEmptyBoxes = {
                    navController.navigate(AppDestination.boxesRoute("empty"))
                },
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                onOpenThemeSelection = { navController.navigate(AppDestination.ThemeSelection.route) },
                onLogoutRequest = onLogoutRequest,
            )
        }
        composable(AppDestination.BoxLookup.route) {
            BoxLookupRoute(
                onBack = { navController.popBackStack() },
                onOpenBox = { boxId ->
                    navController.navigate(AppDestination.boxDetailRoute(boxId))
                },
            )
        }
        composable(
            route = AppDestination.BoxDetail.route,
            arguments = listOf(
                navArgument(AppDestination.BOX_ID_ARG) {
                    type = NavType.LongType
                },
            ),
        ) {
            BoxDetailRoute(
                onBackToMenu = {
                    navController.popBackStack(AppDestination.Menu.route, false)
                },
                onOpenEdit = { boxId ->
                    navController.navigate(AppDestination.boxEditRoute(boxId))
                },
            )
        }
        composable(
            route = AppDestination.BoxEdit.route,
            arguments = listOf(
                navArgument(AppDestination.BOX_ID_ARG) {
                    type = NavType.LongType
                },
            ),
        ) {
            BoxEditRoute(
                onBack = { navController.popBackStack() },
                onBoxDeleted = {
                    navController.popBackStack(AppDestination.Menu.route, false)
                },
            )
        }
        composable(
            route = AppDestination.Boxes.route,
            arguments = listOf(
                navArgument(AppDestination.FILTER_ARG) {
                    type = NavType.StringType
                },
            ),
        ) {
            BoxesListRoute(
                onBack = { navController.popBackStack() },
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
