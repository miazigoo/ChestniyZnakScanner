package ru.devandprod.chestniyznak.data.settings

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.devandprod.chestniyznak.core.i18n.AppLanguage

@Singleton
class LanguagePreferenceStore @Inject constructor(
    private val preferences: SharedPreferences,
) {
    private val _selectedLanguage = MutableStateFlow(readLanguage())
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    fun currentLanguage(): AppLanguage = readLanguage()

    fun setLanguage(language: AppLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.code).apply()
        _selectedLanguage.value = language
    }

    private fun readLanguage(): AppLanguage = AppLanguage.fromCode(
        preferences.getString(KEY_LANGUAGE, AppLanguage.Default.code),
    )

    private companion object {
        const val KEY_LANGUAGE = "app_language"
    }
}
