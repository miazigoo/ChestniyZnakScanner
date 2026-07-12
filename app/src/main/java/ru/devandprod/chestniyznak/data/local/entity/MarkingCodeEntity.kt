package ru.devandprod.chestniyznak.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import ru.devandprod.chestniyznak.data.local.LocalScopeStore

@Entity(
    tableName = "marking_codes",
    indices = [
        Index(value = ["scopeKey", "rawCodeSha256"], unique = true),
        Index(value = ["scopeKey", "gtin", "serial"]),
        Index(value = ["scopeKey", "identityKey"]),
        Index(value = ["scopeKey", "orderId"]),
        Index(value = ["scopeKey", "packageCode"]),
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
    @ColumnInfo(defaultValue = "'legacy:unscoped'")
    val scopeKey: String = LocalScopeStore.LEGACY_SCOPE_KEY,
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
