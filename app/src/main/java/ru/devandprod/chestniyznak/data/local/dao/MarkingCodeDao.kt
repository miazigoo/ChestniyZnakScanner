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

    @Query("SELECT COUNT(*) FROM marking_codes")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM marking_codes WHERE rawCodeSha256 = :rawHash LIMIT 1")
    suspend fun findByRawHash(rawHash: String): MarkingCodeEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM marking_codes WHERE gtin = :gtin AND serial = :serial)")
    suspend fun existsByIdentity(gtin: String, serial: String): Boolean
}
