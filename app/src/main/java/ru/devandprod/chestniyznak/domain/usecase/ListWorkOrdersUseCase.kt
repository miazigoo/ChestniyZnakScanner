package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.WorkOrderPage
import ru.devandprod.chestniyznak.domain.repository.OrdersRepository

class ListWorkOrdersUseCase @Inject constructor(
    private val repository: OrdersRepository,
) {
    suspend operator fun invoke(
        status: String? = null,
        search: String? = null,
        page: Int = 1,
        perPage: Int = 20,
    ): WorkOrderPage = repository.listWorkOrders(
        status = status,
        search = search,
        page = page,
        perPage = perPage,
    )
}
