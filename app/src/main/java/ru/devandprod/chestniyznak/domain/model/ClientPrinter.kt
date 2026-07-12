package ru.devandprod.chestniyznak.domain.model

data class ClientPrinter(
    val id: Long,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val section: String,
    val driver: String,
    val isActive: Boolean,
) {
    val label: String
        get() = buildString {
            append(name)
            append(" · ")
            append(ipAddress)
            append(":")
            append(port)
            if (section.isNotBlank()) {
                append(" · ")
                append(section)
            }
        }
}

data class ClientPrinterSelection(
    val ok: Boolean,
    val deviceId: String,
    val selectedPrinterId: Long? = null,
    val selectedPrinter: ClientPrinter? = null,
    val printers: List<ClientPrinter> = emptyList(),
)

data class PrintJob(
    val id: String? = null,
    val claimToken: String? = null,
    val status: String = "",
    val packageRevision: Int? = null,
    val format: String,
    val driver: String,
    val encoding: String,
    val transport: String,
    val payload: String,
    val printer: ClientPrinter? = null,
)

data class PackageLabelPrintResult(
    val ok: Boolean? = null,
    val reasonCode: String = "",
    val printStatus: String = "",
    val printOk: Boolean = false,
    val printErrorCode: String = "",
    val printError: String = "",
    val printer: ClientPrinter? = null,
    val printJob: PrintJob? = null,
    val box: PackingBox? = null,
)
