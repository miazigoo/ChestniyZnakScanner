package ru.devandprod.chestniyznak.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.data.remote.api.OrdersApi
import ru.devandprod.chestniyznak.data.remote.auth.RemoteAuthRepository
import ru.devandprod.chestniyznak.data.remote.auth.RemoteErrorParser
import ru.devandprod.chestniyznak.data.remote.dto.toDomain
import ru.devandprod.chestniyznak.domain.model.OrderLocalPoolPage
import ru.devandprod.chestniyznak.domain.model.WorkOrderPage
import ru.devandprod.chestniyznak.domain.repository.OrdersRepository

@Singleton
class RemoteOrdersRepository @Inject constructor(
    private val ordersApi: OrdersApi,
    private val authRepository: RemoteAuthRepository,
    private val errorParser: RemoteErrorParser,
    private val strings: AppStringProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OrdersRepository {

    override suspend fun listWorkOrders(
        status: String?,
        search: String?,
        page: Int,
        perPage: Int,
    ): WorkOrderPage = withContext(ioDispatcher) {
        val response = runCatching {
            ordersApi.listOrders(
                status = status,
                search = search,
                page = page,
                perPage = perPage,
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.orders_load_failed))
        }

        when {
            response.isSuccessful && response.body() != null -> response.body()!!.toDomain()
            response.code() == 401 -> {
                authRepository.invalidateSession()
                throw RuntimeException(strings.get(R.string.common_session_expired))
            }
            else -> throw RuntimeException(errorParser.message(response))
        }
    }

    override suspend fun downloadLocalCodePool(
        orderId: String,
        limit: Int,
        offset: Int,
    ): OrderLocalPoolPage = withContext(ioDispatcher) {
        val response = runCatching {
            ordersApi.localCodePool(
                orderId = orderId,
                limit = limit,
                offset = offset,
            )
        }.getOrElse {
            throw RuntimeException(it.message ?: strings.get(R.string.local_pool_download_failed))
        }

        when {
            response.isSuccessful && response.body() != null -> response.body()!!.toDomain()
            response.code() == 401 -> {
                authRepository.invalidateSession()
                throw RuntimeException(strings.get(R.string.common_session_expired))
            }
            else -> throw RuntimeException(errorParser.message(response))
        }
    }
}
