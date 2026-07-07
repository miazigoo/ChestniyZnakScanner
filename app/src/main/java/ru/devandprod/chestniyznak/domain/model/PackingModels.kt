package ru.devandprod.chestniyznak.domain.model

data class PackingBox(
    val boxId: Long,
    val orderId: Long? = null,
    val orderUuid: String? = null,
    val orderName: String? = null,
    val sscc: String? = null,
    val capacity: Int,
    val filled: Int,
    val countInPacking: Boolean = true,
    val allowDuplicateScans: Boolean,
    val isClosed: Boolean,
    val isEditMode: Boolean,
    val activeUserName: String = "",
    val createdByName: String = "",
)

data class PackingBoxItem(
    val id: Long,
    val codeId: Long,
    val scanId: Long? = null,
    val gtin: String,
    val serial: String,
    val visibleCode: String,
)

data class PackingBoxDetail(
    val box: PackingBox,
    val items: List<PackingBoxItem> = emptyList(),
)

data class PackingBoxPage(
    val items: List<PackingBox>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean,
)

data class PackingBoxActionResult(
    val ok: Boolean,
    val reasonCode: String,
    val error: String? = null,
    val box: PackingBoxDetail,
    val removed: Int? = null,
)

data class OpenPackingBoxResult(
    val ok: Boolean,
    val created: Boolean,
    val hasActiveBoxes: Boolean,
    val boxes: List<PackingBox>,
    val box: PackingBox,
)

data class PackingScanResult(
    val ok: Boolean,
    val reasonCode: String,
    val error: String? = null,
    val duplicate: Boolean? = null,
    val verify: VerificationResult? = null,
    val box: PackingBox,
    val boxFullSignal: Boolean? = null,
    val conflictPackageCode: String? = null,
)

data class ClosePackingBoxResult(
    val ok: Boolean,
    val reasonCode: String,
    val error: String? = null,
    val box: PackingBox,
)
