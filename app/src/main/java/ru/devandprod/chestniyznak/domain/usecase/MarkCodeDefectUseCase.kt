package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.DefectMarkResult
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class MarkCodeDefectUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(
        rawInput: String,
        scannerId: String = "",
    ): DefectMarkResult = repository.markDefect(
        rawInput = rawInput,
        scannerId = scannerId,
    )
}
