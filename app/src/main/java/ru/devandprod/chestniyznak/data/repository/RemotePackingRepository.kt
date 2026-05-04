package ru.devandprod.chestniyznak.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.data.remote.api.PackingApi
import ru.devandprod.chestniyznak.data.remote.auth.RemoteAuthRepository
import ru.devandprod.chestniyznak.data.remote.auth.RemoteErrorParser
import ru.devandprod.chestniyznak.data.remote.dto.CloseBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.CurrentBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.OpenBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.OpenBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.ScanToBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.ScanToBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.toDomain
import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.model.PackingBoxPage
import ru.devandprod.chestniyznak.domain.model.PackingScanResult
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.repository.PackingRepository

@Singleton
class RemotePackingRepository @Inject constructor(
    private val packingApi: PackingApi,
    private val authRepository: RemoteAuthRepository,
    private val errorParser: RemoteErrorParser,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PackingRepository {

    override suspend fun listBoxes(
        status: String,
        query: String,
        limit: Int,
        offset: Int,
    ): PackingBoxPage = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.listBoxes(
                query = query,
                status = status,
                limit = limit,
                offset = offset,
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: "Не удалось получить список коробок")
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun getCurrentBox(): PackingBoxDetail? = withContext(ioDispatcher) {
        val response = runCatching { packingApi.currentBox() }.getOrElse {
            throw RuntimeException(it.message ?: "Не удалось получить текущую коробку")
        }

        when {
            response.isSuccessful && response.body() != null -> response.body()!!.toDomain()
            response.code() == 404 -> null
            response.code() == 401 || response.code() == 403 -> {
                authRepository.invalidateSession()
                throw RuntimeException("Сессия истекла. Войдите снова.")
            }
            else -> throw RuntimeException(errorParser.message(response))
        }
    }

    override suspend fun openBox(deviceId: String): OpenPackingBoxResult = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.openBox(
                OpenBoxRequestDto(deviceId = deviceId),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: "Не удалось открыть коробку")
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun scanCodeToBox(
        boxId: Long,
        rawCode: String,
        scannerId: String,
    ): PackingScanResult = withContext(ioDispatcher) {
        val response = runCatching {
            packingApi.scanToBox(
                boxId = boxId,
                request = ScanToBoxRequestDto(
                    code = rawCode,
                    scannerId = scannerId,
                ),
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: "Не удалось добавить код в коробку")
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    override suspend fun closeBox(boxId: Long): ClosePackingBoxResult = withContext(ioDispatcher) {
        val response = runCatching { packingApi.closeBox(boxId) }.getOrElse {
            throw RuntimeException(it.message ?: "Не удалось закрыть коробку")
        }

        mapResponse(response)?.toDomain()
            ?: throw RuntimeException(errorParser.message(response))
    }

    private fun <T> mapResponse(response: retrofit2.Response<T>): T? {
        if (response.code() == 401 || response.code() == 403) {
            authRepository.invalidateSession()
            throw RuntimeException("Сессия истекла. Войдите снова.")
        }
        return if (response.isSuccessful) response.body() else null
    }
}
