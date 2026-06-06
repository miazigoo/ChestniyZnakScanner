package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository
import ru.devandprod.chestniyznak.domain.repository.OrdersRepository

class DownloadOrderLocalPoolUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val chestniyZnakRepository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(orderId: String): Int {
        val rawCodes = mutableListOf<String>()
        var orderNumber = ""
        var offset = 0
        do {
            val page = ordersRepository.downloadLocalCodePool(
                orderId = orderId,
                limit = PAGE_SIZE,
                offset = offset,
            )
            if (orderNumber.isBlank()) orderNumber = page.order.orderNumber
            rawCodes += page.codes.map { it.code }
            offset = page.nextOffset ?: (offset + page.count)
        } while (page.hasMore)

        chestniyZnakRepository.replaceLocalPool(orderNumber = orderNumber, rawCodes = rawCodes)
        return rawCodes.size
    }

    private companion object {
        const val PAGE_SIZE = 5000
    }
}
