package ru.devandprod.chestniyznak.data.local.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.devandprod.chestniyznak.data.local.dao.MarkingCodeDao
import ru.devandprod.chestniyznak.data.local.dao.ScanLogDao
import ru.devandprod.chestniyznak.data.local.entity.MarkingCodeEntity
import ru.devandprod.chestniyznak.data.local.entity.ScanLogEntity

@Database(
    entities = [MarkingCodeEntity::class, ScanLogEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun markingCodeDao(): MarkingCodeDao
    abstract fun scanLogDao(): ScanLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN orderId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN orderLineId TEXT")
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN remoteCodeId TEXT")
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN packageUnitId TEXT")
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN packageCode TEXT")
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN packageStatus TEXT")
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN packageClosedAt TEXT")
                db.execSQL("ALTER TABLE marking_codes ADD COLUMN remoteUpdatedAt TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_marking_codes_orderId ON marking_codes(orderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_marking_codes_packageCode ON marking_codes(packageCode)")
            }
        }
    }
}
