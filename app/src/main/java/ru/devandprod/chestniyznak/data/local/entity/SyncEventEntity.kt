package ru.devandprod.chestniyznak.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_events",
    indices = [
        Index(value = ["scopeKey", "status", "nextRetryAt"]),
        Index(value = ["createdAt"]),
    ],
)
data class SyncEventEntity(
    @PrimaryKey
    val eventId: String,
    val scopeKey: String,
    val eventType: String,
    val payloadJson: String,
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    val nextRetryAt: Long = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }
}
