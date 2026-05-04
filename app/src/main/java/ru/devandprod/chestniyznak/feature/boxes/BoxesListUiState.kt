package ru.devandprod.chestniyznak.feature.boxes

data class BoxesListUiState(
    val title: String = "",
    val filter: String = "all",
    val isLoading: Boolean = true,
    val errorText: String? = null,
    val boxes: List<BoxListItemUi> = emptyList(),
    val totalLabel: String = "",
)

data class BoxListItemUi(
    val boxId: Long,
    val orderName: String?,
    val sscc: String?,
    val filled: Int,
    val capacity: Int,
    val isClosed: Boolean,
    val activeUserName: String,
)
