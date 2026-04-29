package ru.devandprod.chestniyznak.domain.model

data class ParsedMarkingCode(
    val gtin: String,
    val serial: String,
    val aiParts: Map<String, String>,
    val rawCode: String,
    val visibleCode: String,
    val scannerGsNative: Boolean,
    val gsRestored: Boolean,
    val warnings: List<String>,
) {
    val identityKey: String
        get() = "$gtin|$serial"
}
