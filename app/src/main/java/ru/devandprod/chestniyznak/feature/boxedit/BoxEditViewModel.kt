package ru.devandprod.chestniyznak.feature.boxedit

import androidx.lifecycle.SavedStateHandle
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
import ru.devandprod.chestniyznak.app.navigation.AppDestination
import ru.devandprod.chestniyznak.core.audio.AudioFeedbackPlayer
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.usecase.ClearPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.DeleteEmptyPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.GetPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.RemovePackingBoxItemUseCase
import ru.devandprod.chestniyznak.domain.usecase.ScanCodeToPackingBoxUseCase

@HiltViewModel
class BoxEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val getPackingBoxUseCase: GetPackingBoxUseCase,
    private val scanCodeToPackingBoxUseCase: ScanCodeToPackingBoxUseCase,
    private val removePackingBoxItemUseCase: RemovePackingBoxItemUseCase,
    private val clearPackingBoxUseCase: ClearPackingBoxUseCase,
    private val deleteEmptyPackingBoxUseCase: DeleteEmptyPackingBoxUseCase,
) : ViewModel() {

    private val boxId = checkNotNull(savedStateHandle.get<Long>(AppDestination.BOX_ID_ARG))

    private val _uiState = MutableStateFlow(BoxEditUiState())
    val uiState: StateFlow<BoxEditUiState> = _uiState.asStateFlow()

    private val _boxDeleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val boxDeleted: SharedFlow<Unit> = _boxDeleted.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorText = null,
                    statusText = "Загружаем коробку...",
                )
            }
            runCatching { getPackingBoxUseCase(boxId) }
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            box = detail.toUi(),
                            title = "Редактирование #${detail.box.boxId}",
                            statusText = if (detail.items.isEmpty()) {
                                "Коробка пуста"
                            } else {
                                "Кодов в коробке: ${detail.items.size}"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
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

    fun onAddRequested() {
        _uiState.update {
            it.copy(
                isAwaitingScan = true,
                errorText = null,
                statusText = "Сканируйте код для добавления в коробку",
            )
        }
    }

    fun onCodeScanned(rawCode: String) {
        val box = _uiState.value.box ?: return
        if (_uiState.value.isBusy || !_uiState.value.isAwaitingScan) return

        _uiState.update {
            it.copy(
                isBusy = true,
                errorText = null,
                lastScannedCode = rawCode,
                statusText = "Добавляем код в коробку...",
            )
        }

        viewModelScope.launch {
            runCatching {
                scanCodeToPackingBoxUseCase(
                    boxId = box.boxId,
                    rawCode = rawCode,
                    scannerId = "android-hid",
                )
            }.onSuccess { result ->
                when {
                    result.reasonCode == "wrong_order" -> audioFeedbackPlayer.playOtherOrder()
                    result.ok -> audioFeedbackPlayer.playSuccess()
                    result.reasonCode == "code_in_other_box" -> audioFeedbackPlayer.playWarning()
                    result.reasonCode == "duplicate_in_box" -> audioFeedbackPlayer.playWarning()
                    else -> audioFeedbackPlayer.playError()
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAwaitingScan = false,
                        statusText = when {
                            result.ok -> "Код добавлен"
                            result.reasonCode == "wrong_order" && (result.error?.contains("не привязан", ignoreCase = true) == true) ->
                                "Код не привязан к заказу"
                            result.reasonCode == "wrong_order" -> "Другой заказ"
                            else -> result.error ?: result.verify?.message ?: "Код не добавлен"
                        },
                        errorText = if (result.ok) null else result.error ?: result.verify?.message,
                    )
                }
                refresh()
            }.onFailure { error ->
                audioFeedbackPlayer.playError()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAwaitingScan = false,
                        errorText = error.message ?: "Не удалось добавить код",
                        statusText = "Ошибка добавления кода",
                    )
                }
            }
        }
    }

    fun onItemLongPressed(itemId: Long) {
        _uiState.update { it.copy(itemMenuItemId = itemId) }
    }

    fun onDismissItemMenu() {
        _uiState.update { it.copy(itemMenuItemId = null) }
    }

    fun onRemoveItemRequested(itemId: Long) {
        if (_uiState.value.isBusy) return
        _uiState.update {
            it.copy(
                isBusy = true,
                itemMenuItemId = null,
                errorText = null,
                statusText = "Удаляем код из коробки...",
            )
        }
        viewModelScope.launch {
            runCatching { removePackingBoxItemUseCase(boxId, itemId) }
                .onSuccess { result ->
                    if (result.ok) {
                        audioFeedbackPlayer.playSuccess()
                    } else {
                        audioFeedbackPlayer.playError()
                    }
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusText = if (result.ok) "Код удален из коробки" else (result.error ?: "Код не удален"),
                            errorText = if (result.ok) null else result.error,
                        )
                    }
                    refresh()
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorText = error.message ?: "Не удалось удалить код",
                            statusText = "Ошибка удаления кода",
                        )
                    }
                }
        }
    }

    fun onClearActionRequested() {
        _uiState.update { it.copy(confirmClearDialog = true) }
    }

    fun onDismissClearDialog() {
        _uiState.update { it.copy(confirmClearDialog = false) }
    }

    fun onConfirmClearAction() {
        val box = _uiState.value.box ?: return
        if (_uiState.value.isBusy) return

        _uiState.update {
            it.copy(
                isBusy = true,
                confirmClearDialog = false,
                errorText = null,
                statusText = if (box.items.isEmpty()) {
                    "Удаляем пустую коробку..."
                } else {
                    "Удаляем все коды из коробки..."
                },
            )
        }

        viewModelScope.launch {
            val action = runCatching {
                if (box.items.isEmpty()) {
                    deleteEmptyPackingBoxUseCase(box.boxId)
                } else {
                    clearPackingBoxUseCase(box.boxId)
                }
            }
            action.onSuccess { result ->
                if (box.items.isEmpty() && result.ok) {
                    audioFeedbackPlayer.playSuccess()
                    _boxDeleted.tryEmit(Unit)
                } else {
                    if (result.ok) {
                        audioFeedbackPlayer.playSuccess()
                    } else {
                        audioFeedbackPlayer.playError()
                    }
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusText = if (result.ok) {
                                "Коробка очищена"
                            } else {
                                result.error ?: "Операция не выполнена"
                            },
                            errorText = if (result.ok) null else result.error,
                        )
                    }
                    refresh()
                }
            }.onFailure { error ->
                audioFeedbackPlayer.playError()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorText = error.message ?: "Операция не выполнена",
                        statusText = "Ошибка операции",
                    )
                }
            }
        }
    }

    private fun PackingBoxDetail.toUi(): EditableBoxUi = EditableBoxUi(
        boxId = box.boxId,
        orderName = box.orderName,
        sscc = box.sscc,
        filled = box.filled,
        capacity = box.capacity,
        items = items.map {
            EditableBoxItemUi(
                id = it.id,
                visibleCode = it.visibleCode,
                gtin = it.gtin,
                serial = it.serial,
            )
        },
    )
}
