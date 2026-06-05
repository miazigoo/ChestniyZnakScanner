package ru.devandprod.chestniyznak.feature.boxedit

data class BoxEditUiState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val isAwaitingScan: Boolean = false,
    val scanMode: BoxEditScanMode = BoxEditScanMode.Hid,
    val hasCameraPermission: Boolean = false,
    val title: String = "",
    val statusText: String = "",
    val errorText: String? = null,
    val lastScannedCode: String = "",
    val box: EditableBoxUi? = null,
    val confirmClearDialog: Boolean = false,
    val itemMenuItemId: Long? = null,
)

data class EditableBoxUi(
    val boxId: Long,
    val orderName: String?,
    val sscc: String?,
    val filled: Int,
    val capacity: Int,
    val isClosed: Boolean,
    val isEditMode: Boolean,
    val activeUserName: String = "",
    val items: List<EditableBoxItemUi>,
)

data class EditableBoxItemUi(
    val id: Long,
    val visibleCode: String,
    val gtin: String,
    val serial: String,
)

enum class BoxEditScanMode {
    Hid,
    Camera,
}
