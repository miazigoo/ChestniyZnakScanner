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

    @Query("SELECT COUNT(*) FROM marking_codes WHERE scopeKey = :scopeKey")
    suspend fun countInScope(scopeKey: String): Int

    @Query("DELETE FROM marking_codes")
    suspend fun deleteAll()

    @Query("DELETE FROM marking_codes WHERE scopeKey = :scopeKey AND orderId = :orderId AND appStatus != 'pending_local'")
    suspend fun deleteByOrderId(scopeKey: String, orderId: String)

    @Query("DELETE FROM marking_codes WHERE scopeKey = :scopeKey AND orderId != '' AND appStatus != 'pending_local'")
    suspend fun deleteOrderPools(scopeKey: String)

    @Query(
        """
        DELETE FROM marking_codes
        WHERE scopeKey = :scopeKey
            AND orderId != ''
            AND orderId NOT IN (:orderIds)
            AND appStatus != 'pending_local'
        """,
    )
    suspend fun deleteOrdersNotIn(scopeKey: String, orderIds: List<String>)

    @Query("SELECT COUNT(*) FROM marking_codes")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM marking_codes WHERE scopeKey = :scopeKey AND rawCodeSha256 = :rawHash LIMIT 1")
    suspend fun findByRawHash(scopeKey: String, rawHash: String): MarkingCodeEntity?

    @Query(
        """
        SELECT * FROM marking_codes
        WHERE scopeKey = :scopeKey
            AND rawCodeSha256 = :rawHash
            AND orderId = :orderId
        LIMIT 1
        """,
    )
    suspend fun findByRawHashInOrder(scopeKey: String, rawHash: String, orderId: String): MarkingCodeEntity?

    @Query(
        """
        SELECT * FROM marking_codes
        WHERE scopeKey = :scopeKey
            AND appStatus = 'pending_local'
            AND packageCode = :packageCode
        ORDER BY id ASC
        """,
    )
    suspend fun findLocalPackingPending(scopeKey: String, packageCode: String): List<MarkingCodeEntity>

    @Query("SELECT * FROM marking_codes WHERE scopeKey = :scopeKey AND appStatus = 'pending_local'")
    suspend fun findAllLocalPackingPending(scopeKey: String): List<MarkingCodeEntity>

    @Query(
        """
        SELECT * FROM marking_codes
        WHERE scopeKey = :scopeKey
            AND appStatus = 'pending_local'
            AND packageCode = :packageCode
            AND orderId = :orderId
        ORDER BY id ASC
        """,
    )
    suspend fun findLocalPackingPendingInOrder(
        scopeKey: String,
        packageCode: String,
        orderId: String,
    ): List<MarkingCodeEntity>

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = 'pending_local',
            packageCode = :packageCode,
            packageStatus = 'local_pending',
            packageClosedAt = NULL
        WHERE scopeKey = :scopeKey
            AND rawCodeSha256 = :rawHash
        """,
    )
    suspend fun markPackingPending(scopeKey: String, rawHash: String, packageCode: String?): Int

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = 'pending_local',
            packageCode = :packageCode,
            packageStatus = 'local_pending',
            packageClosedAt = NULL
        WHERE scopeKey = :scopeKey
            AND rawCodeSha256 = :rawHash
            AND orderId = :orderId
        """,
    )
    suspend fun markPackingPendingInOrder(
        scopeKey: String,
        rawHash: String,
        orderId: String,
        packageCode: String?,
    ): Int

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = CASE
                WHEN packageUnitId IS NULL THEN 'local_pool'
                ELSE appStatus
            END,
            packageCode = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageCode END,
            packageStatus = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageStatus END,
            packageClosedAt = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageClosedAt END
        WHERE scopeKey = :scopeKey
            AND rawCodeSha256 IN (:rawHashes)
            AND appStatus = 'pending_local'
        """,
    )
    suspend fun clearPackingPending(scopeKey: String, rawHashes: List<String>): Int

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = CASE
                WHEN packageUnitId IS NULL THEN 'local_pool'
                ELSE appStatus
            END,
            packageCode = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageCode END,
            packageStatus = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageStatus END,
            packageClosedAt = CASE WHEN packageUnitId IS NULL THEN NULL ELSE packageClosedAt END
        WHERE scopeKey = :scopeKey
            AND rawCodeSha256 IN (:rawHashes)
            AND orderId = :orderId
            AND appStatus = 'pending_local'
        """,
    )
    suspend fun clearPackingPendingInOrder(scopeKey: String, rawHashes: List<String>, orderId: String): Int

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = 'packed_remote',
            status1c = 'packed',
            packageCode = :packageCode,
            packageStatus = 'closed',
            packageClosedAt = :packageClosedAt
        WHERE scopeKey = :scopeKey
            AND rawCodeSha256 IN (:rawHashes)
        """,
    )
    suspend fun markPackingCommitted(
        scopeKey: String,
        rawHashes: List<String>,
        packageCode: String,
        packageClosedAt: String?,
    )

    @Query(
        """
        UPDATE marking_codes
        SET appStatus = 'packed_remote',
            status1c = 'packed',
            packageCode = :packageCode,
            packageStatus = 'closed',
            packageClosedAt = :packageClosedAt
        WHERE scopeKey = :scopeKey
            AND rawCodeSha256 IN (:rawHashes)
            AND orderId = :orderId
        """,
    )
    suspend fun markPackingCommittedInOrder(
        scopeKey: String,
        rawHashes: List<String>,
        packageCode: String,
        packageClosedAt: String?,
        orderId: String,
    )

    @Query("SELECT EXISTS(SELECT 1 FROM marking_codes WHERE scopeKey = :scopeKey AND gtin = :gtin AND serial = :serial)")
    suspend fun existsByIdentity(scopeKey: String, gtin: String, serial: String): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM marking_codes
            WHERE scopeKey = :scopeKey
                AND gtin = :gtin
                AND serial = :serial
                AND orderId = :orderId
        )
        """,
    )
    suspend fun existsByIdentityInOrder(scopeKey: String, gtin: String, serial: String, orderId: String): Boolean
}
