package ru.devandprod.chestniyznak.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.app.navigation.AppNavHost
import ru.devandprod.chestniyznak.core.designsystem.theme.ChestniyZnakTheme

@Composable
fun ChestniyZnakApp() {
    val themeViewModel: AppThemeViewModel = hiltViewModel()
    val runtimeViewModel: AppRuntimeViewModel = hiltViewModel()
    val selectedTheme by themeViewModel.selectedTheme.collectAsState()

    ChestniyZnakTheme(selectedTheme = selectedTheme) {
        AppNavHost(
            selectedTheme = selectedTheme,
            runtimeViewModel = runtimeViewModel,
        )
    }
}
