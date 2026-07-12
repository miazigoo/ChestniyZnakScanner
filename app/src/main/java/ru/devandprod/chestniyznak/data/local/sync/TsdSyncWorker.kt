package ru.devandprod.chestniyznak.data.local.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import ru.devandprod.chestniyznak.core.device.DeviceIdentity
import ru.devandprod.chestniyznak.data.local.LocalScopeStore
import ru.devandprod.chestniyznak.data.local.dao.SyncEventDao
import ru.devandprod.chestniyznak.data.local.entity.SyncEventEntity
import ru.devandprod.chestniyznak.data.remote.api.TsdSyncApi
import ru.devandprod.chestniyznak.data.remote.dto.TsdSyncEventDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdSyncRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdSyncResultDto

@HiltWorker
class TsdSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncEventDao: SyncEventDao,
    private val localScopeStore: LocalScopeStore,
    private val syncApi: TsdSyncApi,
    private val json: Json,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scopeKey = localScopeStore.currentScopeKey()
        val now = System.currentTimeMillis()
        val events = syncEventDao.readyBatch(
            scopeKey = scopeKey,
            now = now,
            limit = BATCH_LIMIT,
        )
        if (events.isEmpty()) return Result.success()

        val sendableEvents = mutableListOf<TsdSyncEventDto>()
        events.forEach { event ->
            val dto = event.toSendableDto()
            if (dto == null) {
                syncEventDao.markTerminal(
                    eventId = event.eventId,
                    status = SyncEventEntity.STATUS_REJECTED,
                    lastError = "unsupported_or_incomplete_local_event",
                )
            } else {
                sendableEvents += dto
            }
        }

        if (sendableEvents.isEmpty()) return Result.success()

        val response = runCatching {
            syncApi.syncEvents(
                TsdSyncRequestDto(
                    clientDeviceId = DeviceIdentity.clientDeviceId,
                    events = sendableEvents,
                ),
            )
        }.getOrElse {
            markBatchRetryable(events, it.message ?: "sync_request_failed")
            return Result.retry()
        }

        if (!response.isSuccessful) {
            if (response.code() >= HTTP_SERVER_ERROR) {
                markBatchRetryable(events, "server_${response.code()}")
                return Result.retry()
            }
            markBatchRejected(sendableEvents.map { it.eventId }.toSet(), "http_${response.code()}")
            return Result.failure()
        }

        val body = response.body()?.data
        if (body == null) {
            markBatchRetryable(events, "empty_sync_response")
            return Result.retry()
        }

        val byId = body.results.associateBy { it.eventId }
        sendableEvents.forEach { event ->
            val result = byId[event.eventId]
            if (result == null) {
                syncEventDao.markRetryable(
                    eventId = event.eventId,
                    nextRetryAt = retryAt(),
                    lastError = "missing_event_result",
                )
            } else {
                applyResult(result)
            }
        }

        return Result.success()
    }

    private fun SyncEventEntity.toSendableDto(): TsdSyncEventDto? {
        if (eventType != "code.scan") return null
        val payload = runCatching {
            json.parseToJsonElement(payloadJson).jsonObject
        }.getOrNull() ?: return null
        if (payload["package_id"] == null) return null
        return TsdSyncEventDto(
            eventId = eventId,
            eventType = eventType,
            occurredAt = Instant.ofEpochMilli(createdAt).toString(),
            payload = JsonObject(payload),
        )
    }

    private suspend fun applyResult(result: TsdSyncResultDto) {
        when {
            result.accepted || result.replayed || result.status in ACCEPTED_STATUSES -> {
                syncEventDao.markTerminal(
                    eventId = result.eventId,
                    status = SyncEventEntity.STATUS_ACCEPTED,
                    lastError = null,
                )
            }
            result.retryable -> {
                syncEventDao.markRetryable(
                    eventId = result.eventId,
                    nextRetryAt = retryAt(),
                    lastError = result.errorMessage ?: result.errorCode ?: result.status,
                )
            }
            else -> {
                syncEventDao.markTerminal(
                    eventId = result.eventId,
                    status = SyncEventEntity.STATUS_REJECTED,
                    lastError = result.errorMessage ?: result.errorCode ?: result.status,
                )
            }
        }
    }

    private suspend fun markBatchRetryable(events: List<SyncEventEntity>, error: String) {
        val nextRetryAt = retryAt()
        events.forEach { event ->
            syncEventDao.markRetryable(event.eventId, nextRetryAt, error)
        }
    }

    private suspend fun markBatchRejected(eventIds: Set<String>, error: String) {
        eventIds.forEach { eventId ->
            syncEventDao.markTerminal(
                eventId = eventId,
                status = SyncEventEntity.STATUS_REJECTED,
                lastError = error,
            )
        }
    }

    private fun retryAt(): Long = System.currentTimeMillis() + RETRY_DELAY_MS

    private companion object {
        const val BATCH_LIMIT = 50
        const val HTTP_SERVER_ERROR = 500
        const val RETRY_DELAY_MS = 60_000L
        val ACCEPTED_STATUSES = setOf("accepted", "replayed")
    }
}
