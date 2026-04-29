package ru.devandprod.chestniyznak.data.remote.auth

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.HttpUrl

@Singleton
class SessionCookieStore @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
) {
    fun load(url: HttpUrl): List<Cookie> {
        val rawValue = sharedPreferences.getString(KEY_COOKIES, null) ?: return emptyList()
        val stored = runCatching {
            json.decodeFromString<List<StoredCookieDto>>(rawValue)
        }.getOrElse {
            sharedPreferences.edit().remove(KEY_COOKIES).apply()
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val valid = stored.filter { dto ->
            (dto.persistent && dto.expiresAt > now || !dto.persistent) && url.host.endsWith(dto.domain.removePrefix("."))
        }

        if (valid.size != stored.size) {
            persist(valid)
        }

        return valid.map(StoredCookieDto::toCookie)
    }

    fun save(cookies: List<Cookie>) {
        val current = load(HttpUrl.Builder().scheme("https").host(HOST).build())
            .associateBy { cookieKey(it) }
            .toMutableMap()

        cookies.forEach { cookie ->
            current[cookieKey(cookie)] = cookie
        }
        persist(current.values.map(Cookie::toStoredCookie))
    }

    fun clear() {
        sharedPreferences.edit().remove(KEY_COOKIES).apply()
    }

    fun hasSessionCookie(): Boolean = load(HttpUrl.Builder().scheme("https").host(HOST).build())
        .any { it.name == SESSION_COOKIE_NAME }

    fun csrfToken(): String? = load(HttpUrl.Builder().scheme("https").host(HOST).build())
        .firstOrNull { it.name == CSRF_COOKIE_NAME }
        ?.value

    private fun persist(items: List<StoredCookieDto>) {
        sharedPreferences.edit()
            .putString(KEY_COOKIES, json.encodeToString(items))
            .apply()
    }

    private fun cookieKey(cookie: Cookie): String = "${cookie.name}|${cookie.domain}|${cookie.path}"

    private companion object {
        const val KEY_COOKIES = "session_cookies"
        const val HOST = "srv-dnp.argos.loc"
        const val SESSION_COOKIE_NAME = "dnp_session_id"
        const val CSRF_COOKIE_NAME = "csrftoken"
    }
}
