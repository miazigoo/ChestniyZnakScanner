package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class PrintPackingBoxLabelUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(boxId: Long, deviceId: String = ""): ClosePackingBoxResult =
        repository.printBoxLabel(boxId, deviceId)
}
