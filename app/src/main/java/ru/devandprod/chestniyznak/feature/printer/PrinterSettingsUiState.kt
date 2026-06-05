package ru.devandprod.chestniyznak.feature.printer

data class PrinterSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val deviceId: String = "",
    val selectedPrinterId: Long? = null,
    val selectedPrinterLabel: String = "Не выбран",
    val statusText: String = "",
    val errorText: String? = null,
    val printers: List<PrinterItemUi> = emptyList(),
)

data class PrinterItemUi(
    val id: Long,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val section: String,
    val isSelected: Boolean,
)
