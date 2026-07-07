package ru.devandprod.chestniyznak.feature.scanner

import ru.devandprod.chestniyznak.domain.model.VerificationBoxInfo

data class ScanUiState(
    val isLoading: Boolean = true,
    val statsLabel: String = "",
    val scansLabel: String = "",
    val scanMode: ScanMode = ScanMode.PackingTsd,
    val verify: VerifyPaneUiState = VerifyPaneUiState(),
    val packing: PackingPaneUiState = PackingPaneUiState(),
)

data class VerifyPaneUiState(
    val hasCameraPermission: Boolean = false,
    val isScannerEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val scannerRearmKey: Long = 0L,
    val resultCard: ScanResultCardUi? = null,
    val boxInfo: VerificationBoxInfo? = null,
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
    val orderLines: List<PackingOrderLineUi> = emptyList(),
    val selectedOrderLineId: String = "",
    val orderSearch: String = "",
    val ordersLoading: Boolean = false,
    val ordersLoaded: Boolean = false,
    val localPoolLoading: Boolean = false,
    val localPoolOrderId: String = "",
    val localPoolCount: Int = 0,
    val localPendingCodes: List<String> = emptyList(),
    val resultCard: ScanResultCardUi? = null,
    val statusText: String = "",
    val errorText: String? = null,
    val showPrinterSettingsAction: Boolean = false,
    val showPrinterRequiredDialog: Boolean = false,
    val lastScannedCode: String = "",
    val activeBoxesDialog: ActiveBoxesDialogUi? = null,
    val closeDialog: CloseBoxDialogUi? = null,
    val itemMenuItemId: Long? = null,
    val confirmDeleteEmptyBoxDialog: Boolean = false,
)

data class PackingOrderLineUi(
    val orderId: String,
    val orderLineId: String,
    val orderNumber: String,
    val sku: String,
    val productName: String,
    val label: String,
    val packageCapacity: Int? = null,
    val scanRequired: Boolean = true,
)

data class PackingBoxUi(
    val boxId: Long,
    val orderUuid: String? = null,
    val orderName: String? = null,
    val sscc: String? = null,
    val filled: Int,
    val capacity: Int,
    val countInPacking: Boolean = true,
    val allowDuplicateScans: Boolean,
    val activeUserName: String = "",
    val items: List<PackingBoxItemUi> = emptyList(),
)

data class PackingBoxItemUi(
    val id: Long,
    val gtin: String,
    val serial: String,
    val visibleCode: String,
    val rawCode: String = "",
)

data class ActiveBoxesDialogUi(
    val boxes: List<PackingBoxUi>,
)

data class CloseBoxDialogUi(
    val boxId: Long,
    val sscc: String? = null,
    val isFull: Boolean,
    val printOk: Boolean? = null,
    val printError: String = "",
    val printPrinterName: String = "",
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
