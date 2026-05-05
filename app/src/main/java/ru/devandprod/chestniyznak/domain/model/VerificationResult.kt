package ru.devandprod.chestniyznak.domain.model

enum class VerificationStatus {
    OK,
    OK_GS_RESTORED,
    DUPLICATE_SCAN,
    BAD_FORMAT,
    NOT_FOUND,
    TAIL_MISMATCH,
    INTERNAL_ERROR,
}

data class VerificationResult(
    val status: VerificationStatus,
    val message: String,
    val scanId: Long? = null,
    val parsed: ParsedMarkingCode? = null,
    val code: MarkingCode? = null,
    val orderName: String? = null,
    val warnings: List<String> = emptyList(),
) {
    val isSuccess: Boolean
        get() = status == VerificationStatus.OK || status == VerificationStatus.OK_GS_RESTORED
}
