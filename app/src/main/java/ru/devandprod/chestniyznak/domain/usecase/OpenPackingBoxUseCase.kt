package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

class OpenPackingBoxUseCase @Inject constructor(
    private val repository: PackingRepository,
) {
    suspend operator fun invoke(
        deviceId: String = "",
        countInPacking: Boolean = true,
        orderId: String? = null,
        orderLineId: String? = null,
        capacity: Int? = null,
        codeValue: String? = null,
        sscc: String? = null,
    ): OpenPackingBoxResult = repository.openBox(
        deviceId = deviceId,
        countInPacking = countInPacking,
        orderId = orderId,
        orderLineId = orderLineId,
        capacity = capacity,
        codeValue = codeValue,
        sscc = sscc,
    )
}
