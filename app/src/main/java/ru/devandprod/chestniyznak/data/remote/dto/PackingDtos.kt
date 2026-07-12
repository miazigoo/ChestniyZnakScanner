package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.ClientPrinter
import ru.devandprod.chestniyznak.domain.model.ClientPrinterSelection
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackageLabelPrintResult
import ru.devandprod.chestniyznak.domain.model.PackingBox
import ru.devandprod.chestniyznak.domain.model.PackingBoxActionResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.model.PackingBoxItem
import ru.devandprod.chestniyznak.domain.model.PackingBoxPage
import ru.devandprod.chestniyznak.domain.model.PackingScanResult
import ru.devandprod.chestniyznak.domain.model.PrintJob

@Serializable
data class OpenBoxRequestDto(
    val capacity: Int? = null,
    @SerialName("allow_duplicate_scans")
    val allowDuplicateScans: Boolean? = null,
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("count_in_packing")
    val countInPacking: Boolean = true,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("order_line_id")
    val orderLineId: String? = null,
    @SerialName("code_value")
    val codeValue: String? = null,
    val sscc: String? = null,
)

@Serializable
data class EditBoxRequestDto(
    val reason: String = "",
)

@Serializable
data class CountInPackingRequestDto(
    @SerialName("count_in_packing")
    val countInPacking: Boolean,
)

@Serializable
data class ClientPrinterSelectionRequestDto(
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("printer_id")
    val printerId: Long,
)

@Serializable
data class PackageLabelPrintRequestDto(
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("printer_id")
    val printerId: Long? = null,
)

@Serializable
data class PackageLabelPrintResultRequestDto(
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("printer_id")
    val printerId: Long? = null,
    @SerialName("job_id")
    val jobId: String? = null,
    val result: String? = null,
    @SerialName("claim_token")
    val claimToken: String? = null,
    @SerialName("client_event_id")
    val clientEventId: String? = null,
    @SerialName("bytes_attempted")
    val bytesAttempted: Int? = null,
    @SerialName("bytes_written")
    val bytesWritten: Int? = null,
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("print_ok")
    val printOk: Boolean,
    @SerialName("print_error")
    val printError: String = "",
)

@Serializable
data class RemoveBoxItemRequestDto(
    @SerialName("item_id")
    val itemId: Long? = null,
    @SerialName("code_id")
    val codeId: Long? = null,
)

@Serializable
data class ScanToBoxRequestDto(
    val code: String,
    @SerialName("scanner_id")
    val scannerId: String = "",
)

@Serializable
data class ScanBatchToBoxRequestDto(
    val codes: List<String>,
    @SerialName("scanner_id")
    val scannerId: String = "",
)

@Serializable
data class ClientPrinterDto(
    val id: Long,
    val name: String,
    @SerialName("ip_address")
    val ipAddress: String,
    val port: Int = 9100,
    val section: String = "",
    val driver: String = "zpl",
    @SerialName("is_active")
    val isActive: Boolean = true,
)

@Serializable
data class ClientPrinterSelectionResponseDto(
    val ok: Boolean = true,
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("selected_printer_id")
    val selectedPrinterId: Long? = null,
    @SerialName("selected_printer")
    val selectedPrinter: ClientPrinterDto? = null,
    val printers: List<ClientPrinterDto> = emptyList(),
)

@Serializable
data class PrintJobDto(
    val id: String? = null,
    @SerialName("claim_token")
    val claimToken: String? = null,
    val status: String = "",
    @SerialName("package_revision")
    val packageRevision: Int? = null,
    val format: String = "",
    val driver: String = "",
    val encoding: String = "utf-8",
    val transport: String = "",
    val payload: String = "",
    val printer: ClientPrinterDto? = null,
)

@Serializable
data class BoxDto(
    @SerialName("box_id")
    val boxId: Long,
    @SerialName("package_uuid")
    val packageUuid: String? = null,
    @SerialName("order_id")
    val orderId: Long? = null,
    @SerialName("order_uuid")
    val orderUuid: String? = null,
    val name: String? = null,
    @SerialName("order_name")
    val orderName: String? = null,
    val sscc: String? = null,
    val capacity: Int,
    val filled: Int,
    @SerialName("count_in_packing")
    val countInPacking: Boolean = true,
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
    @SerialName("added_at")
    val addedAt: String? = null,
    @SerialName("added_by_id")
    val addedById: Long? = null,
)

@Serializable
data class BoxDetailDto(
    @SerialName("box_id")
    val boxId: Long,
    @SerialName("package_uuid")
    val packageUuid: String? = null,
    @SerialName("order_id")
    val orderId: Long? = null,
    val name: String? = null,
    @SerialName("order_name")
    val orderName: String? = null,
    val sscc: String? = null,
    val capacity: Int,
    val filled: Int,
    @SerialName("count_in_packing")
    val countInPacking: Boolean = true,
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
    @SerialName("package_uuid")
    val packageUuid: String? = null,
    @SerialName("order_id")
    val orderId: Long? = null,
    @SerialName("order_uuid")
    val orderUuid: String? = null,
    val name: String? = null,
    @SerialName("order_name")
    val orderName: String? = null,
    val sscc: String? = null,
    val capacity: Int,
    val filled: Int,
    @SerialName("count_in_packing")
    val countInPacking: Boolean = true,
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
    val items: List<BoxItemDto> = emptyList(),
)

