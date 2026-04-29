package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.repository.AuthRepository

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() {
        repository.logout()
    }
}
