package ru.devandprod.chestniyznak.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.domain.usecase.ObserveThemeUseCase

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    observeThemeUseCase: ObserveThemeUseCase,
) : ViewModel() {
    val selectedTheme: StateFlow<AppThemeOption> = observeThemeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppThemeOption.Workbench,
        )
}
