package ru.devandprod.chestniyznak.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.auth.AuthSession

interface AuthRepository {
    fun observeSession(): Flow<AuthSession>
    suspend fun restoreSession()
    suspend fun login(token: String)
    suspend fun logout()
}
