package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.domain.repository.ThemeRepository

class ObserveThemeUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    operator fun invoke(): Flow<AppThemeOption> = repository.observeTheme()
}
