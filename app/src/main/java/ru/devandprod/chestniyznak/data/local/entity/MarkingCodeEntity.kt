package ru.devandprod.chestniyznak.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marking_codes",
    indices = [
        Index(value = ["rawCodeSha256"], unique = true),
        Index(value = ["gtin", "serial"]),
        Index(value = ["identityKey"]),
    ],
)
data class MarkingCodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gtin: String,
    val serial: String,
    val identityKey: String,
    val aiPartsJson: String,
    val rawCode: String,
    val visibleCode: String,
    val rawCodeSha256: String,
    val status1c: String,
    val appStatus: String,
    val orderNumber: String,
)
