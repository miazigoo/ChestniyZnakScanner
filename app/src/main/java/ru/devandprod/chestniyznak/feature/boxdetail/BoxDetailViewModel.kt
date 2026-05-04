package ru.devandprod.chestniyznak.feature.boxdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.app.navigation.AppDestination
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.usecase.GetPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.OpenPackingBoxEditUseCase
import ru.devandprod.chestniyznak.domain.usecase.PrintPackingBoxLabelUseCase

@HiltViewModel
class BoxDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPackingBoxUseCase: GetPackingBoxUseCase,
    private val openPackingBoxEditUseCase: OpenPackingBoxEditUseCase,
    private val printPackingBoxLabelUseCase: PrintPackingBoxLabelUseCase,
) : ViewModel() {

    private val boxId = checkNotNull(savedStateHandle.get<Long>(AppDestination.BOX_ID_ARG))

    private val _uiState = MutableStateFlow(BoxDetailUiState())
    val uiState: StateFlow<BoxDetailUiState> = _uiState.asStateFlow()
    private val _openEditEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val openEditEvents: SharedFlow<Long> = _openEditEvents.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorText = null, statusText = "Загружаем коробку...") }
            runCatching { getPackingBoxUseCase(boxId) }
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            box = detail.toUi(),
                            title = "Коробка #${detail.box.boxId}",
                            statusText = if (detail.box.isEditMode) {
                                "Коробка открыта в режиме редактирования"
                            } else if (detail.box.isClosed) {
                                "Коробка закрыта"
                            } else {
                                "Коробка открыта"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorText = error.message ?: "Не удалось загрузить коробку",
                            statusText = "Ошибка загрузки коробки",
                        )
                    }
                }
        }
    }

    fun openEdit() {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, errorText = null, statusText = "Открываем редактирование...") }
            runCatching { openPackingBoxEditUseCase(boxId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isActionBusy = false,
                            statusText = "Режим редактирования открыт",
                        )
                    }
                    _openEditEvents.tryEmit(boxId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            errorText = error.message ?: "Не удалось открыть редактирование",
                            statusText = "Редактирование не открыто",
                        )
                    }
                }
        }
    }

    fun printLabel() {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, errorText = null, statusText = "Проверяем принтер и отправляем на печать...") }
            runCatching { printPackingBoxLabelUseCase(boxId) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            statusText = if (result.ok) "Этикетка отправлена на печать" else "Печать завершилась с ошибкой",
                            errorText = result.error ?: result.printError,
                        )
                    }
                    refresh()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            errorText = error.message ?: "Не удалось распечатать этикетку",
                            statusText = "Ошибка печати",
                        )
                    }
                }
        }
    }

    private fun PackingBoxDetail.toUi(): BoxDetailUi = BoxDetailUi(
        boxId = box.boxId,
        orderName = box.orderName,
        sscc = box.sscc,
        filled = box.filled,
        capacity = box.capacity,
        isClosed = box.isClosed,
        isEditMode = box.isEditMode,
        activeUserName = box.activeUserName,
        printOk = box.printOk,
        printError = box.printError,
        items = items.map {
            BoxDetailItemUi(
                id = it.id,
                visibleCode = it.visibleCode,
                gtin = it.gtin,
                serial = it.serial,
            )
        },
    )
}
