package ru.devandprod.chestniyznak.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import ru.devandprod.chestniyznak.core.common.IoDispatcher
import ru.devandprod.chestniyznak.data.remote.api.AccountApi
import ru.devandprod.chestniyznak.data.remote.dto.AccountDto
import ru.devandprod.chestniyznak.data.remote.dto.AuthCheckDto
import ru.devandprod.chestniyznak.data.remote.dto.TokenLoginRequestDto
import ru.devandprod.chestniyznak.domain.auth.AuthException
import ru.devandprod.chestniyznak.domain.auth.AuthSession
import ru.devandprod.chestniyznak.domain.repository.AuthRepository

@Singleton
class RemoteAuthRepository @Inject constructor(
    private val accountApi: AccountApi,
    private val cookieStore: SessionCookieStore,
    private val errorParser: RemoteErrorParser,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    private val sessionState = MutableStateFlow(AuthSession())

    override fun observeSession(): Flow<AuthSession> = sessionState.asStateFlow()

    override suspend fun restoreSession() = withContext(ioDispatcher) {
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
        sessionState.update { it.copy(isLoading = true) }
        val response = runCatching {
            accountApi.login(
                request = TokenLoginRequestDto(
                    token = token,
                ),
            )
        }.getOrElse { exception ->
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(exception.message ?: "Не удалось подключиться к серверу")
        }

        val body = response.body()
        if (!response.isSuccessful || body == null) {
            sessionState.update { AuthSession(isLoading = false) }
            throw AuthException(errorParser.message(response))
        }

        sessionState.value = body.toDomain()
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        runCatching { accountApi.logout() }
        cookieStore.clear()
        sessionState.value = AuthSession(isLoading = false)
    }

    fun invalidateSession() {
        cookieStore.clear()
        sessionState.value = AuthSession(isLoading = false)
    }

    private fun AccountDto.toDomain(): AuthSession {
        val fullName = listOf(firstName, lastName)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .ifBlank { username }
        return AuthSession(
            isLoading = false,
            isAuthenticated = true,
            userId = id,
            username = username,
            displayName = fullName,
        )
    }

    private fun AuthCheckDto.toDomain(): AuthSession = AuthSession(
        isLoading = false,
        isAuthenticated = authenticated,
        userId = userId,
        username = user,
        displayName = user,
    )
}
