package ru.devandprod.chestniyznak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.data.local.entity.MarkingCodeEntity

@Dao
interface MarkingCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MarkingCodeEntity>)

    @Query("SELECT COUNT(*) FROM marking_codes")
    suspend fun count(): Int

    @Query("DELETE FROM marking_codes")
    suspend fun deleteAll()

    @Query("DELETE FROM marking_codes WHERE orderId = :orderId AND appStatus != 'pending_local'")
    suspend fun deleteByOrderId(orderId: String)

    @Query("DELETE FROM marking_codes WHERE orderId != '' AND appStatus != 'pending_local'")
    suspend fun deleteOrderPools()

    @Query(
        """
        DELETE FROM marking_codes
        WHERE orderId != ''
            AND orderId NOT IN (:orderIds)
            AND appStatus != 'pending_local'
        """,
    )
    suspend fun deleteOrdersNotIn(orderIds: List<String>)

    @Query("SELECT COUNT(*) FROM marking_codes")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM marking_codes WHERE rawCodeSha256 = :rawHash LIMIT 1")
    suspend fun findByRawHash(rawHash: String): MarkingCodeEntity?

    @Query(
        """
        SELECT * FROM marking_codes
        WHERE appStatus = 'pending_local'
            AND packageCode = :packageCode
        ORDER BY id ASC
        """,
    )
    suspend fun findLocalPackingPending(packageCode: String): List<MarkingCodeEntity>

    @Query("SELECT * FROM marking_codes WHERE appStatus = 'pending_local'")
    suspend fun findAllLocalPackingPending(): List<MarkingCodeEntity>

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = 'pending_local',
            packageCode = :packageCode,
            packageStatus = 'local_pending',
            packageClosedAt = NULL
        WHERE rawCodeSha256 = :rawHash
        """,
    )
    suspend fun markPackingPending(rawHash: String, packageCode: String?)

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = CASE
                WHEN packageUnitId IS NULL AND (packageCode IS NULL OR packageCode = '') THEN 'local_pool'
                ELSE appStatus
            END,
            packageCode = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageCode END,
            packageStatus = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageStatus END,
            packageClosedAt = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageClosedAt END
        WHERE rawCodeSha256 IN (:rawHashes) AND appStatus = 'pending_local'
        """,
    )
    suspend fun clearPackingPending(rawHashes: List<String>)

    @Query("SELECT EXISTS(SELECT 1 FROM marking_codes WHERE gtin = :gtin AND serial = :serial)")
    suspend fun existsByIdentity(gtin: String, serial: String): Boolean
}
