package ru.devandprod.chestniyznak.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderDtosTest {

    @Test
    fun `orders response maps product nomenclature to domain`() {
        val response = OrdersResponseDto(
            data = listOf(
                RemoteOrderDto(
                    id = "order-1",
                    plantId = "plant-1",
                    supplierId = "supplier-1",
                    orderNumber = "ORDER-1",
                    status = "issued_to_supplier",
                    scanRequired = false,
                    lines = listOf(
                        RemoteOrderLineDto(
                            id = "line-1",
                            orderId = "order-1",
                            productId = "product-1",
                            quantity = 10,
                            requiredCodeQuantity = 10,
                            status = "active",
                            product = RemoteOrderProductDto(
                                id = "product-1",
                                sku = "SKU-1",
                                name = "Номенклатура 1",
                                gtin = "04600000000001",
                            ),
                        ),
                    ),
                ),
            ),
            meta = RemotePaginationMetaDto(page = 1, perPage = 20, count = 1),
        )

        val domain = response.toDomain()

        assertEquals(1, domain.count)
        assertEquals("ORDER-1", domain.orders.first().orderNumber)
        assertEquals(false, domain.orders.first().scanRequired)
        assertEquals("line-1", domain.orders.first().lines.first().id)
        assertEquals("SKU-1", domain.orders.first().lines.first().product?.sku)
        assertEquals("Номенклатура 1", domain.orders.first().lines.first().product?.name)
    }
}
