package ru.devandprod.chestniyznak.domain.model

data class WorkOrderProduct(
    val id: String,
    val sku: String,
    val name: String,
    val gtin: String? = null,
    val unit: String = "pcs",
)

data class WorkOrderLine(
    val id: String,
    val orderId: String,
    val productId: String,
    val quantity: Int,
    val requiredCodeQuantity: Int,
    val packageCapacity: Int? = null,
    val status: String,
    val product: WorkOrderProduct? = null,
)

data class WorkOrder(
    val id: String,
    val plantId: String,
    val supplierId: String,
    val orderNumber: String,
    val externalNumber: String? = null,
    val status: String,
    val scanRequired: Boolean = true,
    val plannedDate: String? = null,
    val lines: List<WorkOrderLine> = emptyList(),
)

data class WorkOrderPage(
    val orders: List<WorkOrder>,
    val page: Int,
    val perPage: Int,
    val count: Int,
)
