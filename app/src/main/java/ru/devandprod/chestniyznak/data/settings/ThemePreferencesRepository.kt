package ru.devandprod.chestniyznak.data.settings

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.domain.repository.ThemeRepository

@Singleton
class ThemePreferencesRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ThemeRepository {

    private val themeState = MutableStateFlow(loadTheme())

    override fun observeTheme(): Flow<AppThemeOption> = themeState.asStateFlow()

    override suspend fun setTheme(theme: AppThemeOption) = withContext(ioDispatcher) {
        sharedPreferences.edit().putString(KEY_THEME, theme.storageKey).apply()
        themeState.value = theme
    }

    private fun loadTheme(): AppThemeOption = AppThemeOption.fromStorageKey(
        sharedPreferences.getString(KEY_THEME, null),
    )

    private companion object {
        const val KEY_THEME = "selected_theme"
    }
}
