package ru.devandprod.chestniyznak.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.DefectMarkResult
import ru.devandprod.chestniyznak.domain.model.LocalPackingPendingCode
import ru.devandprod.chestniyznak.domain.model.OrderLocalCode
import ru.devandprod.chestniyznak.domain.model.VerificationResult

interface ChestniyZnakRepository {
    suspend fun ensureSeedData()
    suspend fun replaceLocalPool(
        orderNumber: String,
        orderId: String,
        codes: List<OrderLocalCode>,
        preserveLocalPending: Boolean,
    )
    suspend fun retainLocalOrders(orderIds: List<String>)
    suspend fun verify(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    suspend fun verifyExists(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    suspend fun verifyLocalOnly(
        rawInput: String,
        scannerId: String = "",
        allowDuplicate: Boolean = false,
        orderId: String? = null,
    ): VerificationResult
    suspend fun getLocalPackingPending(
        packageCode: String,
        orderId: String? = null,
    ): List<LocalPackingPendingCode>
    suspend fun markLocalPackingPending(rawInput: String, packageCode: String?, orderId: String? = null)
    suspend fun clearLocalPackingPending(rawCodes: List<String>, orderId: String? = null)
    suspend fun markLocalPackingCommitted(
        rawCodes: List<String>,
        packageCode: String,
        packageClosedAt: String?,
        orderId: String? = null,
    )
    suspend fun markDefect(rawInput: String, scannerId: String = ""): DefectMarkResult
    suspend fun refreshStats()
    fun observeStats(): Flow<CatalogStats>
}
