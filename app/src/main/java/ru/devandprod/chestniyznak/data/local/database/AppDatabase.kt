package ru.devandprod.chestniyznak.data.local.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.devandprod.chestniyznak.data.local.LocalScopeStore
import ru.devandprod.chestniyznak.data.local.dao.LocalScopeDao
import ru.devandprod.chestniyznak.data.local.dao.MarkingCodeDao
import ru.devandprod.chestniyznak.data.local.dao.ScanLogDao
import ru.devandprod.chestniyznak.data.local.dao.SyncEventDao
import ru.devandprod.chestniyznak.data.local.entity.LocalScopeEntity
import ru.devandprod.chestniyznak.data.local.entity.MarkingCodeEntity
import ru.devandprod.chestniyznak.data.local.entity.ScanLogEntity
import ru.devandprod.chestniyznak.data.local.entity.SyncEventEntity

@Database(
    entities = [
        MarkingCodeEntity::class,
        ScanLogEntity::class,
        SyncEventEntity::class,
        LocalScopeEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun markingCodeDao(): MarkingCodeDao
    abstract fun scanLogDao(): ScanLogDao
    abstract fun syncEventDao(): SyncEventDao
    abstract fun localScopeDao(): LocalScopeDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE marking_codes ADD COLUMN scopeKey TEXT NOT NULL DEFAULT '${LocalScopeStore.LEGACY_SCOPE_KEY}'",
                )
                db.execSQL(
                    "ALTER TABLE scan_logs ADD COLUMN scopeKey TEXT NOT NULL DEFAULT '${LocalScopeStore.LEGACY_SCOPE_KEY}'",
                )
                db.execSQL("DROP INDEX IF EXISTS index_marking_codes_rawCodeSha256")
                db.execSQL("DROP INDEX IF EXISTS index_marking_codes_gtin_serial")
                db.execSQL("DROP INDEX IF EXISTS index_marking_codes_identityKey")
                db.execSQL("DROP INDEX IF EXISTS index_marking_codes_orderId")
                db.execSQL("DROP INDEX IF EXISTS index_marking_codes_packageCode")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_marking_codes_scopeKey_rawCodeSha256
                    ON marking_codes(scopeKey, rawCodeSha256)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_marking_codes_scopeKey_gtin_serial
                    ON marking_codes(scopeKey, gtin, serial)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_marking_codes_scopeKey_identityKey
                    ON marking_codes(scopeKey, identityKey)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_marking_codes_scopeKey_orderId
                    ON marking_codes(scopeKey, orderId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_marking_codes_scopeKey_packageCode
                    ON marking_codes(scopeKey, packageCode)
                    """.trimIndent(),
                )
                db.execSQL("DROP INDEX IF EXISTS index_scan_logs_status_createdAt")
                db.execSQL("DROP INDEX IF EXISTS index_scan_logs_gtin_serial")
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_scan_logs_scopeKey_status_createdAt
                    ON scan_logs(scopeKey, status, createdAt)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_scan_logs_scopeKey_gtin_serial
                    ON scan_logs(scopeKey, gtin, serial)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_events (
                        eventId TEXT NOT NULL PRIMARY KEY,
                        scopeKey TEXT NOT NULL,
                        eventType TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        status TEXT NOT NULL,
                        attempts INTEGER NOT NULL,
                        nextRetryAt INTEGER NOT NULL,
                        lastError TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_sync_events_scopeKey_status_nextRetryAt
                    ON sync_events(scopeKey, status, nextRetryAt)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_sync_events_createdAt
                    ON sync_events(createdAt)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_scopes (
                        scopeKey TEXT NOT NULL PRIMARY KEY,
                        plantId TEXT NOT NULL,
                        supplierId TEXT NOT NULL,
                        clientDeviceId TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
