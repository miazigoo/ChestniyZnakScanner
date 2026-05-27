package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import ru.devandprod.chestniyznak.core.i18n.AppLanguage
import ru.devandprod.chestniyznak.data.settings.LanguagePreferenceStore

@Singleton
class LanguageHeaderInterceptor @Inject constructor(
    private val languagePreferenceStore: LanguagePreferenceStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val language = languagePreferenceStore.currentLanguage()
        val request = chain.request().newBuilder()
            .header("X-App-Language", language.code)
            .header("X-Language", language.code)
            .header("Accept-Language", acceptLanguage(language))
            .build()
        return chain.proceed(request)
    }

    private fun acceptLanguage(language: AppLanguage): String = when (language) {
        AppLanguage.Russian -> "ru-RU,ru;q=0.9,en;q=0.6"
        AppLanguage.English -> "en-US,en;q=0.9,ru;q=0.6"
        AppLanguage.Chinese -> "zh-CN,zh;q=0.9,en;q=0.6"
    }
}
