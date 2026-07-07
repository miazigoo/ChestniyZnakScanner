package ru.devandprod.chestniyznak.domain.model

enum class VerificationStatus {
    OK,
    OK_GS_RESTORED,
    DUPLICATE_SCAN,
    WRONG_ORDER,
    BAD_FORMAT,
    NOT_FOUND,
    TAIL_MISMATCH,
    INTERNAL_ERROR,
}

data class VerificationBoxInfo(
    val boxId: Long,
    val sscc: String? = null,
    val isClosed: Boolean,
)

data class VerificationResult(
    val status: VerificationStatus,
    val message: String,
    val scanId: Long? = null,
    val parsed: ParsedMarkingCode? = null,
    val code: MarkingCode? = null,
    val boxInfo: VerificationBoxInfo? = null,
    val orderName: String? = null,
    val deviceName: String? = null,
    val warnings: List<String> = emptyList(),
) {
    val isSuccess: Boolean
        get() = status == VerificationStatus.OK || status == VerificationStatus.OK_GS_RESTORED
}
