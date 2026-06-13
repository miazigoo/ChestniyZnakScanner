package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.LocalPackingPendingCode
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class GetLocalPackingPendingUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(packageCode: String): List<LocalPackingPendingCode> =
        repository.getLocalPackingPending(packageCode)
}
