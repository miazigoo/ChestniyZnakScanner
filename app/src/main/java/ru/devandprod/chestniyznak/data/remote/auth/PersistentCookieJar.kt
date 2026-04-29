package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

@Singleton
class PersistentCookieJar @Inject constructor(
    private val store: SessionCookieStore,
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store.save(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = store.load(url)
}
