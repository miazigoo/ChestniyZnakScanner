package ru.devandprod.chestniyznak.data.remote.auth

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import ru.devandprod.chestniyznak.BuildConfig

object ApiEndpoint {
    val isSaasApi: Boolean
        get() = BuildConfig.API_MODE == "SAAS"

    private val baseUrl: HttpUrl?
        get() = BuildConfig.API_BASE_URL.toHttpUrlOrNull()

    fun isSameOrigin(url: HttpUrl): Boolean {
        val apiUrl = baseUrl ?: return false
        return url.scheme == apiUrl.scheme &&
            url.host.equals(apiUrl.host, ignoreCase = true) &&
            url.port == apiUrl.port
    }

    fun isPublicPath(url: HttpUrl): Boolean = url.encodedPath.contains("/public/")
}
