package ru.devandprod.chestniyznak.domain.repository

import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxActionResult
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.model.PackingBoxPage
import ru.devandprod.chestniyznak.domain.model.PackingScanResult

interface PackingRepository {
    suspend fun getCurrentBox(): PackingBoxDetail?
    suspend fun getBox(boxId: Long): PackingBoxDetail
    suspend fun listBoxes(status: String = "all", query: String = "", limit: Int = 50, offset: Int = 0): PackingBoxPage
    suspend fun openBox(deviceId: String = ""): OpenPackingBoxResult
    suspend fun openBoxEdit(boxId: Long, reason: String = ""): PackingBoxActionResult
    suspend fun printBoxLabel(boxId: Long): ClosePackingBoxResult
    suspend fun scanCodeToBox(boxId: Long, rawCode: String, scannerId: String = ""): PackingScanResult
    suspend fun closeBox(boxId: Long): ClosePackingBoxResult
}
