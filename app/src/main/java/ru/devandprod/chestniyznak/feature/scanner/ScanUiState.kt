package ru.devandprod.chestniyznak.feature.scanner

data class ScanUiState(
    val isLoading: Boolean = true,
    val statsLabel: String = "В базе 0 кодов",
    val scansLabel: String = "Проверок 0",
    val scanMode: ScanMode = ScanMode.PackingTsd,
    val verify: VerifyPaneUiState = VerifyPaneUiState(),
    val packing: PackingPaneUiState = PackingPaneUiState(),
)

data class VerifyPaneUiState(
    val hasCameraPermission: Boolean = false,
    val isScannerEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val resultCard: ScanResultCardUi? = null,
    val orderName: String? = null,
    val deviceName: String? = null,
    val visibleCode: String = "",
    val technicalStatus: String = "",
    val warnings: List<String> = emptyList(),
)

data class PackingPaneUiState(
    val isBusy: Boolean = false,
    val currentBox: PackingBoxUi? = null,
    val countInPacking: Boolean = true,
    val resultCard: ScanResultCardUi? = null,
    val statusText: String = "Открытая коробка не найдена",
    val errorText: String? = null,
    val lastScannedCode: String = "",
    val activeBoxesDialog: ActiveBoxesDialogUi? = null,
    val closeDialog: CloseBoxDialogUi? = null,
    val itemMenuItemId: Long? = null,
    val confirmDeleteEmptyBoxDialog: Boolean = false,
)

data class PackingBoxUi(
    val boxId: Long,
    val orderName: String? = null,
    val sscc: String? = null,
    val filled: Int,
    val capacity: Int,
    val countInPacking: Boolean = true,
    val allowDuplicateScans: Boolean,
    val activeUserName: String = "",
    val printOk: Boolean = false,
    val printError: String = "",
    val items: List<PackingBoxItemUi> = emptyList(),
)

data class PackingBoxItemUi(
    val id: Long,
    val gtin: String,
    val serial: String,
    val visibleCode: String,
)

data class ActiveBoxesDialogUi(
    val boxes: List<PackingBoxUi>,
)

data class CloseBoxDialogUi(
    val boxId: Long,
    val sscc: String? = null,
    val isFull: Boolean,
)

enum class ScanMode {
    CameraVerify,
    PackingCamera,
    PackingTsd,
}

enum class ScanResultTone {
    Success,
    Error,
    Warning,
}

data class ScanResultCardUi(
    val headline: String,
    val message: String,
    val tone: ScanResultTone,
)
