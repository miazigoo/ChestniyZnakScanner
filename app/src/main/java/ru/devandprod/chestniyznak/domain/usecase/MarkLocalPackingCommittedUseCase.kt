package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class MarkLocalPackingCommittedUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(
        rawCodes: List<String>,
        packageCode: String,
        packageClosedAt: String?,
        orderId: String? = null,
    ) {
        repository.markLocalPackingCommitted(
            rawCodes = rawCodes,
            packageCode = packageCode,
            packageClosedAt = packageClosedAt,
            orderId = orderId,
        )
    }
}
