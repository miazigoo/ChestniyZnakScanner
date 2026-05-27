package ru.devandprod.chestniyznak.core.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import ru.devandprod.chestniyznak.data.settings.LanguagePreferenceStore

@Singleton
class AppStringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languagePreferenceStore: LanguagePreferenceStore,
) {
    fun get(@StringRes resId: Int, vararg args: Any): String {
        val localizedContext = localizedContext(languagePreferenceStore.currentLanguage())
        return localizedContext.getString(resId, *args)
    }

    private fun localizedContext(language: AppLanguage): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language.languageTag))
        return context.createConfigurationContext(configuration)
    }
}
