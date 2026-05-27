package ru.devandprod.chestniyznak.feature.boxdetail

data class BoxDetailUiState(
    val isLoading: Boolean = true,
    val isActionBusy: Boolean = false,
    val title: String = "",
    val errorText: String? = null,
    val box: BoxDetailUi? = null,
    val statusText: String = "",
)

data class BoxDetailUi(
    val boxId: Long,
    val orderName: String?,
    val sscc: String?,
    val filled: Int,
    val capacity: Int,
    val isClosed: Boolean,
    val isEditMode: Boolean,
    val activeUserName: String,
    val items: List<BoxDetailItemUi>,
)

data class BoxDetailItemUi(
    val id: Long,
    val visibleCode: String,
    val gtin: String,
    val serial: String,
)
