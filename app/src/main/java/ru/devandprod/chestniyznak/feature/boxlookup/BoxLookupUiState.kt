package ru.devandprod.chestniyznak.feature.boxlookup

data class BoxLookupUiState(
    val isBusy: Boolean = false,
    val statusText: String = "",
    val lastScannedCode: String = "",
    val errorText: String? = null,
)
