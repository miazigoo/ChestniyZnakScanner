package ru.devandprod.chestniyznak.feature.scanner

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
import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBox
import ru.devandprod.chestniyznak.domain.model.PackingScanResult
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.usecase.ClosePackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.EnsureSeedDataUseCase
import ru.devandprod.chestniyznak.domain.usecase.GetCurrentPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.ObserveCatalogStatsUseCase
import ru.devandprod.chestniyznak.domain.usecase.OpenPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.RefreshCatalogStatsUseCase
import ru.devandprod.chestniyznak.domain.usecase.ScanCodeToPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.VerifyCodeExistsUseCase

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val ensureSeedDataUseCase: EnsureSeedDataUseCase,
    observeCatalogStatsUseCase: ObserveCatalogStatsUseCase,
    private val refreshCatalogStatsUseCase: RefreshCatalogStatsUseCase,
    private val verifyCodeExistsUseCase: VerifyCodeExistsUseCase,
    private val getCurrentPackingBoxUseCase: GetCurrentPackingBoxUseCase,
    private val openPackingBoxUseCase: OpenPackingBoxUseCase,
    private val scanCodeToPackingBoxUseCase: ScanCodeToPackingBoxUseCase,
    private val closePackingBoxUseCase: ClosePackingBoxUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { ensureSeedDataUseCase() }
                .onSuccess {
                    runCatching { refreshCatalogStatsUseCase() }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            verify = state.verify.copy(
                                isScannerEnabled = state.verify.hasCameraPermission && state.scanMode == ScanMode.CameraVerify,
                            ),
                        )
                    }
                    loadCurrentBox()
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            packing = state.packing.copy(
                                errorText = error.message ?: "Не удалось инициализировать приложение",
                            ),
                            verify = state.verify.copy(
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = "Не удалось подготовить локальную базу кодов",
                                    tone = ScanResultTone.Error,
                                ),
                                technicalStatus = "INITIALIZATION_ERROR",
                                warnings = listOfNotNull(error.message),
                            ),
                        )
                    }
                }
        }

        viewModelScope.launch {
            observeCatalogStatsUseCase().collect { stats ->
                _uiState.update { state ->
                    state.copy(
                        statsLabel = "В базе ${stats.totalCodes} кодов",
                        scansLabel = "Проверок ${stats.totalScans}",
                    )
                }
            }
        }
    }

    fun onScanModeSelected(mode: ScanMode) {
        _uiState.update { state ->
            state.copy(
                scanMode = mode,
                verify = state.verify.copy(
                    isScannerEnabled = mode == ScanMode.CameraVerify && state.verify.hasCameraPermission && !state.verify.isProcessing && !state.isLoading,
                ),
            )
        }
        if (mode == ScanMode.PackingTsd) {
            loadCurrentBox()
        }
    }

    fun onCameraPermissionChanged(isGranted: Boolean) {
        _uiState.update { state ->
            state.copy(
                verify = state.verify.copy(
                    hasCameraPermission = isGranted,
                    isScannerEnabled = isGranted && state.scanMode == ScanMode.CameraVerify && !state.isLoading && !state.verify.isProcessing,
                ),
            )
        }
    }

    fun onCameraCodeScanned(rawCode: String) {
        val state = _uiState.value
        if (state.isLoading || state.scanMode != ScanMode.CameraVerify || state.verify.isProcessing) return

        _uiState.update {
            it.copy(
                verify = it.verify.copy(
                    isProcessing = true,
                    isScannerEnabled = false,
                ),
            )
        }

        viewModelScope.launch {
            val result = verifyCodeExistsUseCase(
                rawInput = rawCode,
                scannerId = "android-camera",
            )
            if (result.isSuccess) {
                audioFeedbackPlayer.playSuccess()
            } else {
                audioFeedbackPlayer.playError()
            }
            _uiState.update { current ->
                current.copy(
                    verify = current.verify.copy(
                        isProcessing = false,
                        isScannerEnabled = false,
                        resultCard = result.toVerifyCard(),
                        orderName = result.orderName?.takeIf(String::isNotBlank)
                            ?: result.code?.orderName?.takeIf(String::isNotBlank),
                        visibleCode = result.parsed?.visibleCode ?: rawCode,
                        technicalStatus = result.status.name,
                        warnings = result.warnings,
                    ),
                )
            }
        }
    }

    fun onHardwareCodeScanned(rawCode: String) {
        val state = _uiState.value
        val box = state.packing.currentBox ?: run {
            audioFeedbackPlayer.playError()
            _uiState.update {
                it.copy(
                    packing = it.packing.copy(
                        resultCard = ScanResultCardUi(
                            headline = "NO",
                            message = "Сначала откройте коробку",
                            tone = ScanResultTone.Error,
                        ),
                        statusText = "Открытая коробка не выбрана",
                        errorText = "Сначала откройте коробку",
                        lastScannedCode = rawCode,
                    ),
                )
            }
            return
        }
        if (state.isLoading || state.scanMode != ScanMode.PackingTsd || state.packing.isBusy) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    errorText = null,
                    lastScannedCode = rawCode,
                ),
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
                handlePackingScanResult(result)
                runCatching { refreshCatalogStatsUseCase() }
            }.onFailure { error ->
                audioFeedbackPlayer.playError()
                _uiState.update {
                    it.copy(
                        packing = it.packing.copy(
                            isBusy = false,
                            resultCard = ScanResultCardUi(
                                headline = "NO",
                                message = error.message ?: "Не удалось добавить код в коробку",
                                tone = ScanResultTone.Error,
                            ),
                            statusText = "Ошибка упаковки",
                            errorText = error.message,
                        ),
                    )
                }
            }
        }
    }

    fun onScanNextRequested() {
        _uiState.update { state ->
            state.copy(
                verify = state.verify.copy(
                    isScannerEnabled = state.scanMode == ScanMode.CameraVerify && state.verify.hasCameraPermission && !state.isLoading,
                    isProcessing = false,
                    resultCard = null,
                    orderName = null,
                    visibleCode = "",
                    technicalStatus = "",
                    warnings = emptyList(),
                ),
                packing = state.packing.copy(
                    resultCard = null,
                    errorText = null,
                    lastScannedCode = "",
                ),
            )
        }
    }

    fun onOpenBoxRequested() {
        val state = _uiState.value
        if (state.isLoading || state.packing.isBusy) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    errorText = null,
                    statusText = "Открываем коробку...",
                ),
            )
        }

        viewModelScope.launch {
            runCatching { openPackingBoxUseCase(deviceId = "M3SL20") }
                .onSuccess(::handleOpenBoxResult)
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            packing = it.packing.copy(
                                isBusy = false,
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = error.message ?: "Не удалось открыть коробку",
                                    tone = ScanResultTone.Error,
                                ),
                                statusText = "Коробка не открыта",
                                errorText = error.message,
                            ),
                        )
                    }
                }
        }
    }

    fun onActiveBoxSelected(boxId: Long) {
        _uiState.update { state ->
            val selected = state.packing.activeBoxesDialog?.boxes?.firstOrNull { it.boxId == boxId }
            if (selected == null) {
                state
            } else {
                state.copy(
                    packing = state.packing.copy(
                        activeBoxesDialog = null,
                        currentBox = selected,
                        isBusy = false,
                        statusText = "Продолжайте упаковку в коробку #${selected.boxId}",
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = "Открытая коробка выбрана",
                            tone = ScanResultTone.Success,
                        ),
                    ),
                )
            }
        }
    }

    fun onDismissActiveBoxesDialog() {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    activeBoxesDialog = null,
                    isBusy = false,
                ),
            )
        }
    }

    fun onCloseBoxRequested() {
        val boxId = _uiState.value.packing.currentBox?.boxId ?: return
        if (_uiState.value.packing.isBusy) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    errorText = null,
                    statusText = "Закрываем коробку и ждем печать этикетки...",
                ),
            )
        }

        viewModelScope.launch {
            runCatching { closePackingBoxUseCase(boxId) }
                .onSuccess(::handleCloseBoxResult)
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            packing = it.packing.copy(
                                isBusy = false,
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = error.message ?: "Не удалось закрыть коробку",
                                    tone = ScanResultTone.Error,
                                ),
                                statusText = "Коробка не закрыта",
                                errorText = error.message,
                            ),
                        )
                    }
                }
        }
    }

    fun onDismissCloseDialog() {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    closeDialog = null,
                ),
            )
        }
    }

    private fun loadCurrentBox() {
        viewModelScope.launch {
            runCatching { getCurrentPackingBoxUseCase() }
                .onSuccess { detail ->
                    _uiState.update { state ->
                        state.copy(
                            packing = state.packing.copy(
                                currentBox = detail?.box?.toUi(),
                                statusText = if (detail == null) {
                                    "Открытая коробка не найдена"
                                } else {
                                    "Текущая коробка #${detail.box.boxId}"
                                },
                                errorText = null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            packing = state.packing.copy(
                                currentBox = null,
                                statusText = "Не удалось получить текущую коробку",
                                errorText = error.message,
                            ),
                        )
                    }
                }
        }
    }

    private fun handleOpenBoxResult(result: OpenPackingBoxResult) {
        when {
            result.created -> audioFeedbackPlayer.playSuccess()
            result.hasActiveBoxes -> audioFeedbackPlayer.playWarning()
            else -> audioFeedbackPlayer.playSuccess()
        }
        _uiState.update { state ->
            when {
                result.created -> state.copy(
                    packing = state.packing.copy(
                        isBusy = false,
                        currentBox = result.box.toUi(),
                        statusText = "Коробка #${result.box.boxId} открыта",
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = "Новая коробка открыта",
                            tone = ScanResultTone.Success,
                        ),
                    ),
                )
                result.hasActiveBoxes -> state.copy(
                    packing = state.packing.copy(
                        isBusy = false,
                        currentBox = result.box.toUi(),
                        activeBoxesDialog = ActiveBoxesDialogUi(result.boxes.map { it.toUi() }),
                        statusText = "У вас уже есть открытая коробка",
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = "Найдена уже открытая коробка",
                            tone = ScanResultTone.Warning,
                        ),
                    ),
                )
                else -> state.copy(
                    packing = state.packing.copy(
                        isBusy = false,
                        currentBox = result.box.toUi(),
                        statusText = "Продолжайте работу с коробкой #${result.box.boxId}",
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = "Коробка уже была открыта",
                            tone = ScanResultTone.Success,
                        ),
                    ),
                )
            }
        }
    }

    private fun handlePackingScanResult(result: PackingScanResult) {
        when {
            result.reasonCode == "wrong_order" -> audioFeedbackPlayer.playOtherOrder()
            result.ok && result.duplicate == true -> audioFeedbackPlayer.playWarning()
            result.ok -> audioFeedbackPlayer.playSuccess()
            result.reasonCode == "code_in_other_box" -> audioFeedbackPlayer.playWarning()
            result.verify?.status == VerificationStatus.DUPLICATE_SCAN -> audioFeedbackPlayer.playWarning()
            else -> audioFeedbackPlayer.playError()
        }
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    isBusy = false,
                    currentBox = result.box.toUi(),
                    resultCard = result.toPackingCard(),
                    statusText = result.toPackingStatusText(),
                    errorText = result.error,
                ),
            )
        }
    }

    private fun handleCloseBoxResult(result: ClosePackingBoxResult) {
        when {
            result.ok && result.printOk == false -> audioFeedbackPlayer.playWarning()
            result.ok -> audioFeedbackPlayer.playSuccess()
            else -> audioFeedbackPlayer.playError()
        }
        val boxUi = result.box.toUi()
        val isFull = result.box.filled >= result.box.capacity
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    isBusy = false,
                    currentBox = if (result.ok) null else boxUi,
                    closeDialog = if (result.ok) {
                        CloseBoxDialogUi(
                            boxId = result.box.boxId,
                            sscc = result.box.sscc,
                            isFull = isFull,
                        )
                    } else {
                        null
                    },
                    resultCard = if (result.ok) {
                        ScanResultCardUi(
                            headline = "OK",
                            message = if (result.printOk == false && !result.printError.isNullOrBlank()) {
                                "Коробка закрыта, но печать завершилась с ошибкой"
                            } else {
                                "Коробка закрыта"
                            },
                            tone = if (result.printOk == false) ScanResultTone.Warning else ScanResultTone.Success,
                        )
                    } else {
                        ScanResultCardUi(
                            headline = "NO",
                            message = result.error ?: "Не удалось закрыть коробку",
                            tone = ScanResultTone.Error,
                        )
                    },
                    statusText = if (result.ok) {
                        "Коробка #${boxUi.boxId} закрыта"
                    } else {
                        "Коробка не закрыта"
                    },
                    errorText = result.error ?: result.printError,
                ),
            )
        }
    }

    private fun VerificationResult.toVerifyCard(): ScanResultCardUi = ScanResultCardUi(
        headline = if (isSuccess) "OK" else "NO",
        message = message,
        tone = if (isSuccess) ScanResultTone.Success else ScanResultTone.Error,
    )

    private fun PackingScanResult.toPackingCard(): ScanResultCardUi {
        return when {
            ok && duplicate == true -> ScanResultCardUi(
                headline = "OK",
                message = "Код уже есть в текущей коробке",
                tone = ScanResultTone.Warning,
            )
            ok -> ScanResultCardUi(
                headline = "OK",
                message = "Код добавлен в коробку",
                tone = if (boxFullSignal == true) ScanResultTone.Warning else ScanResultTone.Success,
            )
            reasonCode == "wrong_order" -> ScanResultCardUi(
                headline = "NO",
                message = error ?: "Код не подходит для этой коробки",
                tone = ScanResultTone.Warning,
            )
            reasonCode == "code_in_other_box" -> ScanResultCardUi(
                headline = "NO",
                message = error ?: "Код уже лежит в другой коробке",
                tone = ScanResultTone.Error,
            )
            verify?.status == VerificationStatus.DUPLICATE_SCAN -> ScanResultCardUi(
                headline = "NO",
                message = verify.message,
                tone = ScanResultTone.Warning,
            )
            else -> ScanResultCardUi(
                headline = "NO",
                message = error ?: verify?.message ?: "Код не добавлен в коробку",
                tone = ScanResultTone.Error,
            )
        }
    }

    private fun PackingScanResult.toPackingStatusText(): String = when {
        ok && boxFullSignal == true -> "Коробка заполнена"
        ok && duplicate == true -> "Код уже есть в коробке"
        reasonCode == "wrong_order" && error?.contains("не привязан", ignoreCase = true) == true -> "Код не привязан к заказу"
        reasonCode == "wrong_order" -> "Другой заказ"
        reasonCode == "code_in_other_box" -> "Код уже в другой коробке"
        ok -> "Код добавлен в коробку"
        else -> "Код не добавлен"
    }

    private fun PackingBox.toUi(): PackingBoxUi = PackingBoxUi(
        boxId = boxId,
        orderName = orderName,
        sscc = sscc,
        filled = filled,
        capacity = capacity,
        allowDuplicateScans = allowDuplicateScans,
        activeUserName = activeUserName,
        printOk = printOk,
        printError = printError,
    )
}
