package ru.devandprod.chestniyznak.domain.model

data class ClientPrinter(
    val id: Long,
    val name: String,
    val ipAddress: String,
    val section: String,
    val isActive: Boolean,
)

data class ClientPrinterSelection(
    val ok: Boolean,
    val deviceId: String,
    val selectedPrinterId: Long? = null,
    val selectedPrinter: ClientPrinter? = null,
    val printers: List<ClientPrinter> = emptyList(),
)
