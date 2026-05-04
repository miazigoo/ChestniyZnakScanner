package ru.devandprod.chestniyznak.feature.boxlookup

data class BoxLookupUiState(
    val isBusy: Boolean = false,
    val statusText: String = "Сканируйте штрихкод коробки",
    val lastScannedCode: String = "",
    val errorText: String? = null,
)
