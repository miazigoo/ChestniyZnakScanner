package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.PackingScanResult
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class ScanCodeToPackingBoxUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(
        boxId: Long,
        rawCode: String,
        scannerId: String = "",
    ): PackingScanResult = repository.scanCodeToBox(
        boxId = boxId,
        rawCode = rawCode,
        scannerId = scannerId,
    )
}
