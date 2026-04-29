package ru.devandprod.chestniyznak.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.model.AppThemeOption

interface ThemeRepository {
    fun observeTheme(): Flow<AppThemeOption>
    suspend fun setTheme(theme: AppThemeOption)
}
