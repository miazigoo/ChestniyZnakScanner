package ru.devandprod.chestniyznak.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import ru.devandprod.chestniyznak.core.i18n.AppLanguage
import ru.devandprod.chestniyznak.data.settings.LanguagePreferenceStore

@HiltViewModel
class AppLanguageViewModel @Inject constructor(
    private val languagePreferenceStore: LanguagePreferenceStore,
) : ViewModel() {
    val selectedLanguage: StateFlow<AppLanguage> = languagePreferenceStore.selectedLanguage

    fun setLanguage(language: AppLanguage) {
        languagePreferenceStore.setLanguage(language)
    }
}
