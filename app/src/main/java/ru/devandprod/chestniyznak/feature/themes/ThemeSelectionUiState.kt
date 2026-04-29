package ru.devandprod.chestniyznak.feature.themes

import ru.devandprod.chestniyznak.domain.model.AppThemeOption

data class ThemeSelectionUiState(
    val selectedTheme: AppThemeOption = AppThemeOption.Workbench,
    val availableThemes: List<AppThemeOption> = AppThemeOption.entries,
)
