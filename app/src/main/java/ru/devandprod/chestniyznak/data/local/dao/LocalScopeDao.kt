package ru.devandprod.chestniyznak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import ru.devandprod.chestniyznak.data.local.entity.LocalScopeEntity

@Dao
interface LocalScopeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scope: LocalScopeEntity)
}
