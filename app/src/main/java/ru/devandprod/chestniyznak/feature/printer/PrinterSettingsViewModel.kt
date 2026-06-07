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
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.model.ClientPrinterSelection
import ru.devandprod.chestniyznak.domain.usecase.GetClientPrinterSelectionUseCase
import ru.devandprod.chestniyznak.domain.usecase.SetClientPrinterSelectionUseCase
import ru.devandprod.chestniyznak.R

@HiltViewModel
class PrinterSettingsViewModel @Inject constructor(
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val strings: AppStringProvider,
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
                    statusText = strings.get(R.string.printer_loading),
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
                            errorText = error.message ?: strings.get(R.string.printer_load_failed),
                            statusText = strings.get(R.string.printer_list_unavailable),
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
                    statusText = strings.get(R.string.printer_saving),
                )
            }
            runCatching { setClientPrinterSelectionUseCase(deviceId, printerId) }
                .onSuccess { selection ->
                    audioFeedbackPlayer.playSuccess()
                    _uiState.update {
                        selection.toUiState(
                            isLoading = false,
                            isSaving = false,
                            statusTextOverride = strings.get(R.string.printer_saved),
                        )
                    }
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorText = error.message ?: strings.get(R.string.printer_save_failed),
                            statusText = strings.get(R.string.printer_not_saved),
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
        selectedPrinterLabel = selectedPrinter?.name ?: strings.get(R.string.printer_not_selected),
        statusText = statusTextOverride ?: if (selectedPrinter != null) {
            strings.get(R.string.printer_selected_status, selectedPrinter.name)
        } else {
            strings.get(R.string.printer_personal_not_selected)
        },
        errorText = null,
        printers = printers.map { printer ->
            PrinterItemUi(
                id = printer.id,
                name = printer.name,
                ipAddress = printer.ipAddress,
                port = printer.port,
                section = printer.section,
                isSelected = printer.id == selectedPrinterId,
            )
        },
    )
}
