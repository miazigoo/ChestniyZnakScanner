package ru.devandprod.chestniyznak.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_logs",
    indices = [
        Index(value = ["status", "createdAt"]),
        Index(value = ["gtin", "serial"]),
        Index(value = ["codeId"]),
    ],
)
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val codeId: Long? = null,
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
