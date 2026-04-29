package ru.devandprod.chestniyznak.feature.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.domain.usecase.ObserveThemeUseCase
import ru.devandprod.chestniyznak.domain.usecase.SetThemeUseCase

@HiltViewModel
class ThemeSelectionViewModel @Inject constructor(
    observeThemeUseCase: ObserveThemeUseCase,
    private val setThemeUseCase: SetThemeUseCase,
) : ViewModel() {
    val uiState: StateFlow<ThemeSelectionUiState> = observeThemeUseCase()
        .map { selectedTheme ->
            ThemeSelectionUiState(selectedTheme = selectedTheme)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeSelectionUiState(),
        )

    fun onThemeSelected(theme: AppThemeOption) {
        viewModelScope.launch {
            setThemeUseCase(theme)
        }
    }
}
