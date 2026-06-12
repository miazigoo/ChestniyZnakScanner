package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class MarkLocalPackingPendingUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(rawInput: String, packageCode: String?) {
        repository.markLocalPackingPending(rawInput, packageCode)
    }
}
