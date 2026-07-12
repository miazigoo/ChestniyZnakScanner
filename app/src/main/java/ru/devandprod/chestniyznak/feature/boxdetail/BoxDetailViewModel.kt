package ru.devandprod.chestniyznak.feature.boxdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.app.navigation.AppDestination
import ru.devandprod.chestniyznak.core.audio.AudioFeedbackPlayer
import ru.devandprod.chestniyznak.core.device.DeviceIdentity
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.usecase.GetPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.OpenPackingBoxEditUseCase
import ru.devandprod.chestniyznak.domain.usecase.PrintPackingBoxLabelUseCase

@HiltViewModel
class BoxDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val strings: AppStringProvider,
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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorText = null,
                    statusText = strings.get(R.string.box_detail_loading),
                )
            }
            runCatching { getPackingBoxUseCase(boxId) }
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            box = detail.toUi(),
                            title = strings.get(R.string.box_detail_title_with_id, detail.box.boxId),
                            statusText = if (detail.box.isEditMode) {
                                strings.get(R.string.box_detail_edit_mode)
                            } else if (detail.box.isClosed) {
                                strings.get(R.string.box_detail_closed)
                            } else {
                                strings.get(R.string.box_detail_open)
                            },
                        )
                    }
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorText = error.message ?: strings.get(R.string.box_detail_load_failed),
                            statusText = strings.get(R.string.box_detail_load_error),
                        )
                    }
                }
        }
    }

    fun openEdit() {
        if (_uiState.value.isActionBusy) return
        val box = _uiState.value.box ?: return
        if (!box.isClosed) {
            _openEditEvents.tryEmit(boxId)
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionBusy = true,
                    errorText = null,
                    statusText = strings.get(R.string.box_detail_opening_edit),
                )
            }
            runCatching { openPackingBoxEditUseCase(boxId) }
                .onSuccess {
                    audioFeedbackPlayer.playSuccess()
                    _uiState.update { state ->
                        state.copy(
                            isActionBusy = false,
                            statusText = strings.get(R.string.box_detail_edit_opened),
                        )
                    }
                    _openEditEvents.tryEmit(boxId)
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            errorText = error.message ?: strings.get(R.string.box_detail_edit_failed),
                            statusText = strings.get(R.string.box_detail_edit_not_opened),
                        )
                    }
                }
        }
    }

    fun printLabel() {
        val box = _uiState.value.box ?: return
        if (_uiState.value.isActionBusy || !box.isClosed) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionBusy = true,
                    errorText = null,
                    statusText = strings.get(R.string.packing_printing_label),
                )
            }
            runCatching {
                printPackingBoxLabelUseCase(
                    boxId = box.boxId,
                    deviceId = DeviceIdentity.clientDeviceId,
                )
            }.onSuccess { result ->
                when (result.printStatus) {
                    "queued", "claimed", "prepared", "sending" -> {
                        audioFeedbackPlayer.playWarning()
                        _uiState.update {
                            it.copy(
                                isActionBusy = false,
                                statusText = strings.get(R.string.packing_box_closed_print_deferred),
                                errorText = null,
                            )
                        }
                    }
                    "sent_unconfirmed" -> {
                        audioFeedbackPlayer.playWarning()
                        _uiState.update {
                            it.copy(
                                isActionBusy = false,
                                statusText = strings.get(R.string.packing_box_closed_print_sent_unconfirmed),
                                errorText = null,
                            )
                        }
                    }
                    "result_unknown" -> {
                        audioFeedbackPlayer.playWarning()
                        _uiState.update {
                            it.copy(
                                isActionBusy = false,
                                statusText = strings.get(R.string.packing_box_closed_print_unknown),
                                errorText = result.printError.takeIf(String::isNotBlank),
                            )
                        }
                    }
                    "failed_before_send", "failed" -> {
                        audioFeedbackPlayer.playError()
                        val errorText = result.printError.ifBlank { strings.get(R.string.printer_print_failed) }
                        _uiState.update {
                            it.copy(
                                isActionBusy = false,
                                statusText = strings.get(R.string.packing_box_closed_print_failed),
                                errorText = errorText,
                            )
                        }
                    }
                    else -> {
                        if (result.printOk) {
                            audioFeedbackPlayer.playSuccess()
                            _uiState.update {
                                it.copy(
                                    isActionBusy = false,
                                    statusText = strings.get(
                                        R.string.close_box_printed,
                                        result.printer?.name?.takeIf(String::isNotBlank)
                                            ?: strings.get(R.string.common_unknown),
                                    ),
                                    errorText = null,
                                )
                            }
                        } else if (result.printErrorCode == "printer_not_selected") {
                            audioFeedbackPlayer.playWarning()
                            _uiState.update {
                                it.copy(
                                    isActionBusy = false,
                                    statusText = strings.get(R.string.printer_select_for_reprint),
                                    errorText = null,
                                )
                            }
                        } else {
                            audioFeedbackPlayer.playError()
                            val errorText = result.printError.ifBlank { strings.get(R.string.printer_print_failed) }
                            _uiState.update {
                                it.copy(
                                    isActionBusy = false,
                                    statusText = strings.get(R.string.packing_box_closed_print_failed),
                                    errorText = errorText,
                                )
                            }
                        }
                    }
                }
            }.onFailure { error ->
                audioFeedbackPlayer.playError()
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        statusText = strings.get(R.string.packing_box_closed_print_failed),
                        errorText = error.message ?: strings.get(R.string.printer_print_failed),
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
