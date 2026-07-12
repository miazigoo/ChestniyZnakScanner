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
    val deadlineAt: String? = null,
    val workflowRevision: Int = 1,
    val plantName: String? = null,
    val requiredCodes: Int? = null,
    val packedCodes: Int? = null,
    val remainingToPack: Int? = null,
    val availableToPack: Int? = null,
    val supplierNewCodes: Int? = null,
    val primaryActionCode: String? = null,
    val lines: List<WorkOrderLine> = emptyList(),
)

data class WorkOrderPage(
    val orders: List<WorkOrder>,
    val page: Int,
    val perPage: Int,
    val count: Int,
)

data class OrderLocalCode(
    val id: String,
    val code: String,
    val status: String,
    val orderLineId: String? = null,
    val packageUnitId: String? = null,
    val packageCode: String? = null,
    val packageStatus: String? = null,
    val packageClosedAt: String? = null,
    val updatedAt: String? = null,
)

data class OrderLocalPoolPage(
    val order: WorkOrder,
    val codes: List<OrderLocalCode>,
    val total: Int,
    val count: Int,
    val limit: Int,
    val offset: Int,
    val nextOffset: Int?,
    val hasMore: Boolean,
)
