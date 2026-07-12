package ru.devandprod.chestniyznak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.data.local.entity.SyncEventEntity

@Dao
interface SyncEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: SyncEventEntity): Long

    @Query(
        """
        SELECT * FROM sync_events
        WHERE scopeKey = :scopeKey
            AND status = :status
            AND nextRetryAt <= :now
        ORDER BY createdAt ASC
        LIMIT :limit
        """,
    )
    suspend fun readyBatch(
        scopeKey: String,
        now: Long,
        limit: Int,
        status: String = SyncEventEntity.STATUS_PENDING,
    ): List<SyncEventEntity>

    @Query("SELECT COUNT(*) FROM sync_events WHERE scopeKey = :scopeKey AND status = :status")
    fun observeCount(scopeKey: String, status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_events WHERE scopeKey = :scopeKey AND status = :status")
    suspend fun count(scopeKey: String, status: String): Int

    @Query(
        """
        UPDATE sync_events
        SET status = :status,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE eventId = :eventId
        """,
    )
    suspend fun markTerminal(
        eventId: String,
        status: String,
        lastError: String?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE sync_events
        SET attempts = attempts + 1,
            nextRetryAt = :nextRetryAt,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE eventId = :eventId
        """,
    )
    suspend fun markRetryable(
        eventId: String,
        nextRetryAt: Long,
        lastError: String?,
        updatedAt: Long = System.currentTimeMillis(),
    )
}
