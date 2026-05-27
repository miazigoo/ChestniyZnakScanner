package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import ru.devandprod.chestniyznak.BuildConfig
import ru.devandprod.chestniyznak.data.remote.dto.ApiEnvelopeDto
import ru.devandprod.chestniyznak.data.remote.dto.RefreshTokenRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.TokenPairDto

@Singleton
class BearerTokenAuthenticator @Inject constructor(
    private val tokenStore: BearerTokenStore,
    private val json: Json,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (!BuildConfig.API_BASE_URL.contains("/api/v1")) return null
        if (response.request.header("Authorization").isNullOrBlank()) return null
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null

        val refreshToken = tokenStore.load()?.refreshToken ?: return null
        val refreshedSession = refreshSession(refreshToken) ?: return null

        tokenStore.save(
            accessToken = refreshedSession.accessToken,
            refreshToken = refreshedSession.refreshToken,
        )
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${refreshedSession.accessToken}")
            .build()
    }

    private fun refreshSession(refreshToken: String): TokenPairDto? {
        val body = json.encodeToString(
            RefreshTokenRequestDto(refreshToken = refreshToken),
        ).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL.ensureTrailingSlash() + "public/auth/refresh")
            .post(body)
            .build()

        return runCatching {
            refreshClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    tokenStore.clear()
                    return@use null
                }

                val payload = response.body?.string().orEmpty()
                json.decodeFromString<ApiEnvelopeDto<TokenPairDto>>(payload).data
            }
        }.getOrElse {
            tokenStore.clear()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count += 1
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

    private companion object {
        const val MAX_AUTH_ATTEMPTS = 2
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        val refreshClient = OkHttpClient.Builder().build()
    }
}