@Serializable
data class BoxActionResponseDto(
    val ok: Boolean,
    @SerialName("reason_code")
    val reasonCode: String,
    val error: String? = null,
    val box: CurrentBoxResponseDto,
    val removed: Int? = null,
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
    val details: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class CloseBoxResponseDto(
    val ok: Boolean,
    @SerialName("reason_code")
    val reasonCode: String,
    val error: String? = null,
    val box: BoxDto,
)

@Serializable
data class PackageLabelPrintResultDto(
    val ok: Boolean? = null,
    @SerialName("reason_code")
    val reasonCode: String = "",
    @SerialName("print_status")
    val printStatus: String = "",
    @SerialName("print_ok")
    val printOk: Boolean = false,
    @SerialName("print_error_code")
    val printErrorCode: String = "",
    @SerialName("print_error")
    val printError: String = "",
    val printer: ClientPrinterDto? = null,
    @SerialName("print_job")
    val printJob: PrintJobDto? = null,
    val box: BoxDto? = null,
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

fun BoxActionResponseDto.toDomain(): PackingBoxActionResult = PackingBoxActionResult(
    ok = ok,
    reasonCode = reasonCode,
    error = error,
    box = box.toDomain(),
    removed = removed,
)

fun ScanToBoxResponseDto.toDomain(): PackingScanResult = PackingScanResult(
    ok = ok,
    reasonCode = reasonCode,
    error = error,
    duplicate = duplicate,
    verify = verify?.toDomain(),
    box = box.toDomain(),
    boxFullSignal = boxFullSignal,
    conflictPackageCode = details["package_code"]?.jsonPrimitive?.contentOrNull,
)

fun CloseBoxResponseDto.toDomain(): ClosePackingBoxResult = ClosePackingBoxResult(
    ok = ok,
    reasonCode = reasonCode,
    error = error,
    box = box.toDomain(),
)

fun ClientPrinterSelectionResponseDto.toDomain(): ClientPrinterSelection = ClientPrinterSelection(
    ok = ok,
    deviceId = deviceId,
    selectedPrinterId = selectedPrinterId,
    selectedPrinter = selectedPrinter?.toDomain(),
    printers = printers.map { it.toDomain() },
)

fun PackageLabelPrintResultDto.toDomain(): PackageLabelPrintResult = PackageLabelPrintResult(
    ok = ok,
    reasonCode = reasonCode,
    printStatus = printStatus,
    printOk = printOk,
    printErrorCode = printErrorCode,
    printError = printError,
    printer = printer?.toDomain(),
    printJob = printJob?.toDomain(),
    box = box?.toDomain(),
)

private fun ClientPrinterDto.toDomain(): ClientPrinter = ClientPrinter(
    id = id,
    name = name,
    ipAddress = ipAddress,
    port = port,
    section = section,
    driver = driver,
    isActive = isActive,
)

private fun PrintJobDto.toDomain(): PrintJob = PrintJob(
    id = id,
    claimToken = claimToken,
    status = status,
    packageRevision = packageRevision,
    format = format,
    driver = driver,
    encoding = encoding,
    transport = transport,
    payload = payload,
    printer = printer?.toDomain(),
)

private fun CurrentBoxResponseDto.toBox(): BoxDto = BoxDto(
    boxId = boxId,
    packageUuid = packageUuid,
    orderId = orderId,
    orderUuid = orderUuid,
    name = name,
    orderName = orderName,
    sscc = sscc,
    capacity = capacity,
    filled = filled,
    countInPacking = countInPacking,
    allowDuplicateScans = allowDuplicateScans,
    isClosed = isClosed,
    isEditMode = isEditMode,
    activeUserName = activeUserName,
    createdByName = createdByName,
)

private fun BoxDto.toDomain(): PackingBox = PackingBox(
    boxId = boxId,
    packageUuid = packageUuid,
    orderId = orderId,
    orderUuid = orderUuid,
    orderName = name?.takeIf(String::isNotBlank) ?: orderName,
    sscc = sscc,
    capacity = capacity,
    filled = filled,
    countInPacking = countInPacking,
    allowDuplicateScans = allowDuplicateScans,
    isClosed = isClosed,
    isEditMode = isEditMode,
    activeUserName = activeUserName,
    createdByName = createdByName,
)

private fun BoxItemDto.toDomain(): PackingBoxItem = PackingBoxItem(
    id = id,
    codeId = codeId,
    scanId = scanId,
    gtin = gtin,
    serial = serial,
    visibleCode = visibleCode,
)
