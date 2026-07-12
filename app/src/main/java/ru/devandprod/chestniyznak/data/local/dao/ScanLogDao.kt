package ru.devandprod.chestniyznak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.data.local.entity.ScanLogEntity

@Dao
interface ScanLogDao {
    @Insert
    suspend fun insert(item: ScanLogEntity): Long

    @Query("SELECT COUNT(*) FROM scan_logs")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM scan_logs WHERE scopeKey = :scopeKey")
    suspend fun countInScope(scopeKey: String): Int

    @Query("SELECT COUNT(*) FROM scan_logs")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM scan_logs
            WHERE scopeKey = :scopeKey
                AND codeId = :codeId
                AND status IN ('OK', 'OK_GS_RESTORED')
        )
        """,
    )
    suspend fun hasSuccessfulScan(scopeKey: String, codeId: Long): Boolean
}
