package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class BearerAuthInterceptor @Inject constructor(
    private val tokenStore: BearerTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!ApiEndpoint.isSaasApi || !ApiEndpoint.isSameOrigin(request.url)) {
            return chain.proceed(request)
        }

        val session = tokenStore.load()
        if (session == null || ApiEndpoint.isPublicPath(request.url)) {
            return chain.proceed(request)
        }

        val updatedRequest = request.newBuilder()
            .header("Authorization", "Bearer ${session.accessToken}")
            .build()
        return chain.proceed(updatedRequest)
    }
}
