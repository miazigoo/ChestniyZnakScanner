package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.ClientPrinterSelection
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class GetClientPrinterSelectionUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(deviceId: String): ClientPrinterSelection =
        repository.getClientPrinterSelection(deviceId)
}
