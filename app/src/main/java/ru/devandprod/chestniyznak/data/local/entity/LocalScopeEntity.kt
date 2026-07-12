package ru.devandprod.chestniyznak.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_scopes")
data class LocalScopeEntity(
    @PrimaryKey
    val scopeKey: String,
    val plantId: String = "",
    val supplierId: String = "",
    val clientDeviceId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
