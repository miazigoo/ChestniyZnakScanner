package ru.devandprod.chestniyznak.feature.scanner

data class ScanUiState(
    val isLoading: Boolean = true,
    val hasCameraPermission: Boolean = false,
    val isScannerEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val statsLabel: String = "В базе 0 кодов",
    val scansLabel: String = "Проверок 0",
    val resultCard: ScanResultCardUi? = null,
    val visibleCode: String = "",
    val technicalStatus: String = "",
    val warnings: List<String> = emptyList(),
)

enum class ScanResultTone {
    Success,
    Error,
}

data class ScanResultCardUi(
    val headline: String,
    val message: String,
    val tone: ScanResultTone,
)
