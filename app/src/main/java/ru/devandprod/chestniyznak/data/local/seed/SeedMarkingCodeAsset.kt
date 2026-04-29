package ru.devandprod.chestniyznak.data.local.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedMarkingCodeAsset(
    val rawCode: String,
    val status1c: String = "",
    val appStatus: String = "active",
    val orderNumber: String = "",
)
