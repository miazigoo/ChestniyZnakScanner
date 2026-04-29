package ru.devandprod.chestniyznak.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.VerificationResult

interface ChestniyZnakRepository {
    suspend fun ensureSeedData()
    suspend fun verify(rawInput: String, scannerId: String = "", allowDuplicate: Boolean = false): VerificationResult
    fun observeStats(): Flow<CatalogStats>
}
