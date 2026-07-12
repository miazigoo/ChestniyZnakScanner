package ru.devandprod.chestniyznak.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import ru.devandprod.chestniyznak.data.local.LocalScopeStore

@Entity(
    tableName = "scan_logs",
    indices = [
        Index(value = ["scopeKey", "status", "createdAt"]),
        Index(value = ["scopeKey", "gtin", "serial"]),
        Index(value = ["codeId"]),
    ],
)
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val codeId: Long? = null,
    @ColumnInfo(defaultValue = "'legacy:unscoped'")
    val scopeKey: String = LocalScopeStore.LEGACY_SCOPE_KEY,
    val status: String,
    val message: String,
    val rawInput: String,
    val normalizedCode: String,
    val visibleCode: String,
    val gtin: String,
    val serial: String,
    val aiPartsJson: String,
    val warningsJson: String,
    val scannerId: String,
    val scannerGsNative: Boolean,
    val gsRestored: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)
