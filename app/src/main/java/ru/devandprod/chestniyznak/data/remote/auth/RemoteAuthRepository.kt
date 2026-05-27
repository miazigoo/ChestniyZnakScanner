package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import ru.devandprod.chestniyznak.BuildConfig
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.core.device.DeviceIdentity
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.data.remote.api.AccountApi
import ru.devandprod.chestniyznak.data.remote.dto.AccountDto
import ru.devandprod.chestniyznak.data.remote.dto.AuthCheckDto
import ru.devandprod.chestniyznak.data.remote.dto.SaasTokenLoginRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.TokenLoginRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdBootstrapDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdMeDto
import ru.devandprod.chestniyznak.domain.auth.AuthException
import ru.devandprod.chestniyznak.domain.auth.AuthSession
import ru.devandprod.chestniyznak.domain.repository.AuthRepository

@Singleton
class RemoteAuthRepository @Inject constructor(
    private val accountApi: AccountApi,
    private val cookieStore: SessionCookieStore,
    private val bearerTokenStore: BearerTokenStore,
    private val errorParser: RemoteErrorParser,
    private val strings: AppStringProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    private val sessionState = MutableStateFlow(AuthSession())

    override fun observeSession(): Flow<AuthSession> = sessionState.asStateFlow()

    override suspend fun restoreSession() = withContext(ioDispatcher) {
        if (isSaasApi()) {
            restoreSaasSession()
            return@withContext
        }

        if (!cookieStore.hasSessionCookie()) {
            sessionState.value = AuthSession(isLoading = false)
            return@withContext
        }

        sessionState.update { it.copy(isLoading = true) }
        val response = runCatching { accountApi.authCheck() }.getOrElse {
            cookieStore.clear()
            sessionState.value = AuthSession(isLoading = false)
            return@withContext
        }

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.authenticated) {
                sessionState.value = body.toDomain()
            } else {
                cookieStore.clear()
                sessionState.value = AuthSession(isLoading = false)
            }
        } else {
            cookieStore.clear()
            sessionState.value = AuthSession(isLoading = false)
        }
    }

    override suspend fun login(token: String) = withContext(ioDispatcher) {
        if (isSaasApi()) {
            loginSaas(token)
            return@withContext
        }

        sessionState.update { it.copy(isLoading = true) }
        val response = runCatching {
            accountApi.login(
                request = TokenLoginRequestDto(
                    token = token,
                ),
            )
        }.getOrElse { exception ->
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(exception.message ?: strings.get(R.string.auth_connect_failed))
        }

        val body = response.body()
        if (!response.isSuccessful || body == null) {
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(errorParser.message(response))
        }

        sessionState.value = body.toDomain()
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        if (!isSaasApi()) {
            runCatching { accountApi.logout() }
        }
        cookieStore.clear()
        bearerTokenStore.clear()
        sessionState.value = AuthSession(isLoading = false)
    }

    fun invalidateSession() {
        cookieStore.clear()
        bearerTokenStore.clear()
        sessionState.value = AuthSession(isLoading = false)
    }

    private suspend fun restoreSaasSession() {
        if (bearerTokenStore.load() == null) {
            sessionState.value = AuthSession(isLoading = false)
            return
        }

        sessionState.update { it.copy(isLoading = true) }
        val response = runCatching {
            accountApi.tsdBootstrap(DeviceIdentity.clientDeviceId)
        }.getOrElse {
            bearerTokenStore.clear()
            sessionState.value = AuthSession(isLoading = false)
            return
        }

        if (response.isSuccessful) {
            val body = response.body()?.data
            if (body != null) {
                sessionState.value = body.toDomain()
            } else {
                bearerTokenStore.clear()
                sessionState.value = AuthSession(isLoading = false)
            }
            return
        }

        if (response.code() != HTTP_PAYMENT_REQUIRED) {
            bearerTokenStore.clear()
        }
        sessionState.value = AuthSession(isLoading = false)
    }

    private suspend fun loginSaas(token: String) {
        sessionState.update { it.copy(isLoading = true) }
        val loginResponse = runCatching {
            accountApi.saasLogin(
                request = SaasTokenLoginRequestDto(
                    token = token,
                    deviceUid = DeviceIdentity.clientDeviceId,
                ),
            )
        }.getOrElse { exception ->
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(exception.message ?: strings.get(R.string.auth_connect_failed))
        }

        val loginBody = loginResponse.body()?.data
        if (!loginResponse.isSuccessful || loginBody == null) {
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(errorParser.message(loginResponse))
        }

        bearerTokenStore.save(
            accessToken = loginBody.accessToken,
            refreshToken = loginBody.refreshToken,
        )

        val bootstrapResponse = runCatching {
            accountApi.tsdBootstrap(DeviceIdentity.clientDeviceId)
        }.getOrElse { exception ->
            bearerTokenStore.clear()
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(exception.message ?: strings.get(R.string.auth_connect_failed))
        }
        val bootstrapBody = bootstrapResponse.body()?.data
        if (!bootstrapResponse.isSuccessful || bootstrapBody == null) {
            if (bootstrapResponse.code() != HTTP_PAYMENT_REQUIRED) {
                bearerTokenStore.clear()
            }
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(errorParser.message(bootstrapResponse))
        }

        sessionState.value = bootstrapBody.toDomain()
    }

    private fun AccountDto.toDomain(): AuthSession {
        val fullName = listOf(firstName, lastName)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .ifBlank { username }
        return AuthSession(
            isLoading = false,
            isAuthenticated = true,
            userId = id.toString(),
            username = username,
            displayName = fullName,
        )
    }

    private fun AuthCheckDto.toDomain(): AuthSession = AuthSession(
        isLoading = false,
        isAuthenticated = authenticated,
        userId = userId.toString(),
        username = user,
        displayName = user,
    )

    private fun TsdMeDto.toDomain(): AuthSession {
        val remoteUser = this.user
        val remoteContext = context
        val name = remoteUser?.displayName
            ?: remoteUser?.login
            ?: remoteUser?.email
            ?: strings.get(R.string.auth_operator_fallback)
        return AuthSession(
            isLoading = false,
            isAuthenticated = true,
            userId = remoteUser?.id,
            username = remoteUser?.login ?: name,
            displayName = name,
            plantId = remoteContext?.plantId.orEmpty(),
            deviceId = remoteContext?.deviceId.orEmpty(),
        )
    }

    private fun TsdBootstrapDto.toDomain(): AuthSession {
        val remoteUser = this.user
        val remoteContext = context
        val name = remoteUser?.displayName
            ?: remoteUser?.login
            ?: remoteUser?.email
            ?: strings.get(R.string.auth_operator_fallback)
        return AuthSession(
            isLoading = false,
            isAuthenticated = authenticated,
            userId = remoteUser?.id,
            username = remoteUser?.login ?: name,
            displayName = name,
            plantId = remoteContext?.plantId.orEmpty(),
            deviceId = remoteContext?.deviceId.orEmpty(),
            supplierId = remoteContext?.supplierId.orEmpty(),
            supplierName = supplier?.name.orEmpty(),
            plantName = plant?.name.orEmpty(),
            clientDeviceId = remoteContext?.clientDeviceId.orEmpty(),
            subscriptionStatus = subscription?.status.orEmpty(),
        )
    }

    private fun isSaasApi(): Boolean = BuildConfig.API_BASE_URL.contains("/api/v1")

    private companion object {
        const val HTTP_PAYMENT_REQUIRED = 402
    }
}
