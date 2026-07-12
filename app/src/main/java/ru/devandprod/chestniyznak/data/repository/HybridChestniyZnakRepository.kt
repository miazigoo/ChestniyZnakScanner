package ru.devandprod.chestniyznak.data.repository

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.data.remote.api.ChestniyZnakApi
import ru.devandprod.chestniyznak.data.remote.auth.RemoteAuthRepository
import ru.devandprod.chestniyznak.data.remote.auth.RemoteErrorParser
import ru.devandprod.chestniyznak.data.remote.dto.StatsResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.DefectRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyExistsRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.toDomain
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.DefectMarkResult
import ru.devandprod.chestniyznak.domain.model.LocalPackingPendingCode
import ru.devandprod.chestniyznak.domain.model.OrderLocalCode
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

@Singleton
class HybridChestniyZnakRepository @Inject constructor(
    private val remoteApi: ChestniyZnakApi,
    private val localRepository: LocalChestniyZnakRepository,
    private val authRepository: RemoteAuthRepository,
    private val errorParser: RemoteErrorParser,
    private val strings: AppStringProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChestniyZnakRepository {

    private val statsFlow = MutableStateFlow(CatalogStats())

    override suspend fun ensureSeedData() = withContext(ioDispatcher) {
        localRepository.ensureSeedData()
        statsFlow.value = localRepository.snapshotStats()
        refreshStats()
    }

    override suspend fun replaceLocalPool(
        orderNumber: String,
        orderId: String,
        codes: List<OrderLocalCode>,
        preserveLocalPending: Boolean,
    ) = withContext(ioDispatcher) {
        localRepository.replaceLocalPool(orderNumber, orderId, codes, preserveLocalPending)
        statsFlow.value = localRepository.snapshotStats()
    }

    override suspend fun retainLocalOrders(orderIds: List<String>) = withContext(ioDispatcher) {
        localRepository.retainLocalOrders(orderIds)
        statsFlow.value = localRepository.snapshotStats()
    }

    override suspend fun verify(
        rawInput: String,
        scannerId: String,
        allowDuplicate: Boolean,
    ): VerificationResult = withContext(ioDispatcher) {
        val response = try {
            remoteApi.verify(
                VerifyRequestDto(
                    code = rawInput,
                    scannerId = scannerId,
                    allowDuplicate = allowDuplicate,
                    saveScan = false,
                ),
            )
        } catch (_: IOException) {
            val local = localRepository.verifyExists(rawInput, scannerId, allowDuplicate)
            statsFlow.value = localRepository.snapshotStats()
            return@withContext local
        } catch (exception: Exception) {
            return@withContext VerificationResult(
                status = VerificationStatus.INTERNAL_ERROR,
                message = exception.message ?: strings.get(R.string.verify_remote_failed),
            )
        }

        when {
            response.isSuccessful && response.body() != null -> {
                refreshStats()
                response.body()!!.toDomain()
            }
            response.code() == 401 -> {
                authRepository.invalidateSession()
                VerificationResult(
                    status = VerificationStatus.INTERNAL_ERROR,
                    message = strings.get(R.string.common_session_expired),
                )
            }
            else -> {
                val local = localRepository.verifyExists(rawInput, scannerId, allowDuplicate)
                statsFlow.value = localRepository.snapshotStats()
                local.copy(
                    warnings = local.warnings + errorParser.message(response),
                )
            }
        }
    }

    override suspend fun verifyExists(
        rawInput: String,
        scannerId: String,
        allowDuplicate: Boolean,
    ): VerificationResult = withContext(ioDispatcher) {
        val response = try {
            remoteApi.verifyExists(
                VerifyExistsRequestDto(
                    code = rawInput,
                    scannerId = scannerId,
                    allowDuplicate = allowDuplicate,
                    saveScan = true,
                ),
            )
        } catch (_: IOException) {
            val local = localRepository.verify(rawInput, scannerId, allowDuplicate)
            statsFlow.value = localRepository.snapshotStats()
            return@withContext local
        } catch (exception: Exception) {
            return@withContext VerificationResult(
                status = VerificationStatus.INTERNAL_ERROR,
                message = exception.message ?: strings.get(R.string.verify_exists_remote_failed),
            )
        }

        when {
            response.isSuccessful && response.body() != null -> {
                refreshStats()
                response.body()!!.toDomain()
            }
            response.code() == 401 -> {
                authRepository.invalidateSession()
                VerificationResult(
                    status = VerificationStatus.INTERNAL_ERROR,
                    message = strings.get(R.string.common_session_expired),
                )
            }
            else -> {
                val local = localRepository.verify(rawInput, scannerId, allowDuplicate)
                statsFlow.value = localRepository.snapshotStats()
                local.copy(
                    warnings = local.warnings + errorParser.message(response),
                )
            }
        }
    }

    override suspend fun verifyLocalOnly(
        rawInput: String,
        scannerId: String,
        allowDuplicate: Boolean,
        orderId: String?,
    ): VerificationResult = withContext(ioDispatcher) {
        localRepository.verifyLocalOnly(rawInput, scannerId, allowDuplicate, orderId).also {
            statsFlow.value = localRepository.snapshotStats()
        }
    }

    override suspend fun getLocalPackingPending(
        packageCode: String,
        orderId: String?,
    ): List<LocalPackingPendingCode> =
        withContext(ioDispatcher) {
            localRepository.getLocalPackingPending(packageCode, orderId)
        }

    override suspend fun markLocalPackingPending(
        rawInput: String,
        packageCode: String?,
        orderId: String?,
        packageUuid: String?,
    ) = withContext(ioDispatcher) {
        localRepository.markLocalPackingPending(rawInput, packageCode, orderId, packageUuid)
        statsFlow.value = localRepository.snapshotStats()
    }

    override suspend fun clearLocalPackingPending(
        rawCodes: List<String>,
        orderId: String?,
        packageUuid: String?,
    ) = withContext(ioDispatcher) {
        localRepository.clearLocalPackingPending(rawCodes, orderId, packageUuid)
        statsFlow.value = localRepository.snapshotStats()
    }

    override suspend fun markLocalPackingCommitted(
        rawCodes: List<String>,
        packageCode: String,
        packageClosedAt: String?,
        orderId: String?,
    ) = withContext(ioDispatcher) {
        localRepository.markLocalPackingCommitted(rawCodes, packageCode, packageClosedAt, orderId)
        statsFlow.value = localRepository.snapshotStats()
    }

    override suspend fun markDefect(
        rawInput: String,
        scannerId: String,
    ): DefectMarkResult = withContext(ioDispatcher) {
        val response = try {
            remoteApi.markDefect(
                DefectRequestDto(
                    code = rawInput,
                    scannerId = scannerId,
                ),
            )
        } catch (_: IOException) {
            return@withContext localRepository.markDefect(rawInput, scannerId)
        } catch (exception: Exception) {
            return@withContext DefectMarkResult(
                ok = false,
                reasonCode = "internal_error",
                error = exception.message ?: strings.get(R.string.defect_remote_failed),
            )
        }

        when {
            response.isSuccessful && response.body() != null -> {
                refreshStats()
                response.body()!!.toDomain()
            }
            response.code() == 401 -> {
                authRepository.invalidateSession()
                DefectMarkResult(
                    ok = false,
                    reasonCode = "unauthorized",
                    error = strings.get(R.string.common_session_expired),
                )
            }
            else -> DefectMarkResult(
                ok = false,
                reasonCode = "api_error",
                error = errorParser.message(response),
            )
        }
    }

    override suspend fun refreshStats() = withContext(ioDispatcher) {
        val response = runCatching { remoteApi.stats() }.getOrNull()
        if (response?.isSuccessful == true) {
            val body: StatsResponseDto? = response.body()
            if (body != null) {
                statsFlow.value = body.toDomain()
                return@withContext
            }
        }
        statsFlow.value = localRepository.snapshotStats()
    }

    override fun observeStats(): Flow<CatalogStats> = statsFlow.asStateFlow()
}
