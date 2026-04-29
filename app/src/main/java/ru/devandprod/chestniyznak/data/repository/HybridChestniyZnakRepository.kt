package ru.devandprod.chestniyznak.data.repository

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.data.remote.api.ChestniyZnakApi
import ru.devandprod.chestniyznak.data.remote.auth.RemoteAuthRepository
import ru.devandprod.chestniyznak.data.remote.auth.RemoteErrorParser
import ru.devandprod.chestniyznak.data.remote.dto.StatsResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.toDomain
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

@Singleton
class HybridChestniyZnakRepository @Inject constructor(
    private val remoteApi: ChestniyZnakApi,
    private val localRepository: LocalChestniyZnakRepository,
    private val authRepository: RemoteAuthRepository,
    private val errorParser: RemoteErrorParser,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChestniyZnakRepository {

    private val statsFlow = MutableStateFlow(CatalogStats())

    override suspend fun ensureSeedData() = withContext(ioDispatcher) {
        localRepository.ensureSeedData()
        statsFlow.value = localRepository.snapshotStats()
        refreshStats()
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
                message = exception.message ?: "Не удалось проверить код на сервере",
            )
        }

        when {
            response.isSuccessful && response.body() != null -> {
                refreshStats()
                response.body()!!.toDomain()
            }
            response.code() == 401 || response.code() == 403 -> {
                authRepository.invalidateSession()
                VerificationResult(
                    status = VerificationStatus.INTERNAL_ERROR,
                    message = "Сессия истекла. Войдите снова.",
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
