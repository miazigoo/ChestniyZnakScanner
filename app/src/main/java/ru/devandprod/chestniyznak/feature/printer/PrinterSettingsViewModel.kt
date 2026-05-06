package ru.devandprod.chestniyznak.feature.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.core.audio.AudioFeedbackPlayer
import ru.devandprod.chestniyznak.core.device.DeviceIdentity
import ru.devandprod.chestniyznak.domain.model.ClientPrinterSelection
import ru.devandprod.chestniyznak.domain.usecase.GetClientPrinterSelectionUseCase
import ru.devandprod.chestniyznak.domain.usecase.SetClientPrinterSelectionUseCase

@HiltViewModel
class PrinterSettingsViewModel @Inject constructor(
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val getClientPrinterSelectionUseCase: GetClientPrinterSelectionUseCase,
    private val setClientPrinterSelectionUseCase: SetClientPrinterSelectionUseCase,
) : ViewModel() {

    private val deviceId = DeviceIdentity.clientDeviceId

    private val _uiState = MutableStateFlow(
        PrinterSettingsUiState(
            deviceId = deviceId,
        ),
    )
    val uiState: StateFlow<PrinterSettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorText = null,
                    statusText = "Загружаем принтеры...",
                )
            }
            runCatching { getClientPrinterSelectionUseCase(deviceId) }
                .onSuccess { selection ->
                    _uiState.update { selection.toUiState(isLoading = false) }
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorText = error.message ?: "Не удалось загрузить принтеры",
                            statusText = "Список принтеров недоступен",
                        )
                    }
                }
        }
    }

    fun selectPrinter(printerId: Long) {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.selectedPrinterId == printerId) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorText = null,
                    statusText = "Сохраняем выбранный принтер...",
                )
            }
            runCatching { setClientPrinterSelectionUseCase(deviceId, printerId) }
                .onSuccess { selection ->
                    audioFeedbackPlayer.playSuccess()
                    _uiState.update {
                        selection.toUiState(
                            isLoading = false,
                            isSaving = false,
                            statusTextOverride = "Принтер сохранен",
                        )
                    }
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorText = error.message ?: "Не удалось сохранить выбор принтера",
                            statusText = "Принтер не сохранен",
                        )
                    }
                }
        }
    }

    private fun ClientPrinterSelection.toUiState(
        isLoading: Boolean,
        isSaving: Boolean = false,
        statusTextOverride: String? = null,
    ): PrinterSettingsUiState = PrinterSettingsUiState(
        isLoading = isLoading,
        isSaving = isSaving,
        deviceId = deviceId,
        selectedPrinterId = selectedPrinterId,
        selectedPrinterLabel = selectedPrinter?.name ?: "Не выбран",
        statusText = statusTextOverride ?: if (selectedPrinter != null) {
            "Выбран принтер: ${selectedPrinter.name}"
        } else {
            "Принтер для ТСД еще не выбран"
        },
        errorText = null,
        printers = printers.map { printer ->
            PrinterItemUi(
                id = printer.id,
                name = printer.name,
                ipAddress = printer.ipAddress,
                section = printer.section,
                isSelected = printer.id == selectedPrinterId,
            )
        },
    )
}
