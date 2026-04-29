package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.model.CatalogStats
import ru.devandprod.chestniyznak.domain.repository.ChestniyZnakRepository

class ObserveCatalogStatsUseCase @Inject constructor(
    private val repository: ChestniyZnakRepository,
) {
    operator fun invoke(): Flow<CatalogStats> = repository.observeStats()
}
