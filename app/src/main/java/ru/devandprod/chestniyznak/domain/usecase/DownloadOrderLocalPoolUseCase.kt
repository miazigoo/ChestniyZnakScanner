package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.OrderLocalCode
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository
import ru.devandprod.chestniyznak.domain.repository.OrdersRepository

class DownloadOrderLocalPoolUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val chestniyZnakRepository: ChestniyZnakRepository,
) {
    suspend operator fun invoke(
        orderId: String,
        preserveLocalPending: Boolean = true,
    ): Int {
        val codes = mutableListOf<OrderLocalCode>()
        var orderNumber = ""
        var offset = 0
        do {
            val page = ordersRepository.downloadLocalCodePool(
                orderId = orderId,
                limit = PAGE_SIZE,
                offset = offset,
            )
            if (orderNumber.isBlank()) orderNumber = page.order.orderNumber
            codes += page.codes
            offset = page.nextOffset ?: (offset + page.count)
        } while (page.hasMore)

        chestniyZnakRepository.replaceLocalPool(
            orderNumber = orderNumber,
            orderId = orderId,
            codes = codes,
            preserveLocalPending = preserveLocalPending,
        )
        return codes.size
    }

    private companion object {
        const val PAGE_SIZE = 5000
    }
}
