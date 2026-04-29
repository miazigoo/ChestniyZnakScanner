package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.auth.AuthSession
import ru.devandprod.chestniyznak.domain.repository.AuthRepository

class ObserveAuthSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthSession> = repository.observeSession()
}
