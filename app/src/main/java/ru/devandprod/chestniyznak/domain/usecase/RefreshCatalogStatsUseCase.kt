package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class RefreshCatalogStatsUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    suspend operator fun invoke() {
        repository.refreshStats()
    }
}
