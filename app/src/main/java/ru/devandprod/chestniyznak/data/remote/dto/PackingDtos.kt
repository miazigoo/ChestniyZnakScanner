package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBox
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.model.PackingBoxItem
import ru.devandprod.chestniyznak.domain.model.PackingBoxPage
import ru.devandprod.chestniyznak.domain.model.PackingScanResult

@Serializable
data class OpenBoxRequestDto(
    val capacity: Int? = null,
    @SerialName("allow_duplicate_scans")
    val allowDuplicateScans: Boolean? = null,
    @SerialName("device_id")
    val deviceId: String = "",
)

@Serializable
data class ScanToBoxRequestDto(
    val code: String,
    @SerialName("scanner_id")
    val scannerId: String = "",
)

@Serializable
data class BoxDto(
    @SerialName("box_id")
    val boxId: Long,
    @SerialName("order_id")
    val orderId: Long? = null,
    @SerialName("order_name")
    val orderName: String? = null,
    val sscc: String? = null,
    val capacity: Int,
    val filled: Int,
    @SerialName("allow_duplicate_scans")
    val allowDuplicateScans: Boolean,
    @SerialName("is_closed")
    val isClosed: Boolean,
    @SerialName("is_edit_mode")
    val isEditMode: Boolean,
    @SerialName("active_user_name")
    val activeUserName: String = "",
    @SerialName("created_by_name")
    val createdByName: String = "",
    @SerialName("print_ok")
    val printOk: Boolean = false,
    @SerialName("print_error")
    val printError: String = "",
)

@Serializable
data class BoxItemDto(
    val id: Long,
    @SerialName("code_id")
    val codeId: Long,
    @SerialName("scan_id")
    val scanId: Long? = null,
    val gtin: String,
    val serial: String,
    @SerialName("visible_code")
    val visibleCode: String,
)

@Serializable
data class BoxDetailDto(
    @SerialName("box_id")
    val boxId: Long,
    @SerialName("order_id")
    val orderId: Long? = null,
    @SerialName("order_name")
    val orderName: String? = null,
    val sscc: String? = null,
    val capacity: Int,
    val filled: Int,
    @SerialName("allow_duplicate_scans")
    val allowDuplicateScans: Boolean,
    @SerialName("is_closed")
    val isClosed: Boolean,
    @SerialName("is_edit_mode")
    val isEditMode: Boolean,
    @SerialName("active_user_name")
    val activeUserName: String = "",
    @SerialName("created_by_name")
    val createdByName: String = "",
    @SerialName("print_ok")
    val printOk: Boolean = false,
    @SerialName("print_error")
    val printError: String = "",
    val items: List<BoxItemDto> = emptyList(),
)

@Serializable
data class OpenBoxResponseDto(
    val ok: Boolean,
    val created: Boolean,
    @SerialName("has_active_boxes")
    val hasActiveBoxes: Boolean = false,
    val boxes: List<BoxDto> = emptyList(),
    val box: BoxDto,
)

@Serializable
data class BoxesListResponseDto(
    val items: List<BoxDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

@Serializable
data class CurrentBoxResponseDto(
    @SerialName("box_id")
    val boxId: Long,
    @SerialName("order_id")
    val orderId: Long? = null,
    @SerialName("order_name")
    val orderName: String? = null,
    val sscc: String? = null,
    val capacity: Int,
    val filled: Int,
    @SerialName("allow_duplicate_scans")
    val allowDuplicateScans: Boolean,
    @SerialName("is_closed")
    val isClosed: Boolean,
    @SerialName("is_edit_mode")
    val isEditMode: Boolean,
    @SerialName("active_user_name")
    val activeUserName: String = "",
    @SerialName("created_by_name")
    val createdByName: String = "",
    @SerialName("print_ok")
    val printOk: Boolean = false,
    @SerialName("print_error")
    val printError: String = "",
    val items: List<BoxItemDto> = emptyList(),
)

@Serializable
data class ScanToBoxResponseDto(
    val ok: Boolean,
    @SerialName("reason_code")
    val reasonCode: String,
    val error: String? = null,
    val duplicate: Boolean? = null,
    val verify: VerifyResponseDto? = null,
    val box: BoxDto,
    @SerialName("box_full_signal")
    val boxFullSignal: Boolean? = null,
)

@Serializable
data class CloseBoxResponseDto(
    val ok: Boolean,
    @SerialName("reason_code")
    val reasonCode: String,
    val error: String? = null,
    val box: BoxDto,
    @SerialName("print_ok")
    val printOk: Boolean? = null,
    @SerialName("print_error")
    val printError: String? = null,
)

fun OpenBoxResponseDto.toDomain(): OpenPackingBoxResult = OpenPackingBoxResult(
    ok = ok,
    created = created,
    hasActiveBoxes = hasActiveBoxes,
    boxes = boxes.map { it.toDomain() },
    box = box.toDomain(),
)

fun BoxesListResponseDto.toDomain(): PackingBoxPage = PackingBoxPage(
    items = items.map { it.toDomain() },
    total = total,
    limit = limit,
    offset = offset,
    hasMore = hasMore,
)

fun CurrentBoxResponseDto.toDomain(): PackingBoxDetail = PackingBoxDetail(
    box = toBox().toDomain(),
    items = items.map { it.toDomain() },
)

fun ScanToBoxResponseDto.toDomain(): PackingScanResult = PackingScanResult(
    ok = ok,
    reasonCode = reasonCode,
    error = error,
    duplicate = duplicate,
    verify = verify?.toDomain(),
    box = box.toDomain(),
    boxFullSignal = boxFullSignal,
)

fun CloseBoxResponseDto.toDomain(): ClosePackingBoxResult = ClosePackingBoxResult(
    ok = ok,
    reasonCode = reasonCode,
    error = error,
    box = box.toDomain(),
    printOk = printOk,
    printError = printError,
)

private fun CurrentBoxResponseDto.toBox(): BoxDto = BoxDto(
    boxId = boxId,
    orderId = orderId,
    orderName = orderName,
    sscc = sscc,
    capacity = capacity,
    filled = filled,
    allowDuplicateScans = allowDuplicateScans,
    isClosed = isClosed,
    isEditMode = isEditMode,
    activeUserName = activeUserName,
    createdByName = createdByName,
    printOk = printOk,
    printError = printError,
)

private fun BoxDto.toDomain(): PackingBox = PackingBox(
    boxId = boxId,
    orderId = orderId,
    orderName = orderName,
    sscc = sscc,
    capacity = capacity,
    filled = filled,
    allowDuplicateScans = allowDuplicateScans,
    isClosed = isClosed,
    isEditMode = isEditMode,
    activeUserName = activeUserName,
    createdByName = createdByName,
    printOk = printOk,
    printError = printError,
)

private fun BoxItemDto.toDomain(): PackingBoxItem = PackingBoxItem(
    id = id,
    codeId = codeId,
    scanId = scanId,
    gtin = gtin,
    serial = serial,
    visibleCode = visibleCode,
)
