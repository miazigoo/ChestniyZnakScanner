package ru.devandprod.chestniyznak.domain.usecase

import javax.inject.Inject
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.domain.repository.ThemeRepository

class SetThemeUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    suspend operator fun invoke(theme: AppThemeOption) {
        repository.setTheme(theme)
    }
}
