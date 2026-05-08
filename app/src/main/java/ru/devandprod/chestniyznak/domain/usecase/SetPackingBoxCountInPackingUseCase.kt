package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.PackingBoxActionResult
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class SetPackingBoxCountInPackingUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(
        boxId: Long,
        countInPacking: Boolean,
    ): PackingBoxActionResult = repository.setBoxCountInPacking(
        boxId = boxId,
        countInPacking = countInPacking,
    )
}
