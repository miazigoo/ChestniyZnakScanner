package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.MarkingCode
import ru.devandprod.chestniyznak.domain.model.ParsedMarkingCode
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus

@Serializable
data class VerifyRequestDto(
    val code: String,
    @SerialName("scanner_id")
    val scannerId: String = "",
    @SerialName("allow_duplicate")
    val allowDuplicate: Boolean = false,
    @SerialName("save_scan")
    val saveScan: Boolean = true,
)

@Serializable
data class VerifyExistsRequestDto(
    val code: String,
    @SerialName("scanner_id")
    val scannerId: String = "",
    @SerialName("allow_duplicate")
    val allowDuplicate: Boolean = true,
    @SerialName("save_scan")
    val saveScan: Boolean = true,
)

@Serializable
data class VerifyResponseDto(
    val status: String,
    val message: String,
    @SerialName("scan_id")
    val scanId: Long? = null,
    val parsed: ParsedCodeDto? = null,
    val code: RemoteCodeDto? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class VerifyExistsResponseDto(
    val ok: Boolean,
    val exists: Boolean,
    val status: String,
    val message: String,
    @SerialName("order_name")
    val orderName: String? = null,
    @SerialName("device_name")
    val deviceName: String? = null,
    @SerialName("scan_id")
    val scanId: Long? = null,
    val code: RemoteCodeDto? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class ParsedCodeDto(
    val gtin: String,
    val serial: String,
    @SerialName("ai_parts")
    val aiParts: Map<String, String> = emptyMap(),
    @SerialName("visible_code")
    val visibleCode: String,
    @SerialName("scanner_gs_native")
    val scannerGsNative: Boolean,
    @SerialName("gs_restored")
    val gsRestored: Boolean,
)

@Serializable
data class RemoteCodeDto(
    val id: Long,
    val gtin: String,
    val serial: String,
    @SerialName("ai_parts")
    val aiParts: Map<String, String> = emptyMap(),
    @SerialName("visible_code")
    val visibleCode: String,
    @SerialName("status_1c")
    val status1c: String = "",
    @SerialName("app_status")
    val appStatus: String = "",
    @SerialName("order_dnp_name")
    val orderNumber: String = "",
    @SerialName("order_name")
    val orderName: String = "",
    @SerialName("device_name")
    val deviceName: String = "",
)

@Serializable
data class StatsResponseDto(
    @SerialName("codes_count")
    val codesCount: Int,
    @SerialName("scans_count")
    val scansCount: Int,
)

fun VerifyResponseDto.toDomain(): VerificationResult = VerificationResult(
    status = runCatching { VerificationStatus.valueOf(status) }
        .getOrDefault(VerificationStatus.INTERNAL_ERROR),
    message = message,
    scanId = scanId,
    parsed = parsed?.toDomain(),
    code = code?.toDomain(),
    orderName = code?.orderName?.takeIf(String::isNotBlank),
    deviceName = code?.deviceName?.takeIf(String::isNotBlank),
    warnings = warnings,
)

fun VerifyExistsResponseDto.toDomain(): VerificationResult = VerificationResult(
    status = runCatching { VerificationStatus.valueOf(status) }
        .getOrDefault(if (exists) VerificationStatus.OK else VerificationStatus.NOT_FOUND),
    message = message,
    scanId = scanId,
    parsed = null,
    code = code?.toDomain(),
    orderName = orderName?.takeIf(String::isNotBlank) ?: code?.orderName?.takeIf(String::isNotBlank),
    deviceName = deviceName?.takeIf(String::isNotBlank) ?: code?.deviceName?.takeIf(String::isNotBlank),
    warnings = warnings,
)

fun StatsResponseDto.toDomain(): CatalogStats = CatalogStats(
    totalCodes = codesCount,
    totalScans = scansCount,
)

private fun ParsedCodeDto.toDomain(): ParsedMarkingCode = ParsedMarkingCode(
    gtin = gtin,
    serial = serial,
    aiParts = aiParts,
    rawCode = visibleCode.replace("<GS>", "\u001D"),
    visibleCode = visibleCode,
    scannerGsNative = scannerGsNative,
    gsRestored = gsRestored,
    warnings = emptyList(),
)

private fun RemoteCodeDto.toDomain(): MarkingCode = MarkingCode(
    id = id,
    gtin = gtin,
    serial = serial,
    aiParts = aiParts,
    visibleCode = visibleCode,
    status1c = status1c,
    appStatus = appStatus,
    orderNumber = orderNumber,
    orderName = orderName,
    deviceName = deviceName,
)
