package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class VerifyLocalPoolCodeUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(
        rawInput: String,
        scannerId: String = "",
        allowDuplicate: Boolean = false,
        orderId: String? = null,
    ): VerificationResult = repository.verifyLocalOnly(
        rawInput = rawInput,
        scannerId = scannerId,
        allowDuplicate = allowDuplicate,
        orderId = orderId,
    )
}
