package ru.devandprod.chestniyznak.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.DefectMarkResult
import ru.devandprod.chestniyznak.domain.model.VerificationResult

interface ChestniyZnakRepository {
    suspend fun ensureSeedData()
    suspend fun verify(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    suspend fun verifyExists(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    suspend fun markDefect(rawInput: String, scannerId: String = ""): DefectMarkResult
    suspend fun refreshStats()
    fun observeStats(): Flow<CatalogStats>
}
