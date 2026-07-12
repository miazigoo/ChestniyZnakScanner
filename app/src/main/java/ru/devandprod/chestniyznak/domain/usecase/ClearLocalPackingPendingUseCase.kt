package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class ClearLocalPackingPendingUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(
        rawCodes: List<String>,
        orderId: String? = null,
        packageUuid: String? = null,
    ) {
        repository.clearLocalPackingPending(rawCodes, orderId, packageUuid)
    }
}
