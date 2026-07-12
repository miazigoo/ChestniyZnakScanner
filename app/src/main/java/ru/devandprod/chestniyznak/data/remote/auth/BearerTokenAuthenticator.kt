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
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (!ApiEndpoint.isSaasApi || !ApiEndpoint.isSameOrigin(response.request.url)) return null
        if (response.request.header("Authorization").isNullOrBlank()) return null
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null

        return synchronized(refreshLock) {
            val currentSession = tokenStore.load() ?: return@synchronized null
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
            if (!failedToken.isNullOrBlank() && failedToken != currentSession.accessToken) {
                return@synchronized response.request.withAccessToken(currentSession.accessToken)
            }

            val refreshedSession = refreshSession(currentSession.refreshToken) ?: return@synchronized null
            tokenStore.save(
                accessToken = refreshedSession.accessToken,
                refreshToken = refreshedSession.refreshToken,
            )
            response.request.withAccessToken(refreshedSession.accessToken)
        }
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
                    if (response.code in PERMANENT_REFRESH_FAILURE_CODES) {
                        tokenStore.clear()
                    }
                    response.close()
                    return@use null
                }

                val payload = response.body?.string().orEmpty()
                json.decodeFromString<ApiEnvelopeDto<TokenPairDto>>(payload).data
            }
        }.getOrElse {
            null
        }
    }

    private fun Request.withAccessToken(accessToken: String): Request =
        newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

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
        val PERMANENT_REFRESH_FAILURE_CODES = setOf(400, 401, 403)
        val refreshClient = OkHttpClient.Builder().build()
    }
}
