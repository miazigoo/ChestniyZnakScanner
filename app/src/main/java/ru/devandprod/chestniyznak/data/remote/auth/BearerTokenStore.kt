package ru.devandprod.chestniyznak.data.remote.auth

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class BearerSession(
    val accessToken: String,
    val refreshToken: String,
)

@Singleton
class BearerTokenStore @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
) {
    fun load(): BearerSession? {
        val rawValue = sharedPreferences.getString(KEY_BEARER_SESSION, null) ?: return null
        val stored = runCatching {
            json.decodeFromString<StoredBearerSessionDto>(rawValue)
        }.getOrElse {
            clear()
            return null
        }

        if (stored.accessToken.isBlank() || stored.refreshToken.isBlank()) {
            clear()
            return null
        }

        return BearerSession(
            accessToken = stored.accessToken,
            refreshToken = stored.refreshToken,
        )
    }

    fun save(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString(
                KEY_BEARER_SESSION,
                json.encodeToString(
                    StoredBearerSessionDto(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                    ),
                ),
            )
            .apply()
    }

    fun clear() {
        sharedPreferences.edit().remove(KEY_BEARER_SESSION).apply()
    }

    private companion object {
        const val KEY_BEARER_SESSION = "saas_bearer_session"
    }
}

@Serializable
private data class StoredBearerSessionDto(
    val accessToken: String,
    val refreshToken: String,
)
