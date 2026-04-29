package ru.devandprod.chestniyznak.app

import androidx.compose.runtime.Composable
import ru.devandprod.chestniyznak.app.navigation.AppNavHost
import ru.devandprod.chestniyznak.core.designsystem.theme.ChestniyZnakTheme

@Composable
fun ChestniyZnakApp() {
    ChestniyZnakTheme {
        AppNavHost()
    }
}
