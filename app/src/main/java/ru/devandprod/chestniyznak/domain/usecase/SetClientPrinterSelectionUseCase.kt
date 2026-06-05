package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.ClientPrinterSelection
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class SetClientPrinterSelectionUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(deviceId: String = "", printerId: Long): ClientPrinterSelection =
        repository.setClientPrinterSelection(deviceId, printerId)
}
