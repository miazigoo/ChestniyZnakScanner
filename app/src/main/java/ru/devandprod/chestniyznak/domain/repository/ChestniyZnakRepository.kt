package ru.devandprod.chestniyznak.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.DefectMarkResult
import ru.devandprod.chestniyznak.domain.model.LocalPackingPendingCode
import ru.devandprod.chestniyznak.domain.model.OrderLocalCode
import ru.devandprod.chestniyznak.domain.model.VerificationResult

interface ChestniyZnakRepository {
    suspend fun ensureSeedData()
    suspend fun replaceLocalPool(orderNumber: String, orderId: String, codes: List<OrderLocalCode>)
    suspend fun verify(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    suspend fun verifyExists(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    suspend fun verifyLocalOnly(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    suspend fun getLocalPackingPending(packageCode: String): List<LocalPackingPendingCode>
    suspend fun markLocalPackingPending(rawInput: String, packageCode: String?)
    suspend fun clearLocalPackingPending(rawCodes: List<String>)
    suspend fun markDefect(rawInput: String, scannerId: String = ""): DefectMarkResult
    suspend fun refreshStats()
    fun observeStats(): Flow<CatalogStats>
}
