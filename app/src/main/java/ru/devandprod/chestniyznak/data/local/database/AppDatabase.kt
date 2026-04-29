package ru.devandprod.chestniyznak.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.devandprod.chestniyznak.data.local.dao.MarkingCodeDao
import ru.devandprod.chestniyznak.data.local.dao.ScanLogDao
import ru.devandprod.chestniyznak.data.local.entity.MarkingCodeEntity
import ru.devandprod.chestniyznak.data.local.entity.ScanLogEntity

@Database(
    entities = [MarkingCodeEntity::class, ScanLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun markingCodeDao(): MarkingCodeDao
    abstract fun scanLogDao(): ScanLogDao
}
