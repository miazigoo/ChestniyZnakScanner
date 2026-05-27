package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import ru.devandprod.chestniyznak.BuildConfig

@Singleton
class TsdSurfaceInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!BuildConfig.API_BASE_URL.contains("/api/v1")) {
            return chain.proceed(request)
        }

        val path = request.url.encodedPath
        if (!path.startsWith(API_PREFIX) || path.hasSurfacePrefix()) {
            return chain.proceed(request)
        }

        val suffix = path.removePrefix(API_PREFIX).trimStart('/')
        val updatedUrl = request.url.newBuilder()
            .encodedPath("$API_PREFIX/tsd/$suffix")
            .build()
        return chain.proceed(request.newBuilder().url(updatedUrl).build())
    }

    private fun String.hasSurfacePrefix(): Boolean = SURFACE_PREFIXES.any(::startsWith)

    private companion object {
        const val API_PREFIX = "/api/v1"
        val SURFACE_PREFIXES = listOf(
            "/api/v1/public/",
            "/api/v1/plant/",
            "/api/v1/supplier/",
            "/api/v1/tsd/",
            "/api/v1/admin/",
            "/api/v1/integration/",
        )
    }
}
