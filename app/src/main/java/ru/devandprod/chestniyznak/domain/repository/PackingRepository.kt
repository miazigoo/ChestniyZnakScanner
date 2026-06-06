package ru.devandprod.chestniyznak.domain.repository

import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.ClientPrinterSelection
import ru.devandprod.chestniyznak.domain.model.PackageLabelPrintResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxActionResult
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.model.PackingBoxPage
import ru.devandprod.chestniyznak.domain.model.PackingScanResult

interface PackingRepository {
    suspend fun getClientPrinterSelection(deviceId: String = ""): ClientPrinterSelection
    suspend fun setClientPrinterSelection(deviceId: String = "", printerId: Long): ClientPrinterSelection
    suspend fun getCurrentBox(): PackingBoxDetail?
    suspend fun getBox(boxId: Long): PackingBoxDetail
    suspend fun listBoxes(status: String = "all", query: String = "", limit: Int = 50, offset: Int = 0): PackingBoxPage
    suspend fun openBox(
        deviceId: String = "",
        countInPacking: Boolean = true,
        orderId: String? = null,
        orderLineId: String? = null,
        capacity: Int? = null,
        codeValue: String? = null,
        sscc: String? = null,
    ): OpenPackingBoxResult
    suspend fun setBoxCountInPacking(boxId: Long, countInPacking: Boolean): PackingBoxActionResult
    suspend fun openBoxEdit(boxId: Long, reason: String = ""): PackingBoxActionResult
    suspend fun removeBoxItem(boxId: Long, itemId: Long): PackingBoxActionResult
    suspend fun clearBox(boxId: Long): PackingBoxActionResult
    suspend fun deleteEmptyBox(boxId: Long): PackingBoxActionResult
    suspend fun scanCodeToBox(boxId: Long, rawCode: String, scannerId: String = ""): PackingScanResult
    suspend fun closeBox(boxId: Long, deviceId: String = ""): ClosePackingBoxResult
    suspend fun printBoxLabel(boxId: Long, deviceId: String = ""): PackageLabelPrintResult
}
