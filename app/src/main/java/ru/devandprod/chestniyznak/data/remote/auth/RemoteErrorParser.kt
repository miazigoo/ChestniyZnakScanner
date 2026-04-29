package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.Response
import ru.devandprod.chestniyznak.data.remote.dto.ApiDetailDto

@Singleton
class RemoteErrorParser @Inject constructor(
    private val json: Json,
) {
    fun message(response: Response<*>): String {
        val payload = runCatching {
            response.errorBody()?.string().orEmpty()
        }.getOrDefault("")

        if (payload.isBlank()) {
            return "Ошибка сервера: ${response.code()}"
        }

        val parsed = runCatching {
            json.decodeFromString<ApiDetailDto>(payload)
        }.getOrNull()

        return parsed?.detail
            ?: parsed?.message
            ?: "Ошибка сервера: ${response.code()}"
    }
}
