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
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.data.local.dao.MarkingCodeDao
import ru.devandprod.chestniyznak.data.local.dao.ScanLogDao
import ru.devandprod.chestniyznak.data.local.entity.MarkingCodeEntity
import ru.devandprod.chestniyznak.data.local.entity.ScanLogEntity
import ru.devandprod.chestniyznak.data.local.seed.SeedAssetLoader
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.MarkingCode
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
            if (hasOkScan && !allowDuplicate) {
                status = VerificationStatus.DUPLICATE_SCAN
                message = "Код уже сканировали ранее"
            } else if (parsed.gsRestored) {
                status = VerificationStatus.OK_GS_RESTORED
                message = "Код найден в базе; GS восстановлен из ввода сканера"
            } else {
                status = VerificationStatus.OK
                message = "Код найден в базе"
            }
        } else if (markingCodeDao.existsByIdentity(parsed.gtin, parsed.serial)) {
            status = VerificationStatus.TAIL_MISMATCH
            message = "GTIN и serial найдены, но полный криптохвост отличается"
        } else {
            status = VerificationStatus.NOT_FOUND
            message = "Код не найден в базе"
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
    )

    private fun rawHash(rawCode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawCode.toByteArray(Charsets.ISO_8859_1)).joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    private companion object {
        const val EMPTY_JSON_OBJECT = "{}"
        const val EMPTY_JSON_ARRAY = "[]"
    }
}
