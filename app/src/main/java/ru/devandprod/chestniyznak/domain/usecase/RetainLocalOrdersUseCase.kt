package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class RetainLocalOrdersUseCase @Inject constructor(
    private val chestniyZnakRepository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(orderIds: List<String>) {
        chestniyZnakRepository.retainLocalOrders(orderIds)
    }
}
