package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import ru.devandprod.chestniyznak.BuildConfig

@Singleton
class BearerAuthInterceptor @Inject constructor(
    private val tokenStore: BearerTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!BuildConfig.API_BASE_URL.contains("/api/v1")) {
            return chain.proceed(request)
        }

        val session = tokenStore.load()
        if (session == null || request.url.encodedPath.contains("/public/")) {
            return chain.proceed(request)
        }

        val updatedRequest = request.newBuilder()
            .header("Authorization", "Bearer ${session.accessToken}")
            .build()
        return chain.proceed(updatedRequest)
    }
}
