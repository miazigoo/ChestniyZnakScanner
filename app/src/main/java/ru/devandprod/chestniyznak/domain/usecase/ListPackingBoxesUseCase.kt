package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.PackingBoxPage
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class ListPackingBoxesUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(
        status: String = "all",
        query: String = "",
        limit: Int = 50,
        offset: Int = 0,
    ): PackingBoxPage = repository.listBoxes(
        status = status,
        query = query,
        limit = limit,
        offset = offset,
    )
}
