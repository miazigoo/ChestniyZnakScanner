package ru.devandprod.chestniyznak.feature.boxes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.app.navigation.AppDestination
import ru.devandprod.chestniyznak.domain.model.PackingBox
import ru.devandprod.chestniyznak.domain.usecase.ListPackingBoxesUseCase

@HiltViewModel
class BoxesListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listPackingBoxesUseCase: ListPackingBoxesUseCase,
) : ViewModel() {

    private val filter = savedStateHandle.get<String>(AppDestination.FILTER_ARG).orEmpty().ifBlank { "all" }

    private val _uiState = MutableStateFlow(
        BoxesListUiState(
            title = if (filter == "empty") "Пустые коробки" else "Список коробок",
            filter = filter,
        ),
    )
    val uiState: StateFlow<BoxesListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorText = null,
                )
            }
            runCatching {
                listPackingBoxesUseCase(status = filter)
            }.onSuccess { page ->
                _uiState.update {
                    it.copy(
                    isLoading = false,
                        boxes = page.items.map { it.toUi() },
                        totalLabel = "Найдено ${page.total}",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorText = error.message ?: "Не удалось получить список коробок",
                    )
                }
            }
        }
    }

    private fun PackingBox.toUi(): BoxListItemUi = BoxListItemUi(
        boxId = boxId,
        orderName = orderName,
        sscc = sscc,
        filled = filled,
        capacity = capacity,
        isClosed = isClosed,
        activeUserName = activeUserName,
    )
}
