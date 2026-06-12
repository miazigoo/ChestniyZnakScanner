package ru.devandprod.chestniyznak.data.repository

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.data.local.dao.MarkingCodeDao
import ru.devandprod.chestniyznak.data.local.dao.ScanLogDao
import ru.devandprod.chestniyznak.data.local.entity.MarkingCodeEntity
import ru.devandprod.chestniyznak.data.local.entity.ScanLogEntity
import ru.devandprod.chestniyznak.data.local.seed.SeedAssetLoader
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.DefectMarkResult
import ru.devandprod.chestniyznak.domain.model.MarkingCode
import ru.devandprod.chestniyznak.domain.model.OrderLocalCode
import ru.devandprod.chestniyznak.domain.model.ParsedMarkingCode
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.parser.ChestniyZnakParseException
import ru.devandprod.chestniyznak.domain.parser.ChestniyZnakParser
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

@Singleton
class LocalChestniyZnakRepository @Inject constructor(
    private val markingCodeDao: MarkingCodeDao,
    private val scanLogDao: ScanLogDao,
    private val seedAssetLoader: SeedAssetLoader,
    private val parser: ChestniyZnakParser,
    private val json: Json,
    private val strings: AppStringProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChestniyZnakRepository {

    override suspend fun ensureSeedData() = withContext(ioDispatcher) {
        if (markingCodeDao.count() > 0) return@withContext

        val entities = seedAssetLoader.loadCodes().map { seed ->
            val parsed = parser.parse(seed.rawCode)
            MarkingCodeEntity(
                gtin = parsed.gtin,
                serial = parsed.serial,
                identityKey = parsed.identityKey,
                aiPartsJson = json.encodeToString(parsed.aiParts),
                rawCode = parsed.rawCode,
                visibleCode = parsed.visibleCode,
                rawCodeSha256 = rawHash(parsed.rawCode),
                status1c = seed.status1c,
                appStatus = seed.appStatus,
                orderNumber = seed.orderNumber,
            )
        }

        markingCodeDao.insertAll(entities)
    }

    override suspend fun replaceLocalPool(
        orderNumber: String,
        orderId: String,
        codes: List<OrderLocalCode>,
    ) = withContext(ioDispatcher) {
        val entities = codes
            .distinctBy { it.code }
            .map { code ->
                val parsed = parser.parse(code.code)
                MarkingCodeEntity(
                    gtin = parsed.gtin,
                    serial = parsed.serial,
                    identityKey = parsed.identityKey,
                    aiPartsJson = json.encodeToString(parsed.aiParts),
                    rawCode = parsed.rawCode,
                    visibleCode = parsed.visibleCode,
                    rawCodeSha256 = rawHash(parsed.rawCode),
                    status1c = code.status,
                    appStatus = if (code.packageCode.isNullOrBlank()) {
                        "local_pool"
                    } else {
                        "packed_remote"
                    },
                    orderNumber = orderNumber,
                    orderId = orderId,
                    orderLineId = code.orderLineId,
                    remoteCodeId = code.id,
                    packageUnitId = code.packageUnitId,
                    packageCode = code.packageCode,
                    packageStatus = code.packageStatus,
                    packageClosedAt = code.packageClosedAt,
                    remoteUpdatedAt = code.updatedAt,
                )
            }
        markingCodeDao.deleteAll()
        markingCodeDao.insertAll(entities)
    }

    override suspend fun verify(
        rawInput: String,
        scannerId: String,
        allowDuplicate: Boolean,
    ): VerificationResult = withContext(ioDispatcher) {
        val parsed = try {
            parser.parse(rawInput)
        } catch (exception: ChestniyZnakParseException) {
            val scanId = scanLogDao.insert(
                ScanLogEntity(
                    status = VerificationStatus.BAD_FORMAT.name,
                    message = exception.message.orEmpty(),
                    rawInput = rawInput,
                    normalizedCode = "",
                    visibleCode = "",
                    gtin = "",
                    serial = "",
                    aiPartsJson = EMPTY_JSON_OBJECT,
                    warningsJson = EMPTY_JSON_ARRAY,
                    scannerId = scannerId,
                    scannerGsNative = false,
                    gsRestored = false,
                ),
            )
            return@withContext VerificationResult(
                status = VerificationStatus.BAD_FORMAT,
                message = exception.message.orEmpty(),
                scanId = scanId,
            )
        }

        val hash = rawHash(parsed.rawCode)
        val matchedCode = markingCodeDao.findByRawHash(hash)
        val status: VerificationStatus
        val message: String

        if (matchedCode != null) {
            val hasOkScan = scanLogDao.hasSuccessfulScan(matchedCode.id)
            if (matchedCode.isPendingLocalPacking()) {
                status = VerificationStatus.DUPLICATE_SCAN
                message = strings.get(R.string.packing_code_duplicate_current)
            } else if (matchedCode.isPackedRemotely()) {
                status = VerificationStatus.DUPLICATE_SCAN
                message = matchedCode.packageCode
                    ?.takeIf(String::isNotBlank)
                    ?.let { strings.get(R.string.packing_code_in_named_box, it) }
                    ?: strings.get(R.string.packing_code_in_other_box)
            } else if (hasOkScan && !allowDuplicate) {
                status = VerificationStatus.DUPLICATE_SCAN
                message = strings.get(R.string.local_duplicate_scan)
            } else if (parsed.gsRestored) {
                status = VerificationStatus.OK_GS_RESTORED
                message = strings.get(R.string.local_found_gs_restored)
            } else {
                status = VerificationStatus.OK
                message = strings.get(R.string.local_found)
            }
        } else if (markingCodeDao.existsByIdentity(parsed.gtin, parsed.serial)) {
            status = VerificationStatus.TAIL_MISMATCH
            message = strings.get(R.string.local_tail_mismatch)
        } else {
            status = VerificationStatus.NOT_FOUND
            message = strings.get(R.string.local_not_found)
        }

        val scanId = scanLogDao.insert(
            ScanLogEntity(
                codeId = matchedCode?.id,
                status = status.name,
                message = message,
                rawInput = rawInput,
                normalizedCode = parsed.rawCode,
                visibleCode = parsed.visibleCode,
                gtin = parsed.gtin,
                serial = parsed.serial,
                aiPartsJson = json.encodeToString(parsed.aiParts),
                warningsJson = json.encodeToString(parsed.warnings),
                scannerId = scannerId,
                scannerGsNative = parsed.scannerGsNative,
                gsRestored = parsed.gsRestored,
            ),
        )

        VerificationResult(
            status = status,
            message = message,
            scanId = scanId,
            parsed = parsed,
            code = matchedCode?.toDomain(json),
            warnings = parsed.warnings,
        )
    }

    override suspend fun verifyExists(
        rawInput: String,
        scannerId: String,
        allowDuplicate: Boolean,
    ): VerificationResult = verify(
        rawInput = rawInput,
        scannerId = scannerId,
        allowDuplicate = allowDuplicate,
    )

    override suspend fun verifyLocalOnly(
        rawInput: String,
        scannerId: String,
        allowDuplicate: Boolean,
    ): VerificationResult = verify(
        rawInput = rawInput,
        scannerId = scannerId,
        allowDuplicate = allowDuplicate,
    )

    override suspend fun markLocalPackingPending(
        rawInput: String,
        packageCode: String?,
    ) = withContext(ioDispatcher) {
        val parsed = parser.parse(rawInput)
        markingCodeDao.markPackingPending(
            rawHash = rawHash(parsed.rawCode),
            packageCode = packageCode,
        )
    }

    override suspend fun clearLocalPackingPending(rawCodes: List<String>) = withContext(ioDispatcher) {
        val hashes = rawCodes
            .mapNotNull { rawCode ->
                runCatching { rawHash(parser.parse(rawCode).rawCode) }.getOrNull()
            }
            .distinct()
        if (hashes.isNotEmpty()) {
            markingCodeDao.clearPackingPending(hashes)
        }
    }

    override suspend fun markDefect(
        rawInput: String,
        scannerId: String,
    ): DefectMarkResult = DefectMarkResult(
        ok = false,
        reasonCode = "unsupported_offline",
        error = strings.get(R.string.local_defect_online_only),
    )

    override fun observeStats(): Flow<CatalogStats> = combine(
        markingCodeDao.observeCount(),
        scanLogDao.observeCount(),
    ) { codesCount, scansCount ->
        CatalogStats(
            totalCodes = codesCount,
            totalScans = scansCount,
        )
    }

    override suspend fun refreshStats() = Unit

    suspend fun snapshotStats(): CatalogStats = withContext(ioDispatcher) {
        CatalogStats(
            totalCodes = markingCodeDao.count(),
            totalScans = scanLogDao.count(),
        )
    }

    private fun MarkingCodeEntity.toDomain(json: Json): MarkingCode = MarkingCode(
        id = id,
        gtin = gtin,
        serial = serial,
        aiParts = json.decodeFromString(aiPartsJson),
        visibleCode = visibleCode,
        status1c = status1c,
        appStatus = appStatus,
        orderNumber = orderNumber,
        orderName = orderNumber,
        deviceName = "",
        packageCode = packageCode,
        packageStatus = packageStatus,
    )

    private fun MarkingCodeEntity.isPackedRemotely(): Boolean =
        !isPendingLocalPacking() && (status1c in PACKED_REMOTE_STATUSES || !packageCode.isNullOrBlank())

    private fun MarkingCodeEntity.isPendingLocalPacking(): Boolean =
        appStatus == "pending_local"

    private fun rawHash(rawCode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawCode.toByteArray(Charsets.ISO_8859_1)).joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    private companion object {
        const val EMPTY_JSON_OBJECT = "{}"
        const val EMPTY_JSON_ARRAY = "[]"
        val PACKED_REMOTE_STATUSES = setOf("packed", "exported")
    }
}
