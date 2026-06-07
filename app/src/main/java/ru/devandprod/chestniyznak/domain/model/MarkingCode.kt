package ru.devandprod.chestniyznak.domain.model

data class MarkingCode(
    val id: Long,
    val gtin: String,
    val serial: String,
    val aiParts: Map<String, String>,
    val visibleCode: String,
    val status1c: String,
    val appStatus: String,
    val orderNumber: String,
    val orderName: String = "",
    val deviceName: String = "",
    val packageCode: String? = null,
    val packageStatus: String? = null,
)
