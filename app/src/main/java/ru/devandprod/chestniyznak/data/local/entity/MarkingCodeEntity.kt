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
        Index(value = ["orderId"]),
        Index(value = ["packageCode"]),
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
    val orderId: String = "",
    val orderLineId: String? = null,
    val remoteCodeId: String? = null,
    val packageUnitId: String? = null,
    val packageCode: String? = null,
    val packageStatus: String? = null,
    val packageClosedAt: String? = null,
    val remoteUpdatedAt: String? = null,
)
