package ru.devandprod.chestniyznak.data.remote.auth

import kotlinx.serialization.Serializable
import okhttp3.Cookie

@Serializable
data class StoredCookieDto(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
    val persistent: Boolean,
)

fun Cookie.toStoredCookie(): StoredCookieDto = StoredCookieDto(
    name = name,
    value = value,
    domain = domain,
    path = path,
    expiresAt = expiresAt,
    secure = secure,
    httpOnly = httpOnly,
    hostOnly = hostOnly,
    persistent = persistent,
)

fun StoredCookieDto.toCookie(): Cookie = Cookie.Builder()
    .name(name)
    .value(value)
    .apply {
        if (hostOnly) {
            hostOnlyDomain(domain)
        } else {
            domain(domain)
        }
        path(path)
        if (persistent) {
            expiresAt(expiresAt)
        }
        if (secure) secure()
        if (httpOnly) httpOnly()
    }
    .build()
