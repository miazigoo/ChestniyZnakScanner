package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.PackingScanResult
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class ScanCodesToPackingBoxUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(
        boxId: Long,
        rawCodes: List<String>,
        scannerId: String = "",
    ): PackingScanResult = repository.scanCodesToBox(
        boxId = boxId,
        rawCodes = rawCodes,
        scannerId = scannerId,
    )
}
