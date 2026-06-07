package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.devandprod.chestniyznak.domain.model.OrderLocalCode
import ru.devandprod.chestniyznak.domain.model.OrderLocalPoolPage
import ru.devandprod.chestniyznak.domain.model.WorkOrder
import ru.devandprod.chestniyznak.domain.model.WorkOrderLine
import ru.devandprod.chestniyznak.domain.model.WorkOrderPage
import ru.devandprod.chestniyznak.domain.model.WorkOrderProduct

@Serializable
data class OrdersResponseDto(
    val data: List<RemoteOrderDto> = emptyList(),
    val meta: RemotePaginationMetaDto = RemotePaginationMetaDto(),
)

@Serializable
data class LocalCodePoolResponseDto(
    val data: LocalCodePoolDto,
)

@Serializable
data class LocalCodePoolDto(
    val order: RemoteOrderDto,
    val codes: List<LocalPoolCodeDto> = emptyList(),
    val total: Int = 0,
    val count: Int = 0,
    val limit: Int = 5000,
    val offset: Int = 0,
    @SerialName("next_offset")
    val nextOffset: Int? = null,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

@Serializable
data class LocalPoolCodeDto(
    val id: String,
    val code: String,
    val status: String,
    @SerialName("order_line_id")
    val orderLineId: String? = null,
    @SerialName("package_unit_id")
    val packageUnitId: String? = null,
    @SerialName("package_code")
    val packageCode: String? = null,
    @SerialName("package_status")
    val packageStatus: String? = null,
    @SerialName("package_closed_at")
    val packageClosedAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class RemotePaginationMetaDto(
    val page: Int = 1,
    @SerialName("per_page")
    val perPage: Int = 20,
    val count: Int = 0,
)

@Serializable
data class RemoteOrderDto(
    val id: String,
    @SerialName("plant_id")
    val plantId: String,
    @SerialName("supplier_id")
    val supplierId: String,
    @SerialName("order_number")
    val orderNumber: String,
    @SerialName("external_number")
    val externalNumber: String? = null,
    val status: String,
    @SerialName("scan_required")
    val scanRequired: Boolean = true,
    @SerialName("planned_date")
    val plannedDate: String? = null,
    val lines: List<RemoteOrderLineDto> = emptyList(),
)

@Serializable
data class RemoteOrderLineDto(
    val id: String,
    @SerialName("order_id")
    val orderId: String,
    @SerialName("product_id")
    val productId: String,
    val quantity: Int,
    @SerialName("required_code_quantity")
    val requiredCodeQuantity: Int,
    @SerialName("package_capacity")
    val packageCapacity: Int? = null,
    val status: String,
    val product: RemoteOrderProductDto? = null,
)

@Serializable
data class RemoteOrderProductDto(
    val id: String,
    val sku: String,
    val name: String,
    val gtin: String? = null,
    val unit: String = "pcs",
)

fun OrdersResponseDto.toDomain(): WorkOrderPage = WorkOrderPage(
    orders = data.map { it.toDomain() },
    page = meta.page,
    perPage = meta.perPage,
    count = meta.count,
)

fun LocalCodePoolResponseDto.toDomain(): OrderLocalPoolPage = data.toDomain()

fun LocalCodePoolDto.toDomain(): OrderLocalPoolPage = OrderLocalPoolPage(
    order = order.toDomain(),
    codes = codes.map { it.toDomain() },
    total = total,
    count = count,
    limit = limit,
    offset = offset,
    nextOffset = nextOffset,
    hasMore = hasMore,
)

fun LocalPoolCodeDto.toDomain(): OrderLocalCode = OrderLocalCode(
    id = id,
    code = code,
    status = status,
    orderLineId = orderLineId,
    packageUnitId = packageUnitId,
    packageCode = packageCode,
    packageStatus = packageStatus,
    packageClosedAt = packageClosedAt,
    updatedAt = updatedAt,
)

fun RemoteOrderDto.toDomain(): WorkOrder = WorkOrder(
    id = id,
    plantId = plantId,
    supplierId = supplierId,
    orderNumber = orderNumber,
    externalNumber = externalNumber,
    status = status,
    scanRequired = scanRequired,
    plannedDate = plannedDate,
    lines = lines.map { it.toDomain() },
)

fun RemoteOrderLineDto.toDomain(): WorkOrderLine = WorkOrderLine(
    id = id,
    orderId = orderId,
    productId = productId,
    quantity = quantity,
    requiredCodeQuantity = requiredCodeQuantity,
    packageCapacity = packageCapacity,
    status = status,
    product = product?.toDomain(),
)

fun RemoteOrderProductDto.toDomain(): WorkOrderProduct = WorkOrderProduct(
    id = id,
    sku = sku,
    name = name,
    gtin = gtin,
    unit = unit,
)
