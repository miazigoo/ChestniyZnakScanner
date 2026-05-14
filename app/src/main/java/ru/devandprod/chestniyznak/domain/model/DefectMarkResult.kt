package ru.devandprod.chestniyznak.domain.model

data class DefectRemovedBox(
    val boxId: Long,
    val sscc: String? = null,
    val filled: Int = 0,
)

data class DefectMarkResult(
    val ok: Boolean,
    val reasonCode: String,
    val error: String? = null,
    val verify: VerificationResult? = null,
    val removedFromBox: DefectRemovedBox? = null,
)
