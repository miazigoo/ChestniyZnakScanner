package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import ru.devandprod.chestniyznak.BuildConfig

@Singleton
class CsrfInterceptor @Inject constructor(
    private val cookieStore: SessionCookieStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method !in UNSAFE_METHODS) {
            return chain.proceed(request)
        }

        val csrfToken = cookieStore.csrfToken()
        if (csrfToken.isNullOrBlank()) {
            return chain.proceed(request)
        }

        val updatedRequest = request.newBuilder()
            .header("X-CSRFToken", csrfToken)
            .header("Referer", BASE_REFERER)
            .build()

        return chain.proceed(updatedRequest)
    }

    private companion object {
        val UNSAFE_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        val BASE_REFERER = BuildConfig.API_BASE_URL.removeSuffix("/") + "/"
    }
}
