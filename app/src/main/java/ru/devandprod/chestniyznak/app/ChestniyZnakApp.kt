package ru.devandprod.chestniyznak.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.app.navigation.AppNavHost
import ru.devandprod.chestniyznak.core.designsystem.theme.ChestniyZnakTheme
import ru.devandprod.chestniyznak.core.i18n.AppLanguage
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun ChestniyZnakApp() {
    val themeViewModel: AppThemeViewModel = hiltViewModel()
    val languageViewModel: AppLanguageViewModel = hiltViewModel()
    val runtimeViewModel: AppRuntimeViewModel = hiltViewModel()
    val selectedTheme by themeViewModel.selectedTheme.collectAsState()
    val selectedLanguage by languageViewModel.selectedLanguage.collectAsState()

    LocalizedApp(language = selectedLanguage) {
        ChestniyZnakTheme(selectedTheme = selectedTheme) {
            AppNavHost(
                selectedTheme = selectedTheme,
                runtimeViewModel = runtimeViewModel,
            )
        }
    }
}

@Composable
private fun LocalizedApp(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedContext = remember(context, configuration, language) {
        val locale = Locale.forLanguageTag(language.languageTag)
        Locale.setDefault(locale)
        val nextConfiguration = android.content.res.Configuration(configuration)
        nextConfiguration.setLocales(LocaleList(locale))
        val resourcesContext = context.createConfigurationContext(nextConfiguration)
        LocalizedResourcesContextWrapper(
            base = context,
            resourcesContext = resourcesContext,
        )
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        content = content,
    )
}

private class LocalizedResourcesContextWrapper(
    base: Context,
    private val resourcesContext: Context,
) : ContextWrapper(base) {
    override fun getResources(): Resources = resourcesContext.resources

    override fun getAssets(): AssetManager = resourcesContext.assets
}
