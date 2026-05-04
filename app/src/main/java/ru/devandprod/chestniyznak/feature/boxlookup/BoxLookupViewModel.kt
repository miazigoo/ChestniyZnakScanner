package ru.devandprod.chestniyznak.feature.boxlookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.domain.usecase.ListPackingBoxesUseCase

@HiltViewModel
class BoxLookupViewModel @Inject constructor(
    private val listPackingBoxesUseCase: ListPackingBoxesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoxLookupUiState())
    val uiState: StateFlow<BoxLookupUiState> = _uiState.asStateFlow()

    private val _openBoxEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val openBoxEvents: SharedFlow<Long> = _openBoxEvents.asSharedFlow()

    fun onCodeScanned(rawCode: String) {
        if (_uiState.value.isBusy) return

        val normalized = rawCode.trim()
        if (normalized.isBlank()) return

        _uiState.update {
            it.copy(
                isBusy = true,
                errorText = null,
                lastScannedCode = normalized,
                statusText = "Ищем коробку...",
            )
        }

        viewModelScope.launch {
            runCatching {
                listPackingBoxesUseCase(query = normalized, limit = 10)
            }.onSuccess { page ->
                val exact = page.items.firstOrNull {
                    it.sscc == normalized || it.boxId.toString() == normalized
                } ?: page.items.firstOrNull()

                if (exact == null) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorText = "Коробка не найдена",
                            statusText = "Коробка не найдена",
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusText = "Коробка #${exact.boxId} найдена",
                        )
                    }
                    _openBoxEvents.tryEmit(exact.boxId)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorText = error.message ?: "Не удалось найти коробку",
                        statusText = "Ошибка поиска коробки",
                    )
                }
            }
        }
    }

    fun onResetStatus() {
        _uiState.update {
            it.copy(
                errorText = null,
                lastScannedCode = "",
                statusText = "Сканируйте штрихкод коробки",
            )
        }
    }
}
