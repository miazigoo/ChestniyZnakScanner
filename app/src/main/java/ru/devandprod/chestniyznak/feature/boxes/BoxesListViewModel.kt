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
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.app.navigation.AppDestination
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.model.PackingBox
import ru.devandprod.chestniyznak.domain.usecase.ListPackingBoxesUseCase

@HiltViewModel
class BoxesListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val strings: AppStringProvider,
    private val listPackingBoxesUseCase: ListPackingBoxesUseCase,
) : ViewModel() {

    private val filter = savedStateHandle.get<String>(AppDestination.FILTER_ARG).orEmpty().ifBlank { "all" }

    private val _uiState = MutableStateFlow(
        BoxesListUiState(
            title = if (filter == "empty") {
                strings.get(R.string.boxes_title_empty)
            } else {
                strings.get(R.string.boxes_title_all)
            },
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
                        totalLabel = strings.get(R.string.boxes_found_count, page.total),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorText = error.message ?: strings.get(R.string.boxes_load_failed),
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
        isEditMode = isEditMode,
        activeUserName = activeUserName,
    )
}
