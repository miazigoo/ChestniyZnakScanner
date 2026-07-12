package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TsdSyncRequestDto(
    @SerialName("client_device_id")
    val clientDeviceId: String? = null,
    val events: List<TsdSyncEventDto>,
)

@Serializable
data class TsdSyncEventDto(
    @SerialName("event_id")
    val eventId: String,
    @SerialName("event_type")
    val eventType: String,
    @SerialName("occurred_at")
    val occurredAt: String? = null,
    val payload: JsonObject,
)

@Serializable
data class TsdSyncResponseDto(
    val summary: TsdSyncSummaryDto = TsdSyncSummaryDto(),
    val results: List<TsdSyncResultDto> = emptyList(),
)

@Serializable
data class TsdSyncSummaryDto(
    val accepted: Int = 0,
    val replayed: Int = 0,
    val rejected: Int = 0,
    val retryable: Int = 0,
)

@Serializable
data class TsdSyncResultDto(
    @SerialName("event_id")
    val eventId: String,
    val status: String = "",
    val accepted: Boolean = false,
    val replayed: Boolean = false,
    val retryable: Boolean = false,
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
)
