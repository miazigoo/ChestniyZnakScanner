package ru.devandprod.chestniyznak.feature.scanner

data class DefectMarkUiState(
    val hasCameraPermission: Boolean = false,
    val isScannerEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val resultCard: ScanResultCardUi? = null,
    val orderName: String? = null,
    val deviceName: String? = null,
    val visibleCode: String = "",
    val technicalStatus: String = "",
    val warnings: List<String> = emptyList(),
    val removedFromBoxLabel: String? = null,
)
