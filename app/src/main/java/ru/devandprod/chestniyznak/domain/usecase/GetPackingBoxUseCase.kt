package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class GetPackingBoxUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(boxId: Long): PackingBoxDetail = repository.getBox(boxId)
}
