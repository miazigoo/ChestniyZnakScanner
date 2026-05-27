package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.Response
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.data.remote.dto.ApiDetailDto

@Singleton
class RemoteErrorParser @Inject constructor(
    private val json: Json,
    private val strings: AppStringProvider,
) {
    fun message(response: Response<*>): String {
        val payload = runCatching {
            response.errorBody()?.string().orEmpty()
        }.getOrDefault("")

        if (payload.isBlank()) {
            return strings.get(R.string.server_error_with_code, response.code())
        }

        val parsed = runCatching {
            json.decodeFromString<ApiDetailDto>(payload)
        }.getOrNull()

        return parsed?.error?.message
            ?: parsed?.detail
            ?: parsed?.message
            ?: parsed?.error?.code?.toSubscriptionMessage()
            ?: strings.get(R.string.server_error_with_code, response.code())
    }

    private fun String.toSubscriptionMessage(): String? = when (this) {
        "plant_subscription_inactive" -> strings.get(R.string.plant_subscription_inactive)
        else -> null
    }
}
