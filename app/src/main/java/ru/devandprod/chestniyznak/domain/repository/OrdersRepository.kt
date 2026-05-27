package ru.devandprod.chestniyznak.domain.repository

import ru.devandprod.chestniyznak.domain.model.WorkOrderPage

interface OrdersRepository {
    suspend fun listWorkOrders(
        status: String? = null,
        search: String? = null,
        page: Int = 1,
        perPage: Int = 20,
    ): WorkOrderPage
}
